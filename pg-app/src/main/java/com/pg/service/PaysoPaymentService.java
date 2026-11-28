package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
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
    private final PgBuyerEmailAttestService pgBuyerEmailAttestService;

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
                               MerchantOutboundNotifyService merchantOutboundNotifyService,
                               PgBuyerEmailAttestService pgBuyerEmailAttestService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.chillPayService = chillPayService;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.apiClient = apiClient;
        this.paysoSaleRecordService = paysoSaleRecordService;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.hostedPayReturnTokenService = hostedPayReturnTokenService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.pgBuyerEmailAttestService = pgBuyerEmailAttestService;
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

    /** 거래 van(PAYSO / PAYSO JPDP 등) 기준 자격증명 — 후속조치 Void 용. */
    public Optional<PaysoCredentials> resolveCredentialsForTxn(PgTrnsctn t) {
        if (t == null || !PgVendor.isPaysoFamily(t.getVan())) {
            return Optional.empty();
        }
        String van = t.getVan().trim();
        Optional<PaysoCredentials> byVan = pgAgencyRepository.findByPgCd(van).map(PaysoCredentials::fromAgency);
        if (byVan.isPresent() && byVan.get().isInquiryUsable()) {
            return byVan;
        }
        String norm = PgVendor.normalizePgCdKey(van);
        if (!norm.equalsIgnoreCase(van)) {
            Optional<PaysoCredentials> alt = pgAgencyRepository.findByPgCd(norm).map(PaysoCredentials::fromAgency);
            if (alt.isPresent() && alt.get().isInquiryUsable()) {
                return alt;
            }
        }
        String mid = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        if (!mid.isBlank()) {
            Optional<OrgUnit> ou = orgUnitRepository.findByCodeIgnoreCase(mid);
            if (ou.isPresent()) {
                return resolveCredentials(ou.get().getId());
            }
        }
        return byVan;
    }

    /**
     * 결제내역 후속조치 — Payso Void API({@code /payment/gateway/v2/payment/voidpayment}).
     * 공개 가이드: 정산 후 환불 REST 없음 · 정산 전 Void 만.
     * 운영 정책(문서화): Void 는 <strong>태국(Asia/Bangkok) 달력 당일</strong> 승인 건에 한함
     * (API에 HH:mm 마감 문구는 없으나 당일만 성공하는 것으로 확인). ICOPAY 무효 창은
     * 후속조치 「시간 선택 국가」Zone 당일 시작~마감으로 노출·제한한다.
     * 자동무효·자동환불·강제환불이 동일 Void API 를 호출한다(강제=수동 반영인 경우 제외).
     */
    public String requestVoid(PgTrnsctn t, String reason) {
        if (t == null || !PgVendor.isPaysoFamily(t.getVan())) {
            throw new IllegalStateException("해당 거래는 취소 API를 호출할 수 없습니다.");
        }
        String txnId = resolveVoidTransactionId(t);
        if (txnId.isBlank()) {
            throw new IllegalStateException("결제대행사 거래번호가 없어 취소할 수 없습니다.");
        }
        if (t.getAmtKrw() == null || t.getAmtKrw().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("취소할 거래 금액이 없습니다.");
        }
        Optional<PaysoCredentials> credOpt = resolveCredentialsForTxn(t);
        if (credOpt.isEmpty() || !credOpt.get().isInquiryUsable()) {
            throw new IllegalStateException("결제대행사 API 키가 설정되지 않았습니다.");
        }
        Map<String, Object> res = apiClient.voidPayment(credOpt.get(), txnId, t.getAmtKrw());
        if (!Boolean.TRUE.equals(res.get("ok"))) {
            String msg = str(res.get("message"));
            String code = str(res.get("statusCode"));
            if (msg.isBlank()) {
                msg = "Payso Void 실패";
            }
            if ("4A010".equalsIgnoreCase(code) || msg.toLowerCase(Locale.ROOT).contains("cannot be voided")) {
                throw new IllegalStateException(
                        "PAYSO 취소(Void) 불가입니다. 태국(Asia/Bangkok) 당일·정산 전만 Void 가능합니다. "
                                + "정산·당일 창이 지났으면 결제대행사 포털에서 환불을 처리하세요"
                                + (msg.isBlank() ? "" : ": " + msg));
            }
            throw new IllegalStateException("결제 취소 요청에 실패했습니다"
                    + (msg.isBlank() ? "" : ": " + msg));
        }
        String detail = "PAYSO void ok";
        if (reason != null && !reason.isBlank()) {
            detail = detail + " (" + reason.trim() + ")";
        }
        String voidedAt = str(res.get("voidedAt"));
        if (!voidedAt.isBlank()) {
            detail = detail + " @" + voidedAt;
        }
        return detail.length() > 120 ? detail.substring(0, 120) : detail;
    }

    /**
     * Void 의 transactionId — chill_transaction_id(hex) 우선, 없으면 pay_no.
     * 12자리 ICOPAY reference 만 있으면 그대로 시도(일부 환경 호환).
     */
    static String resolveVoidTransactionId(PgTrnsctn t) {
        if (t == null) {
            return "";
        }
        String chill = t.getChillTransactionId() != null ? t.getChillTransactionId().trim() : "";
        if (!chill.isBlank() && !looksLikeMerchantOrderOnly(chill)) {
            return chill;
        }
        String payNo = t.getPayNo() != null ? t.getPayNo().trim() : "";
        if (!payNo.isBlank()) {
            return payNo;
        }
        return chill;
    }

    private static boolean looksLikeMerchantOrderOnly(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        String t = s.trim();
        return t.matches("(?i)^FS\\d+.*") || t.length() > 40;
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
        String mid = ouOpt.get().getCode();
        /*
         * 동일 orderNo 이중 HPP 방지(OMSPAY 패턴).
         * 이미 승인 → PAID 반환. 08 대기 + 기존 ref → Order Detail 확정 또는 PENDING_IN_PROGRESS(신규 createUiPayment 금지).
         */
        Optional<PgTrnsctn> existingOpt = paysoSaleRecordService.findByMerchantAndOrder(mid, orderNo);
        if (existingOpt.isPresent()) {
            PgTrnsctn existing = existingOpt.get();
            String st = existing.getStatus() != null ? existing.getStatus().trim() : "";
            String existingRef = firstNonBlank(existing.getPayNo(), existing.getApprovalNo());
            String existingTxn = firstNonBlank(existing.getChillTransactionId(), "");
            if ("10".equals(st) || "00".equals(st) || "01".equals(st)) {
                out.put("success", true);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("orderNo", orderNo);
                data.put("pgTxnId", !existingTxn.isBlank() ? existingTxn : existingRef);
                data.put("paymentStatus", "PAID");
                data.put("paid", true);
                data.put("reusedPending", false);
                data.put("amount", charge.pgAmount());
                data.put("currency", charge.settlementCurrency());
                data.put("pgVendor", "ICOPAY");
                out.put("data", data);
                log.info("PAYSO start already paid mid={} order={} ip={}", mid, orderNo, clientIp);
                return out;
            }
            if ("08".equals(st) && (!existingRef.isBlank() || !existingTxn.isBlank())) {
                try {
                    Optional<PgTrnsctn> reconciled = confirmPaidViaOrderDetail(mid, orderNo);
                    if (reconciled.isPresent()) {
                        out.put("success", true);
                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("orderNo", orderNo);
                        data.put("pgTxnId", firstNonBlank(reconciled.get().getChillTransactionId(),
                                reconciled.get().getPayNo()));
                        data.put("paymentStatus", "PAID");
                        data.put("paid", true);
                        data.put("reusedPending", true);
                        data.put("amount", charge.pgAmount());
                        data.put("currency", charge.settlementCurrency());
                        data.put("pgVendor", "ICOPAY");
                        out.put("data", data);
                        log.info("PAYSO start reconciled paid mid={} order={} ip={}", mid, orderNo, clientIp);
                        return out;
                    }
                } catch (Exception e) {
                    log.debug("PAYSO start Order Detail check: {}", e.getMessage());
                }
                return fail(out, "PAYSO_PENDING_IN_PROGRESS",
                        "이미 진행 중인 결제가 있습니다. 이전 결제 창에서 완료하거나 잠시 후 다시 시도해 주세요.");
            }
        }

        /* 가맹 orderNo ≠ Payso referenceNo — 신규 시도에만 고유 12자리 숫자 */
        String referenceNo = toPaysoReferenceNo(orderNo);
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
                mid,
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
         * Payso 로는 가맹 상호·홈페이지·도메인·IP·상품명·가맹 orderNo·가맹 도메인 이메일을 보내지 않음.
         * description = "Payment", customerEmail = ICOPAY 중립(클라이언트에서 강제),
         * referenceNo = ICOPAY 12자리 숫자 해시만. 가맹 식별은 DB·가맹 통보에만.
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
                lastName,
                pgBuyerEmailAttestService.shouldSendRealBuyerEmail(orgUnitId));
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
        String mid = ouOpt.get().getCode();
        var txn = paysoSaleRecordService.findByMerchantAndOrder(mid, orderNo.trim());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo.trim());
        data.put("pgVendor", "ICOPAY");
        if (txn.isEmpty()) {
            data.put("paymentStatus", "NOT_FOUND");
            data.put("found", false);
        } else {
            var t = txn.get();
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            boolean paid = "10".equals(st) || "00".equals(st);
            boolean failSt = "99".equals(st) || "F0".equals(st);
            /* 웹훅 지연·잘못된 orderNo 키 — 저장 pay_no 로 Order Detail 조회 후 승인 확정 */
            if (!paid && !failSt) {
                try {
                    Optional<PaysoCredentials> credOpt = resolveCredentials(orgUnitId);
                    String storedRef = PaysoSaleRecordService.firstPaysoReference(
                            t.getPayNo(), t.getApprovalNo());
                    String storedChill = t.getChillTransactionId() != null
                            ? t.getChillTransactionId().trim() : "";
                    if (credOpt.isPresent() && credOpt.get().isInquiryUsable()
                            && (!storedRef.isBlank() || PaysoSaleRecordService.isUsablePaysoPgKey(
                            orderNo.trim(), storedChill))) {
                        Map<String, Object> detail = apiClient.orderDetail(
                                credOpt.get(),
                                !storedRef.isBlank() ? storedRef : "x",
                                orderNo.trim());
                        if (Boolean.TRUE.equals(detail.get("ok"))) {
                            String sc = detail.get("statusCode") != null
                                    ? String.valueOf(detail.get("statusCode")) : "";
                            String sd = detail.get("statusDesc") != null
                                    ? String.valueOf(detail.get("statusDesc")) : "";
                            String mapped = PaysoIntegratedListService.mapPaysoStatus(sc, sd);
                            if ("10".equals(mapped)) {
                                String ref = PaysoSaleRecordService.firstPaysoReference(
                                        str(detail.get("referenceNo")), storedRef);
                                String pgTxn = firstNonBlank(str(detail.get("transactionId")), storedChill);
                                Optional<com.pg.entity.PgTrnsctn> paidTxn =
                                        paysoSaleRecordService.applyPaidOutcome(
                                                orderNo.trim(), mid, pgTxn, ref, null, true,
                                                str(detail.get("orderDateTime")));
                                if (paidTxn.isPresent()) {
                                    t = paidTxn.get();
                                    st = t.getStatus() != null ? t.getStatus().trim() : "10";
                                    paid = true;
                                    failSt = false;
                                    log.info("PAYSO result-status reconciled paid mid={} order={} ref={}",
                                            mid, orderNo.trim(), ref);
                                    try {
                                        merchantOutboundNotifyService.scheduleAfterTxnCommit(
                                                t, null, "PAYSO_RESULT_STATUS");
                                    } catch (Exception ne) {
                                        log.warn("PAYSO result-status notify: {}", ne.getMessage());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("PAYSO result-status Order Detail probe failed mid={} order={}: {}",
                            mid, orderNo.trim(), e.getMessage());
                }
            }
            data.put("found", true);
            data.put("trnId", t.getTrnId());
            data.put("pgTxnId", t.getChillTransactionId());
            String paymentStatus = paid ? "PAID" : (failSt ? "FAIL" : ("08".equals(st) ? "PENDING" : st));
            data.put("paymentStatus", paymentStatus);
            data.put("paid", paid);
            data.put("amount", t.getAmtKrw());
            data.put("currency", t.getCurType());
            String origin = t.getOrigin() != null ? t.getOrigin().trim() : "";
            boolean merchantApi = "MERCHANT_API".equalsIgnoreCase(origin);
            data.put("txnOrigin", origin.isBlank() ? "URL" : origin);
            data.put("merchantApi", merchantApi);
            if (merchantApi) {
                String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(
                        hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl());
                data.put("siteBackUrl", notiBase + NOTI_RESULT_PATH
                        + "?order=" + urlEnc(orderNo.trim())
                        + "&orderNo=" + urlEnc(orderNo.trim())
                        + "&compId=" + urlEnc(mid)
                        + "&merchantId=" + urlEnc(mid));
            }
        }
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String urlEnc(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }

    /**
     * Order Detail 로 PAYSO 실승인 여부 확인 후 승인 확정.
     * 웹훅 키가 없거나 주문번호만 올 때·이미 실패(99)인 건의 복구에 사용.
     */
    public Optional<com.pg.entity.PgTrnsctn> confirmPaidViaOrderDetail(String compId, String orderNo) {
        String mid = compId != null ? compId.trim() : "";
        String on = orderNo != null ? orderNo.trim() : "";
        if (mid.isBlank() || on.isBlank()) {
            return Optional.empty();
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(mid);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(mid);
        }
        if (ou.isEmpty()) {
            return Optional.empty();
        }
        Optional<com.pg.entity.PgTrnsctn> txn = paysoSaleRecordService.findByMerchantAndOrder(mid, on);
        if (txn.isEmpty()) {
            return Optional.empty();
        }
        com.pg.entity.PgTrnsctn t = txn.get();
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        if ("10".equals(st) || "00".equals(st)) {
            return Optional.of(t);
        }
        Optional<PaysoCredentials> credOpt = resolveCredentials(ou.get().getId());
        if (credOpt.isEmpty() || !credOpt.get().isInquiryUsable()) {
            return Optional.empty();
        }
        String storedRef = PaysoSaleRecordService.firstPaysoReference(t.getPayNo(), t.getApprovalNo());
        String storedChill = t.getChillTransactionId() != null ? t.getChillTransactionId().trim() : "";
        if (storedRef.isBlank() && !PaysoSaleRecordService.isUsablePaysoPgKey(on, storedChill)) {
            return Optional.empty();
        }
        try {
            Map<String, Object> detail = apiClient.orderDetail(
                    credOpt.get(),
                    !storedRef.isBlank() ? storedRef : "x",
                    on);
            if (!Boolean.TRUE.equals(detail.get("ok"))) {
                log.info("PAYSO Order Detail 미확인 mid={} order={} ref={}", mid, on, storedRef);
                return Optional.empty();
            }
            String sc = detail.get("statusCode") != null ? String.valueOf(detail.get("statusCode")) : "";
            String sd = detail.get("statusDesc") != null ? String.valueOf(detail.get("statusDesc")) : "";
            String mapped = PaysoIntegratedListService.mapPaysoStatus(sc, sd);
            if (!"10".equals(mapped)) {
                log.info("PAYSO Order Detail 미승인 mid={} order={} status={} desc={}", mid, on, sc, sd);
                return Optional.empty();
            }
            String ref = PaysoSaleRecordService.firstPaysoReference(str(detail.get("referenceNo")), storedRef);
            String pgTxn = firstNonBlank(str(detail.get("transactionId")), storedChill);
            Optional<com.pg.entity.PgTrnsctn> paid = paysoSaleRecordService.applyPaidOutcome(
                    on, mid, pgTxn, ref, null, true, str(detail.get("orderDateTime")));
            if (paid.isPresent()) {
                log.info("PAYSO Order Detail 승인확정 mid={} order={} ref={}", mid, on, ref);
                try {
                    paysoSaleRecordService.applyInquiryEnrichment(
                            paid.get(),
                            ref,
                            str(detail.get("orderNo")),
                            pgTxn,
                            str(detail.get("cardNo")),
                            null,
                            str(detail.get("orderDateTime")));
                } catch (Exception ignored) {
                    /* 승인 확정이 우선 */
                }
            }
            return paid;
        } catch (Exception e) {
            log.warn("PAYSO Order Detail confirm failed mid={} order={}: {}", mid, on, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 구매자 복귀·폴링 타임아웃 — 웹훅 없이 Wrong payment 등으로 요청(08)이 남을 때 실패(99) 반영.
     * 이미 승인·실패면 덮지 않음. 수신통보 우선 outbound.
     */
    public Map<String, Object> markBuyerFailed(String compIdHint, String orderNo, String reason) {
        return markBuyerFailed(compIdHint, orderNo, reason, false);
    }

    /**
     * @param forceCorrectFalsePaid 오성공(승인키=주문번호) 10→99 정정. 실패 수신통보 재발송.
     */
    public Map<String, Object> markBuyerFailed(String compIdHint, String orderNo, String reason,
                                               boolean forceCorrectFalsePaid) {
        Map<String, Object> out = new LinkedHashMap<>();
        String on = orderNo != null ? orderNo.trim() : "";
        if (on.isBlank()) {
            return fail(out, "INVALID_ORDER_NO", "orderNo가 필요합니다.");
        }
        String comp = compIdHint != null ? compIdHint.trim() : "";
        if (comp.isBlank()) {
            return fail(out, "NOT_FOUND", "compId가 필요합니다.");
        }
        Optional<com.pg.entity.PgTrnsctn> before = paysoSaleRecordService.findByMerchantAndOrder(comp, on);
        String prevStatus = before.map(x -> x.getStatus() != null ? x.getStatus().trim() : "").orElse("");
        /* 타임아웃 실패 직전 Order Detail 재확인 — 실승인이면 실패로 쓰지 않음 */
        if (!forceCorrectFalsePaid && ("08".equals(prevStatus) || prevStatus.isBlank())) {
            Optional<com.pg.entity.PgTrnsctn> recovered = confirmPaidViaOrderDetail(comp, on);
            if (recovered.isPresent()
                    && ("10".equals(recovered.get().getStatus() != null ? recovered.get().getStatus().trim() : "")
                    || "00".equals(recovered.get().getStatus() != null ? recovered.get().getStatus().trim() : ""))) {
                com.pg.entity.PgTrnsctn t = recovered.get();
                try {
                    merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "PAYSO_ORDER_DETAIL_BEFORE_FAIL");
                } catch (Exception e) {
                    log.warn("PAYSO mark-failed recover notify: {}", e.getMessage());
                }
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("orderNo", on);
                data.put("compId", comp);
                data.put("prevStatus", prevStatus);
                data.put("paymentStatus", "PAID");
                data.put("recoveredPaid", true);
                data.put("pgVendor", "ICOPAY");
                out.put("success", true);
                out.put("data", data);
                return out;
            }
        }
        String reasonCode = forceCorrectFalsePaid ? "PAYSO_FALSE_PAID_CORRECTED" : "PAYSO_BUYER_RETURN_FAIL";
        String reasonText = reason != null && !reason.isBlank()
                ? reason.trim()
                : (forceCorrectFalsePaid
                ? "PAYSO portal has no matching paid order — corrected false success to failed"
                : "Buyer return: payment failed or abandoned");
        Optional<com.pg.entity.PgTrnsctn> updated = paysoSaleRecordService.applyFailedOutcome(
                on, comp, null, reasonCode, reasonText, forceCorrectFalsePaid);
        if (updated.isEmpty()) {
            return fail(out, "NOT_FOUND", "주문을 찾을 수 없습니다.");
        }
        com.pg.entity.PgTrnsctn t = updated.get();
        try {
            if ("99".equals(t.getStatus() != null ? t.getStatus().trim() : "")) {
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, reasonCode);
            }
        } catch (Exception e) {
            log.warn("PAYSO buyer mark-failed notify: {}", e.getMessage());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", on);
        data.put("compId", comp);
        data.put("prevStatus", prevStatus);
        data.put("paymentStatus", "99".equals(t.getStatus() != null ? t.getStatus().trim() : "") ? "FAIL" : t.getStatus());
        data.put("correctedFalsePaid", forceCorrectFalsePaid && "10".equals(prevStatus));
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
