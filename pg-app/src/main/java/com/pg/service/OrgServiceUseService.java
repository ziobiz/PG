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
        String t = compCode.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(t);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(t);
        }
        return ou.map(o -> isOrgServiceActive(o.getId())).orElse(false);
    }

    /**
     * PG 노티 수신·적재용: <strong>해당 조직 단일</strong> 프로필의 {@code use_yn} 만 봅니다.
     * 상위 총판·본사가 관리 목적으로만 미사용(N)인 경우에도, 하위 가맹점에 프로필이 없거나 Y이면 true 입니다.
     * (신규 결제·정산 게이트는 {@link #isOrgServiceActive} 로 상위 체인을 계속 검사합니다.)
     */
    public boolean isOrgEligibleForPgNotifyProcessing(Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isPresent() && "N".equalsIgnoreCase(mp.get().getUseYn())) {
            return false;
        }
        return true;
    }
}
