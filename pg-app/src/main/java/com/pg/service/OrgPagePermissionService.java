package com.pg.service;

import com.pg.catalog.PageMenuCatalog;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgPagePermission;
import com.pg.entity.OrgUnit;
import com.pg.entity.OrgUnitAssistantPagePermission;
import com.pg.entity.OrgUnitPagePermission;
import com.pg.repository.OrgPagePermissionRepository;
import com.pg.repository.OrgUnitAssistantPagePermissionRepository;
import com.pg.repository.OrgUnitPagePermissionRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 조직 단계(OrgLevel) 기본 권한 + 개별 조직(OrgUnit) 오버라이드 — NONE / OBSERVER / MODIFY / DELETE
 */
@Service
public class OrgPagePermissionService {

    public static final String P_NONE = "NONE";
    public static final String P_OBSERVER = "OBSERVER";
    public static final String P_MODIFY = "MODIFY";
    public static final String P_DELETE = "DELETE";

    public static final String MODE_LEVEL_DEFAULT = "LEVEL_DEFAULT";
    public static final String MODE_CUSTOM = "CUSTOM";

    public static final List<String> ASSISTANT_ROLE_TYPES = List.of("MANAGER", "OPERATOR", "SETTLEMENT", "TECH");

    private final OrgPagePermissionRepository orgPagePermissionRepository;
    private final OrgUnitPagePermissionRepository orgUnitPagePermissionRepository;
    private final OrgUnitAssistantPagePermissionRepository orgUnitAssistantPagePermissionRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final AuthService authService;

    public OrgPagePermissionService(OrgPagePermissionRepository orgPagePermissionRepository,
                                      OrgUnitPagePermissionRepository orgUnitPagePermissionRepository,
                                      OrgUnitAssistantPagePermissionRepository orgUnitAssistantPagePermissionRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      AuthService authService) {
        this.orgPagePermissionRepository = orgPagePermissionRepository;
        this.orgUnitPagePermissionRepository = orgUnitPagePermissionRepository;
        this.orgUnitAssistantPagePermissionRepository = orgUnitAssistantPagePermissionRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.authService = authService;
    }

    /** ADMIN·미연결 계정: 제한 없음 → null */
    public Map<String, String> resolvePagePermissionsForUser(AppUser user) {
        if (user == null) return null;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return null;
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        if (org == null) return null;
        Object ol = org.get("orgLevel");
        if (ol == null) return null;
        String level = ol.toString().trim().toUpperCase(Locale.ROOT);
        Object ouIdObj = org.get("orgUnitId");
        Map<String, String> base;
        if (ouIdObj != null) {
            try {
                long ouId = Long.parseLong(ouIdObj.toString().trim());
                base = effectiveMapForOrgUnit(ouId, level);
            } catch (NumberFormatException ignored) {
                base = effectiveMapForOrgLevel(level);
            }
        } else {
            base = effectiveMapForOrgLevel(level);
        }
        return applyAssistantRoleOverlayIfNeeded(user, base, org);
    }

