package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.urlpay.UrlPayFollowHqYnUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PG 아웃바운드 이메일증명 — 본사 Y/N + 가맹 FOLLOW_HQ|Y|N.
 * 활성(Y): 실제 구매자 이메일을 PG로 전송. 비활성(N): noreply@icopay.co.kr 치환.
 */
@Service
public class PgBuyerEmailAttestService {

    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public PgBuyerEmailAttestService(HqApiConfigRepository hqApiConfigRepository,
                                     MerchantProfileRepository merchantProfileRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    @Transactional(readOnly = true)
    public boolean shouldSendRealBuyerEmail(Long orgUnitId) {
        String hq = hqDefaultYn();
        if (orgUnitId == null) {
            return UrlPayFollowHqYnUtil.Y.equals(hq);
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        String merchant = mp != null ? mp.getPgBuyerEmailAttestYn() : UrlPayFollowHqYnUtil.FOLLOW_HQ;
        String eff = UrlPayFollowHqYnUtil.resolveEffective(merchant, hq, UrlPayFollowHqYnUtil.Y);
        return UrlPayFollowHqYnUtil.Y.equals(eff);
    }

    @Transactional(readOnly = true)
    public String hqDefaultYn() {
        return hqApiConfigRepository.findAll().stream()
                .findFirst()
                .map(HqApiConfig::getPgBuyerEmailAttestYn)
                .map(v -> UrlPayFollowHqYnUtil.normalizeHqDefault(v, UrlPayFollowHqYnUtil.Y))
                .orElse(UrlPayFollowHqYnUtil.Y);
    }
}
