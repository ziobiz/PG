package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.integration.pg.PgVendor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/** ILK checkout-context — 카드 인라인·인증모드. */
@Component
public class IlkUrlPayCheckoutContextEnricher implements UrlPayCheckoutContextEnricher {

    private final CardAuthModeService cardAuthModeService;

    public IlkUrlPayCheckoutContextEnricher(CardAuthModeService cardAuthModeService) {
        this.cardAuthModeService = cardAuthModeService;
    }

    @Override
    public boolean supports(UrlPayVendorCapability capability) {
        return capability != null
                && PgVendor.ILK.equals(capability.vendorFamily())
                && UrlPayInlineWidgetKind.ILK_INLINE.equals(capability.inlineWidgetKind());
    }

    @Override
    public void enrich(Map<String, Object> data,
                       Long orgUnitId,
                       Optional<MerchantProfile> profile,
                       HttpServletRequest request) {
        data.put("integrationMode", "INLINE");
        data.put("urlPayFormMode", "FULL");
        cardAuthModeService.putEffectiveIntoMap(data, orgUnitId);
        data.put("ilkSupports3ds", true);
        data.put("ilkSupportsNone3d", true);
    }
}
