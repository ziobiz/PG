package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pg.entity.OrgUnit;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.qxipay.QxipayAmountUtil;
import com.pg.integration.pg.qxipay.QxipayApiClient;
import com.pg.integration.pg.qxipay.QxipayCredentials;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * QXIPAY — 카드 토큰화 + charge 생성(3DS nextAction) + 환불.
 * 웹훅 입구는 NOTI {@code /noti/qxipay}. 가맹 MID는 PG로 보내지 않습니다.
 */
@Service
public class QxipayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(QxipayPaymentService.class);

    public static final String NOTI_WEBHOOK_PATH = "/noti/qxipay";
    public static final String NOTI_RESULT_PATH = "/noti/result/qxipay";

    private final QxipayApiClient apiClient;
    private final PgAgencyRepository pgAgencyRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ChillPayService chillPayService;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final QxipaySaleRecordService qxipaySaleRecordService;

    @Value("${app.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    @Value("${app.noti.public-base:https://noti.icopay.net}")
    private String notiPublicBase;

    public QxipayPaymentService(QxipayApiClient apiClient,
                                PgAgencyRepository pgAgencyRepository,
                                OrgUnitRepository orgUnitRepository,
                                ChillPayService chillPayService,
                                UrlPayChargeResolutionService urlPayChargeResolutionService,
                                QxipaySaleRecordService qxipaySaleRecordService) {
        this.apiClient = apiClient;
        this.pgAgencyRepository = pgAgencyRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.chillPayService = chillPayService;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.qxipaySaleRecordService = qxipaySaleRecordService;
    }

    public Map<String, String> publicNotiIngressUrls() {
        String base = notiPublicBase != null ? notiPublicBase.trim().replaceAll("/+$", "") : "https://noti.icopay.net";
        Map<String, String> m = new LinkedHashMap<>();
        m.put("qxipayWebhookUrl", base + NOTI_WEBHOOK_PATH);
        m.put("qxipayResultUrl", base + NOTI_RESULT_PATH);
        m.put("pgVendor", PgVendor.QXIPAY);
        return m;
    }

    public String buildIcopayIngressUrl(String ingressToken) {
        return PgNotifyIngressPaths.buildIngressBase(publicBaseUrl, ingressToken) + "/" + PgVendor.QXIPAY;
    }

    public Optional<QxipayCredentials> resolveCredentials(Long orgUnitId) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (opPg == null || !PgVendor.isQxipayFamily(opPg)) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByPgCd(opPg).map(QxipayCredentials::fromAgency);
    }

    /**
     * URL/인라인 승인 — 카드 → token → charge. 3DS면 nextAction.redirect.url 반환.
     */
    public Map<String, Object> executeSale(Long orgUnitId, Map<String, Object> body,
                                           HttpServletRequest request, String clientIp) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        Optional<QxipayCredentials> credOpt = resolveCredentials(orgUnitId);
        if (credOpt.isEmpty() || !credOpt.get().isUsable()) {
            return fail(out, "QXIPAY_CREDENTIALS_MISSING", "QXIPAY API 키가 없습니다. 결제대행사 설정에서 등록하세요.");
        }
        QxipayCredentials creds = credOpt.get();
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);

        UrlPayChargeResolutionService.ResolvedCharge charge;
        try {
            charge = urlPayChargeResolutionService.resolve(orgUnitId, body != null ? body : Map.of(), opPg);
        } catch (IllegalArgumentException e) {
            return fail(out, "INVALID_AMOUNT", e.getMessage());
        }
        String orderNo = str(body, "orderNo", "order_no", "reference");
        if (orderNo.isBlank()) {
            return fail(out, "INVALID_ORDER_NO", "orderNo가 필요합니다.");
        }
        Map<String, Object> card = cardMap(body);
        if (str(card.get("number")).isBlank()) {
            return fail(out, "CARD_REQUIRED", "카드 정보가 필요합니다.");
        }

        Map<String, Object> tok = apiClient.createCardToken(creds, card);
        if (!Boolean.TRUE.equals(tok.get("ok"))) {
            return fail(out, str(tok.get("errorCode"), "QXIPAY_TOKEN"),
                    str(tok.get("message"), "카드 토큰 생성에 실패했습니다."));
        }
        String tokenId = str(tok.get("id"));
        JsonNode tokJson = (JsonNode) tok.get("json");
        if (tokenId.isBlank() && tokJson != null && tokJson.has("id")) {
            tokenId = tokJson.get("id").asText("");
        }
        if (tokenId.isBlank()) {
            return fail(out, "QXIPAY_TOKEN", "토큰 ID가 없습니다.");
        }

        String returnUrl = str(body, "returnUrl", "return_url");
        if (returnUrl.isBlank()) {
            String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "https://api.icopay.co.kr";
            returnUrl = base + "/checkout/" + ouOpt.get().getCode()
                    + "?orderNo=" + java.net.URLEncoder.encode(orderNo, java.nio.charset.StandardCharsets.UTF_8)
                    + "&qxipay=1";
        }

        long minor = QxipayAmountUtil.toMinorUnits(charge.pgAmount());
        String settleCcy = charge.settlementCurrency() != null && !charge.settlementCurrency().isBlank()
                ? charge.settlementCurrency().trim().toUpperCase(java.util.Locale.ROOT)
                : "THB";
        Map<String, Object> payMethod = new LinkedHashMap<>();
        payMethod.put("type", "card");
        Map<String, Object> cardPm = new LinkedHashMap<>();
        cardPm.put("token", tokenId);
        payMethod.put("card", cardPm);

        Map<String, Object> chargeReq = new LinkedHashMap<>();
        chargeReq.put("amount", minor);
        chargeReq.put("currency", settleCcy);
        chargeReq.put("reference", orderNo.length() > 64 ? orderNo.substring(0, 64) : orderNo);
        chargeReq.put("paymentMethod", payMethod);
        chargeReq.put("returnUrl", returnUrl);
        chargeReq.put("device", "web");
        chargeReq.put("threeDS", Map.of("mode", "required"));

        String idem = "qx-" + ouOpt.get().getCode() + "-" + orderNo + "-" + UUID.randomUUID();
        String sandboxOutcome = str(body, "sandboxOutcome", "GW-Sandbox-Outcome");

        qxipaySaleRecordService.recordOrTouchPending(
                orgUnitId, orderNo, charge.pgAmount(), charge.settlementCurrency(),
                str(body, "productName", "item"), null, str(body, "txnOrigin", "origin"),
                charge.shopperDisplayAmount(), charge.shopperDisplayCurrency());

        Map<String, Object> chg = apiClient.createCharge(creds, chargeReq, idem,
                sandboxOutcome.isBlank() ? null : sandboxOutcome);
        if (!Boolean.TRUE.equals(chg.get("ok"))) {
            return fail(out, str(chg.get("errorCode"), "QXIPAY_CHARGE"),
                    str(chg.get("message"), "결제 생성에 실패했습니다."));
        }
        JsonNode chgJson = (JsonNode) chg.get("json");
        String chargeId = str(chg.get("id"));
        String status = str(chg.get("status"));
        if (chgJson != null) {
            if (chargeId.isBlank() && chgJson.has("id")) {
                chargeId = chgJson.get("id").asText("");
            }
            if (status.isBlank() && chgJson.has("status")) {
                status = chgJson.get("status").asText("");
            }
        }

        qxipaySaleRecordService.recordOrTouchPending(
                orgUnitId, orderNo, charge.pgAmount(), charge.settlementCurrency(),
                str(body, "productName", "item"), chargeId, str(body, "txnOrigin", "origin"),
                charge.shopperDisplayAmount(), charge.shopperDisplayCurrency());

        if ("succeeded".equalsIgnoreCase(status)) {
            qxipaySaleRecordService.applyChargeOutcome(orderNo, ouOpt.get().getCode(), chargeId, "succeeded", chargeId);
        } else if ("failed".equalsIgnoreCase(status) || "expired".equalsIgnoreCase(status)) {
            qxipaySaleRecordService.applyChargeOutcome(orderNo, ouOpt.get().getCode(), chargeId, status, null);
        }

        out.put("success", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("chargeId", chargeId);
        data.put("status", status);
        data.put("orderNo", orderNo);
        data.put("amount", charge.pgAmount());
        data.put("currency", charge.settlementCurrency());
        data.put("pgVendor", "ICOPAY");
        if (chgJson != null && chgJson.has("nextAction") && !chgJson.get("nextAction").isNull()) {
            JsonNode na = chgJson.get("nextAction");
            String type = na.has("type") ? na.get("type").asText("") : "";
            data.put("nextActionType", type);
            if ("redirect".equalsIgnoreCase(type) && na.has("redirect") && na.get("redirect").has("url")) {
                data.put("redirectUrl", na.get("redirect").get("url").asText(""));
                data.put("paymentStatus", "PENDING_3DS");
            } else if ("display_qr".equalsIgnoreCase(type)) {
                data.put("paymentStatus", "PENDING_QR");
                if (na.has("qr")) {
                    data.put("qr", objectMap(na.get("qr")));
                }
            } else {
                data.put("paymentStatus", mapStatus(status));
            }
        } else {
            data.put("paymentStatus", mapStatus(status));
        }
        data.put("noti", publicNotiIngressUrls());
        out.put("data", data);
        log.info("QXIPAY charge created mid={} order={} chargeId={} status={} ip={}",
                ouOpt.get().getCode(), orderNo, chargeId, status, clientIp);
        return out;
    }

    public Map<String, Object> refund(Long orgUnitId, String chargeId, BigDecimal amountMajor, String reason) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<QxipayCredentials> credOpt = resolveCredentials(orgUnitId);
        if (credOpt.isEmpty() || !credOpt.get().isUsable()) {
            return fail(out, "QXIPAY_CREDENTIALS_MISSING", "QXIPAY API 키가 없습니다.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (amountMajor != null && amountMajor.compareTo(BigDecimal.ZERO) > 0) {
            body.put("amount", QxipayAmountUtil.toMinorUnits(amountMajor));
        }
        if (reason != null && !reason.isBlank()) {
            body.put("reason", reason.trim());
        }
        Map<String, Object> resp = apiClient.createRefund(credOpt.get(), chargeId, body,
                "rf-" + chargeId + "-" + UUID.randomUUID());
        out.put("success", Boolean.TRUE.equals(resp.get("ok")));
        out.put("data", resp);
        if (!Boolean.TRUE.equals(resp.get("ok"))) {
            out.put("errorCode", resp.get("errorCode"));
            out.put("message", resp.get("message"));
        }
        return out;
    }

    private static String mapStatus(String status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status.trim().toLowerCase()) {
            case "succeeded" -> "PAID";
            case "pending" -> "PENDING";
            case "failed", "expired" -> "FAILED";
            default -> status.trim().toUpperCase();
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cardMap(Map<String, Object> body) {
        if (body == null) {
            return Map.of();
        }
        Object c = body.get("card");
        if (c instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("number", body.get("cardNumber") != null ? body.get("cardNumber") : body.get("number"));
        out.put("name", body.get("cardHolder") != null ? body.get("cardHolder") : body.get("name"));
        out.put("expMonth", body.get("expMonth") != null ? body.get("expMonth") : body.get("expiryMonth"));
        out.put("expYear", body.get("expYear") != null ? body.get("expYear") : body.get("expiryYear"));
        out.put("cvv", body.get("cvv") != null ? body.get("cvv") : body.get("cvc"));
        return out;
    }

    private static Map<String, Object> objectMap(JsonNode n) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (n == null || !n.isObject()) {
            return m;
        }
        n.fields().forEachRemaining(e -> {
            if (e.getValue() != null && e.getValue().isValueNode()) {
                m.put(e.getKey(), e.getValue().asText(""));
            }
        });
        return m;
    }

    private static Map<String, Object> fail(Map<String, Object> out, String code, String message) {
        out.put("success", false);
        out.put("errorCode", code);
        out.put("message", message);
        return out;
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String str(Object a, Object b) {
        String s = str(a);
        return s.isBlank() ? str(b) : s;
    }

    private static String str(Map<String, Object> body, String... keys) {
        if (body == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            if (k != null && body.get(k) != null) {
                String s = String.valueOf(body.get(k)).trim();
                if (!s.isBlank() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return "";
    }
}
