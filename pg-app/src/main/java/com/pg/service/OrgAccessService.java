package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    /**
     * 통합리포트·결제내역 조직 필터.
     * {@code null} — 추가 제한 없음(파라미터 없음). 빈 집합 — 조건 불충족(0건).
     */
    public Set<String> resolveSearchOrgMerchantCodes(String searchOrgLevel, String searchOrgUnitCode,
                                                     Authentication authentication) {
        String levelRaw = searchOrgLevel != null ? searchOrgLevel.trim().toUpperCase(Locale.ROOT) : "";
        String orgCodeRaw = searchOrgUnitCode != null ? searchOrgUnitCode.trim() : "";
        if (levelRaw.isEmpty() && orgCodeRaw.isEmpty()) {
            return null;
        }
        Set<Long> viewerOrgIds = resolveViewerSubtreeOrgIds(authentication);
        if (viewerOrgIds.isEmpty()) {
            return Set.of();
        }
        Set<String> out = new HashSet<>();
        if (!orgCodeRaw.isEmpty()) {
            Optional<OrgUnit> ouOpt = orgUnitRepository.findByCodeIgnoreCase(orgCodeRaw);
            if (ouOpt.isEmpty() || !viewerOrgIds.contains(ouOpt.get().getId())) {
                return Set.of();
            }
            OrgUnit ou = ouOpt.get();
            if (!levelRaw.isEmpty()) {
                OrgLevel reqLevel = parseOrgLevelName(levelRaw);
                if (reqLevel == null || ou.getOrgLevel() != reqLevel) {
                    return Set.of();
                }
                if (!isSearchOrgLevelAllowedForViewer(reqLevel, authentication)) {
                    return Set.of();
                }
            }
            out.addAll(merchantCompCodesUnderOrgUnit(ou));
        } else {
        OrgLevel reqLevel = parseOrgLevelName(levelRaw);
        if (reqLevel == null) {
            return Set.of();
        }
        if (!isSearchOrgLevelAllowedForViewer(reqLevel, authentication)) {
            return Set.of();
        }
        for (OrgUnit o : orgUnitRepository.findAll()) {
                if (o.getOrgLevel() == reqLevel && viewerOrgIds.contains(o.getId())) {
                    out.addAll(merchantCompCodesUnderOrgUnit(o));
                }
            }
        }
        Set<String> visible = visibleMerchantCompCodes(authentication);
        if (visible != null) {
            out.retainAll(visible);
        }
        return out;
    }

    /** 로그인 범위 내 특정 조직 단계 목록(통합리포트 검색어 드롭다운) */
    public List<Map<String, Object>> listOrgUnitsForSearchLevel(String searchOrgLevel, Authentication authentication) {
        OrgLevel level = parseOrgLevelName(searchOrgLevel);
        if (level == null) {
            return List.of();
        }
        if (!isSearchOrgLevelAllowedForViewer(level, authentication)) {
            return List.of();
        }
        Set<Long> viewerOrgIds = resolveViewerSubtreeOrgIds(authentication);
        if (viewerOrgIds.isEmpty()) {
            return List.of();
        }
        return orgUnitRepository.findAll().stream()
                .filter(o -> o.getOrgLevel() == level && viewerOrgIds.contains(o.getId()))
                .sorted(Comparator.comparing(OrgUnit::getName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(OrgUnit::getCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(o -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("code", o.getCode() != null ? o.getCode().trim() : "");
                    row.put("name", o.getName() != null ? o.getName().trim() : "");
                    row.put("orgLevel", o.getOrgLevel() != null ? o.getOrgLevel().name() : "");
                    row.put("orgLevelNm", o.getOrgLevel() != null ? o.getOrgLevel().getNameKo() : "");
                    return row;
                })
                .collect(Collectors.toList());
    }

    /** 통합리포트 조직구분 — 로그인 조직 단계부터 하위만(총판이면 총판~가맹). */
    public List<Map<String, Object>> listSearchOrgLevelsForViewer(Authentication authentication) {
        OrgLevel viewerLevel = resolveViewerOrgLevel(authentication);
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrgLevel lv : OrgLevel.values()) {
            if (viewerLevel == null || lv.getCode() >= viewerLevel.getCode()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("level", lv.name());
                row.put("labelKo", lv.getNameKo());
                row.put("ord", lv.getCode());
                out.add(row);
            }
        }
        return out;
    }

    /** ADMIN은 전 단계, 그 외는 본인 조직 단계·하위만 검색 가능 */
    public boolean isSearchOrgLevelAllowedForViewer(OrgLevel requestedLevel, Authentication authentication) {
        if (requestedLevel == null) {
            return false;
        }
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        OrgLevel viewerLevel = resolveViewerOrgLevel(authentication);
        if (viewerLevel == null) {
            return false;
        }
        return requestedLevel.getCode() >= viewerLevel.getCode();
    }

    /** 로그인 사용자 소속 조직 단계(ADMIN은 null = 전 단계 허용). */
    public OrgLevel resolveViewerOrgLevel(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return null;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return null;
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        if (org == null || org.get("compId") == null) {
            return null;
        }
        String rootCode = org.get("compId").toString().trim();
        if (rootCode.isEmpty()) {
            return null;
        }
        Optional<OrgUnit> rootOpt = orgUnitRepository.findByCodeIgnoreCase(rootCode);
        if (rootOpt.isEmpty() && user.getOrgUnitCode() != null && !user.getOrgUnitCode().isBlank()) {
            rootOpt = orgUnitRepository.findByCodeIgnoreCase(user.getOrgUnitCode().trim());
        }
        return rootOpt.map(OrgUnit::getOrgLevel).orElse(null);
    }

    private Set<Long> resolveViewerSubtreeOrgIds(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return Set.of();
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return orgUnitRepository.findAll().stream().map(OrgUnit::getId).collect(Collectors.toSet());
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
        if (root.getOrgLevel() == OrgLevel.HEADQUARTERS) {
            return orgUnitRepository.findAll().stream().map(OrgUnit::getId).collect(Collectors.toSet());
        }
        Set<Long> allowed = new HashSet<>();
        allowed.add(root.getId());
        allowed.addAll(collectDescendantIds(root.getId()));
        return allowed;
    }

    private Set<String> merchantCompCodesUnderOrgUnit(OrgUnit root) {
        if (root == null) {
            return Set.of();
        }
        if (root.getOrgLevel() == OrgLevel.MERCHANT) {
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

    private static OrgLevel parseOrgLevelName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return OrgLevel.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
