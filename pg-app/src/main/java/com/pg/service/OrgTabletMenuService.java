package com.pg.service;

import com.pg.catalog.PageMenuCatalog;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgTabletMenu;
import com.pg.repository.OrgTabletMenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 * 태블릿 모드에서 사이드바에 노출할 URL — 조직 단계(OrgLevel)별 설정. 일반 모드와 동일 URL 사용.
 */
@Service
public class OrgTabletMenuService {

    /** HQ가 태블릿에서 켤 수 있는 고정 URL 목록(순서 유지) */
    public static final List<String> TABLET_MENU_URLS = List.of(
            "/comp/compReg",
            "/comp/compMngTree",
            "/commission/commisionList",
            "/calc/payList",
            "/calc/dailyPay",
            "/calc/feeList",
            "/calc/dailyFee",
            "/calc/settlementReport",
            "/chatbot/chatbotKbMng",
            "/chatbot/productMng",
            "/chatbot/orderMng",
            "/calc/chillPayTrList",
            "/pay/chatbotPay",
            "/pay/splitPay",
            /* 검수관리 */
            "/calc/integratedCheck",
            "/ops/agencyTxnList",
            "/calc/jpayTrList",
            "/calc/payOverview",
            "/calc/queryIntegrated",
            "/risk/list",
            "/ops/integratedReport",
            "/ops/verifyReport",
            "/user/userMng"
    );

    private final OrgTabletMenuRepository orgTabletMenuRepository;
    private final AuthService authService;

    public OrgTabletMenuService(OrgTabletMenuRepository orgTabletMenuRepository, AuthService authService) {
        this.orgTabletMenuRepository = orgTabletMenuRepository;
        this.authService = authService;
    }

    /** 본사권한설정 화면과 동일하게 열람 가능한 역할 */
    public boolean mayOpenOpsModeMng(AppUser u) {
        if (u == null) return false;
        if ("ADMIN".equalsIgnoreCase(u.getRole())) return true;
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        if (org == null) return false;
        String ol = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        return "HEADQUARTERS".equals(ol) || "REGIONAL".equals(ol) || "MASTER_DIST".equals(ol);
    }

