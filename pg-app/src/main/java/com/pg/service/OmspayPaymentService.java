package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.omspay.OmspayApiClient;
import com.pg.integration.pg.omspay.OmspayCredentials;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OMSPAY — 주문 생성 + card-payment(HPP URL) + status 조회.
 * 가맹 MID는 OMS로 보내지 않음(note = ICOPAY 주문번호만).
 */
@Service
public class OmspayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(OmspayPaymentService.class);

    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final ChillPayService chillPayService;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final OmspayApiClient apiClient;
    private final OmspaySaleRecordService omspaySaleRecordService;
    private final HqNotifyEnvService hqNotifyEnvService;

    @Value("${pg.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    public OmspayPaymentService(OrgUnitRepository orgUnitRepository,
                                PgAgencyRepository pgAgencyRepository,
                                ChillPayService chillPayService,
                                UrlPayChargeResolutionService urlPayChargeResolutionService,
                                OmspayApiClient apiClient,
                                OmspaySaleRecordService omspaySaleRecordService,
                                HqNotifyEnvService hqNotifyEnvService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.chillPayService = chillPayService;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.apiClient = apiClient;
        this.omspaySaleRecordService = omspaySaleRecordService;
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    public Map<String, String> publicNotiIngressUrls() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("pgVendor", PgVendor.OMSPAY);
        m.put("notiWebhookPath", "/noti/omspay");
        String ingressToken = hqNotifyEnvService.getOrCreate().getIngressToken();
        m.put("icopayIngressBase",
                PgNotifyIngressPaths.buildIngressBase(publicBaseUrl, ingressToken) + "/" + PgVendor.OMSPAY);
        return m;
    }

    public Optional<OmspayCredentials> resolveCredentials(Long orgUnitId) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (opPg == null || !PgVendor.isOmspayFamily(opPg)) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByPgCd(opPg).map(OmspayCredentials::fromAgency);
    }

    /**
     * Create OMS order + card-payment page. Returns paymentUrl for browser redirect.
     */
    public Map<String, Object> startRedirect(Long orgUnitId, Map<String, Object> body,
                                             HttpServletRequest request, String clientIp) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        Optional<OmspayCredentials> credOpt = resolveCredentials(orgUnitId);
        if (credOpt.isEmpty() || !credOpt.get().canCreateOrder()) {
            return fail(out, "OMSPAY_CREDENTIALS_MISSING",
                    "OMSPAY API 키 또는 customerId가 없습니다. 결제대행사 설정에서 등록하세요.");
        }
        OmspayCredentials creds = credOpt.get();
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

        apiClient.ensureProduct(creds);

        /* note = ICOPAY order only — never merchant MID */
        Map<String, Object> ord = apiClient.createOrder(creds, charge.pgAmount(), orderNo);
        if (!Boolean.TRUE.equals(ord.get("ok"))) {
            return fail(out, str(ord.get("errorCode"), "OMSPAY_ORDER"),
                    str(ord.get("message"), "OMS 주문 생성에 실패했습니다."));
        }
        String omsCode = str(ord.get("code"), "");
        JsonNode ordJson = (JsonNode) ord.get("json");
        if (omsCode.isBlank() && ordJson != null && ordJson.has("code")) {
            omsCode = ordJson.get("code").asText("");
        }
        if (omsCode.isBlank()) {
            return fail(out, "OMSPAY_ORDER", "OMS 주문코드가 없습니다.");
        }

        String returnUrl = str(body, "returnUrl", "return_url");
        if (returnUrl.isBlank()) {
            String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "https://api.icopay.co.kr";
            returnUrl = base + "/checkout/" + ouOpt.get().getCode()
                    + "?orderNo=" + java.net.URLEncoder.encode(orderNo, java.nio.charset.StandardCharsets.UTF_8)
                    + "&omspay=1&done=1";
        }

        String buyerName = str(body, "customerNm", "buyerName", "buyer", "payerName", "name");
        String buyerEmail = str(body, "payEmailAddress", "buyerEmail", "email", "customerEmail");
        String buyerTel = str(body, "payTelephone", "customerTel", "buyerTel", "phone", "tel");
        omspaySaleRecordService.recordOrTouchPending(
                orgUnitId, orderNo, charge.pgAmount(), charge.settlementCurrency(),
                str(body, "productName", "item"), omsCode, str(body, "txnOrigin", "origin"),
                charge.shopperDisplayAmount(), charge.shopperDisplayCurrency(),
                buyerName, buyerEmail, buyerTel);

        Map<String, Object> pay = apiClient.createCardPayment(creds, omsCode, returnUrl);
        if (!Boolean.TRUE.equals(pay.get("ok"))) {
            return fail(out, str(pay.get("errorCode"), "OMSPAY_CARD_PAYMENT"),
                    str(pay.get("message"), "결제 페이지 생성에 실패했습니다."));
        }
        String paymentUrl = str(pay.get("paymentUrl"), "");
        JsonNode payJson = (JsonNode) pay.get("json");
        if (paymentUrl.isBlank() && payJson != null && payJson.has("payment_url")) {
            paymentUrl = payJson.get("payment_url").asText("");
        }
        if (paymentUrl.isBlank()) {
            return fail(out, "OMSPAY_CARD_PAYMENT", "payment_url이 없습니다.");
        }

        out.put("success", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("omsOrderCode", omsCode);
        data.put("paymentUrl", paymentUrl);
        data.put("redirectUrl", paymentUrl);
        data.put("paymentStatus", "PENDING_REDIRECT");
        data.put("amount", charge.pgAmount());
        data.put("currency", charge.settlementCurrency());
        data.put("pgVendor", "ICOPAY");
        data.put("noti", publicNotiIngressUrls());
        out.put("data", data);
        log.info("OMSPAY redirect started mid={} order={} oms={} ip={}",
                ouOpt.get().getCode(), orderNo, omsCode, clientIp);
        return out;
    }

    public Map<String, Object> pollStatus(Long orgUnitId, String omsOrderCode) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<OmspayCredentials> credOpt = resolveCredentials(orgUnitId);
        if (credOpt.isEmpty() || !credOpt.get().isUsable()) {
            return fail(out, "OMSPAY_CREDENTIALS_MISSING", "OMSPAY API 키가 없습니다.");
        }
        Map<String, Object> st = apiClient.getOrderStatus(credOpt.get(), omsOrderCode);
        if (!Boolean.TRUE.equals(st.get("ok"))) {
            return fail(out, str(st.get("errorCode"), "OMSPAY_STATUS"),
                    str(st.get("message"), "상태 조회 실패"));
        }
        out.put("success", true);
        out.put("data", st);
        return out;
    }

    private static Map<String, Object> fail(Map<String, Object> out, String code, String message) {
        out.put("success", false);
        out.put("errorCode", code);
        out.put("message", message);
        return out;
    }

    private static String str(Map<String, Object> body, String... keys) {
        if (body == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            Object v = body.get(k);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isBlank()) {
                    return s;
                }
            }
        }
        return "";
    }

    private static String str(Object v, String fallback) {
        if (v == null) {
            return fallback != null ? fallback : "";
        }
        String s = String.valueOf(v).trim();
        return s.isBlank() ? (fallback != null ? fallback : "") : s;
    }
}
