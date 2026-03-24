package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 업체 사용여부(미사용) — 본인 및 상위 조직 체인을 따라 결제·정산·연동 허용 여부 판별.
 * 상위가 미사용이면 하위도 서비스 불가(프로필에 N이 쌓인 경우와 동일 효과).
 */
@Service
public class OrgServiceUseService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public OrgServiceUseService(OrgUnitRepository orgUnitRepository,
                                MerchantProfileRepository merchantProfileRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    public boolean isOrgServiceActive(Long orgUnitId) {
        if (orgUnitId == null) return false;
        Optional<OrgUnit> cur = orgUnitRepository.findById(orgUnitId);
        while (cur.isPresent()) {
            Long id = cur.get().getId();
            Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(id);
            if (mp.isPresent() && "N".equalsIgnoreCase(mp.get().getUseYn())) {
                return false;
            }
            Long pid = cur.get().getParentId();
            if (pid == null) break;
            cur = orgUnitRepository.findById(pid);
        }
        return true;
    }

    public boolean isOrgServiceActiveByCompCode(String compCode) {
        if (compCode == null || compCode.isBlank()) return false;
        return orgUnitRepository.findByCode(compCode.trim())
                .map(o -> isOrgServiceActive(o.getId()))
                .orElse(false);
    }
}