    /** 매트릭스 저장: 총본사·ADMIN만 */
    public boolean maySaveTabletMatrix(AppUser u) {
        if (u == null) return false;
        if ("ADMIN".equalsIgnoreCase(u.getRole())) return true;
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        if (org == null) return false;
        return "HEADQUARTERS".equals(String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT));
    }

    public Map<String, Object> buildOpsModeMngPayload() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            labels.put(item.pageUrl(), item.menuName());
        }
        List<Map<String, String>> items = new ArrayList<>();
        for (String url : TABLET_MENU_URLS) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("url", url);
            row.put("label", labels.getOrDefault(url, url));
            items.add(row);
        }
        List<Map<String, String>> orgLevels = new ArrayList<>();
        for (OrgLevel lv : OrgLevel.values()) {
            orgLevels.add(Map.of("code", lv.name(), "name", lv.getNameKo()));
        }
        Map<String, Map<String, String>> matrix = new LinkedHashMap<>();
        for (Map<String, String> levelRow : orgLevels) {
            String level = levelRow.get("code");
            matrix.put(level, loadMatrixRowForOrgLevel(level));
        }
        return Map.of(
                "tabletItems", items,
                "orgLevels", orgLevels,
                "matrix", matrix
        );
    }

    public static boolean isTabletCatalogUrl(String pageUrl) {
        if (pageUrl == null || pageUrl.isBlank()) {
            return false;
        }
        return TABLET_MENU_URLS.contains(pageUrl.trim());
    }

    /** 조직 단계에서 태블릿설정(use_yn=Y)으로 노출된 URL만 */
    public List<String> listExposedTabletUrlsForOrgLevel(String orgLevel) {
        String level = orgLevel != null ? orgLevel.trim().toUpperCase(Locale.ROOT) : "";
        if (level.isEmpty()) {
            return List.of();
        }
        Map<String, String> row = loadMatrixRowForOrgLevel(level);
        List<String> out = new ArrayList<>();
        for (String url : TABLET_MENU_URLS) {
            if ("Y".equalsIgnoreCase(row.get(url))) {
                out.add(url);
            }
        }
        return out;
    }

    public boolean isTabletUrlExposedForOrgLevel(String orgLevel, String pageUrl) {
        if (!isTabletCatalogUrl(pageUrl)) {
            return true;
        }
        String level = orgLevel != null ? orgLevel.trim().toUpperCase(Locale.ROOT) : "";
        if (level.isEmpty()) {
            return false;
        }
        return "Y".equalsIgnoreCase(loadMatrixRowForOrgLevel(level).get(pageUrl.trim()));
    }

    /** 사용자설정·본사권한 UI — 조직 단계별 태블릿 노출 URL 목록 */
    public Map<String, List<String>> buildTabletExposureByLevelForApi() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (OrgLevel lv : OrgLevel.values()) {
            out.put(lv.name(), listExposedTabletUrlsForOrgLevel(lv.name()));
        }
        return out;
    }

    private Map<String, String> loadMatrixRowForOrgLevel(String orgLevel) {
        Map<String, String> row = new LinkedHashMap<>();
        List<OrgTabletMenu> rows = orgTabletMenuRepository.findByOrgLevelOrderByPageUrlAsc(orgLevel);
        Set<String> known = new LinkedHashSet<>(TABLET_MENU_URLS);
        for (String u : TABLET_MENU_URLS) {
            row.put(u, "N");
        }
        for (OrgTabletMenu r : rows) {
            if (r.getPageUrl() != null && known.contains(r.getPageUrl())) {
                row.put(r.getPageUrl(), "Y".equalsIgnoreCase(trim(r.getUseYn())) ? "Y" : "N");
            }
        }
        /* DB에 행이 전혀 없으면 기본 Y(초기·마이그레이션 직후와 동일 동작) */
        if (rows.isEmpty()) {
            for (String u : TABLET_MENU_URLS) {
                row.put(u, "Y");
            }
        }
        return row;
    }

    @Transactional
    public void saveTabletMatrix(Map<String, Map<String, String>> matrix) {
        if (matrix == null) return;
        Set<String> allowed = new LinkedHashSet<>(TABLET_MENU_URLS);
        for (Map.Entry<String, Map<String, String>> e : matrix.entrySet()) {
            String orgLevel = e.getKey() != null ? e.getKey().trim().toUpperCase(Locale.ROOT) : "";
            if (orgLevel.isEmpty()) continue;
            Map<String, String> pages = e.getValue();
            if (pages == null) continue;
            Map<String, OrgTabletMenu> existingByUrl = new HashMap<>();
            for (OrgTabletMenu r : orgTabletMenuRepository.findByOrgLevelOrderByPageUrlAsc(orgLevel)) {
                if (r.getPageUrl() != null && !r.getPageUrl().isBlank()) {
                    existingByUrl.put(r.getPageUrl().trim(), r);
                }
            }
            for (Map.Entry<String, String> pe : pages.entrySet()) {
                String url = pe.getKey() != null ? pe.getKey().trim() : "";
                if (!allowed.contains(url)) continue;
                String yn = "Y".equalsIgnoreCase(trim(pe.getValue())) ? "Y" : "N";
                OrgTabletMenu row = existingByUrl.get(url);
                if (row != null) {
                    row.setUseYn(yn);
                } else {
                    row = new OrgTabletMenu();
                    row.setOrgLevel(orgLevel);
                    row.setPageUrl(url);
                    row.setUseYn(yn);
                }
                orgTabletMenuRepository.save(row);
            }
        }
    }

    /**
     * 로그인 사용자에게 태블릿 모드에서 네비게이션 허용할 URL 목록.
     * 조직 단계 설정(use_yn=Y)과 {@link OrgPagePermissionService#resolvePagePermissionsForUser} 교집합.
     */
    public List<String> resolveTabletMenuUrlsForUser(AppUser user, Map<String, String> pagePermissions) {
        List<String> out = new ArrayList<>();
        if (user == null) return out;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            for (String url : TABLET_MENU_URLS) {
                if (pagePermissions == null || !isNone(pagePermissions.get(url))) {
                    out.add(url);
                }
            }
            return out;
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        if (org == null) return out;
        Object tfu = org.get("tabletFeatureUseYn");
        if (tfu != null && "N".equalsIgnoreCase(String.valueOf(tfu).trim())) {
            return out;
        }
        String level = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        if (level.isEmpty()) return out;

        Map<String, String> tabletRow = loadMatrixRowForOrgLevel(level);
        for (String url : TABLET_MENU_URLS) {
            if (!"Y".equalsIgnoreCase(tabletRow.get(url))) continue;
            if (pagePermissions == null || !isNone(pagePermissions.get(url))) {
                out.add(url);
            }
        }
        return out;
    }

    private static boolean isNone(String perm) {
        return "NONE".equalsIgnoreCase(trim(perm));
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