    /**
     * ASSISTANT 계정이면 조직에 저장된 담당자(관리/운영/정산/기술)별 권한을 조직 최종 권한 상한 내에서 병합합니다.
     * 행이 없는 URL은 조직 최종 권한을 그대로 둡니다.
     */
    private Map<String, String> applyAssistantRoleOverlayIfNeeded(AppUser user, Map<String, String> orgEffective,
                                                                   Map<String, Object> orgMap) {
        if (user == null || orgEffective == null || orgMap == null) {
            return orgEffective;
        }
        if (!"ASSISTANT".equalsIgnoreCase(trim(user.getUserType()))) {
            return orgEffective;
        }
        String art = trim(user.getAssistantRoleType()).toUpperCase(Locale.ROOT);
        if (art.isEmpty() || !ASSISTANT_ROLE_TYPES.contains(art)) {
            return orgEffective;
        }
        Object ouIdObj = orgMap.get("orgUnitId");
        if (ouIdObj == null) {
            return orgEffective;
        }
        long ouId;
        try {
            ouId = Long.parseLong(ouIdObj.toString().trim());
        } catch (NumberFormatException e) {
            return orgEffective;
        }
        List<OrgUnitAssistantPagePermission> rows =
                orgUnitAssistantPagePermissionRepository.findByOrgUnitIdOrderByAssistantRoleTypeAscPageUrlAsc(ouId);
        Map<String, String> byUrl = new HashMap<>();
        for (OrgUnitAssistantPagePermission r : rows) {
            if (r.getAssistantRoleType() != null && art.equalsIgnoreCase(r.getAssistantRoleType().trim())
                    && r.getPageUrl() != null && !r.getPageUrl().isBlank()) {
                byUrl.put(r.getPageUrl().trim(), normalizePerm(r.getPermission()));
            }
        }
        if (byUrl.isEmpty()) {
            return orgEffective;
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            String url = item.pageUrl();
            String ceiling = orgEffective.getOrDefault(url, P_DELETE);
            if (!byUrl.containsKey(url)) {
                out.put(url, ceiling);
                continue;
            }
            if (P_NONE.equals(ceiling)) {
                out.put(url, P_NONE);
                continue;
            }
            String roleP = byUrl.get(url);
            out.put(url, intersectPermission(ceiling, roleP));
        }
        return out;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /** 조직 상한(ceiling)과 담당자 권한의 교집합(더 제한적인 쪽). */
    public String intersectPermission(String ceiling, String rolePerm) {
        String c = normalizePerm(ceiling);
        String r = normalizePerm(rolePerm);
        int sc = strength(c);
        int sr = strength(r);
        return permFromStrength(Math.min(sc, sr));
    }

    private static int strength(String p) {
        return switch (normalizePermStatic(p)) {
            case P_DELETE -> 4;
            case P_MODIFY -> 3;
            case P_OBSERVER -> 2;
            case P_NONE -> 1;
            default -> 1;
        };
    }

    private static String permFromStrength(int s) {
        if (s <= 1) return P_NONE;
        if (s == 2) return P_OBSERVER;
        if (s == 3) return P_MODIFY;
        return P_DELETE;
    }

    private static String normalizePermStatic(String p) {
        if (p == null || p.isBlank()) return P_DELETE;
        String u = p.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case P_NONE, P_OBSERVER, P_MODIFY, P_DELETE -> u;
            default -> P_DELETE;
        };
    }

    /** org 정보를 이미 알 때 */
    public Map<String, String> resolvePagePermissionsForOrgLevel(String orgLevel) {
        if (orgLevel == null || orgLevel.isBlank()) return null;
        return effectiveMapForOrgLevel(orgLevel.trim().toUpperCase(Locale.ROOT));
    }

    public Map<String, String> effectiveMapForOrgLevel(String orgLevel) {
        List<OrgPagePermission> rows = orgPagePermissionRepository.findByOrgLevelOrderByPageUrlAsc(orgLevel);
        if (OrgLevel.HEADQUARTERS.name().equals(orgLevel) && rows.isEmpty()) {
            return defaultHeadquartersFullAccessMap();
        }
        if (OrgLevel.MERCHANT.name().equals(orgLevel) && rows.isEmpty()) {
            return defaultMerchantRestrictedMap();
        }
        Map<String, String> byUrl = new HashMap<>();
        for (OrgPagePermission r : rows) {
            if (r.getPageUrl() != null && r.getPermission() != null) {
                byUrl.put(r.getPageUrl().trim(), normalizePerm(r.getPermission()));
            }
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            String url = item.pageUrl();
            String p = byUrl.get(url);
            if (p == null && OrgLevel.MERCHANT.name().equals(orgLevel)) {
                if ("/system/noticeList".equals(url) || "/comp/myCompMng".equals(url)) {
                    p = P_OBSERVER;
                }
            }
            out.put(url, p != null ? p : P_DELETE);
        }
        return out;
    }

