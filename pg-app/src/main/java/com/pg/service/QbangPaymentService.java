package com.pg.service;

import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.qbang.QbangApiClient;
import com.pg.integration.pg.qbang.QbangCredentials;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.MerchantPgBindingRouterService.RoutingHint;
import com.pg.util.PgTrnsctnOrderLookup;
import com.pg.util.QrWalletScopeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * QBANG(큐뱅) POS QR — PayPay는 Alipay+ 군(큐뱅 개발 회신).
 * <ul>
 *   <li>흐름: TK → QR → PL(1초·서버·브라우저 병행) → 승인 즉시 OKAY(completedTime=KST)</li>
 *   <li>paymentCompanyId 미전송(지갑 선택 필드 아님). QR은 PayPay 전용이 아니나 ICOPAY 안내는 PayPay</li>
 *   <li>승인 표기: paymentBrand=Alipay+, subWalletName=PayPay</li>
 *   <li>실결제 KRW만. 소수 있으면 원단위 절사(큐뱅). 유효 기본 66초·최대 120초(PL remainingTime)</li>
 * </ul>
 */
@Service
public class QbangPaymentService {

    private static final Logger log = LoggerFactory.getLogger(QbangPaymentService.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter OKAY_COMPLETED_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /**
     * QR 유효 기본(초) — 큐뱅 회신: 결제 요청 시점부터 66초.
     * 최대 120초 연장은 PL {@code remainingTime} 으로 화면·서버가 따름(강제 120 아님).
     */
    public static final int QR_TTL_SECONDS = 66;
    public static final int QR_TTL_MAX_SECONDS = 120;

    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantPgBindingRouterService pgBindingRouter;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final QbangApiClient apiClient;
    private final QbangSaleRecordService qbangSaleRecordService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final QbangNotiMirrorService qbangNotiMirrorService;

    @Value("${pg.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    public QbangPaymentService(OrgUnitRepository orgUnitRepository,
                               PgAgencyRepository pgAgencyRepository,
                               PgTrnsctnRepository pgTrnsctnRepository,
                               MerchantPgBindingRouterService pgBindingRouter,
                               UrlPayChargeResolutionService urlPayChargeResolutionService,
                               UrlPayDisplayFxService urlPayDisplayFxService,
                               QbangApiClient apiClient,
                               QbangSaleRecordService qbangSaleRecordService,
                               MerchantOutboundNotifyService merchantOutboundNotifyService,
                               HqNotifyEnvService hqNotifyEnvService,
                               QbangNotiMirrorService qbangNotiMirrorService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.pgBindingRouter = pgBindingRouter;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.apiClient = apiClient;
        this.qbangSaleRecordService = qbangSaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.qbangNotiMirrorService = qbangNotiMirrorService;
    }

    public Map<String, String> publicNotiIngressUrls() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("pgVendor", PgVendor.QBANG);
        m.put("notiWebhookPath", "/noti/qbang");
        String ingressToken = hqNotifyEnvService.getOrCreate().getIngressToken();
        m.put("icopayIngressBase",
                PgNotifyIngressPaths.buildIngressBase(publicBaseUrl, ingressToken) + "/" + PgVendor.QBANG);
        return m;
    }

    public Optional<ResolvedQbang> resolveBinding(Long orgUnitId, String currency) {
        Optional<MerchantPgBinding> b = pgBindingRouter.resolveOperationalBinding(
                orgUnitId, RoutingHint.qr(QrWalletScopeUtil.QBANG, currency));
        if (b.isEmpty()) {
            return Optional.empty();
        }
        String pgCd = b.get().getPgCd() != null ? b.get().getPgCd().trim() : "";
        if (!PgVendor.isQbangFamily(pgCd)) {
            return Optional.empty();
        }
        Optional<PgAgency> agency = pgAgencyRepository.findByPgCd(pgCd);
        if (agency.isEmpty()) {
            return Optional.empty();
        }
        QbangCredentials creds = QbangCredentials.fromAgencyAndBinding(agency.get(), b.get());
        if (!creds.isUsable()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedQbang(pgCd, b.get(), creds));
    }

