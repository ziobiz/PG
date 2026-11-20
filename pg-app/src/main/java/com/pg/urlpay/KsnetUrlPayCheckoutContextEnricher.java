package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.ksnet.KsnetCredentials;
import com.pg.repository.PgAgencyRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * KSNET 전용 checkout-context 보강 — 해외 USD 전용·호스티드 결제창(ICOPAY 카드폼 없음).
 * 구매자·가맹 응답에는 PG명 미노출({@code pgVendor=ICOPAY}).
 */
@Component
public class KsnetUrlPayCheckoutContextEnricher implements UrlPayCheckoutContextEnricher {

    private final PgAgencyRepository pgAgencyRepository;

    public KsnetUrlPayCheckoutContextEnricher(PgAgencyRepository pgAgencyRepository) {
        this.pgAgencyRepository = pgAgencyRepository;
    }

    @Override
    public boolean supports(UrlPayVendorCapability capability) {
        return capability != null
                && PgVendor.KSNET.equals(capability.vendorFamily())
                && UrlPayInlineWidgetKind.KSNET_SDK.equals(capability.inlineWidgetKind());
    }

    @Override
    public void enrich(Map<String, Object> data,
                       Long orgUnitId,
                       Optional<MerchantProfile> profile,
                       HttpServletRequest request) {
        data.put("pgVendor", "ICOPAY");
        data.put("integrationMode", "INLINE");
        data.put("urlPayFormMode", "FULL");
        /* 해외 USD 전용 — 실결제 통화·결제수단 고정(카드). DP면 enrichPresentation 값이 우선. */
        data.put("overseasUsdOnly", true);
        data.put("hostedPayWindow", true);
        data.put("inlineCardUi", false);
        Object existingSettle = data.get("urlPaySettlementCurrencyCode");
        if (existingSettle == null || String.valueOf(existingSettle).isBlank()) {
            data.put("urlPaySettlementCurrencyCode", "USD");
        }
        String opPg = String.valueOf(data.getOrDefault("urlPayOperationalPgCd", "")).trim();
        Optional<PgAgency> agency = opPg.isEmpty() ? Optional.empty() : pgAgencyRepository.findByPgCd(opPg);
        boolean sandbox = agency.map(a -> KsnetCredentials.fromAgency(a).sandbox()).orElse(true);
        data.put("sandbox", sandbox);
    }
}
