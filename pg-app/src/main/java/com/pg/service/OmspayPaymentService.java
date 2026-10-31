package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.omspay.OmspayApiClient;
import com.pg.integration.pg.omspay.OmspayCredentials;
import com.pg.receipt.TransactionReceiptEmailService;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.noti.NotiProvisionClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    /** 브라우저 사이트백 — NOTI Result → 가맹 resultUrl (ElementPay와 동일 역할). */
    public static final String NOTI_RESULT_PATH = "/noti/result/omspay";

    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final ChillPayService chillPayService;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final OmspayApiClient apiClient;
    private final OmspaySaleRecordService omspaySaleRecordService;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final TransactionReceiptEmailService transactionReceiptEmailService;

    @Value("${pg.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    public OmspayPaymentService(OrgUnitRepository orgUnitRepository,
                                PgAgencyRepository pgAgencyRepository,
                                ChillPayService chillPayService,
                                UrlPayChargeResolutionService urlPayChargeResolutionService,
                                OmspayApiClient apiClient,
                                OmspaySaleRecordService omspaySaleRecordService,
                                HqNotifyEnvService hqNotifyEnvService,
                                @Lazy TransactionReceiptEmailService transactionReceiptEmailService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.chillPayService = chillPayService;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.apiClient = apiClient;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
        this.omspaySaleRecordService = omspaySaleRecordService;
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    public Map<String, String> publicNotiIngressUrls() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("pgVendor", PgVendor.OMSPAY);
        m.put("notiWebhookPath", "/noti/omspay");
        String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(
                hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl());
        m.put("omspayResultUrl", notiBase + NOTI_RESULT_PATH);
        String ingressToken = hqNotifyEnvService.getOrCreate().getIngressToken();
        String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "https://api.icopay.co.kr";
        m.put("icopayIngressBase",
                PgNotifyIngressPaths.buildIngressBase(publicBaseUrl, ingressToken) + "/" + PgVendor.OMSPAY);
        /* NOTI 전 PG 공통 Comp-Id 조회 (OMSPAY 전용 URL도 호환 유지) */
        m.put("icopayOrderLookupUrl", base + "/api/middleware/notify/v1/order-lookup?order={order}");
        return m;
    }

    /**
     * 가맹 API REDIRECT 사이트백 — NOTI {@code /noti/result/omspay} (가맹 resultUrl 릴레이).
     * OMS return 은 ICOPAY 도메인(omspay-pay)으로 두고, 결제창이 이 URL로 넘긴다.
     */
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
            q.append("&merchantId=").append(URLEncoder.encode(cid, StandardCharsets.UTF_8));
        }
        if (q.length() > 0) {
            u.append('?').append(q);
        }
        return u.toString();
    }

    public Optional<OmspayCredentials> resolveCredentials(Long orgUnitId) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                orgUnitId, PgVendor::isOmspayFamily);
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
        /* 혼용 가맹: 기본 운영 PG가 ElementPay 등이어도 OMSPAY 창은 OMSPAY 바인딩·견적 PG 사용 */
        String opPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                orgUnitId, PgVendor::isOmspayFamily);
        String bodyPg = str(body, "operationalPgCd");
        if (!bodyPg.isBlank() && PgVendor.isOmspayFamily(bodyPg)) {
            opPg = bodyPg.trim();
        }
        if (opPg == null || opPg.isBlank()) {
            opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        }

        UrlPayChargeResolutionService.ResolvedCharge charge;
        try {
            /* UrlPaySaleDispatcher 가 이미 환산해 둔 경우 재해석(견적 PG 불일치)을 피함 */
            if (body != null && body.get("shopperDisplayAmount") != null && body.get("amount") != null
                    && body.get("currency") != null) {
                BigDecimal pgAmt = new BigDecimal(String.valueOf(body.get("amount")).replace(",", "").trim());
                String settleCur = String.valueOf(body.get("currency")).trim().toUpperCase();
                BigDecimal shopAmt = new BigDecimal(String.valueOf(body.get("shopperDisplayAmount")).replace(",", "").trim());
                String shopCur = body.get("shopperDisplayCurrency") != null
                        ? String.valueOf(body.get("shopperDisplayCurrency")).trim().toUpperCase() : "";
                charge = new UrlPayChargeResolutionService.ResolvedCharge(pgAmt, settleCur, shopAmt, shopCur);
            } else {
                charge = urlPayChargeResolutionService.resolve(orgUnitId, body != null ? body : Map.of(), opPg);
            }
        } catch (IllegalArgumentException e) {
            /* NumberFormatException ⊂ IllegalArgumentException */
            String code = (e instanceof NumberFormatException || e.getMessage() == null || e.getMessage().isBlank())
                    ? "INVALID_AMOUNT"
                    : e.getMessage();
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
            /* 직접 omspay-pay.html + 해시 백업 — OMS가 쿼리를 잘라도 결과 UI 진입 */
            String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "https://api.icopay.co.kr";
            String mid = java.net.URLEncoder.encode(ouOpt.get().getCode(), java.nio.charset.StandardCharsets.UTF_8);
            String onEnc = java.net.URLEncoder.encode(orderNo, java.nio.charset.StandardCharsets.UTF_8);
            returnUrl = base + "/omspay-pay.html"
                    + "?m=" + mid
                    + "&orderNo=" + onEnc
                    + "&omspay=1&omspayReturn=1&done=1"
                    + "#m=" + mid
                    + "&orderNo=" + onEnc
                    + "&omspayReturn=1&done=1";
        }

        if (body != null) {
            com.pg.urlpay.PayerContextCapture.enrichSaleBody(body, request, clientIp);
            if (str(body, "txnOrigin", "origin").isBlank()) {
                body.put("txnOrigin", "URL");
            }
        }
        String buyerName = str(body, "customerNm", "buyerName", "buyer", "payerName", "name");
        if (buyerName.isBlank()) {
            String fn = str(body, "payFirstname", "firstName");
            String ln = str(body, "payLastname", "lastName");
            buyerName = (fn + " " + ln).trim();
        }
        String buyerEmail = str(body, "payEmailAddress", "buyerEmail", "email", "customerEmail");
        String buyerTel = str(body, "payTelephone", "customerTel", "buyerTel", "phone", "tel");
        String origin = str(body, "txnOrigin", "origin");
        if (origin.isBlank()) {
            origin = "URL";
        }
        omspaySaleRecordService.recordOrTouchPending(
                orgUnitId, orderNo, charge.pgAmount(), charge.settlementCurrency(),
                str(body, "productName", "item"), omsCode, origin,
                charge.shopperDisplayAmount(), charge.shopperDisplayCurrency(),
                buyerName, buyerEmail, buyerTel, body);

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

    /**
     * 구매자 return 화면용 — 로컬 {@code pg_trnsctn} 상태(웹훅 반영 후 PAID).
     * {@code paymentStatus}: PAID | PENDING | FAIL | NOT_FOUND
     */
    public Map<String, Object> buyerResultStatus(Long orgUnitId, String orderNo) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null || orderNo == null || orderNo.isBlank()) {
            return fail(out, "INVALID_ORDER_NO", "orderNo가 필요합니다.");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        String mid = ouOpt.get().getCode();
        Optional<PgTrnsctn> txn = omspaySaleRecordService.findByMerchantAndOrder(mid, orderNo.trim());
        if (txn.isEmpty()) {
            out.put("success", true);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderNo", orderNo.trim());
            data.put("paymentStatus", "NOT_FOUND");
            data.put("paid", false);
            data.put("pending", true);
            out.put("data", data);
            return out;
        }
        PgTrnsctn t = txn.get();
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        boolean paid = "10".equals(st) || "00".equals(st) || "01".equals(st) || "PAID".equalsIgnoreCase(st);
        boolean fail = "20".equals(st) || "30".equals(st) || "90".equals(st) || "99".equals(st)
                || "FAIL".equalsIgnoreCase(st);
        String origin = t.getOrigin() != null ? t.getOrigin().trim() : "";
        boolean merchantApi = "MERCHANT_API".equalsIgnoreCase(origin);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", t.getOrderNo());
        data.put("status", st);
        data.put("paymentStatus", paid ? "PAID" : (fail ? "FAIL" : "PENDING"));
        data.put("paid", paid);
        data.put("pending", !paid && !fail);
        data.put("txnOrigin", origin.isBlank() ? "URL" : origin);
        data.put("merchantApi", merchantApi);
        /* 가맹 API REDIRECT: ICOPAY 완료화면 대신 NOTI Result → 가맹 resultUrl 사이트백 */
        if (merchantApi) {
            data.put("siteBackUrl", buildNotiBrowserResultUrl(mid,
                    t.getOrderNo() != null ? t.getOrderNo() : orderNo.trim()));
        }
        data.put("amount", t.getAmtKrw() != null ? t.getAmtKrw() : t.getTotalAmt());
        data.put("currency", t.getCurType());
        if (t.getDisplayAmt() != null) {
            data.put("shopperDisplayAmount", t.getDisplayAmt());
        }
        if (t.getDisplayCurType() != null && !t.getDisplayCurType().isBlank()) {
            data.put("shopperDisplayCurrency", t.getDisplayCurType());
        }
        /* 웹훅 경로에서 영수증이 누락된 건 — 결과 폴링 시 1회 보완 발송 */
        if (paid || fail) {
            try {
                transactionReceiptEmailService.scheduleIfDue(t);
            } catch (Exception e) {
                log.warn("OMSPAY result-status receipt hook failed trnId={}: {}", t.getTrnId(), e.getMessage());
            }
        }
        out.put("success", true);
        out.put("data", data);
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
