package com.pg.urlpay;

import com.pg.entity.MerchantDefaultProduct;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantDefaultProductRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.service.MerchantPgBindingRouterService;
import com.pg.service.PayContactRememberPolicyService;
import com.pg.service.PaymentCurrencyScaleService;
import com.pg.service.UrlPayCardCopyService;
import com.pg.service.UrlPayCheckoutCurrencyService;
import com.pg.service.UrlPayDisplayFxService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * PG사 무관 URL 공개 결제 checkout-context 조립.
 * <p>본사 URL결제설정·표시통화(FX)·결제문구·폼 모드는 여기서 일괄 적용하고,
 * PG별 차이는 {@link UrlPayVendorCapabilityRegistry}·{@link UrlPayCheckoutContextEnricher} 로 확장합니다.
 */
@Service
public class UrlPayPublicCheckoutService {

    private final ChillPayService chillPayService;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantDefaultProductRepository merchantDefaultProductRepository;
    private final CheckoutHeaderLogoResolver checkoutHeaderLogoResolver;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final PaymentCurrencyScaleService paymentCurrencyScaleService;
    private final UrlPayCardCopyService urlPayCardCopyService;
    private final UrlPayVendorCapabilityRegistry capabilityRegistry;
    private final List<UrlPayCheckoutContextEnricher> contextEnrichers;
    private final MobileCheckoutModeService mobileCheckoutModeService;
    private final UrlPayInputModeService urlPayInputModeService;
    private final UrlPayCardExpiryModeService urlPayCardExpiryModeService;
    private final PayContactRememberPolicyService payContactRememberPolicyService;
    private final UrlPayCheckoutDisplayPolicyService urlPayCheckoutDisplayPolicyService;
    private final MerchantPgBindingRouterService pgBindingRouter;
    private final CardAuthModeService cardAuthModeService;

    public UrlPayPublicCheckoutService(ChillPayService chillPayService,
                                       OrgUnitRepository orgUnitRepository,
                                       MerchantProfileRepository merchantProfileRepository,
                                       MerchantDefaultProductRepository merchantDefaultProductRepository,
                                       CheckoutHeaderLogoResolver checkoutHeaderLogoResolver,
                                       UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService,
                                       UrlPayDisplayFxService urlPayDisplayFxService,
                                       PaymentCurrencyScaleService paymentCurrencyScaleService,
                                       UrlPayCardCopyService urlPayCardCopyService,
                                       UrlPayVendorCapabilityRegistry capabilityRegistry,
                                       List<UrlPayCheckoutContextEnricher> contextEnrichers,
                                       MobileCheckoutModeService mobileCheckoutModeService,
                                       UrlPayInputModeService urlPayInputModeService,
                                       UrlPayCardExpiryModeService urlPayCardExpiryModeService,
                                       PayContactRememberPolicyService payContactRememberPolicyService,
                                       UrlPayCheckoutDisplayPolicyService urlPayCheckoutDisplayPolicyService,
                                       MerchantPgBindingRouterService pgBindingRouter,
                                       CardAuthModeService cardAuthModeService) {
        this.chillPayService = chillPayService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.checkoutHeaderLogoResolver = checkoutHeaderLogoResolver;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.paymentCurrencyScaleService = paymentCurrencyScaleService;
        this.urlPayCardCopyService = urlPayCardCopyService;
        this.capabilityRegistry = capabilityRegistry;
        this.contextEnrichers = contextEnrichers;
        this.mobileCheckoutModeService = mobileCheckoutModeService;
        this.urlPayInputModeService = urlPayInputModeService;
        this.urlPayCardExpiryModeService = urlPayCardExpiryModeService;
        this.payContactRememberPolicyService = payContactRememberPolicyService;
        this.urlPayCheckoutDisplayPolicyService = urlPayCheckoutDisplayPolicyService;
        this.pgBindingRouter = pgBindingRouter;
        this.cardAuthModeService = cardAuthModeService;
    }

