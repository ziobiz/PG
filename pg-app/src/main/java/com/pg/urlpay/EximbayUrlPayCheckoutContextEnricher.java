package com.pg.urlpay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantProfile;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.PgAgencyRepository;
import com.pg.service.EximbayPaymentMethodCatalog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Eximbay 전용 checkout-context 보강: 결제수단 버튼 목록({@code eximbayPaymentMethods})과 호스티드 모드 표식.
 *
 * <p>노출 결제수단은 {@link EximbayPaymentMethodCatalog#displayOrder()} 기본 순서(카드→PayPay→…)를 쓰되,
 * {@code tb_pg_agency.credentials_extra_json.eximbayMethodsVisible}(쉼표 구분 키 목록)로 제한할 수 있습니다.
 * 프론트는 키만 서버로 보내고, 실제 Eximbay {@code payment_method} 코드 변환은 서버가 담당합니다.
 */
@Component
public class EximbayUrlPayCheckoutContextEnricher implements UrlPayCheckoutContextEnricher {

    private final PgAgencyRepository pgAgencyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EximbayUrlPayCheckoutContextEnricher(PgAgencyRepository pgAgencyRepository) {
        this.pgAgencyRepository = pgAgencyRepository;
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

        String opPg = String.valueOf(data.getOrDefault("urlPayOperationalPgCd", "")).trim();
        Optional<PgAgency> agency = opPg.isEmpty() ? Optional.empty() : pgAgencyRepository.findByPgCd(opPg);

        Set<String> visible = readVisibleKeys(agency);
        List<String> order = EximbayPaymentMethodCatalog.displayOrder();
        List<Map<String, Object>> methods = new ArrayList<>();
        for (String key : order) {
            if (visible != null && !visible.contains(key)) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", key);
            methods.add(m);
        }
        data.put("eximbayPaymentMethods", methods);
        data.put("eximbayHostedWindow", true);
    }

    private Set<String> readVisibleKeys(Optional<PgAgency> agency) {
        if (agency.isEmpty()) {
            return null;
        }
        String raw = agency.get().getCredentialsExtraJson();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode v = root.get("eximbayMethodsVisible");
            if (v == null || v.isNull()) {
                return null;
            }
            String csv = v.asText("").trim();
            if (csv.isEmpty()) {
                return null;
            }
            return Arrays.stream(csv.split(","))
                    .map(s -> EximbayPaymentMethodCatalog.normalizeKey(s.trim()))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        } catch (Exception e) {
            return null;
        }
    }
}
