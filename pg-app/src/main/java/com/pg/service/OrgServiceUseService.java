package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.OrgUseYnUtil;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 업체 사용여부(Y/N/S) — 본인 및 상위 조직 체인을 따라 결제·정산·로그인 허용 여부 판별.
 * <ul>
 *   <li>Y — 서비스·로그인 허용</li>
 *   <li>N — 서비스 중단, 로그인 허용(과거 조회 등)</li>
 *   <li>S — 서비스 중단, 로그인 차단(영구정지)</li>
 * </ul>
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
        if (orgUnitId == null) {
            return false;
        }
        return !hasNonActiveUseYnInChain(orgUnitId);
    }

    public boolean isOrgServiceActiveByCompCode(String compCode) {
        if (compCode == null || compCode.isBlank()) {
            return false;
        }
        String t = compCode.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(t);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(t);
        }
        return ou.map(o -> isOrgServiceActive(o.getId())).orElse(false);
    }

    /** 조직 또는 상위 조직에 영구정지(S)가 있으면 로그인 불가 */
    public boolean isOrgLoginSuspended(Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        Optional<OrgUnit> cur = orgUnitRepository.findById(orgUnitId);
        while (cur.isPresent()) {
            Long id = cur.get().getId();
            Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(id);
            if (mp.isPresent() && OrgUseYnUtil.isLoginBlocked(mp.get().getUseYn())) {
                return true;
            }
            Long pid = cur.get().getParentId();
            if (pid == null) {
                break;
            }
            cur = orgUnitRepository.findById(pid);
        }
        return false;
    }

    /**
     * PG 노티 수신·적재용: <strong>해당 조직 단일</strong> 프로필의 {@code use_yn} 만 봅니다.
     * Y 가 아니면(N·S 포함) false.
     */
    public boolean isOrgEligibleForPgNotifyProcessing(Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isPresent() && !OrgUseYnUtil.isServiceAllowed(mp.get().getUseYn())) {
            return false;
        }
        return true;
    }

    private boolean hasNonActiveUseYnInChain(Long orgUnitId) {
        Optional<OrgUnit> cur = orgUnitRepository.findById(orgUnitId);
        while (cur.isPresent()) {
            Long id = cur.get().getId();
            Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(id);
            if (mp.isPresent() && !OrgUseYnUtil.isServiceAllowed(mp.get().getUseYn())) {
                return true;
            }
            Long pid = cur.get().getParentId();
            if (pid == null) {
                break;
            }
            cur = orgUnitRepository.findById(pid);
        }
        return false;
    }
}