    /**
     * @param repay {@code urlPayVariant=REPAY} 또는 가맹 재결제 모드
     */
    public Map<String, Object> buildCheckoutContext(Long orgUnitId,
                                                    HttpServletRequest request,
                                                    boolean repay) throws IllegalStateException {
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            throw new IllegalStateException("NOT_FOUND");
        }
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        Map<String, Object> data = new LinkedHashMap<>();
        if (repay) {
            data.putAll(chillPayService.getUrlPayRepayPresentationForCheckout(orgUnitId));
        } else {
            data.putAll(chillPayService.getUrlPayPresentationForCheckout(orgUnitId));
        }
        data.put("compId", ou.get().getCode());
        data.put("merchantName", ou.get().getName());
        urlPayCheckoutCurrencyService.resolveFromOrgChain(orgUnitId).ifPresent(cur ->
                data.put("checkoutCurrencyCode", cur.trim().toUpperCase(Locale.ROOT)));
        if (!data.containsKey("checkoutCurrencyCode")) {
            data.put("checkoutCurrencyCode",
                    urlPayCheckoutCurrencyService.resolveCheckoutCurrency(orgUnitId, null));
        }
        Optional<MerchantDefaultProduct> dp = merchantDefaultProductRepository.findByOrgUnitId(orgUnitId);
        if (prof.isPresent()) {
            MerchantProfile p = prof.get();
            urlPayCheckoutDisplayPolicyService.putEffectiveYnIntoMap(data, orgUnitId);
            data.put("checkoutContactRememberEnabled", payContactRememberPolicyService.isEnabledForOrgUnit(orgUnitId));
            if (p.getBaseCurrency() != null && !p.getBaseCurrency().isBlank()) {
                data.put("defaultCurrency", p.getBaseCurrency().trim().toUpperCase(Locale.ROOT));
            }
            if (p.getCountryCd() != null && !p.getCountryCd().isBlank()) {
                data.put("defaultCountryIso2", p.getCountryCd().trim().toUpperCase(Locale.ROOT));
            } else if (p.getAddrCountryCd() != null && !p.getAddrCountryCd().isBlank()) {
                data.put("defaultCountryIso2", p.getAddrCountryCd().trim().toUpperCase(Locale.ROOT));
            }
        } else {
            urlPayCheckoutDisplayPolicyService.putEffectiveYnIntoMap(data, orgUnitId);
            data.put("checkoutContactRememberEnabled", payContactRememberPolicyService.isEnabledForOrgUnit(orgUnitId));
        }
        urlPayInputModeService.putEffectiveIntoMap(data, orgUnitId, request);
        urlPayCardExpiryModeService.putEffectiveIntoMap(data, orgUnitId);
        cardAuthModeService.putEffectiveIntoMap(data, orgUnitId);
        urlPayCheckoutDisplayPolicyService.putEffectiveDefaultProductIntoMap(data, orgUnitId, dp);
        enrichPresentation(data, orgUnitId);
        String opPg = String.valueOf(data.getOrDefault("urlPayOperationalPgCd", ""));
        UrlPayVendorCapability cap = capabilityRegistry.resolve(opPg);
        data.put("urlPayCapabilities", cap.toMap());
        data.put("urlPayCheckoutPagePath", cap.checkoutPagePath());
        data.put("urlPayEmbedPagePath", cap.embedPagePath());
        data.put("urlPayInlineWidgetKind", cap.inlineWidgetKind());
        data.put("urlPayRepayUrlEnabled", cap.repayUrlEnabled());
        if (!data.containsKey("pgVendor")) {
            data.put("pgVendor", cap.vendorFamily());
        }
        checkoutHeaderLogoResolver.applyToCheckoutMap(data, orgUnitId);
        data.put("urlPayResultPageUrl", chillPayService.resolveUrlPayResultAbsolute(request, ou.get().getCode()));
        data.put("urlPayCheckoutMode", merchantUrlPayCheckoutMode(orgUnitId));
        data.put("effectiveUrlPayVariant", repay ? UrlPayCheckoutModeUtil.REPAY : UrlPayCheckoutModeUtil.STANDARD);
        mobileCheckoutModeService.putEffectiveIntoMap(data, orgUnitId);
        for (UrlPayCheckoutContextEnricher enricher : contextEnrichers) {
            if (enricher.supports(cap)) {
                enricher.enrich(data, orgUnitId, prof, request);
            }
        }
        data.put("multiPgRoutingEnabled", pgBindingRouter.isMultiPgRoutingEnabled());
        data.put("multiPgRoutingMode", pgBindingRouter.resolveMultiPgRoutingMode());
        if (pgBindingRouter.isMultiPgRoutingEnabled()) {
            data.put("urlPayOperationalRoutes", pgBindingRouter.listOperationalRouteSummaries(orgUnitId, repay));
        }
        return data;
    }

    /** 표시통화·결제문구·금액 스케일 — 모든 PG 공통 */
    public void enrichPresentation(Map<String, Object> data, Long orgUnitId) {
        if (data == null || orgUnitId == null) {
            return;
        }
        String opPg = String.valueOf(data.getOrDefault("urlPayOperationalPgCd", "")).trim();
        if (opPg.isEmpty()) {
            opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
            if (!opPg.isEmpty()) {
                data.put("urlPayOperationalPgCd", opPg);
            }
        }
        String pricingMode = String.valueOf(data.getOrDefault("urlPayPricingMode", "CHECKOUT_CURRENCY"));
        boolean fxHq = urlPayDisplayFxService.isHqFeatureEnabled();
        data.put("urlPayDisplayFxHqEnabled", fxHq);
        if (UrlPayDisplayFxService.MODE_DISPLAY_FX_THB.equals(pricingMode) && fxHq) {
            data.put("urlPayDisplayFxActive", true);
            data.put("urlPayDisplayFxRefreshSeconds", urlPayDisplayFxService.refreshSeconds());
            String setCur = urlPayDisplayFxService.settlementCurrencyForPg(opPg);
            data.put("urlPaySettlementCurrencyCode", setCur);
            data.put("urlPayDisplayFxDefaultDisplayCurrency", urlPayDisplayFxService.defaultDisplayCurrencyForPg(opPg));
            data.put("urlPayDisplayFxDisplayCurrencyMulti", urlPayDisplayFxService.isDisplayCurrencyMultiForPg(opPg));
            data.put("urlPayDisplayFxDisplayCurrencies", urlPayDisplayFxService.allowedDisplayCurrenciesForCheckout(opPg));
            data.put("urlPayFxUiBlind", urlPayDisplayFxService.isUrlPayFxUiBlind(opPg));
        } else {
            data.put("urlPayDisplayFxActive", false);
            data.put("urlPayFxUiBlind", false);
        }
        Object checkoutCurObj = data.get("checkoutCurrencyCode");
        String checkoutCur = checkoutCurObj instanceof String ? (String) checkoutCurObj : null;
        String scaleCur = checkoutCur;
        if (Boolean.TRUE.equals(data.get("urlPayDisplayFxActive"))) {
            Object scObj = data.get("urlPaySettlementCurrencyCode");
            scaleCur = scObj instanceof String && !((String) scObj).isBlank() ? (String) scObj : "THB";
        }
        String scaleMode = paymentCurrencyScaleService.resolveModeForUi(opPg,
                scaleCur != null && !scaleCur.isBlank() ? scaleCur : "");
        data.put("urlPayAmountScaleMode", scaleMode);
        Map<String, Object> cardCopy = null;
        if (!opPg.isEmpty()) {
            cardCopy = urlPayCardCopyService.resolveActiveCopyByPg(opPg).orElse(null);
        }
        /* 총본사 브랜드 파비콘 — PG 활성 결제구문이 없어도 탭 아이콘 자동 연동 */
        cardCopy = urlPayCardCopyService.mergeHeadquartersBrandFavicon(cardCopy);
        if (cardCopy != null && !cardCopy.isEmpty()) {
            data.put("urlPayCardCopy", cardCopy);
        }
    }

    private String merchantUrlPayCheckoutMode(Long orgUnitId) {
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(p -> UrlPayCheckoutModeUtil.normalize(p.getUrlPayCheckoutMode()))
                .orElse(UrlPayCheckoutModeUtil.STANDARD);
    }
}
