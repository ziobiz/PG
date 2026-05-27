package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.MerchantProfile;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.PgAgencyRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** JPAY API 구독 — 본사·가맹·PG 바인딩 게이트. */
@Service
public class JpaySubscriptionConfigService {

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final PgAgencyRepository pgAgencyRepository;

    public JpaySubscriptionConfigService(HqApiConfigRepository hqApiConfigRepository,
                                         MerchantProfileRepository merchantProfileRepository,
                                         MerchantPgBindingRepository merchantPgBindingRepository,
                                         PgAgencyRepository pgAgencyRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.pgAgencyRepository = pgAgencyRepository;
    }

    public Optional<HqApiConfig> hqConfig() {
        return hqApiConfigRepository.findAll().stream().findFirst();
    }

    public boolean isHqSubscriptionEnabled() {
        return hqConfig()
                .map(c -> "Y".equalsIgnoreCase(str(c.getJpaySubscriptionEnabledYn())))
                .orElse(false);
    }

    public boolean isHqSubscriptionInlineEnabled() {
        return isHqSubscriptionEnabled()
                && hqConfig()
                .map(c -> "Y".equalsIgnoreCase(str(c.getJpaySubscriptionInlineEnabledYn())))
                .orElse(false);
    }

    public String resolveSubscriptionPathTemplate() {
        return hqConfig()
                .map(HqApiConfig::getJpaySubscriptionPathTemplate)
                .filter(s -> s != null && !s.isBlank())
                .orElse("/jpay-subscribe/{compCode}");
    }

    public String resolveHqDefaultsJson() {
        return hqConfig().map(HqApiConfig::getJpaySubscriptionConfigJson).orElse(null);
    }

    public boolean isMerchantSubscriptionEnabled(Long orgUnitId) {
        if (!isHqSubscriptionEnabled() || orgUnitId == null) {
            return false;
        }
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        return prof.map(p -> "Y".equalsIgnoreCase(str(p.getApiJpaySubscriptionUseYn()))).orElse(false);
    }

    public Optional<MerchantPgBinding> findOperationalSubscriptionBinding(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        return list.stream()
                .filter(b -> "Y".equalsIgnoreCase(str(b.getOperationalYn())))
                .filter(b -> b.getActivationYn() == null || "Y".equalsIgnoreCase(str(b.getActivationYn())))
                .filter(b -> b.getPgCd() != null && PgVendor.isJpayFamily(b.getPgCd()))
                .filter(b -> isAgencySubscriptionIntegration(b.getPgCd()))
                .filter(b -> {
                    String pm = b.getPayMethod();
                    return pm == null || pm.isBlank() || "WEB".equalsIgnoreCase(pm.trim());
                })
                .min(Comparator.comparingInt(b -> b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE));
    }

    public boolean hasOperationalSubscriptionBinding(Long orgUnitId) {
        return findOperationalSubscriptionBinding(orgUnitId).isPresent();
    }

    private boolean isAgencySubscriptionIntegration(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return false;
        }
        return pgAgencyRepository.findByPgCd(pgCd.trim())
                .filter(a -> "Y".equalsIgnoreCase(str(a.getUseYn())))
                .map(a -> "Y".equalsIgnoreCase(str(a.getIntegApiSubscriptionYn())))
                .orElse(false);
    }

    private static String str(String s) {
        return s != null ? s.trim() : "";
    }
}
