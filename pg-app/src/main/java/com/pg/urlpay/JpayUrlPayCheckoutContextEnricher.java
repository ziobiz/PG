package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.integration.pg.PgVendor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * JPAY 전용 checkout-context — 접속국가·국가코드 필수 플래그.
 * 구매자 이메일·전화·국가 노출은 {@link UrlPayCheckoutDisplayPolicyService} 공통 토글을 사용합니다.
 */
@Component
public class JpayUrlPayCheckoutContextEnricher implements UrlPayCheckoutContextEnricher {

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
        /* 공통 putEffectiveYnIntoMap 이 이미 checkoutFieldMode·buyer*UseYn 을 넣음 — JPAY도 동일 */
        data.put("jpayCountryCodeRequired", true);
        data.put("urlPayFormMode", "FULL");
        String visitorIso = VisitorCountryResolver.resolveIso2(request);
        if (!visitorIso.isEmpty()) {
            data.put("visitorCountryIso2", visitorIso);
        }
        data.put("pgVendor", PgVendor.JPAY);
        data.put("integrationMode", "INLINE");
    }
}
