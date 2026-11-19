package com.pg.service;

import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.ksnet.KsnetApiClient;
import com.pg.integration.pg.ksnet.KsnetCredentials;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.urlpay.CheckoutFailI18n;
import com.pg.urlpay.PayerContextCapture;
import com.pg.urlpay.UrlPayPublicCheckoutService;
import com.pg.urlpay.UrlPayVendorCapabilityRegistry;
import com.pg.util.PayPresaleRiskFilterCodes;
import com.pg.util.PgOutboundUrlPolicy;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KSNET(KSPAY) 해외 USD 결제창.
 *
 * <p>플로우: 구매자 ICOPAY 결제창({@code ksnet-pay.html}) → {@code /api/pay/url/sale}(requestPay 파라미터) →
 * 브라우저 {@code KSPAY.requestPay}(PC=POPUP / 모바일=MOBILE+callbackUrl) → {@code payKey(commConId)} →
 * {@code POST /api/pay/ksnet/approve} → 서버가 {@code recv_post.jsp} 승인. <b>인증≠결제</b>, 승인 성공만 10.
 *
 * <p>해외 전용: 통화 USD · showCard=C(25) · payMethod=card. KSNET 전송 금액은 USD×1000(예 130.00 → "130000"),
 * ICOPAY DB·표시는 실제 USD. 카드번호 입력(키인)·ICOPAY 카드폼 없음. 가맹 MID 는 KSNET 으로 보내지 않는다(집계 mid 만).
 */
@Service
public class KsnetPaymentService {

    private static final Logger log = LoggerFactory.getLogger(KsnetPaymentService.class);

    public static final String CURRENCY_USD = "USD";

    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final ChillPayService chillPayService;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final KsnetApiClient apiClient;
    private final KsnetSaleRecordService ksnetSaleRecordService;
    private final KsnetPostApproveService ksnetPostApproveService;
    private final PayPresaleRiskFilterService payPresaleRiskFilterService;
    private final UrlPayPublicCheckoutService urlPayPublicCheckoutService;

    private final ConcurrentHashMap<String, Object> approveLocks = new ConcurrentHashMap<>();

    @Value("${pg.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    public KsnetPaymentService(OrgUnitRepository orgUnitRepository,
                               PgAgencyRepository pgAgencyRepository,
                               MerchantPgBindingRepository merchantPgBindingRepository,
                               ChillPayService chillPayService,
                               HqNotifyEnvService hqNotifyEnvService,
                               KsnetApiClient apiClient,
                               KsnetSaleRecordService ksnetSaleRecordService,
                               KsnetPostApproveService ksnetPostApproveService,
                               PayPresaleRiskFilterService payPresaleRiskFilterService,
                               @Lazy UrlPayPublicCheckoutService urlPayPublicCheckoutService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.chillPayService = chillPayService;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.apiClient = apiClient;
        this.ksnetSaleRecordService = ksnetSaleRecordService;
        this.ksnetPostApproveService = ksnetPostApproveService;
        this.payPresaleRiskFilterService = payPresaleRiskFilterService;
        this.urlPayPublicCheckoutService = urlPayPublicCheckoutService;
    }

    /* ================= 자격증명 · 컨텍스트 ================= */

