package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.payso.PaysoApiClient;
import com.pg.integration.pg.payso.PaysoCredentials;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.noti.NotiProvisionClient;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Payso — ui-payments HPP 리다이렉트.
 * 가맹 MID·compId는 Payso로 보내지 않음(ICOPAY 집계 MID만). referenceNo ≤12.
 * Return URL 도 {@link HostedPayReturnTokenService} 불투명 {@code rt} 만 사용(평문 m·orderNo 금지).
 */
@Service
public class PaysoPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaysoPaymentService.class);
    public static final String NOTI_RESULT_PATH = "/noti/result/payso";
    public static final String NOTI_WEBHOOK_PATH = "/noti/payso";

    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final ChillPayService chillPayService;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final PaysoApiClient apiClient;
    private final PaysoSaleRecordService paysoSaleRecordService;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final HostedPayReturnTokenService hostedPayReturnTokenService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;

    @Value("${pg.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    public PaysoPaymentService(OrgUnitRepository orgUnitRepository,
                               PgAgencyRepository pgAgencyRepository,
                               ChillPayService chillPayService,
                               UrlPayChargeResolutionService urlPayChargeResolutionService,
                               PaysoApiClient apiClient,
                               PaysoSaleRecordService paysoSaleRecordService,
                               HqNotifyEnvService hqNotifyEnvService,
                               HostedPayReturnTokenService hostedPayReturnTokenService,
                               MerchantOutboundNotifyService merchantOutboundNotifyService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.chillPayService = chillPayService;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.apiClient = apiClient;
        this.paysoSaleRecordService = paysoSaleRecordService;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.hostedPayReturnTokenService = hostedPayReturnTokenService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
    }

    public Map<String, String> publicNotiIngressUrls() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("pgVendor", PgVendor.PAYSO);
        m.put("notiWebhookPath", NOTI_WEBHOOK_PATH);
        String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(
                hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl());
        m.put("paysoResultUrl", notiBase + NOTI_RESULT_PATH);
        String ingressToken = hqNotifyEnvService.getOrCreate().getIngressToken();
        String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "https://api.icopay.co.kr";
        m.put("icopayIngressBase",
                PgNotifyIngressPaths.buildIngressBase(publicBaseUrl, ingressToken) + "/" + PgVendor.PAYSO);
        m.put("icopayOrderLookupUrl", base + "/api/middleware/notify/v1/order-lookup?order={order}");
        return m;
    }

    public String buildNotiBrowserResultUrl(String compId, String orderNo) {
        String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(
                hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl());
        String cid = compId != null ? compId.trim() : "";
        String on = orderNo != null ? orderNo.trim() : "";
        StringBuilder u = new StringBuilder(notiBase).append(NOTI_RESULT_PATH);
        StringBuilder q = new StringBuilder();
        if (!on.isEmpty()) {
            q.append("order=").append(URLEncoder.encode(on, StandardCharsets.UTF_8));
            q.append("&orderNo=").append(URLEncoder.encode(on, StandardCharsets.UTF_8));
        }
        if (!cid.isEmpty()) {
            if (q.length() > 0) {
                q.append('&');
            }
            q.append("compId=").append(URLEncoder.encode(cid, StandardCharsets.UTF_8));
        }
        if (q.length() > 0) {
            u.append('?').append(q);
        }
        return u.toString();
    }

    public Optional<PaysoCredentials> resolveCredentials(Long orgUnitId) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                orgUnitId, PgVendor::isPaysoFamily);
        if (opPg == null || !PgVendor.isPaysoFamily(opPg)) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByPgCd(opPg).map(PaysoCredentials::fromAgency);
    }

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
        Optional<PaysoCredentials> credOpt = resolveCredentials(orgUnitId);
        if (credOpt.isEmpty() || !credOpt.get().isUsable()) {
            return fail(out, "PAYSO_CREDENTIALS_MISSING",
                    "Payso API 키·secret·merchantId·terminalId가 없습니다. 결제대행사 설정에서 등록하세요.");
        }
        PaysoCredentials creds = credOpt.get();
        String opPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                orgUnitId, PgVendor::isPaysoFamily);
        String bodyPg = str(body, "operationalPgCd");
        if (!bodyPg.isBlank() && PgVendor.isPaysoFamily(bodyPg)) {
            opPg = bodyPg.trim();
        }
        if (opPg == null || opPg.isBlank()) {
            opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        }

        UrlPayChargeResolutionService.ResolvedCharge charge;
        try {
            if (body != null && body.get("shopperDisplayAmount") != null && body.get("amount") != null
                    && body.get("currency") != null) {
                BigDecimal pgAmt = new BigDecimal(String.valueOf(body.get("amount")).replace(",", "").trim());
                String settleCur = String.valueOf(body.get("currency")).trim().toUpperCase(Locale.ROOT);
                BigDecimal shopAmt = new BigDecimal(String.valueOf(body.get("shopperDisplayAmount")).replace(",", "").trim());
                String shopCur = body.get("shopperDisplayCurrency") != null
                        ? String.valueOf(body.get("shopperDisplayCurrency")).trim().toUpperCase(Locale.ROOT) : "";
                charge = new UrlPayChargeResolutionService.ResolvedCharge(pgAmt, settleCur, shopAmt, shopCur);
            } else {
                charge = urlPayChargeResolutionService.resolve(orgUnitId, body != null ? body : Map.of(), opPg);
            }
        } catch (IllegalArgumentException e) {
            String code = (e instanceof NumberFormatException || e.getMessage() == null || e.getMessage().isBlank())
                    ? "INVALID_AMOUNT" : e.getMessage();
            if (!(code.startsWith("INVALID_") || code.startsWith("DISPLAY_") || code.startsWith("FX_")
                    || "NOT_FOUND".equals(code))) {
                code = "INVALID_AMOUNT";
            }
            return fail(out, code, UrlPayChargeResolutionService.failMessageForCode(code));
        }

        String orderNo = str(body, "orderNo", "order_no", "reference");
        if (orderNo.isBlank()) {
            return fail(out, "INVALID_ORDER_NO", "orderNo가 필요합니다.");
        }
        /* 가맹 orderNo ≠ Payso referenceNo — 거래마다 고유 12자리 숫자 */
        String referenceNo = toPaysoReferenceNo(orderNo);

        if (body != null) {
            com.pg.urlpay.PayerContextCapture.enrichSaleBody(body, request, clientIp);
            if (str(body, "txnOrigin", "origin").isBlank()) {
                body.put("txnOrigin", "URL");
            }
        }
        String buyerName = str(body, "customerNm", "buyerName", "buyer", "payerName", "name");
        if (buyerName.isBlank()) {
            buyerName = (str(body, "payFirstname", "firstName") + " " + str(body, "payLastname", "lastName")).trim();
        }
        String buyerEmail = str(body, "payEmailAddress", "buyerEmail", "email", "customerEmail");
        String buyerTel = str(body, "payTelephone", "customerTel", "buyerTel", "phone", "tel");
        String origin = str(body, "txnOrigin", "origin");
        if (origin.isBlank()) {
            origin = "URL";
        }
        /*
         * Return URL — OMSPAY 와 동일하게 rt 암호화 토큰만 (가맹코드·주문번호 평문 금지).
         * Payso에 m=/orderNo= 를 넘기면 PG가 ICOPAY 가맹을 식별할 수 있어 수익권 침해 위험.
         */
        boolean merchantApiReturn = "MERCHANT_API".equalsIgnoreCase(origin)
                || "1".equals(str(body, "merchantApiEntry"))
                || str(body, "returnUrl", "return_url").contains("entry=merchant_api");
        String returnUrl = hostedPayReturnTokenService.buildOpaqueReturnUrl(
                publicBaseUrl,
                "/payso-pay.html",
                PgVendor.PAYSO,
                ouOpt.get().getCode(),
                orderNo,
                merchantApiReturn,
                "payso=1&paysoReturn=1&done=1");
        /*
         * Payso Mode 1: postbackUrl="-" → Control Panel 의 Post Back URL(POST form) 사용.
         * 커스텀 URL을 넣으면 Mode 2(GET·쿼리 고정)로 바뀌어 승인 통보가 빠져
         * 결과 화면이「확인 시간 초과」가 난다. Return URL 만 ICOPAY 동적 URL 유지.
         */
        String postbackUrl = "-";

        /* pending with short reference in pay_no until Payso transactionId returns */
        paysoSaleRecordService.recordOrTouchPending(
                orgUnitId, orderNo, charge.pgAmount(), charge.settlementCurrency(),
                str(body, "productName", "item"), referenceNo, origin,
                charge.shopperDisplayAmount(), charge.shopperDisplayCurrency(),
                buyerName, buyerEmail, buyerTel, body);

        String firstName = str(body, "payFirstname", "firstName", "firstname");
        String lastName = str(body, "payLastname", "lastName", "lastname");
        /*
         * Payso 로는 가맹 상품명·가맹 orderNo 를 보내지 않음.
         * description = 중립 "Payment", referenceNo = ICOPAY 12자리 숫자 해시만.
         * 가맹 orderNo·상품명은 DB(pending)에만 보관 → 통보·조회 시 가맹 쪽 키로 사용.
         */
        Map<String, Object> pay = apiClient.createUiPayment(
                creds,
                referenceNo,
                buyerEmail,
                "Payment",
                charge.pgAmount(),
                toPaysoCurrencyCode(charge.settlementCurrency()),
                str(body, "lang", "langCode"),
                returnUrl,
                postbackUrl,
                /* Appendix A: CreditCard = 카드만 (ALL 이면 QR·Wallet·Banking 등 전부) */
                "CreditCard",
                buyerName,
                firstName,
                lastName);
        if (!Boolean.TRUE.equals(pay.get("ok"))) {
            return fail(out, str(pay.get("errorCode"), "PAYSO_CREATE"),
                    str(pay.get("message"), "Payso 결제 페이지 생성에 실패했습니다."));
        }
        String txnId = str(pay.get("transactionId"), "");
        String paymentUrl = str(pay.get("redirectUrl"), str(pay.get("paymentUrl"), ""));
        if (paymentUrl.isBlank()) {
            return fail(out, "PAYSO_CREATE", "redirectUrl이 없습니다.");
        }
        if (!txnId.isBlank()) {
            paysoSaleRecordService.recordOrTouchPending(
                    orgUnitId, orderNo, charge.pgAmount(), charge.settlementCurrency(),
                    str(body, "productName", "item"), txnId, origin,
                    charge.shopperDisplayAmount(), charge.shopperDisplayCurrency(),
                    buyerName, buyerEmail, buyerTel, body);
        }

        out.put("success", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("pgTxnId", txnId.isBlank() ? referenceNo : txnId);
        data.put("paymentUrl", paymentUrl);
        data.put("redirectUrl", paymentUrl);
        data.put("paymentStatus", "PENDING_REDIRECT");
        data.put("amount", charge.pgAmount());
        data.put("currency", charge.settlementCurrency());
        data.put("pgVendor", "ICOPAY");
        data.put("noti", publicNotiIngressUrls());
        out.put("data", data);
        log.info("PAYSO redirect started mid={} order={} ref={} txn={} amt={} cur={} ip={}",
                ouOpt.get().getCode(), orderNo, referenceNo, txnId,
                charge.pgAmount(), charge.settlementCurrency(), clientIp);
        return out;
    }

    public Map<String, Object> buyerResultStatus(Long orgUnitId, String orderNo) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null || orderNo == null || orderNo.isBlank()) {
            return fail(out, "INVALID_ORDER_NO", "orderNo가 필요합니다.");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        var txn = paysoSaleRecordService.findByMerchantAndOrder(ouOpt.get().getCode(), orderNo.trim());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo.trim());
        data.put("pgVendor", "ICOPAY");
        if (txn.isEmpty()) {
            data.put("paymentStatus", "NOT_FOUND");
            data.put("found", false);
        } else {
            var t = txn.get();
            data.put("found", true);
            data.put("trnId", t.getTrnId());
            data.put("pgTxnId", t.getChillTransactionId());
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            String paymentStatus = switch (st) {
                case "10", "00" -> "PAID";
                case "08" -> "PENDING";
                case "99", "F0" -> "FAIL";
                default -> st;
            };
            data.put("paymentStatus", paymentStatus);
            data.put("paid", "PAID".equals(paymentStatus));
            data.put("amount", t.getAmtKrw());
            data.put("currency", t.getCurType());
        }
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    /**
     * 구매자 복귀·폴링 타임아웃 — 웹훅 없이 Wrong payment 등으로 요청(08)이 남을 때 실패(99) 반영.
     * 이미 승인·실패면 덮지 않음. 수신통보 우선 outbound.
     */
    public Map<String, Object> markBuyerFailed(String compIdHint, String orderNo, String reason) {
        Map<String, Object> out = new LinkedHashMap<>();
        String on = orderNo != null ? orderNo.trim() : "";
        if (on.isBlank()) {
            return fail(out, "INVALID_ORDER_NO", "orderNo가 필요합니다.");
        }
        String comp = compIdHint != null ? compIdHint.trim() : "";
        if (comp.isBlank()) {
            return fail(out, "NOT_FOUND", "compId가 필요합니다.");
        }
        String reasonText = reason != null && !reason.isBlank()
                ? reason.trim()
                : "Buyer return: payment failed or abandoned";
        Optional<com.pg.entity.PgTrnsctn> updated = paysoSaleRecordService.applyFailedOutcome(
                on, comp, null, "PAYSO_BUYER_RETURN_FAIL", reasonText);
        if (updated.isEmpty()) {
            return fail(out, "NOT_FOUND", "주문을 찾을 수 없습니다.");
        }
        com.pg.entity.PgTrnsctn t = updated.get();
        try {
            if ("99".equals(t.getStatus() != null ? t.getStatus().trim() : "")) {
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "PAYSO_BUYER_RETURN_FAIL");
            }
        } catch (Exception e) {
            log.warn("PAYSO buyer mark-failed notify: {}", e.getMessage());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", on);
        data.put("compId", comp);
        data.put("paymentStatus", "99".equals(t.getStatus() != null ? t.getStatus().trim() : "") ? "FAIL" : t.getStatus());
        data.put("pgVendor", "ICOPAY");
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public com.pg.api.ApiResponse<Map<String, Object>> resolveHostedReturn(String rt) {
        Optional<HostedPayReturnTokenService.ReturnPayload> p =
                hostedPayReturnTokenService.parseValid(rt, PgVendor.PAYSO);
        if (p.isEmpty()) {
            return com.pg.api.ApiResponse.fail("복귀 토큰이 유효하지 않거나 만료되었습니다.", "INVALID_RETURN_TOKEN");
        }
        HostedPayReturnTokenService.ReturnPayload rp = p.get();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", rp.compId());
        data.put("orderNo", rp.orderNo());
        data.put("merchantApi", rp.merchantApi());
        data.put("entry", rp.merchantApi() ? "merchant_api" : "");
        return com.pg.api.ApiResponse.ok(data);
    }

    /**
     * Payso referenceNo ≤12, digits only — {@link com.pg.util.IcopayPgOrderRef}.
     * 가맹 orderNo는 인자로 받아도 PG ref 생성에 쓰지 않는다(호환 시그니처).
     */
    public static String toPaysoReferenceNo(String orderNo) {
        return com.pg.util.IcopayPgOrderRef.paysoDigits12();
    }

    public static String toPaysoCurrencyCode(String currency) {
        if (currency == null || currency.isBlank()) {
            return "00";
        }
        return switch (currency.trim().toUpperCase(Locale.ROOT)) {
            case "THB" -> "00";
            case "USD" -> "01";
            case "JPY" -> "02";
            case "SGD" -> "03";
            case "HKD" -> "04";
            case "EUR" -> "05";
            case "GBP" -> "06";
            case "AUD" -> "07";
            case "CHF" -> "08";
            default -> "00";
        };
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
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v).trim();
            }
        }
        return "";
    }

    private static String str(Object v, String def) {
        if (v == null) {
            return def != null ? def : "";
        }
        String s = String.valueOf(v).trim();
        return s.isBlank() ? (def != null ? def : "") : s;
    }
}