    /**
     * 공지 등록: 화면 권한이 MODIFY/DELETE 이고, 조직이 총본사·본사(REGIONAL)·총판(MASTER_DIST)일 때만.
     * ADMIN 은 항상 가능. 페이지 권한은 조직별 권한 세팅에서 조정.
     */
    public boolean canWriteNotice(AppUser user) {
        if (user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        Map<String, String> m = resolvePagePermissionsForUser(user);
        if (m != null) {
            String raw = m.get("/system/noticeList");
            String p = raw != null ? normalizePerm(raw) : P_DELETE;
            if (P_NONE.equals(p) || P_OBSERVER.equals(p)) {
                return false;
            }
            if (!P_MODIFY.equals(p) && !P_DELETE.equals(p)) {
                return false;
            }
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        if (org == null) {
            return false;
        }
        String level = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        return "HEADQUARTERS".equals(level) || "REGIONAL".equals(level) || "MASTER_DIST".equals(level);
    }

    /**
     * 로그인 조직 단위 최종 권한: CUSTOM이면 URL별 오버라이드 병합, 아니면 단계 기본만.
     */
    public Map<String, String> effectiveMapForOrgUnit(long orgUnitId, String fallbackOrgLevel) {
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return effectiveMapForOrgLevel(fallbackOrgLevel);
        }
        OrgUnit ou = ouOpt.get();
        String level = ou.getOrgLevel() != null ? ou.getOrgLevel().name() : fallbackOrgLevel;
        Map<String, String> base = effectiveMapForOrgLevel(level);
        String mode = ou.getPagePermissionMode();
        if (mode == null || MODE_LEVEL_DEFAULT.equalsIgnoreCase(mode.trim())) {
            return base;
        }
        if (!MODE_CUSTOM.equalsIgnoreCase(mode.trim())) {
            return base;
        }
        List<OrgUnitPagePermission> rows = orgUnitPagePermissionRepository.findByOrgUnitIdOrderByPageUrlAsc(orgUnitId);
        Map<String, String> byUrl = new HashMap<>();
        for (OrgUnitPagePermission r : rows) {
            if (r.getPageUrl() != null && r.getPermission() != null) {
                byUrl.put(r.getPageUrl().trim(), normalizePerm(r.getPermission()));
            }
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            String url = item.pageUrl();
            if (byUrl.containsKey(url)) {
                out.put(url, byUrl.get(url));
            } else {
                out.put(url, base.get(url));
            }
        }
        return out;
    }

    /**
     * 관리 화면용: 개별 조직 상세 + 단계 기본 + 최종 적용(동일).
     */
    public Map<String, Object> buildOrgUnitPermissionPayload(long orgUnitId) {
        OrgUnit ou = orgUnitRepository.findById(orgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다."));
        String level = ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "";
        Map<String, String> levelDefault = effectiveMapForOrgLevel(level);
        String mode = ou.getPagePermissionMode() != null ? ou.getPagePermissionMode() : MODE_LEVEL_DEFAULT;
        Map<String, String> effective = effectiveMapForOrgUnit(orgUnitId, level);

        Map<String, Object> org = new LinkedHashMap<>();
        org.put("id", ou.getId());
        org.put("code", ou.getCode());
        org.put("name", ou.getName());
        org.put("orgLevel", level);
        org.put("orgLevelName", ou.getOrgLevel() != null ? ou.getOrgLevel().getNameKo() : "");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orgUnit", org);
        out.put("mode", MODE_CUSTOM.equalsIgnoreCase(mode.trim()) ? MODE_CUSTOM : MODE_LEVEL_DEFAULT);
        out.put("levelDefault", levelDefault);
        out.put("effective", effective);
        out.put("assistantRoles", ASSISTANT_ROLE_TYPES);
        out.put("assistantMatrix", buildAssistantMatrixMap(orgUnitId));
        return out;
    }

    private Map<String, Map<String, String>> buildAssistantMatrixMap(long orgUnitId) {
        Map<String, Map<String, String>> assist = new LinkedHashMap<>();
        for (String role : ASSISTANT_ROLE_TYPES) {
            assist.put(role, new LinkedHashMap<>());
        }
        for (OrgUnitAssistantPagePermission r : orgUnitAssistantPagePermissionRepository
                .findByOrgUnitIdOrderByAssistantRoleTypeAscPageUrlAsc(orgUnitId)) {
            if (r.getAssistantRoleType() == null || r.getPageUrl() == null || r.getPageUrl().isBlank()) {
                continue;
            }
            String role = r.getAssistantRoleType().trim().toUpperCase(Locale.ROOT);
            if (!assist.containsKey(role)) {
                continue;
            }
            assist.get(role).put(r.getPageUrl().trim(), normalizePerm(r.getPermission()));
        }
        return assist;
    }

    /**
     * 담당자(관리/운영/정산/기술)별 메뉴 권한 저장. 조직 최종 권한이 접근불가인 URL은 무시합니다.
     * 저장 값은 조직 상한을 넘지 않으며, 상한과 동일하면 행을 두지 않습니다(해당 URL은 조직 권한을 그대로 따름).
     */
    @Transactional
    public void saveOrgUnitAssistantPermission(long orgUnitId, Map<String, Map<String, String>> matrix) {
        OrgUnit ou = orgUnitRepository.findById(orgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다."));
        String level = ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "";
        Map<String, String> effective = effectiveMapForOrgUnit(orgUnitId, level);
        orgUnitAssistantPagePermissionRepository.deleteByOrgUnitId(orgUnitId);
        orgUnitAssistantPagePermissionRepository.flush();
        if (matrix == null) {
            return;
        }
        for (Map.Entry<String, Map<String, String>> re : matrix.entrySet()) {
            String role = re.getKey() != null ? re.getKey().trim().toUpperCase(Locale.ROOT) : "";
            if (!ASSISTANT_ROLE_TYPES.contains(role)) {
                continue;
            }
            Map<String, String> pages = re.getValue();
            if (pages == null) {
                continue;
            }
            for (Map.Entry<String, String> pe : pages.entrySet()) {
                String url = pe.getKey() != null ? pe.getKey().trim() : "";
                if (url.isBlank()) {
                    continue;
                }
                String ceiling = effective.getOrDefault(url, P_DELETE);
                if (P_NONE.equals(ceiling)) {
                    continue;
                }
                String want = normalizePerm(pe.getValue());
                String saved = intersectPermission(ceiling, want);
                if (P_NONE.equals(saved) || saved.equals(ceiling)) {
                    continue;
                }
                PageMenuCatalog.PageMenuItem meta = PageMenuCatalog.items().stream()
                        .filter(i -> i.pageUrl().equals(url))
                        .findFirst()
                        .orElse(null);
                OrgUnitAssistantPagePermission row = new OrgUnitAssistantPagePermission();
                row.setOrgUnitId(orgUnitId);
                row.setAssistantRoleType(role);
                row.setPageUrl(url);
                row.setPermission(saved);
                if (meta != null) {
                    row.setMenuId(meta.menuId());
                }
                orgUnitAssistantPagePermissionRepository.save(row);
            }
        }
    }

    /**
     * 조직별 권한 화면 초기 데이터 — 역할(총본사/본사·총판)에 따라 편집 가능 영역이 다릅니다.
     */
    public Map<String, Object> buildPermissionMngPayload(AppUser actor) {
        Map<String, Object> payload = buildAdminPayload();
        Map<String, Object> caps = new LinkedHashMap<>();
        caps.put("canSaveLevelMatrix", false);
        caps.put("canSaveOrgUnit", false);
        caps.put("canSaveAssistant", false);
        caps.put("showLevelTabs", false);
        caps.put("showOrgUnitPanel", false);
        caps.put("showAssistantPanel", false);
        if (actor == null) {
            payload.put("uiCaps", caps);
            return payload;
        }
        if ("ADMIN".equalsIgnoreCase(actor.getRole())) {
            caps.put("canSaveLevelMatrix", true);
            caps.put("canSaveOrgUnit", true);
            caps.put("canSaveAssistant", true);
            caps.put("showLevelTabs", true);
            caps.put("showOrgUnitPanel", true);
            caps.put("showAssistantPanel", true);
            payload.put("uiCaps", caps);
            return payload;
        }
        Map<String, Object> orgMap = authService.getOrgInfo(actor.getUsername());
        if (orgMap == null) {
            payload.put("uiCaps", caps);
            return payload;
        }
        String ol = String.valueOf(orgMap.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        boolean hq = "HEADQUARTERS".equals(ol);
        boolean reg = "REGIONAL".equals(ol);
        boolean md = "MASTER_DIST".equals(ol);
        caps.put("showAssistantPanel", hq || reg || md);
        caps.put("canSaveAssistant", hq || reg || md);
        if (hq) {
            caps.put("canSaveLevelMatrix", true);
            caps.put("canSaveOrgUnit", true);
            caps.put("showLevelTabs", true);
            caps.put("showOrgUnitPanel", true);
            payload.put("uiCaps", caps);
            return payload;
        }
        if (reg || md) {
            payload.put("matrix", new LinkedHashMap<>());
            payload.put("orgLevels", new ArrayList<>());
            String compId = String.valueOf(orgMap.getOrDefault("compId", "")).trim();
            List<Map<String, Object>> all = listAllOrgUnitsForPermissionAdmin();
            List<Map<String, Object>> one = all.stream()
                    .filter(m -> compId.equals(String.valueOf(m.getOrDefault("code", "")).trim()))
                    .toList();
            payload.put("orgUnits", one);
        }
        payload.put("uiCaps", caps);
        return payload;
    }

    /**
     * 관리자 목록: 모든 조직(총본사~가맹점) — 개별 설정 탭 선택용.
     */
    public List<Map<String, Object>> listAllOrgUnitsForPermissionAdmin() {
        List<OrgUnit> all = orgUnitRepository.findAll();
        all.sort(Comparator.comparing(OrgUnit::getOrgLevel, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(o -> o.getCode() != null ? o.getCode() : ""));
        List<Map<String, Object>> list = new ArrayList<>();
        for (OrgUnit ou : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ou.getId());
            m.put("code", ou.getCode());
            m.put("name", ou.getName());
            m.put("orgLevel", ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "");
            m.put("orgLevelName", ou.getOrgLevel() != null ? ou.getOrgLevel().getNameKo() : "");
            String mode = ou.getPagePermissionMode() != null ? ou.getPagePermissionMode() : MODE_LEVEL_DEFAULT;
            m.put("mode", MODE_CUSTOM.equalsIgnoreCase(mode.trim()) ? MODE_CUSTOM : MODE_LEVEL_DEFAULT);
            list.add(m);
        }
        return list;
    }

    @Transactional
    public void saveOrgUnitPermission(long orgUnitId, String mode, Map<String, String> pages) {
        OrgUnit ou = orgUnitRepository.findById(orgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다."));
        String m = mode != null ? mode.trim().toUpperCase(Locale.ROOT) : MODE_LEVEL_DEFAULT;
        if (!MODE_CUSTOM.equals(m) && !MODE_LEVEL_DEFAULT.equals(m)) {
            m = MODE_LEVEL_DEFAULT;
        }
        ou.setPagePermissionMode(m);
        orgUnitRepository.save(ou);

        orgUnitPagePermissionRepository.deleteByOrgUnitId(orgUnitId);
        orgUnitPagePermissionRepository.flush();

        if (!MODE_CUSTOM.equals(m) || pages == null) {
            return;
        }
        Map<String, String> dedup = pages.entrySet().stream()
                .filter(pe -> pe.getKey() != null && !pe.getKey().isBlank())
                .collect(Collectors.toMap(
                        pe -> pe.getKey().trim(),
                        Map.Entry::getValue,
                        (a, b) -> b,
                        LinkedHashMap::new));
        for (Map.Entry<String, String> pe : dedup.entrySet()) {
            String url = pe.getKey();
            String perm = normalizePerm(pe.getValue());
            if (P_DELETE.equals(perm)) {
                continue;
            }
            PageMenuCatalog.PageMenuItem meta = PageMenuCatalog.items().stream()
                    .filter(i -> i.pageUrl().equals(url))
                    .findFirst()
                    .orElse(null);
            OrgUnitPagePermission row = new OrgUnitPagePermission();
            row.setOrgUnitId(orgUnitId);
            row.setPageUrl(url);
            row.setPermission(perm);
            if (meta != null) row.setMenuId(meta.menuId());
            orgUnitPagePermissionRepository.save(row);
        }
    }

    /**
     * 총본사(HEADQUARTERS) — DB에 권한 행이 없을 때 기본값: 카탈로그 전체 DELETE(모든 기능 사용 가능)
     */
    private Map<String, String> defaultHeadquartersFullAccessMap() {
        Map<String, String> out = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            out.put(item.pageUrl(), P_DELETE);
        }
        return out;
    }

    /**
     * DB에 MERCHANT 행이 없을 때만 사용 — 구 프론트 하드코딩(공지·업체정보조회만)과 동일.
     */
    private Map<String, String> defaultMerchantRestrictedMap() {
        Map<String, String> out = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            String u = item.pageUrl();
            boolean allow = "/system/noticeList".equals(u) || "/comp/myCompMng".equals(u);
            out.put(u, allow ? P_OBSERVER : P_NONE);
        }
        return out;
    }

    public Map<String, Object> buildAdminPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        List<Map<String, Object>> catalog = new ArrayList<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pageUrl", item.pageUrl());
            row.put("menuId", item.menuId());
            row.put("menuNm", item.menuName());
            row.put("parentGroup", item.parentGroup());
            catalog.add(row);
        }
        payload.put("catalog", catalog);
        Map<String, Map<String, String>> matrix = new LinkedHashMap<>();
        for (OrgLevel lv : OrgLevel.values()) {
            String code = lv.name();
            matrix.put(code, effectiveMapForOrgLevel(code));
        }
        payload.put("matrix", matrix);
        List<Map<String, String>> orgLevels = new ArrayList<>();
        for (OrgLevel lv : OrgLevel.values()) {
            orgLevels.add(Map.of("code", lv.name(), "name", lv.getNameKo()));
        }
        payload.put("orgLevels", orgLevels);
        payload.put("orgUnits", listAllOrgUnitsForPermissionAdmin());
        payload.put("permOptions", List.of(
                Map.of("v", P_NONE, "t", "접근불가"),
                Map.of("v", P_OBSERVER, "t", "옵저버(조회만)"),
                Map.of("v", P_MODIFY, "t", "수정(쓰기·수정, 삭제 제한)"),
                Map.of("v", P_DELETE, "t", "삭제(전체)")
        ));
        return payload;
    }

    @Transactional
    public void saveMatrix(Map<String, Map<String, String>> matrix) {
        if (matrix == null) return;
        for (Map.Entry<String, Map<String, String>> e : matrix.entrySet()) {
            String orgLevel = e.getKey();
            if (orgLevel == null || orgLevel.isBlank()) continue;
            orgLevel = orgLevel.trim().toUpperCase(Locale.ROOT);
            try {
                OrgLevel.valueOf(orgLevel);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            orgPagePermissionRepository.deleteByOrgLevel(orgLevel);
            orgPagePermissionRepository.flush();
            Map<String, String> pages = e.getValue();
            if (pages == null) continue;
            Map<String, String> dedup = pages.entrySet().stream()
                    .filter(pe -> pe.getKey() != null && !pe.getKey().isBlank())
                    .collect(Collectors.toMap(
                            pe -> pe.getKey().trim(),
                            Map.Entry::getValue,
                            (a, b) -> b,
                            LinkedHashMap::new));
            for (Map.Entry<String, String> pe : dedup.entrySet()) {
                String url = pe.getKey();
                String perm = normalizePerm(pe.getValue());
                if (P_DELETE.equals(perm)) continue;
                PageMenuCatalog.PageMenuItem meta = PageMenuCatalog.items().stream()
                        .filter(i -> i.pageUrl().equals(url))
                        .findFirst()
                        .orElse(null);
                OrgPagePermission row = new OrgPagePermission();
                row.setOrgLevel(orgLevel);
                row.setPageUrl(url);
                row.setPermission(perm);
                if (meta != null) row.setMenuId(meta.menuId());
                orgPagePermissionRepository.save(row);
            }
        }
    }

    private static String normalizePerm(String p) {
        if (p == null || p.isBlank()) return P_DELETE;
        String u = p.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case P_NONE, P_OBSERVER, P_MODIFY, P_DELETE -> u;
            default -> P_DELETE;
        };
    }
}
