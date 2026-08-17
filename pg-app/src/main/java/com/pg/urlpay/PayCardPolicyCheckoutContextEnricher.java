package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.service.PayCardPolicyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class PayCardPolicyCheckoutContextEnricher implements UrlPayCheckoutContextEnricher {

    private final PayCardPolicyService payCardPolicyService;

    public PayCardPolicyCheckoutContextEnricher(PayCardPolicyService payCardPolicyService) {
        this.payCardPolicyService = payCardPolicyService;
    }

    @Override
    public boolean supports(UrlPayVendorCapability capability) {
        return capability != null && capability.saleChannel() != UrlPaySaleChannel.NOT_REGISTERED;
    }

    @Override
    public void enrich(Map<String, Object> data,
                       Long orgUnitId,
                       Optional<MerchantProfile> profile,
                       HttpServletRequest request) {
        String opPg = String.valueOf(data.getOrDefault("urlPayOperationalPgCd", ""));
        data.put("cardPayPolicy", payCardPolicyService.buildClientPolicy(opPg, orgUnitId));
    }
}
