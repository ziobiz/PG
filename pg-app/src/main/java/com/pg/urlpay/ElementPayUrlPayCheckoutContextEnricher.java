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
 * ElementPay checkout-context — THB·카드·PromptPay 결제수단 목록.
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
        List<Map<String, Object>> methods = new ArrayList<>();
        methods.add(method("CARD", "Credit Card"));
        methods.add(method("PROMPTPAY", "PromptPay"));
        data.put("elementPayPaymentMethods", methods);
        data.put("elementPayHostedWindow", true);
    }

    private static Map<String, Object> method(String key, String label) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        return m;
    }
}
