package com.pg.urlpay;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.service.EximbayPaymentMethodCatalog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Eximbay 전용 checkout-context 보강: 결제수단 목록({@code eximbayPaymentMethods}).
 *
 * <p>노출 수단은 본사 결제 라우팅 {@code tb_hq_api_config.eximbay_methods_visible}(가맹은 본사설정 따름).
 * 신용카드만이면 {@code eximbayInlineCardUi=true} — 다른 PG와 동일한 카드번호 입력 UI.
 */
@Component
public class EximbayUrlPayCheckoutContextEnricher implements UrlPayCheckoutContextEnricher {

    private final PgAgencyRepository pgAgencyRepository;
    private final HqApiConfigRepository hqApiConfigRepository;

    public EximbayUrlPayCheckoutContextEnricher(PgAgencyRepository pgAgencyRepository,
                                                HqApiConfigRepository hqApiConfigRepository) {
        this.pgAgencyRepository = pgAgencyRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    @Override
    public boolean supports(UrlPayVendorCapability capability) {
        return capability != null
                && PgVendor.EXIMBAY.equals(capability.vendorFamily())
                && UrlPayInlineWidgetKind.EXIMBAY_SDK.equals(capability.inlineWidgetKind());
    }

    @Override
    public void enrich(Map<String, Object> data,
                       Long orgUnitId,
                       Optional<MerchantProfile> profile,
                       HttpServletRequest request) {
        data.put("pgVendor", PgVendor.EXIMBAY);
        data.put("integrationMode", "INLINE");
        data.put("urlPayFormMode", "FULL");

        String csv = hqApiConfigRepository.findAll().stream().findFirst()
                .map(HqApiConfig::getEximbayMethodsVisible)
                .orElse(EximbayPaymentMethodCatalog.DEFAULT_VISIBLE_CSV);
        List<String> visible = EximbayPaymentMethodCatalog.resolveVisible(csv);
        List<Map<String, Object>> methods = new ArrayList<>();
        for (String key : visible) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", key);
            methods.add(m);
        }
        data.put("eximbayPaymentMethods", methods);
        String opPg = String.valueOf(data.getOrDefault("urlPayOperationalPgCd", "")).trim();
        Optional<PgAgency> agency = opPg.isEmpty() ? Optional.empty() : pgAgencyRepository.findByPgCd(opPg);
        boolean sandbox = agency
                .map(a -> "Y".equalsIgnoreCase(a.getSandboxYn() != null ? a.getSandboxYn().trim() : ""))
                .orElse(false);
        agency.ifPresent(a -> data.put("eximbayPgCd", a.getPgCd()));
        data.put("eximbaySandbox", sandbox);

        boolean cardOnly = EximbayPaymentMethodCatalog.isCardOnly(visible);
        /* 샌드박스는 Eximbay 호스티드 카드창(테스트 카드)으로 승인 연동. 운영만 ICOPAY 카드 입력 UI. */
        boolean inlineCard = cardOnly && !sandbox;
        data.put("eximbayInlineCardUi", inlineCard);
        data.put("eximbayHostedWindow", !inlineCard);
    }
}