    public Optional<KsnetCredentials> resolveCredentials(Long orgUnitId) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                orgUnitId, PgVendor::isKsnetFamily);
        if (opPg == null || !PgVendor.isKsnetFamily(opPg)) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByPgCd(opPg).map(KsnetCredentials::fromAgency);
    }

    /**
     * 공개 checkout-context — DP PG 등이 기본 운영이어도 KSNET 전용 창은 KSNET 바인딩·금액모드를 쓴다.
     *
     * @throws IllegalStateException KSNET 운영 바인딩 없음 또는 라우트 미구성
     */
    public Map<String, Object> checkoutContext(Long orgUnitId, HttpServletRequest request) {
        Map<String, Object> data = urlPayPublicCheckoutService.buildCheckoutContext(orgUnitId, request, false);
        String opPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                orgUnitId, PgVendor::isKsnetFamily);
        if (opPg == null || opPg.isBlank()) {
            throw new IllegalStateException("KSNET_URL_NOT_CONFIGURED");
        }
        String curOp = String.valueOf(data.getOrDefault("urlPayOperationalPgCd", "")).trim();
        if (!opPg.equalsIgnoreCase(curOp)) {
            data.put("urlPayOperationalPgCd", opPg);
            data.put("urlPayInlineWidgetKind", UrlPayVendorCapabilityRegistry.resolveInlineWidgetKindLegacy(opPg));
            urlPayPublicCheckoutService.enrichPresentation(data, orgUnitId);
        }
        return data;
    }

    /* ================= 결제창 요청(requestPay 파라미터) ================= */

    /**
     * URL 결제 승인 채널({@link com.pg.urlpay.UrlPaySaleChannel#KSNET_SDK_SALE}) 진입점 —
     * 대기거래 적재 후 브라우저가 {@code KSPAY.requestPay} 에 그대로 넘길 파라미터를 반환한다.
     * 실결제 금액·통화는 {@code UrlPaySaleDispatcher} 의 ChargeResolution 결과(body.amount/currency)를 쓴다.
     */
    public Map<String, Object> executeSale(Long orgUnitId, Map<String, Object> body,
                                           HttpServletRequest req, String clientIp) {
        if (orgUnitId == null) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        Optional<KsnetCredentials> credOpt = resolveCredentials(orgUnitId);
        if (credOpt.isEmpty() || !credOpt.get().isUsable()) {
            return fail("해외 결제 설정(storeid)이 없습니다. 결제대행사 설정에서 등록하세요.", "KSNET_CREDENTIALS_MISSING");
        }
        KsnetCredentials creds = credOpt.get();

        PayerContextCapture.enrichSaleBody(body, req, clientIp);
        String compCode = ou.get().getCode();

        String currency = str(body.get("currency")).toUpperCase(Locale.ROOT);
        if (!CURRENCY_USD.equals(currency)) {
            return CheckoutFailI18n.fail("KSNET_USD_ONLY",
                    "해외 결제창은 USD 결제만 지원합니다.",
                    "This payment window supports USD only.",
                    "この決済画面はUSDのみ対応しています。",
                    "该支付窗口仅支持美元(USD)。",
                    "หน้าต่างชำระเงินนี้รองรับเฉพาะ USD");
        }
        BigDecimal amountUsd = parseAmount(body.get("amount"));
        if (amountUsd == null || amountUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("결제 금액이 올바르지 않습니다.", "INVALID_AMOUNT");
        }
        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        String productName = firstNonBlank(str(body.get("item")), str(body.get("productName")), "Order");
        String buyerName = firstNonBlank(str(body.get("customerNm")), str(body.get("buyerName")),
                joinName(str(body.get("payFirstname")), str(body.get("payLastname"))), "Customer");
        String buyerEmail = firstNonBlank(str(body.get("payEmailAddress")), str(body.get("buyerEmail")),
                str(body.get("email")));
        String buyerTel = firstNonBlank(str(body.get("payTelephone")), str(body.get("phone")), str(body.get("tel")));
        String txnOrigin = firstNonBlank(str(body.get("txnOrigin")), str(body.get("origin")), "URL");
        BigDecimal shopAmt = parseAmount(body.get("shopperDisplayAmount"));
        String shopCur = str(body.get("shopperDisplayCurrency")).toUpperCase(Locale.ROOT);

        Integer routeNo = resolveRouteNo(orgUnitId);

        /* 사전 리스크 필터 — 샌드박스는 테스트 반복 결제를 막지 않는다 */
        if (!creds.sandbox()) {
            Optional<PayPresaleRiskFilterService.PresaleRiskBlock> presale =
                    payPresaleRiskFilterService.evaluate(orgUnitId, compCode, PgVendor.KSNET, body);
            if (presale.isPresent()) {
                return presaleRiskBlockOut(presale.get(), orgUnitId, compCode, orderNo, txnOrigin,
                        amountUsd, routeNo, productName, buyerName, buyerEmail, buyerTel,
                        shopAmt, shopCur, body);
            }
        }

        boolean mobile = resolveMobile(req, str(body.get("deviceType")));
        String publicBase = resolvePublicApiBase(req);
        /* 가맹 식별정보 미포함 — 복귀 URL 에는 주문번호만(가맹 MID·코드 금지) */
        String callbackUrl = PgOutboundUrlPolicy.enforceOwnDomain(
                publicBase + "/ksnet-pay.html?ksnetReturn=1&orderNo=" + enc(orderNo), publicBase, publicBase);

        ksnetSaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amountUsd, CURRENCY_USD, routeNo,
                productName, txnOrigin, shopAmt, shopCur, buyerName, buyerEmail, buyerTel, body);

        Map<String, Object> params = buildRequestPayParams(creds, orderNo, productName, amountUsd,
                buyerName, buyerEmail, buyerTel, mobile, callbackUrl);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("pgVendor", "ICOPAY");
        out.put("orderNo", orderNo);
        out.put("amount", amountUsd.stripTrailingZeros().toPlainString());
        out.put("currency", CURRENCY_USD);
        out.put("paymentStatus", "PENDING_AUTH");
        out.put("deviceType", mobile ? "MOBILE" : "POPUP");
        out.put("sdkScriptUrl", creds.sdkUrl(mobile));
        out.put("requestParams", params);
        out.put("returnUrl", callbackUrl);
        out.put("sandbox", creds.sandbox());
        return out;
    }

    /**
     * KSPAY.requestPay 파라미터(WebFEP 결제창형 문서 필드명).
     * 해외 전용: currency=USD, payMethod=card, showCard=C(25), payAmount=USD×1000.
     * PC=POPUP, 모바일=MOBILE+callbackUrl. system=TEST|REAL.
     */
    public Map<String, Object> buildRequestPayParams(KsnetCredentials creds, String orderNo, String productName,
                                                     BigDecimal amountUsd, String buyerName, String buyerEmail,
                                                     String buyerTel, boolean mobile, String callbackUrl) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("system", creds.sandbox() ? "TEST" : "REAL");
        p.put("winType", mobile ? "MOBILE" : "POPUP");
        p.put("mid", creds.storeId());
        p.put("orderNumb", truncate(orderNo, 50));
        p.put("productName", truncate(
                productName != null && !productName.isBlank() ? productName : "ICOPAY", 50));
        p.put("payAmount", KsnetApiClient.toKsnetUsdAmount(amountUsd));
        p.put("userName", truncate(
                buyerName != null && !buyerName.isBlank() ? buyerName : "CUSTOMER", 50));
        String tel = buyerTel != null ? buyerTel.replaceAll("[^0-9]", "") : "";
        if (!tel.isBlank()) {
            p.put("userPhoneNumb", truncate(tel, 12));
        }
        p.put("userEmail", truncate(
                buyerEmail != null && !buyerEmail.isBlank() ? buyerEmail : "noreply@icopay.co.kr", 50));
        p.put("payMethod", "card");
        p.put("showCard", creds.showCard());
        p.put("currency", CURRENCY_USD);
        /* 해외카드: 일시불 중심 */
        p.put("installMonth", "00");
        p.put("interestType", "NONE");
        p.put("goodType", "1");
        if (mobile && callbackUrl != null && !callbackUrl.isBlank()) {
            p.put("callbackUrl", callbackUrl);
        }
        return p;
    }

    /* ================= 승인 ================= */

    /**
     * payKey(commConId) 수신 후 서버 승인. 금액은 클라이언트가 아니라 ICOPAY 대기거래 USD 에서 ×1000 으로 산출한다.
     * 이미 승인(10)이면 멱등 성공.
     */
    public Map<String, Object> executeApprove(String compIdHint, String orderNo, String payKey,
                                              boolean mobile, String clientIp) {
        String on = orderNo != null ? orderNo.trim() : "";
        String pk = payKey != null ? payKey.trim() : "";
        if (on.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        if (pk.isBlank()) {
            return fail("payKey가 필요합니다.", "INVALID_PAY_KEY");
        }
        String comp = compIdHint != null ? compIdHint.trim() : "";
        Optional<PgTrnsctn> txnOpt = Optional.empty();
        if (!comp.isBlank()) {
            txnOpt = ksnetSaleRecordService.findByMerchantAndOrder(comp, on);
        }
        if (txnOpt.isEmpty()) {
            txnOpt = ksnetSaleRecordService.findAnyByOrder(on);
        }
        if (txnOpt.isEmpty()) {
            return fail("주문을 찾을 수 없습니다.", "NOT_FOUND");
        }
        PgTrnsctn txn = txnOpt.get();
        comp = txn.getMerchantId() != null ? txn.getMerchantId().trim() : "";
        String lockKey = comp + "|" + on;
        synchronized (approveLocks.computeIfAbsent(lockKey, k -> new Object())) {
            try {
                return approveLocked(comp, on, pk, mobile, clientIp);
            } finally {
                approveLocks.remove(lockKey);
            }
        }
    }

    private Map<String, Object> approveLocked(String comp, String orderNo, String payKey,
                                              boolean mobile, String clientIp) {
        Optional<PgTrnsctn> cur = ksnetSaleRecordService.findByMerchantAndOrder(comp, orderNo);
        if (cur.isEmpty()) {
            return fail("주문을 찾을 수 없습니다.", "NOT_FOUND");
        }
        PgTrnsctn txn = cur.get();
        String st = txn.getStatus() != null ? txn.getStatus().trim() : "";
        if ("10".equals(st)) {
            return successOut(txn, true);
        }
        if (!"08".equals(st)) {
            return fail("승인 가능한 상태가 아닙니다.", "KSNET_NOT_PENDING");
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(comp);
        if (ou.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        Optional<KsnetCredentials> credOpt = resolveCredentials(ou.get().getId());
        if (credOpt.isEmpty() || !credOpt.get().isUsable()) {
            return fail("해외 결제 설정(storeid)이 없습니다.", "KSNET_CREDENTIALS_MISSING");
        }
        if (!CURRENCY_USD.equalsIgnoreCase(txn.getCurType() != null ? txn.getCurType().trim() : "")) {
            return fail("USD 거래만 승인할 수 있습니다.", "KSNET_USD_ONLY");
        }
        BigDecimal usd = txn.getAmtKrw();
        if (usd == null || usd.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("승인 금액이 없습니다.", "INVALID_AMOUNT");
        }
        String amountKsnet = KsnetApiClient.toKsnetUsdAmount(usd);
        Map<String, Object> res = apiClient.approve(credOpt.get(), mobile, payKey, amountKsnet);
        if (!Boolean.TRUE.equals(res.get("ok"))) {
            String msg = str(res.get("message"));
            log.warn("KSNET approve 통신 실패 comp={} order={} code={} ip={}", comp, orderNo, res.get("errorCode"), clientIp);
            /* 통신 실패는 승인 여부 불명 — 대기 유지(재시도 가능), 실패 확정하지 않음 */
            return fail(msg.isBlank() ? "승인 요청에 실패했습니다." : msg,
                    str(res.get("errorCode")).isBlank() ? "KSNET_APPROVE_ERROR" : str(res.get("errorCode")));
        }
        boolean approved = Boolean.TRUE.equals(res.get("approved"));
        String tid = str(res.get("tid"));
        String authNo = str(res.get("authNo"));
        String msg = str(res.get("message"));
        String card = str(res.get("cardMasked"));
        if (approved && amountMismatch(str(res.get("amount")), amountKsnet)) {
            log.warn("KSNET 승인 금액 불일치 comp={} order={} sent={} got={} → 승인 취소 시도",
                    comp, orderNo, amountKsnet, res.get("amount"));
            try {
                apiClient.cancel(credOpt.get(), tid, orderNo, amountKsnet, "amount mismatch");
            } catch (Exception e) {
                log.warn("KSNET 금액불일치 취소 실패 order={}: {}", orderNo, e.getMessage());
            }
            ksnetSaleRecordService.applyApproveOutcome(comp, orderNo, false, tid, authNo,
                    "Amount mismatch", card, "KSNET_AMOUNT_MISMATCH");
            return fail("승인 금액이 일치하지 않아 결제가 취소되었습니다.", "KSNET_AMOUNT_MISMATCH");
        }
        Optional<PgTrnsctn> saved = ksnetSaleRecordService.applyApproveOutcome(
                comp, orderNo, approved, tid, authNo, msg, card, str(res.get("code")));
        if (saved.isEmpty()) {
            return fail("거래 반영에 실패했습니다.", "KSNET_RECORD_FAILED");
        }
        PgTrnsctn t = saved.get();
        log.info("KSNET approve comp={} order={} approved={} tid={} ip={}", comp, orderNo, approved, tid, clientIp);
        if (!approved) {
            ksnetPostApproveService.afterApproved(t, "KSNET_APPROVE_FAIL");
            return fail(msg.isBlank() ? "승인이 거절되었습니다." : msg, "KSNET_APPROVE_DECLINED");
        }
        ksnetPostApproveService.afterApproved(t, "KSNET_APPROVE");
        return successOut(t, false);
    }

    /** 구매자가 결제창을 닫음·인증 실패 — 대기(08)만 실패 처리(금전 이동 없음). */
    public Map<String, Object> reportAuthAborted(String compIdHint, String orderNo, String reason) {
        String on = orderNo != null ? orderNo.trim() : "";
        if (on.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        String comp = compIdHint != null ? compIdHint.trim() : "";
        Optional<PgTrnsctn> t = comp.isBlank() ? Optional.empty()
                : ksnetSaleRecordService.findByMerchantAndOrder(comp, on);
        if (t.isEmpty()) {
            t = ksnetSaleRecordService.findAnyByOrder(on);
        }
        if (t.isEmpty()) {
            return fail("주문을 찾을 수 없습니다.", "NOT_FOUND");
        }
        PgTrnsctn txn = t.get();
        ksnetSaleRecordService.applyAuthFailed(txn.getMerchantId(), on, reason, "KSNET_AUTH_ABORTED");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", on);
        data.put("pgVendor", "ICOPAY");
        out.put("data", data);
        return out;
    }

    /* ================= 취소(환불) ================= */

    /**
     * 결제내역 자동/강제환불 — KSNET REST {@code /api/v1/card/cancel}(Authorization: pgapi {apiKey}).
     */
    public String requestCancel(PgTrnsctn t, String reason) {
        if (t == null || !PgVendor.isKsnetFamily(t.getVan())) {
            throw new IllegalStateException("해당 거래는 취소 API를 호출할 수 없습니다.");
        }
        String tid = t.getChillTransactionId() != null ? t.getChillTransactionId().trim() : "";
        if (tid.isBlank()) {
            throw new IllegalStateException("거래번호가 없어 취소 API를 호출할 수 없습니다.");
        }
        if (t.getAmtKrw() == null || t.getAmtKrw().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("취소할 거래 금액이 없습니다.");
        }
        String comp = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(comp);
        if (ou.isEmpty()) {
            throw new IllegalStateException("가맹점을 찾을 수 없습니다.");
        }
        Optional<KsnetCredentials> credOpt = resolveCredentials(ou.get().getId());
        if (credOpt.isEmpty() || !credOpt.get().isUsable()) {
            throw new IllegalStateException("결제대행사 storeid 가 설정되지 않았습니다.");
        }
        Map<String, Object> res = apiClient.cancel(credOpt.get(), tid, t.getOrderNo(),
                KsnetApiClient.toKsnetUsdAmount(t.getAmtKrw()), reason);
        if (!Boolean.TRUE.equals(res.get("ok"))) {
            throw new IllegalStateException("결제 취소 요청에 실패했습니다: " + str(res.get("message")));
        }
        if (!Boolean.TRUE.equals(res.get("approved"))) {
            String msg = str(res.get("message"));
            throw new IllegalStateException("결제 취소가 거부되었습니다" + (msg.isBlank() ? "" : ": " + msg));
        }
        return "KSNET cancel ok";
    }

    /* ================= 구매자 결과 ================= */

    public Map<String, Object> buyerResultStatus(Long orgUnitId, String orderNo) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null || orderNo == null || orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        Optional<PgTrnsctn> txn = ksnetSaleRecordService.findByMerchantAndOrder(ouOpt.get().getCode(), orderNo.trim());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo.trim());
        data.put("pgVendor", "ICOPAY");
        if (txn.isEmpty()) {
            data.put("paymentStatus", "NOT_FOUND");
            data.put("found", false);
            data.put("paid", false);
        } else {
            PgTrnsctn t = txn.get();
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            data.put("found", true);
            data.put("trnId", t.getTrnId());
            data.put("paymentStatus", switch (st) {
                case "10", "00" -> "PAID";
                case "08" -> "PENDING";
                case "99", "F0", "20" -> "FAIL";
                default -> st;
            });
            data.put("paid", "10".equals(st) || "00".equals(st));
            data.put("amount", t.getAmtKrw());
            data.put("currency", t.getCurType());
        }
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    /* ================= 내부 ================= */

    private Map<String, Object> successOut(PgTrnsctn t, boolean already) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", t.getOrderNo());
        data.put("paymentStatus", "PAID");
        data.put("paid", true);
        data.put("already", already);
        data.put("trnId", t.getTrnId());
        data.put("amount", t.getAmtKrw());
        data.put("currency", t.getCurType());
        data.put("pgVendor", "ICOPAY");
        out.put("data", data);
        return out;
    }

    private Map<String, Object> presaleRiskBlockOut(PayPresaleRiskFilterService.PresaleRiskBlock block,
                                                    Long orgUnitId, String merchantCode, String orderNo,
                                                    String txnOrigin, BigDecimal amountUsd, Integer routeNo,
                                                    String productName, String buyerName, String buyerEmail,
                                                    String buyerTel, BigDecimal shopAmt, String shopCur,
                                                    Map<String, Object> body) {
        ksnetSaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amountUsd, CURRENCY_USD, routeNo,
                productName, txnOrigin, shopAmt, shopCur, buyerName, buyerEmail, buyerTel, body);
        String trnId = ksnetSaleRecordService.applyIcopayPresaleRiskCancel(merchantCode, orderNo, block.message());
        payPresaleRiskFilterService.recordEvent(orgUnitId, merchantCode, orderNo, trnId, PgVendor.KSNET, block);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", PayPresaleRiskFilterCodes.ERROR_CODE);
        out.put("filterCode", block.filterCode());
        out.put("message", block.message());
        out.put("messages", block.messages());
        out.put("icopayPresaleBlock", true);
        out.put("txnStatus", "20");
        return out;
    }

    private Integer resolveRouteNo(Long orgUnitId) {
        try {
            List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
            return list.stream()
                    .filter(b -> PgVendor.isKsnetFamily(b.getPgCd()))
                    .filter(b -> !"N".equalsIgnoreCase(b.getActivationYn() != null ? b.getActivationYn().trim() : ""))
                    .filter(b -> "Y".equalsIgnoreCase(b.getOperationalYn() != null ? b.getOperationalYn().trim() : ""))
                    .findFirst()
                    .map(MerchantPgBinding::getSortOrder)
                    .orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }

    /** KSNET 응답 금액이 있을 때만 비교(숫자 동등). 파싱 불가·없음은 불일치로 보지 않는다. */
    private static boolean amountMismatch(String returned, String sent) {
        if (returned == null || returned.isBlank()) {
            return false;
        }
        try {
            return new BigDecimal(returned.replace(",", "").trim())
                    .compareTo(new BigDecimal(sent.trim())) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean resolveMobile(HttpServletRequest req, String override) {
        if ("MOBILE".equalsIgnoreCase(override) || "M".equalsIgnoreCase(override)) {
            return true;
        }
        if ("PC".equalsIgnoreCase(override) || "POPUP".equalsIgnoreCase(override) || "P".equalsIgnoreCase(override)) {
            return false;
        }
        String ua = req != null ? str(req.getHeader("User-Agent")) : "";
        return ua.matches("(?i).*(iphone|ipad|ipod|android|mobile|silk|opera mini|webos).*");
    }

    private String resolvePublicApiBase(HttpServletRequest req) {
        String base = null;
        try {
            base = hqNotifyEnvService.getOrCreate().getPublicBaseUrl();
        } catch (Exception ignored) {
            /* fallback */
        }
        if (base != null && !base.isBlank()) {
            return base.trim().replaceAll("/+$", "");
        }
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.trim().replaceAll("/+$", "");
        }
        return "https://api.icopay.co.kr";
    }

    private static BigDecimal parseAmount(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(v.toString().trim().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static String joinName(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        return (f + " " + l).trim();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String str(Object v) {
        return v != null ? v.toString().trim() : "";
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code != null ? code : "KSNET_ERROR");
        return out;
    }
}
