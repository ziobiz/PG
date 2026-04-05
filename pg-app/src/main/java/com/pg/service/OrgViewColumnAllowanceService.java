package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.HqViewCustomColumn;
import com.pg.entity.OrgViewColumnAllowance;
import com.pg.repository.HqViewCustomColumnRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.OrgViewColumnAllowanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrgViewColumnAllowanceService {

    public static final String SCOPE_REGIONAL = "REGIONAL";
    public static final String SCOPE_MASTER_DIST = "MASTER_DIST";
    public static final String SCOPE_BRANCH_GROUP = "BRANCH_GROUP";
    public static final String SCOPE_MERCHANT = "MERCHANT";

    private static final Set<String> VALID_SCOPES = Set.of(
            SCOPE_REGIONAL, SCOPE_MASTER_DIST, SCOPE_BRANCH_GROUP, SCOPE_MERCHANT);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AuthService authService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgViewColumnAllowanceRepository allowanceRepository;
    private final HqViewCustomColumnRepository hqViewCustomColumnRepository;

    public OrgViewColumnAllowanceService(AuthService authService,
                                         OrgUnitRepository orgUnitRepository,
                                         OrgViewColumnAllowanceRepository allowanceRepository,
                                         HqViewCustomColumnRepository hqViewCustomColumnRepository) {
        this.authService = authService;
        this.orgUnitRepository = orgUnitRepository;
        this.allowanceRepository = allowanceRepository;
        this.hqViewCustomColumnRepository = hqViewCustomColumnRepository;
    }

    public boolean canManageOrgViewAllowance(AppUser user) {
        if (user == null) return false;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return true;
        return authService.resolveOrgUnitForLoginId(user.getUsername())
                .map(ou -> ou.getOrgLevel() == OrgLevel.HEADQUARTERS)
                .orElse(false);
    }

    /**
     * 로그인 사용자의 본사(REGIONAL) 스코프 + 조직 유형에 맞는 컬럼 허용 정책.
     * empty() = 정책 없음(제한 없음). of(empty list) = 정책은 있으나 선택 가능한 열 없음.
     * 지사·대리점·영업점(BRANCH_GROUP) 및 가맹점(MERCHANT)은 별도 행이 없으면 총판(MASTER_DIST) 정책을 따름.
     */
    public Optional<List<String>> getRestrictedAllowedKeys(String loginId, String pageUrl) {
        Optional<OrgUnit> ouOpt = authService.resolveOrgUnitForLoginId(loginId);
        if (ouOpt.isEmpty()) return Optional.empty();
        OrgUnit ou = ouOpt.get();
        if (ou.getOrgLevel() == OrgLevel.HEADQUARTERS) return Optional.empty();
        Optional<String> regional = resolveRegionalAncestorOrgCode(loginId);
        if (regional.isEmpty()) return Optional.empty();
        String scope = viewerScopeForOrgLevel(ou.getOrgLevel());
        if (scope == null) return Optional.empty();
        String p = safe(pageUrl);
        Optional<OrgViewColumnAllowance> row = allowanceRepository
                .findByRegionalOrgCodeAndPageUrlAndViewerScope(regional.get(), p, scope);
        if ((SCOPE_BRANCH_GROUP.equals(scope) || SCOPE_MERCHANT.equals(scope)) && row.isEmpty()) {
            row = allowanceRepository.findByRegionalOrgCodeAndPageUrlAndViewerScope(regional.get(), p, SCOPE_MASTER_DIST);
        }
        if (row.isEmpty()) return Optional.empty();
        List<String> allowed = parseJsonArray(row.get().getAllowedKeysJson());
        return Optional.of(mergeCustomColumnKeysForPage(p, allowed));
    }

    /** 본사 등록 추가 항목 키는 조직 노출 정책이 있을 때 항상 허용 목록에 포함 */
    public List<String> mergeCustomColumnKeysForPage(String pageUrl, List<String> allowedKeys) {
        String p = safe(pageUrl);
        LinkedHashSet<String> merged = new LinkedHashSet<>(allowedKeys != null ? allowedKeys : List.of());
        if (!p.isEmpty()) {
            for (HqViewCustomColumn c : hqViewCustomColumnRepository.findByPageUrlOrderBySortOrderAscIdAsc(p)) {
                merged.add(c.getColumnKey());
            }
        }
        return new ArrayList<>(merged);
    }

    /** 로그인 사용자 소속 조직의 OrgLevel → viewer_scope (HEADQUARTERS 등은 null) */
    public static String viewerScopeForOrgLevel(OrgLevel level) {
        if (level == null) return null;
        return switch (level) {
            case REGIONAL -> SCOPE_REGIONAL;
            case MASTER_DIST -> SCOPE_MASTER_DIST;
            case BRANCH, AGENCY, SALES_OFFICE -> SCOPE_BRANCH_GROUP;
            case MERCHANT -> SCOPE_MERCHANT;
            default -> null;
        };
    }

    /**
     * 소속 조직에서 위로 올라가며 첫 REGIONAL 의 업체코드. HEADQUARTERS 소속이면 empty (총본사 직원은 트리 제한 없음).
     */
    public Optional<String> resolveRegionalAncestorOrgCode(String loginId) {
        Optional<OrgUnit> ouOpt = authService.resolveOrgUnitForLoginId(loginId);
        if (ouOpt.isEmpty()) return Optional.empty();
        OrgUnit ou = ouOpt.get();
        if (ou.getOrgLevel() == OrgLevel.HEADQUARTERS) return Optional.empty();
        OrgUnit walk = ou;
        while (walk != null) {
            if (walk.getOrgLevel() == OrgLevel.REGIONAL) {
                return Optional.of(walk.getCode());
            }
            Long pid = walk.getParentId();
            if (pid == null) break;
            walk = orgUnitRepository.findById(pid).orElse(null);
        }
        return Optional.empty();
    }

    public Map<String, Object> getAllowanceRow(String regionalOrgCode, String pageUrl, String viewerScope) {
        String r = safe(regionalOrgCode);
        String p = safe(pageUrl);
        String vs = normalizeScope(viewerScope);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("regionalOrgCode", r);
        m.put("pageUrl", p);
        m.put("viewerScope", vs);
        var opt = allowanceRepository.findByRegionalOrgCodeAndPageUrlAndViewerScope(r, p, vs);
        m.put("hasPolicy", opt.isPresent());
        m.put("allowedKeysJson", opt.map(OrgViewColumnAllowance::getAllowedKeysJson).orElse("[]"));
        opt.ifPresent(row -> m.put("updatedAt", row.getUpdatedAt() != null ? row.getUpdatedAt().toString() : null));
        return m;
    }

    @Transactional
    public Map<String, Object> saveAllowance(String regionalOrgCode, String pageUrl, String viewerScope,
                                            String allowedKeysJson, AppUser actor) {
        if (!canManageOrgViewAllowance(actor)) {
            throw new IllegalArgumentException("총본사(또는 ADMIN)만 컬럼 허용 정책을 저장할 수 있습니다.");
        }
        String r = safe(regionalOrgCode);
        String p = safe(pageUrl);
        String vs = normalizeScope(viewerScope);
        if (r.isEmpty() || p.isEmpty()) {
            throw new IllegalArgumentException("본사 업체코드와 화면 경로(pageUrl)가 필요합니다.");
        }
        if (!VALID_SCOPES.contains(vs)) {
            throw new IllegalArgumentException("노출 대상 조직 유형이 올바르지 않습니다.");
        }
        OrgUnit target = orgUnitRepository.findByCode(r)
                .orElseThrow(() -> new IllegalArgumentException("본사 업체코드를 찾을 수 없습니다: " + r));
        if (target.getOrgLevel() != OrgLevel.REGIONAL) {
            throw new IllegalArgumentException("대상은 본사(REGIONAL) 업체만 지정할 수 있습니다.");
        }
        List<String> keys = parseJsonArray(allowedKeysJson);
        OrgViewColumnAllowance row = allowanceRepository
                .findByRegionalOrgCodeAndPageUrlAndViewerScope(r, p, vs)
                .orElseGet(OrgViewColumnAllowance::new);
        row.setRegionalOrgCode(r);
        row.setPageUrl(p);
        row.setViewerScope(vs);
        row.setAllowedKeysJson(writeJsonArray(keys));
        allowanceRepository.save(row);
        return getAllowanceRow(r, p, vs);
    }

    @Transactional
    public Map<String, Object> deleteAllowance(String regionalOrgCode, String pageUrl, String viewerScope, AppUser actor) {
        if (!canManageOrgViewAllowance(actor)) {
            throw new IllegalArgumentException("총본사(또는 ADMIN)만 컬럼 허용 정책을 해제할 수 있습니다.");
        }
        String r = safe(regionalOrgCode);
        String p = safe(pageUrl);
        String vs = normalizeScope(viewerScope);
        allowanceRepository.deleteByRegionalOrgCodeAndPageUrlAndViewerScope(r, p, vs);
        return getAllowanceRow(r, p, vs);
    }

    public List<Map<String, String>> listRegionalBranches() {
        return orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.REGIONAL).stream()
                .map(ou -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("code", ou.getCode());
                    m.put("name", ou.getName() != null ? ou.getName() : ou.getCode());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * 선택한 본사(REGIONAL)에 저장된 조직별·화면별 노출 정책 요약 목록.
     */
    public List<Map<String, Object>> listAllowancesByRegional(String regionalOrgCode, AppUser actor) {
        if (!canManageOrgViewAllowance(actor)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        String r = safe(regionalOrgCode);
        if (r.isEmpty()) {
            throw new IllegalArgumentException("본사 업체코드를 선택하세요.");
        }
        orgUnitRepository.findByCode(r)
                .filter(ou -> ou.getOrgLevel() == OrgLevel.REGIONAL)
                .orElseThrow(() -> new IllegalArgumentException("본사 업체코드를 찾을 수 없습니다: " + r));
        return allowanceRepository.findByRegionalOrgCodeOrderByPageUrlAscViewerScopeAsc(r).stream()
                .map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("pageUrl", row.getPageUrl());
                    m.put("viewerScope", row.getViewerScope());
                    m.put("allowedColumnCount", parseJsonArray(row.getAllowedKeysJson()).size());
                    m.put("updatedAt", row.getUpdatedAt() != null ? row.getUpdatedAt().toString() : null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    private static String normalizeScope(String viewerScope) {
        String s = viewerScope == null ? "" : viewerScope.trim().toUpperCase();
        return s.isEmpty() ? SCOPE_REGIONAL : s;
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<String> list = MAPPER.readValue(json.trim(), new TypeReference<List<String>>() {});
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String writeJsonArray(List<String> keys) {
        try {
            return MAPPER.writeValueAsString(keys != null ? keys : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }
}
