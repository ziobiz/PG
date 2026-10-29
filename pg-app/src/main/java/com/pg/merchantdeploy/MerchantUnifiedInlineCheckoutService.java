package com.pg.merchantdeploy;

import com.pg.crypto.CryptoCheckoutService;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.MerchantPayLimitService;
import com.pg.service.MerchantPgBindingRouterService;
import com.pg.service.UrlPayChargeResolutionService;
import com.pg.splitpay.SplitPayCheckoutModeGuard;
import com.pg.urlpay.MobileCheckoutModeService;
import com.pg.urlpay.CheckoutFailI18n;
import com.pg.urlpay.IcipayBuyerContactUtil;
import com.pg.urlpay.NeutralCheckoutRoute;
import com.pg.urlpay.QrPaymentMeansService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * PG 무관 통합 가맹 인라인 checkout — 운영 WEB PG({@link ChillPayService#resolveUrlPayOperationalPgCd})에 따라
 * ChillPay/JPAY prepare 로 위임합니다.
 */
@Service
public class MerchantUnifiedInlineCheckoutService {

    private final ChillPayService chillPayService;
    private final MerchantInlineCheckoutService chillpayInlineCheckoutService;
    private final MerchantJpayInlineCheckoutService jpayInlineCheckoutService;
    private final MerchantEximbayInlineCheckoutService eximbayInlineCheckoutService;
    private final MerchantElementPayInlineCheckoutService elementPayInlineCheckoutService;
    private final MerchantIlkInlineCheckoutService ilkInlineCheckoutService;
    private final MerchantOxInlineCheckoutService oxInlineCheckoutService;
    private final MerchantQxipayInlineCheckoutService qxipayInlineCheckoutService;
    private final MerchantOmspayInlineCheckoutService omspayInlineCheckoutService;
    private final CryptoCheckoutService cryptoCheckoutService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final MerchantChatbotProductService productService;
    private final MerchantApiIntegrationChannelService integrationChannelService;

    private final SplitPayCheckoutModeGuard splitPayCheckoutModeGuard;
    private final MobileCheckoutModeService mobileCheckoutModeService;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantPayLimitService merchantPayLimitService;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final MerchantPgBindingRouterService pgBindingRouter;
    private final QrPaymentMeansService qrPaymentMeansService;

    public MerchantUnifiedInlineCheckoutService(ChillPayService chillPayService,
                                                MerchantInlineCheckoutService chillpayInlineCheckoutService,
                                                MerchantJpayInlineCheckoutService jpayInlineCheckoutService,
                                                MerchantEximbayInlineCheckoutService eximbayInlineCheckoutService,
                                                MerchantElementPayInlineCheckoutService elementPayInlineCheckoutService,
                                                MerchantIlkInlineCheckoutService ilkInlineCheckoutService,
                                                MerchantOxInlineCheckoutService oxInlineCheckoutService,
                                                MerchantQxipayInlineCheckoutService qxipayInlineCheckoutService,
                                                MerchantOmspayInlineCheckoutService omspayInlineCheckoutService,
                                                CryptoCheckoutService cryptoCheckoutService,
                                                MerchantInlineCheckoutTokenService tokenService,
                                                MerchantChatbotProductService productService,
                                                MerchantApiIntegrationChannelService integrationChannelService,
                                                SplitPayCheckoutModeGuard splitPayCheckoutModeGuard,
                                                MobileCheckoutModeService mobileCheckoutModeService,
                                                OrgUnitRepository orgUnitRepository,
                                                MerchantPayLimitService merchantPayLimitService,
                                                UrlPayChargeResolutionService urlPayChargeResolutionService,
                                                MerchantPgBindingRouterService pgBindingRouter,
                                                QrPaymentMeansService qrPaymentMeansService) {
        this.chillPayService = chillPayService;
        this.chillpayInlineCheckoutService = chillpayInlineCheckoutService;
        this.jpayInlineCheckoutService = jpayInlineCheckoutService;
        this.eximbayInlineCheckoutService = eximbayInlineCheckoutService;
        this.elementPayInlineCheckoutService = elementPayInlineCheckoutService;
        this.ilkInlineCheckoutService = ilkInlineCheckoutService;
        this.oxInlineCheckoutService = oxInlineCheckoutService;
        this.qxipayInlineCheckoutService = qxipayInlineCheckoutService;
        this.omspayInlineCheckoutService = omspayInlineCheckoutService;
        this.cryptoCheckoutService = cryptoCheckoutService;
        this.tokenService = tokenService;
        this.productService = productService;
        this.integrationChannelService = integrationChannelService;
        this.splitPayCheckoutModeGuard = splitPayCheckoutModeGuard;
        this.mobileCheckoutModeService = mobileCheckoutModeService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantPayLimitService = merchantPayLimitService;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.pgBindingRouter = pgBindingRouter;
        this.qrPaymentMeansService = qrPaymentMeansService;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        Optional<Map<String, Object>> splitDeny = splitPayCheckoutModeGuard.denyInlineOneShotPrepare(orgUnitId);
        if (splitDeny.isPresent()) {
            return splitDeny.get();
        }
        Optional<String> inlineDeny = integrationChannelService.denyMessage(orgUnitId,
                MerchantApiIntegrationChannelService.Channel.API_BROKER_INLINE);
        if (inlineDeny.isPresent()) {
            return fail(inlineDeny.get(), MerchantApiIntegrationChannelService.CODE_INTEGRATION_CHANNEL_DISABLED);
        }
        Map<String, String> buyer;
        try {
            buyer = IcipayBuyerContactUtil.extractAndValidateRequired(body);
        } catch (IcipayBuyerContactUtil.BuyerRequiredException ex) {
            return ex.toFailMap();
        } catch (IllegalArgumentException ex) {
            return CheckoutFailI18n.buyerRequiredGeneric();
        }

        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        Map<String, Object> enriched = new LinkedHashMap<>(body != null ? body : Map.of());
        enriched.put("buyerPrefill", IcipayBuyerContactUtil.toPublicMap(buyer));

        String payMeans = firstStr(enriched, "payMeans", "checkoutMeans", "paymentMeans").toUpperCase();
        boolean wantCrypto = "CRYPTO".equals(payMeans) || PgVendor.isCryptoFamily(opPg);
        if (wantCrypto && cryptoCheckoutService.cryptoMeansAllowed(orgUnitId)) {
            Map<String, Object> cryptoResult = cryptoCheckoutService.prepare(orgUnitId, enriched, request);
            patchUnifiedPrepareResponse(cryptoResult, request, PgVendor.CRYPTO, orgUnitId);
            return cryptoResult;
        }

        String qrWallet = firstStr(enriched, "qrWallet", "qrWalletCode");
        boolean wantQr = "QR".equals(payMeans) || !qrWallet.isBlank();
        if (wantQr && qrPaymentMeansService.qrMeansAllowed(orgUnitId)) {
            if (qrWallet.isBlank()) {
                var wallets = qrPaymentMeansService.listOperationalQrWalletsIfAllowed(orgUnitId);
                qrWallet = wallets.isEmpty() ? "" : wallets.get(0);
                enriched.put("qrWallet", qrWallet);
            }
            enriched.put("payMeans", "QR");
            String qrPg = pgBindingRouter.resolveOperationalPgCd(
                    orgUnitId, MerchantPgBindingRouterService.RoutingHint.qr(qrWallet,
                            firstStr(enriched, "currency", "displayCurrency")));
            if (qrPg != null && !qrPg.isBlank()) {
                try {
                    UrlPayChargeResolutionService.ResolvedCharge charge =
                            urlPayChargeResolutionService.resolve(orgUnitId, enriched, qrPg);
                    String lang = firstStr(enriched, "lang", "langCode", "language");
                    Optional<Map<String, Object>> limitBlock =
                            merchantPayLimitService.check(orgUnitId, charge.pgAmount(), charge.settlementCurrency(), lang);
                    if (limitBlock.isPresent()) {
                        return limitBlock.get();
                    }
                } catch (IllegalArgumentException ignored) {
                    /* 금액 오류는 ILK prepare 에서 처리 */
                }
            }
            Map<String, Object> qrResult = ilkInlineCheckoutService.prepare(orgUnitId, enriched, request);
            patchUnifiedPrepareResponse(qrResult, request, PgVendor.ILK, orgUnitId);
            appendPayMeans(qrResult, "QR");
            return qrResult;
        }

        if (opPg == null || opPg.isBlank()) {
            return fail("URL 결제 운영 PG가 설정되지 않았습니다.", "URL_PAYMENT_PG_MISSING");
        }

        try {
            UrlPayChargeResolutionService.ResolvedCharge charge =
                    urlPayChargeResolutionService.resolve(orgUnitId, enriched, opPg);
            String lang = firstStr(enriched, "lang", "langCode", "language");
            Optional<Map<String, Object>> limitBlock =
                    merchantPayLimitService.check(orgUnitId, charge.pgAmount(), charge.settlementCurrency(), lang);
            if (limitBlock.isPresent()) {
                return limitBlock.get();
            }
        } catch (IllegalArgumentException ignored) {
            /* 금액 오류는 각 PG prepare 에서 처리 */
        }

        Map<String, Object> result;
        if (PgVendor.isJpayFamily(opPg)) {
            result = jpayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isEximbayFamily(opPg)) {
            result = eximbayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isElementPayFamily(opPg)) {
            result = elementPayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isIlkFamily(opPg)) {
            result = ilkInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isOxFamily(opPg)) {
            result = oxInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isQxipayFamily(opPg)) {
            result = qxipayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isOmspayFamily(opPg)) {
            result = omspayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isCryptoFamily(opPg)) {
            result = cryptoCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isChillPayFamily(opPg)) {
            result = chillpayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else {
            return fail("지원하지 않는 결제 구성입니다.", "PG_NOT_SUPPORTED");
        }

        patchUnifiedPrepareResponse(result, request, opPg, orgUnitId);
        return result;
    }

    public Map<String, Object> readSession(String token, HttpServletRequest request) {
        if (token != null && token.startsWith("cr_")) {
            return cryptoCheckoutService.session(token);
        }
        return tokenService.parseValid(token)
                .map(session -> {
                    Map<String, Object> data = new LinkedHashMap<>(session.toPublicMap());
                    data.put("sessionToken", token);
                    orgUnitRepository.findByCode(session.compId()).ifPresent(ou ->
                            mobileCheckoutModeService.enrichInlineSession(data, ou.getId(), request));
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("success", true);
                    out.put("data", data);
                    return out;
                })
                .orElseGet(() -> fail("세션이 유효하지 않거나 만료되었습니다.", "INVALID_SESSION"));
    }

    public Map<String, Object> orderStatus(Long orgUnitId, String orderNo) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (opPg == null || opPg.isBlank()) {
            return fail("URL 결제 운영 PG가 설정되지 않았습니다.", "URL_PAYMENT_PG_MISSING");
        }
        if (PgVendor.isJpayFamily(opPg)) {
            return jpayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isEximbayFamily(opPg)) {
            return eximbayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isElementPayFamily(opPg)) {
            return elementPayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isIlkFamily(opPg)) {
            return ilkInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isOxFamily(opPg)) {
            return oxInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isQxipayFamily(opPg)) {
            return qxipayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isOmspayFamily(opPg)) {
            return omspayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isCryptoFamily(opPg)) {
            return cryptoCheckoutService.status(null, orderNo, orgUnitId);
        }
        if (cryptoCheckoutService.cryptoMeansAllowed(orgUnitId)) {
            Map<String, Object> cryptoSt = cryptoCheckoutService.status(null, orderNo, orgUnitId);
            Object ok = cryptoSt.get("success");
            if (ok instanceof Boolean && (Boolean) ok) {
                return cryptoSt;
            }
        }
        if (PgVendor.isChillPayFamily(opPg)) {
            return chillpayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        return fail("지원하지 않는 결제 구성입니다.", "PG_NOT_SUPPORTED");
    }

    @SuppressWarnings("unchecked")
    private void patchUnifiedPrepareResponse(Map<String, Object> result, HttpServletRequest request,
                                             String opPg, Long orgUnitId) {
        Object ok = result.get("success");
        if (!(ok instanceof Boolean) || !(Boolean) ok) {
            return;
        }
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            return;
        }
        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        String compId = str(data.get("compId"));
        if (compId.isBlank()) {
            return;
        }
        String embedPath = "/v1/embed-checkout/" + compId;
        data.put("embedScriptUrl", base.isEmpty() ? embedPath : base + embedPath);
        data.put("inlineCheckoutPrepareUrl",
                base + "/api/middleware/v1/merchant/checkout/prepare");
        // 가맹점·구매자에게 PG(ChillPay/JPAY/Eximbay)를 노출하지 않는 중립 결제창 경로로 통일
        String sessionToken = str(data.get("sessionToken"));
        data.put("payUrl", NeutralCheckoutRoute.buildPayUrl(base, compId, sessionToken, str(data.get("langCode")), true));
        // 실제 PG 는 응답에 노출하지 않는다 — 항상 ICOPAY (operationalPgCd 미노출)
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("integrationMode", "INLINE_UNIFIED");
        if (orgUnitId != null) {
            mobileCheckoutModeService.putEffectiveIntoMap(data, orgUnitId);
        }
        data.put("embedUsageHint",
                "ICOPAY 통합 prepare: buyer(email·phone·countryIso2) 필수. sessionToken → /v1/embed-checkout/{compId}. "
                        + "결제망은 ICOPAY가 자동 선택·처리하며, 가맹점 연동은 결제 대행사 변경과 무관하게 그대로 유지됩니다.");
    }

    @SuppressWarnings("unchecked")
    private static void appendPayMeans(Map<String, Object> result, String payMeans) {
        if (result == null || payMeans == null || payMeans.isBlank()) {
            return;
        }
        Object dataObj = result.get("data");
        if (!(dataObj instanceof Map<?, ?> raw)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) raw;
        String payUrl = str(data.get("payUrl"));
        if (payUrl.isBlank() || payUrl.contains("payMeans=")) {
            data.put("payMeans", payMeans);
            return;
        }
        data.put("payUrl", payUrl + (payUrl.contains("?") ? "&" : "?") + "payMeans=" + payMeans);
        data.put("payMeans", payMeans);
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String firstStr(Map<String, Object> body, String... keys) {
        if (body == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            Object v = body.get(k);
            if (v != null) {
                String s = v.toString().trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return "";
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
