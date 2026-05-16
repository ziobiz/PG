package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 로그인 사용자(ADMIN 제외)가 조회·후속조치할 수 있는 가맹점(결제 주체) 범위.
 * 루트 조직은 {@link AuthService#getOrgInfo(String)} / {@link AuthService#resolveOrgUnitForLoginId(String)} 과 동일
 * (tb_user.org_unit_code 우선 — 본사·총판·지사 등은 로그인 조직 기준 하위 트리, 총본사만 예외적으로 HEADQUARTERS 트리 전체).
 * 업체관리 {@link CompService#isTargetUnderViewerOrg(String, String)} 와 동일한 상·하위 판별.
 */
@Service
public class OrgAccessService {

    private final AuthService authService;
    private final OrgUnitRepository orgUnitRepository;

    public OrgAccessService(AuthService authService, OrgUnitRepository orgUnitRepository) {
        this.authService = authService;
        this.orgUnitRepository = orgUnitRepository;
    }

    /**
     * @return {@code null} — 제한 없음(ADMIN). 빈 집합 — 소속 조직 없음 등으로 조회 0건.
     *         그 외 — 허용된 가맹점 {@link OrgUnit#getCode()} 집합(본인이 가맹점이면 1건).
     */
    public Set<String> visibleMerchantCompCodes(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return Set.of();
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return null;
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        if (org == null || org.get("compId") == null) {
            return Set.of();
        }
        String rootCode = org.get("compId").toString().trim();
        if (rootCode.isEmpty()) {
            return Set.of();
        }
        Optional<OrgUnit> rootOpt = orgUnitRepository.findByCodeIgnoreCase(rootCode);
        if (rootOpt.isEmpty() && user.getOrgUnitCode() != null && !user.getOrgUnitCode().isBlank()) {
            rootOpt = orgUnitRepository.findByCodeIgnoreCase(user.getOrgUnitCode().trim());
        }
        if (rootOpt.isEmpty()) {
            return Set.of();
        }
        OrgUnit root = rootOpt.get();
        OrgLevel lvl = root.getOrgLevel();
        if (lvl == OrgLevel.HEADQUARTERS) {
            return null;
        }
        if (lvl == OrgLevel.MERCHANT) {
            if (root.getCode() == null || root.getCode().isBlank()) {
                return Set.of();
            }
            return Set.of(root.getCode().trim());
        }
        Set<Long> allowedIds = new HashSet<>();
        allowedIds.add(root.getId());
        allowedIds.addAll(collectDescendantIds(root.getId()));
        Set<String> out = new HashSet<>();
        for (OrgUnit o : orgUnitRepository.findAll()) {
            if (o.getOrgLevel() == OrgLevel.MERCHANT && o.getCode() != null && !o.getCode().isBlank()
                    && allowedIds.contains(o.getId())) {
                out.add(o.getCode().trim());
            }
        }
        return out;
    }

    /** viewer 업체코드 기준 target 가맹점 코드가 viewer 본인 또는 직·간접 하위인지 */
    public boolean isTargetUnderViewerOrg(String viewerCompCode, String targetCompCode) {
        if (viewerCompCode == null || targetCompCode == null) {
            return false;
        }
        String v = viewerCompCode.trim();
        String t = targetCompCode.trim();
        if (v.isEmpty() || t.isEmpty()) {
            return false;
        }
        if (v.equalsIgnoreCase(t)) {
            return true;
        }
        Optional<OrgUnit> cur = orgUnitRepository.findByCode(t);
        while (cur.isPresent()) {
            Long pid = cur.get().getParentId();
            if (pid == null) {
                return false;
            }
            Optional<OrgUnit> parent = orgUnitRepository.findById(pid);
            if (parent.isEmpty()) {
                return false;
            }
            String pc = parent.get().getCode();
            if (pc != null && v.equalsIgnoreCase(pc.trim())) {
                return true;
            }
            cur = parent;
        }
        return false;
    }

    private List<Long> collectDescendantIds(Long rootId) {
        List<OrgUnit> all = orgUnitRepository.findAll();
        Map<Long, List<OrgUnit>> byParent = all.stream()
                .filter(o -> o.getParentId() != null)
                .collect(Collectors.groupingBy(OrgUnit::getParentId));
        List<Long> result = new ArrayList<>();
        collectDescendantIdsRec(rootId, byParent, result);
        return result;
    }

    private void collectDescendantIdsRec(Long id, Map<Long, List<OrgUnit>> byParent, List<Long> result) {
        for (OrgUnit child : byParent.getOrDefault(id, Collections.emptyList())) {
            result.add(child.getId());
            collectDescendantIdsRec(child.getId(), byParent, result);
        }
    }
}
