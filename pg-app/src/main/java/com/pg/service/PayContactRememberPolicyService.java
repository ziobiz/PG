package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import com.pg.urlpay.UrlPayCheckoutDisplayPolicyService;
import com.pg.urlpay.UrlPayFollowHqYnUtil;
import org.springframework.stereotype.Service;

@Service
public class PayContactRememberPolicyService {

    public static final String MODE_FOLLOW_HQ = UrlPayFollowHqYnUtil.FOLLOW_HQ;
    public static final String MODE_Y = UrlPayFollowHqYnUtil.Y;
    public static final String MODE_N = UrlPayFollowHqYnUtil.N;

    private final UrlPayCheckoutDisplayPolicyService checkoutDisplayPolicyService;
    private final HqApiConfigRepository hqApiConfigRepository;

    public PayContactRememberPolicyService(UrlPayCheckoutDisplayPolicyService checkoutDisplayPolicyService,
                                           HqApiConfigRepository hqApiConfigRepository) {
        this.checkoutDisplayPolicyService = checkoutDisplayPolicyService;
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    public boolean isEnabledForOrgUnit(Long orgUnitId) {
        String mode = resolveMode(orgUnitId);
        if (MODE_N.equals(mode)) {
            return false;
        }
        if (MODE_Y.equals(mode)) {
            return true;
        }
        return "Y".equalsIgnoreCase(hqRememberDefaultYn());
    }

    public String resolveMode(Long orgUnitId) {
        return checkoutDisplayPolicyService.effectiveRememberMode(orgUnitId);
    }

    /** 본사 기본은 결제 URL 「결제창 표시 기본값」만 사용 */
    private String hqRememberDefaultYn() {
        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        if (hq == null) {
            return MODE_Y;
        }
        return UrlPayFollowHqYnUtil.normalizeHqDefault(hq.getCheckoutContactRememberDefaultYn(), "Y");
    }
}
