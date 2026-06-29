package com.pg.urlpay;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqApiConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/** JPAY 전용 checkout-context: {@code checkoutFieldMode}·접속국가(국가코드 기본값) 등 */
@Component
public class JpayUrlPayCheckoutContextEnricher implements UrlPayCheckoutContextEnricher {

    private final HqApiConfigRepository hqApiConfigRepository;
    private final UrlPayInputModeService urlPayInputModeService;

    public JpayUrlPayCheckoutContextEnricher(HqApiConfigRepository hqApiConfigRepository,
                                             UrlPayInputModeService urlPayInputModeService) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.urlPayInputModeService = urlPayInputModeService;
    }

    @Override
    public boolean supports(UrlPayVendorCapability capability) {
        return capability != null
                && PgVendor.JPAY.equals(capability.vendorFamily())
                && UrlPayInlineWidgetKind.JPAY_INLINE.equals(capability.inlineWidgetKind());
    }

    @Override
    public void enrich(Map<String, Object> data,
                       Long orgUnitId,
                       Optional<MerchantProfile> profile,
                       HttpServletRequest request) {
        Optional<HqApiConfig> hqOpt = hqApiConfigRepository.findAll().stream().findFirst();
        String hqMode = hqOpt.map(HqApiConfig::getJpayCheckoutFieldMode).orElse(null);
        String merchantMode = profile.map(MerchantProfile::getJpayCheckoutFieldMode).orElse(null);
        String inputMode = data.containsKey("urlPayInputModeEffective")
                ? String.valueOf(data.get("urlPayInputModeEffective"))
                : urlPayInputModeService.resolveEffective(orgUnitId, request);
        inputMode = UrlPayInputModeUtil.normalize(inputMode);
        String resolved = JpayCheckoutFieldModeUtil.resolve(merchantMode, hqMode);
        if (UrlPayInputModeUtil.isMinimalForm(inputMode)) {
            resolved = JpayCheckoutFieldModeUtil.CARD_PREFILL;
        } else if (UrlPayInputModeUtil.forcesFullPresentation(inputMode)) {
            resolved = JpayCheckoutFieldModeUtil.FULL;
        }
        data.put("checkoutFieldMode", resolved);
        data.put("jpayCountryCodeRequired", true);
        /* JPAY 결제창은 1·2·3형(checkoutFieldMode)만 사용 — 본사 urlPayFormMode(SIMPLE) 무시 */
        data.put("urlPayFormMode", "FULL");
        String visitorIso = VisitorCountryResolver.resolveIso2(request);
        if (!visitorIso.isEmpty()) {
            data.put("visitorCountryIso2", visitorIso);
        }
        data.put("pgVendor", PgVendor.JPAY);
        data.put("integrationMode", "INLINE");
    }
}