    public Map<String, Object> startQr(Long orgUnitId, Map<String, Object> body,
                                      HttpServletRequest request, String clientIp) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        String currencyHint = str(body, "currency", "displayCurrency");
        Optional<ResolvedQbang> resolved = resolveBinding(orgUnitId, currencyHint);
        if (resolved.isEmpty()) {
            return fail(out, "QBANG_NOT_CONFIGURED",
                    "QBANG QR이 설정되지 않았습니다. 본사 결제대행사(QBANG)에 ICOPAY 집계 posId를 넣고, 가맹에 QBANG·QR 바인딩을 등록하세요.");
        }
        String opPg = resolved.get().pgCd();
        QbangCredentials creds = resolved.get().creds();

        UrlPayChargeResolutionService.ResolvedCharge charge;
        try {
            charge = urlPayChargeResolutionService.resolve(orgUnitId, body != null ? body : Map.of(), opPg);
        } catch (IllegalArgumentException e) {
            String code = e.getMessage() != null ? e.getMessage().trim() : "INVALID_AMOUNT";
            return fail(out, code, UrlPayChargeResolutionService.failMessageForCode(code));
        }
        /* 본사 URL결제설정 settlementCurrency(QBANG = KRW)로 맞춘다 */
        String settleCur = charge.settlementCurrency();
        BigDecimal pgAmt = charge.pgAmount();
        BigDecimal shopperAmt = charge.shopperDisplayAmount();
        String shopperCur = charge.shopperDisplayCurrency();
        String targetCur = urlPayDisplayFxService.settlementCurrencyForPg(opPg);
        if (targetCur == null || targetCur.isBlank()) {
            targetCur = "KRW";
        }
        if (settleCur == null || settleCur.isBlank()) {
            settleCur = targetCur;
        }
        boolean displayFxRequest = UrlPayDisplayFxService.MODE_DISPLAY_FX_THB.equalsIgnoreCase(
                str(body, "urlPayPricingMode"));
        /* STANDARD·실입력 KRW만 그대로 청구. DP(표시 JPY + currency=KRW)는 견적 결과를 덮어쓰지 않음 */
        String bodyCur = str(body, "currency");
        String bodyDispCur = str(body, "displayCurrency");
        BigDecimal bodyAmt = null;
        try {
            String rawAmt = str(body, "amount", "displayAmount");
            if (!rawAmt.isBlank()) {
                bodyAmt = new BigDecimal(rawAmt.trim());
            }
        } catch (Exception ignored) {
            bodyAmt = null;
        }
        boolean bodyIsTrueKrw = !displayFxRequest
                && "KRW".equalsIgnoreCase(bodyCur)
                && (bodyDispCur.isBlank() || "KRW".equalsIgnoreCase(bodyDispCur))
                && bodyAmt != null
                && bodyAmt.compareTo(BigDecimal.ZERO) > 0;
        if (bodyIsTrueKrw) {
            if (shopperAmt == null) {
                shopperAmt = pgAmt;
                shopperCur = settleCur;
            }
            pgAmt = urlPayDisplayFxService.applyChargeAmountRound("KRW", bodyAmt);
            settleCur = "KRW";
        } else if (!targetCur.equalsIgnoreCase(settleCur)) {
            /* BOT → 본사 URL결제 MANUAL 폴백 → (from→)THB→KRW agency 폴백 */
            Optional<BigDecimal> conv = urlPayDisplayFxService.convertAmountForOperationalPg(
                    pgAmt, settleCur, targetCur, opPg);
            if (conv.isEmpty() && "KRW".equalsIgnoreCase(targetCur)) {
                BigDecimal rate = agencyThbToKrwFallbackRate(opPg);
                if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                    if ("THB".equalsIgnoreCase(settleCur)) {
                        conv = Optional.of(pgAmt.multiply(rate));
                        log.warn("QBANG FX fallback THB→KRW rate={} sandbox={} orderHint={}",
                                rate, creds.sandbox(), str(body, "orderNo"));
                    } else {
                        /* BOT에 KRW만 없을 때: JPY/USD 등 → THB(BOT) → KRW(폴백) */
                        Optional<BigDecimal> thbAmt = urlPayDisplayFxService.convertAmount(
                                pgAmt, settleCur, "THB");
                        if (thbAmt.isPresent()) {
                            conv = Optional.of(thbAmt.get().multiply(rate));
                            log.warn("QBANG FX bridge {}→THB→KRW rate={} sandbox={} orderHint={}",
                                    settleCur, rate, creds.sandbox(), str(body, "orderNo"));
                        }
                    }
                }
            }
            if (conv.isEmpty()) {
                log.warn("QBANG_FX from={} to={} amt={} pg={} sandbox={} orderHint={}",
                        settleCur, targetCur, pgAmt, opPg, creds.sandbox(), str(body, "orderNo"));
                return fail(out, "QBANG_FX",
                        "QBANG 실결제 통화(" + targetCur + ") 환전에 실패했습니다. 본사 URL결제설정(QBANG)에서 FX 자동(BOT)·수동 환율 또는 표시통화·실결제(KRW)를 확인하세요.");
            }
            if (shopperAmt == null) {
                shopperAmt = pgAmt;
                shopperCur = settleCur;
            }
            pgAmt = urlPayDisplayFxService.applyChargeAmountRound(targetCur, conv.get());
            settleCur = targetCur;
        }
        /* 큐뱅: 거래 통화 KRW만. 원미만 처리는 본사 URL결제 「통화별 커스텀」(활성 시) / 정책 기본값 */
        if (!"KRW".equalsIgnoreCase(settleCur)) {
            return fail(out, "QBANG_CURRENCY",
                    "QBANG QR 실결제는 KRW만 가능합니다. 본사 URL결제 실결제 통화를 KRW로 설정하세요.");
        }
        settleCur = "KRW";
        pgAmt = urlPayDisplayFxService.applyChargeAmountRound("KRW", pgAmt);
        /* 큐뱅 회신: KRW만·소수 절사. HQ 라운딩 후 API 전송 전 원단위 DOWN */
        if (pgAmt != null) {
            pgAmt = pgAmt.setScale(0, RoundingMode.DOWN);
        }
        if (pgAmt == null || pgAmt.compareTo(BigDecimal.ZERO) <= 0) {
            return fail(out, "INVALID_AMOUNT", "결제 금액이 올바르지 않습니다.");
        }
        /* 입력통화 미기록 방지: body 표시·입력 통화가 KRW와 다르면 shopper 필드로 보존 */
        if ((shopperAmt == null || shopperCur == null || shopperCur.isBlank())
                && bodyAmt != null && bodyAmt.compareTo(BigDecimal.ZERO) > 0) {
            String hintCur = !bodyDispCur.isBlank() ? bodyDispCur
                    : (!bodyCur.isBlank() ? bodyCur : "");
            if (!hintCur.isBlank() && !"KRW".equalsIgnoreCase(hintCur)) {
                shopperAmt = bodyAmt;
                shopperCur = hintCur.trim().toUpperCase(java.util.Locale.ROOT);
            }
        }
        String orderNo = str(body, "orderNo", "order_no", "reference");
        if (orderNo.isBlank()) {
            /* URL QR: 가맹/구매자 입력 없이 서버에서 qr+epoch 자동 채번 */
            orderNo = "QR" + System.currentTimeMillis();
        }

        Map<String, Object> tk = apiClient.requestToken(creds);
        if (!Boolean.TRUE.equals(tk.get("ok"))) {
            return fail(out, strDef(tk.get("errorCode"), "QBANG_TK"),
                    strDef(tk.get("message"), "QBANG Token 요청에 실패했습니다."));
        }
        String key = str(tk.get("key"));
        String iv = str(tk.get("iv"));
        String txnNo = str(tk.get("transactionNo"));
        if (key.isBlank() || iv.isBlank() || txnNo.isBlank()) {
            return fail(out, "QBANG_TK", "Token 응답에 key/iv/transactionNo가 없습니다.");
        }

        BigDecimal vat = BigDecimal.ZERO;
        String vatRaw = str(body, "vat", "vatAmount");
        if (!vatRaw.isBlank()) {
            try {
                vat = new BigDecimal(vatRaw);
            } catch (Exception ignored) {
                vat = BigDecimal.ZERO;
            }
        } else if ("KRW".equalsIgnoreCase(settleCur)) {
            vat = pgAmt.multiply(new BigDecimal("10"))
                    .divide(new BigDecimal("110"), 0, RoundingMode.DOWN);
        }
        if (vat != null) {
            vat = vat.setScale(0, RoundingMode.DOWN);
        }

        String productName = str(body, "productName", "item");
        Map<String, Object> qr = apiClient.requestQr(creds, key, iv, txnNo,
                settleCur, pgAmt, vat, productName);
        if (!Boolean.TRUE.equals(qr.get("ok"))) {
            return fail(out, strDef(qr.get("errorCode"), "QBANG_QR"),
                    strDef(qr.get("message"), "QBANG QR 요청에 실패했습니다."));
        }
        String qrCode = str(qr.get("qrCode")).replaceAll("\\s+", "");
        if (qrCode.isBlank()) {
            return fail(out, "QBANG_QR", "qrCode가 없습니다.");
        }
        String step2Qr = str(qr.get("step2QrCode")).replaceAll("\\s+", "");
        String payCo = str(qr.get("paymentCompanyCode"));
        if (payCo.isBlank() && qr.get("json") instanceof com.fasterxml.jackson.databind.JsonNode jn) {
            payCo = jn.path("data").path("paymentCompanyCode").asText("").trim();
        }
        log.info("QBANG QR payload mid={} order={} txn={} qrLen={} qrPrefix={} payCo={} step2Len={}",
                ouOpt.get().getCode(), orderNo, txnNo, qrCode.length(),
                qrCode.length() > 24 ? qrCode.substring(0, 24) : qrCode,
                payCo.isBlank() ? "-" : payCo,
                step2Qr.length());

        String buyerName = str(body, "customerNm", "buyerName", "buyer", "payerName", "name");
        if (buyerName.isBlank()) {
            String fn = str(body, "firstName", "buyerFirstName", "givenName");
            String ln = str(body, "lastName", "buyerLastName", "familyName");
            buyerName = (fn + " " + ln).trim();
        }
        String buyerEmail = str(body, "payEmailAddress", "buyerEmail", "email");
        String buyerTel = str(body, "payTelephone", "customerTel", "buyerTel", "phone", "buyerPhone");
        qbangSaleRecordService.recordOrTouchPending(
                orgUnitId, orderNo, pgAmt, settleCur,
                productName, txnNo, str(body, "txnOrigin", "origin"),
                shopperAmt, shopperCur,
                buyerName, buyerEmail, buyerTel);

        out.put("success", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("transactionNo", txnNo);
        data.put("qrCode", qrCode);
        if (!step2Qr.isBlank()) {
            data.put("step2QrCode", step2Qr);
        }
        if (!payCo.isBlank()) {
            data.put("paymentCompanyCode", payCo);
        }
        data.put("amount", pgAmt);
        data.put("currency", settleCur);
        if (shopperAmt != null && shopperCur != null && !shopperCur.isBlank()) {
            data.put("shopperDisplayAmount", shopperAmt);
            data.put("shopperDisplayCurrency", shopperCur);
        }
        data.put("paymentStatus", "PENDING_QR");
        data.put("remainingTimeHint", QR_TTL_SECONDS);
        data.put("remainingTimeMaxHint", QR_TTL_MAX_SECONDS);
        data.put("pollIntervalMs", 1000);
        data.put("pgVendor", "ICOPAY");
        data.put("noti", publicNotiIngressUrls());
        out.put("data", data);
        log.info("QBANG QR started mid={} order={} txn={} ip={}",
                ouOpt.get().getCode(), orderNo, txnNo, clientIp);
        return out;
    }

    /**
     * PL 폴링. actionFlag=3 이고 승인 상태면 OKAY 후 거래 확정.
     */
    public Map<String, Object> pollAndMaybeConfirm(Long orgUnitId, String orderNo, String transactionNo) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null || transactionNo == null || transactionNo.isBlank()) {
            return fail(out, "INVALID", "transactionNo가 필요합니다.");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail(out, "NOT_FOUND", "가맹점을 찾을 수 없습니다.");
        }
        Optional<ResolvedQbang> resolved = resolveBinding(orgUnitId, "");
        if (resolved.isEmpty()) {
            return fail(out, "QBANG_NOT_CONFIGURED", "QBANG 설정이 없습니다.");
        }
        QbangCredentials creds = resolved.get().creds();
        Map<String, Object> pl = apiClient.pollStatus(creds, transactionNo.trim());
        if (!Boolean.TRUE.equals(pl.get("ok"))) {
            return fail(out, strDef(pl.get("errorCode"), "QBANG_PL"),
                    strDef(pl.get("message"), "상태 조회에 실패했습니다."));
        }

        int actionFlag = pl.get("actionFlag") instanceof Number n ? n.intValue() : 0;
        String state = str(pl.get("state"));
        int remaining = pl.get("remainingTime") instanceof Number n ? n.intValue() : 0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionNo", transactionNo.trim());
        data.put("orderNo", orderNo);
        data.put("state", state);
        data.put("stage", str(pl.get("stage")));
        data.put("actionFlag", actionFlag);
        data.put("remainingTime", remaining);
        data.put("paymentBrand", str(pl.get("paymentBrand")));
        data.put("paymentBrandName", str(pl.get("paymentBrandName")));
        data.put("subWalletName", str(pl.get("subWalletName")));
        data.put("approvalNo", str(pl.get("approvalNo")));
        String step2 = str(pl.get("step2QrCode"));
        if (!step2.isBlank()) {
            data.put("step2QrCode", step2);
            data.put("step2CompanyCode", str(pl.get("step2CompanyCode")));
            data.put("step2CompanyName", str(pl.get("step2CompanyName")));
        }

        /* F·E: 이미 실패 — AT 불필요(큐뱅 회신: remainingTime=0·미확정/진행중 취소 시 AT) */
        if ("F".equalsIgnoreCase(state) || "E".equalsIgnoreCase(state)) {
            if (orderNo != null && !orderNo.isBlank()) {
                qbangSaleRecordService.applyFailedOutcome(orderNo, ouOpt.get().getCode(), transactionNo);
            }
            data.put("paymentStatus", "FAILED");
            out.put("success", true);
            out.put("data", data);
            return out;
        }
        /* remainingTime=0 이고 승인(C / actionFlag=3) 아님 → AT (승인 건 AT는 5분 내 망취소 가능 — 승인 전만) */
        if (remaining <= 0 && actionFlag != 3 && !"C".equalsIgnoreCase(state)) {
            if (orderNo != null && !orderNo.isBlank()) {
                qbangSaleRecordService.applyFailedOutcome(orderNo, ouOpt.get().getCode(), transactionNo);
            }
            try {
                apiClient.abortNetwork(creds, transactionNo.trim());
            } catch (Exception ignored) {
                /* best effort */
            }
            data.put("paymentStatus", "FAILED");
            out.put("success", true);
            out.put("data", data);
            return out;
        }

        if (actionFlag == 3 || "C".equalsIgnoreCase(state)) {
            /* PL로 승인 확인한 시각 = completedTime (KST). 즉시 OKAY — 미호출 시 내부 망취소 */
            String completedTime = OKAY_COMPLETED_FMT.format(LocalDateTime.now(KST));
            Map<String, Object> okay = apiClient.confirmOkay(creds, transactionNo.trim(), completedTime);
            if (!Boolean.TRUE.equals(okay.get("ok"))) {
                /* OKAY 실패 시 로컬 승인 확정 보류 — 서버 폴러가 재시도(미호출 시 망취소 위험) */
                log.warn("QBANG OKAY pending retry txn={} completedTime={} msg={}",
                        transactionNo, completedTime, okay.get("message"));
                data.put("paymentStatus", "PENDING");
                data.put("okayPending", true);
                out.put("success", true);
                out.put("data", data);
                return out;
            }
            String appr = str(pl.get("approvalNo"));
            String brand = str(pl.get("paymentBrand"));
            String subWallet = str(pl.get("subWalletName"));
            if (!subWallet.isBlank()) {
                brand = brand.isBlank() ? subWallet : (brand + "/" + subWallet);
            }
            String on = orderNo;
            if ((on == null || on.isBlank()) && transactionNo != null) {
                Optional<PgTrnsctn> byTxn = pgTrnsctnRepository
                        .findFirstByChillTransactionIdOrderByCreatedAtDesc(transactionNo.trim());
                if (byTxn.isPresent() && PgVendor.isQbangFamily(byTxn.get().getVan())) {
                    on = byTxn.get().getOrderNo();
                }
            }
            if (on != null && !on.isBlank()) {
                Optional<PgTrnsctn> paid = qbangSaleRecordService.applyPaidOutcome(
                        on, ouOpt.get().getCode(), transactionNo, appr, brand);
                paid.ifPresent(t -> {
                    try {
                        merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "QBANG_PL");
                    } catch (Exception e) {
                        log.warn("QBANG merchant notify: {}", e.getMessage());
                    }
                    try {
                        qbangNotiMirrorService.schedulePaidMirrorAfterCommit(t);
                    } catch (Exception e) {
                        log.warn("QBANG NOTI mirror: {}", e.getMessage());
                    }
                });
            }
            data.put("paymentStatus", "PAID");
            data.put("orderNo", on);
            out.put("success", true);
            out.put("data", data);
            return out;
        }

        data.put("paymentStatus", "PENDING");
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public record ResolvedQbang(String pgCd, MerchantPgBinding binding, QbangCredentials creds) {
    }

    /**
     * BOT·본사 MANUAL 모두 불가할 때 THB→KRW 폴백.
     * agency {@code credentials_extra_json.sandboxThbToKrw} / {@code thbToKrw} 우선, 없으면 40
     * (LIVE에서도 BOT 장애 시 QR이 막히지 않도록 동일 폴백 허용).
     */
    private BigDecimal agencyThbToKrwFallbackRate(String pgCd) {
        try {
            Optional<PgAgency> ag = pgAgencyRepository.findByPgCd(pgCd != null ? pgCd.trim() : "");
            if (ag.isPresent() && ag.get().getCredentialsExtraJson() != null) {
                String extra = ag.get().getCredentialsExtraJson().trim();
                if (extra.startsWith("{")) {
                    com.fasterxml.jackson.databind.JsonNode n =
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree(extra);
                    for (String key : new String[] {"thbToKrw", "sandboxThbToKrw"}) {
                        if (n.has(key) && !n.get(key).isNull()) {
                            BigDecimal r = new BigDecimal(n.get(key).asText().trim());
                            if (r.compareTo(BigDecimal.ZERO) > 0) {
                                return r;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            /* default below */
        }
        return new BigDecimal("40");
    }

    private static Map<String, Object> fail(Map<String, Object> out, String code, String message) {
        out.put("success", false);
        out.put("errorCode", code);
        out.put("message", message);
        return out;
    }

    private static String str(Object o) {
        return o != null ? String.valueOf(o).trim() : "";
    }

    private static String strDef(Object o, String fallback) {
        String s = str(o);
        return s.isBlank() ? (fallback != null ? fallback : "") : s;
    }

    private static String str(Map<String, Object> body, String... keys) {
        if (body == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            Object v = body.get(k);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isBlank() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return "";
    }
}
