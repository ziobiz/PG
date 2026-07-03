package com.pg.service;

import com.pg.entity.HqRiskCardPolicy;
import com.pg.entity.MerchantProfile;
import com.pg.repository.MerchantProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class PayContactRememberPolicyService {

    public static final String MODE_FOLLOW_HQ = "FOLLOW_HQ";
    public static final String MODE_Y = "Y";
    public static final String MODE_N = "N";

    private final HqRiskCardPolicyService hqRiskCardPolicyService;
    private final MerchantProfileRepository merchantProfileRepository;

    public PayContactRememberPolicyService(HqRiskCardPolicyService hqRiskCardPolicyService,
                                           MerchantProfileRepository merchantProfileRepository) {
        this.hqRiskCardPolicyService = hqRiskCardPolicyService;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public boolean isEnabledForOrgUnit(Long orgUnitId) {
        String mode = resolveMode(orgUnitId);
        if (MODE_N.equals(mode)) {
            return false;
        }
        if (MODE_Y.equals(mode)) {
            return true;
        }
        HqRiskCardPolicy hq = hqRiskCardPolicyService.getOrCreate();
        return "Y".equalsIgnoreCase(trim(hq.getCheckoutContactRememberDefaultYn()));
    }

    public String resolveMode(Long orgUnitId) {
        if (orgUnitId == null) {
            return MODE_FOLLOW_HQ;
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isEmpty()) {
            return MODE_FOLLOW_HQ;
        }
        String raw = mp.get().getCheckoutContactRememberMode();
        if (raw == null || raw.isBlank()) {
            return MODE_FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (MODE_Y.equals(u) || MODE_N.equals(u)) {
            return u;
        }
        return MODE_FOLLOW_HQ;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
