package com.pg.urlpay;

import com.pg.entity.MerchantDefaultProduct;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantDefaultProductRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
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
    private final OrgBrandingRepository orgBrandingRepository;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final PaymentCurrencyScaleService paymentCurrencyScaleService;
    private final UrlPayCardCopyService urlPayCardCopyService;
    private final UrlPayVendorCapabilityRegistry capabilityRegistry;
    private final List<UrlPayCheckoutContextEnricher> contextEnrichers;

    public UrlPayPublicCheckoutService(ChillPayService chillPayService,
                                       OrgUnitRepository orgUnitRepository,
                                       MerchantProfileRepository merchantProfileRepository,
                                       MerchantDefaultProductRepository merchantDefaultProductRepository,
                                       OrgBrandingRepository orgBrandingRepository,
                                       UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService,
                                       UrlPayDisplayFxService urlPayDisplayFxService,
                                       PaymentCurrencyScaleService paymentCurrencyScaleService,
                                       UrlPayCardCopyService urlPayCardCopyService,
                                       UrlPayVendorCapabilityRegistry capabilityRegistry,
                                       List<UrlPayCheckoutContextEnricher> contextEnrichers) {
        this.chillPayService = chillPayService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.orgBrandingRepository = orgBrandingRepository;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.paymentCurrencyScaleService = paymentCurrencyScaleService;
        this.urlPayCardCopyService = urlPayCardCopyService;
        this.capabilityRegistry = capabilityRegistry;
        this.contextEnrichers = contextEnrichers;
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
        if (dp.isPresent()) {
            MerchantDefaultProduct p = dp.get();
            if (p.getProductName() != null && !p.getProductName().isBlank()) {
                data.put("defaultProductName", p.getProductName().trim());
            }
            if (p.getDefaultAmount() != null) {
                long amt = p.getDefaultAmount().longValue();
                data.put("defaultAmountYen", amt);
                data.put("defaultCheckoutAmount", amt);
                data.put("defaultAmount", p.getDefaultAmount().stripTrailingZeros().toPlainString());
            }
        }
        prof.ifPresent(p -> {
            if (p.getBaseCurrency() != null && !p.getBaseCurrency().isBlank()) {
                data.put("defaultCurrency", p.getBaseCurrency().trim().toUpperCase(Locale.ROOT));
            }
            if (p.getCountryCd() != null && !p.getCountryCd().isBlank()) {
                data.put("defaultCountryIso2", p.getCountryCd().trim().toUpperCase(Locale.ROOT));
            } else if (p.getAddrCountryCd() != null && !p.getAddrCountryCd().isBlank()) {
                data.put("defaultCountryIso2", p.getAddrCountryCd().trim().toUpperCase(Locale.ROOT));
            }
        });
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
        resolveCheckoutHeaderLogoUrl(orgUnitId).ifPresent(u -> data.put("checkoutHeaderLogoUrl", u));
        data.put("urlPayResultPageUrl", chillPayService.resolveUrlPayResultAbsolute(request, ou.get().getCode()));
        data.put("urlPayCheckoutMode", merchantUrlPayCheckoutMode(orgUnitId));
        data.put("effectiveUrlPayVariant", repay ? UrlPayCheckoutModeUtil.REPAY : UrlPayCheckoutModeUtil.STANDARD);
        for (UrlPayCheckoutContextEnricher enricher : contextEnrichers) {
            if (enricher.supports(cap)) {
                enricher.enrich(data, orgUnitId, prof, request);
            }
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
        if (!opPg.isEmpty()) {
            urlPayCardCopyService.resolveActiveCopyByPg(opPg).ifPresent(copy -> data.put("urlPayCardCopy", copy));
        }
    }

    private Optional<String> resolveCheckoutHeaderLogoUrl(Long merchantOrgUnitId) {
        Long cur = merchantOrgUnitId;
        while (cur != null) {
            Optional<OrgUnit> opt = orgUnitRepository.findById(cur);
            if (opt.isEmpty()) {
                break;
            }
            OrgUnit u = opt.get();
            if (u.getOrgLevel() == OrgLevel.MASTER_DIST) {
                return orgBrandingRepository.findByOrgUnitId(u.getId()).flatMap(b -> {
                    String up = b.getUrlPayImageUrl();
                    if (up != null && !up.isBlank()) {
                        return Optional.of(up.trim());
                    }
                    String lg = b.getLogoImageUrl();
                    if (lg != null && !lg.isBlank()) {
                        return Optional.of(lg.trim());
                    }
                    return Optional.empty();
                });
            }
            cur = u.getParentId();
        }
        return Optional.empty();
    }

    private String merchantUrlPayCheckoutMode(Long orgUnitId) {
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(p -> UrlPayCheckoutModeUtil.normalize(p.getUrlPayCheckoutMode()))
                .orElse(UrlPayCheckoutModeUtil.STANDARD);
    }
}
