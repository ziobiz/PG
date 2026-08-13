package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.PgAgencyRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ElementPay checkout-context — THB·신용카드(INLINE) 전용.
 */
@Component
public class ElementPayUrlPayCheckoutContextEnricher implements UrlPayCheckoutContextEnricher {

    public ElementPayUrlPayCheckoutContextEnricher() {
    }

    @Override
    public boolean supports(UrlPayVendorCapability capability) {
        return capability != null
                && PgVendor.ELEMENTPAY.equals(capability.vendorFamily())
                && UrlPayInlineWidgetKind.ELEMENTPAY_INLINE.equals(capability.inlineWidgetKind());
    }

    @Override
    public void enrich(Map<String, Object> data,
                       Long orgUnitId,
                       Optional<MerchantProfile> profile,
                       HttpServletRequest request) {
        data.put("integrationMode", "INLINE");
        data.put("urlPayFormMode", "FULL");
        data.put("checkoutCurrencyFixed", "THB");
        /* URL결제: 신용카드만 — JPAY INLINE 과 동일 카드 입력. Light 호스티드 팝업 미사용 */
        List<Map<String, Object>> methods = new ArrayList<>();
        methods.add(method("CARD", "Credit Card"));
        data.put("elementPayPaymentMethods", methods);
        data.put("elementPayCardOnly", true);
        data.put("elementPayHostedWindow", false);
        data.put("elementPayInlineCardUi", true);
        data.put("paymentUiMode", "INLINE");
    }

    private static Map<String, Object> method(String key, String label) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        return m;
    }
}
