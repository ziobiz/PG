package com.pg.service;

import com.pg.catalog.PageMenuCatalog;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgPagePermission;
import com.pg.entity.OrgUnit;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnitAssistantPagePermission;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.OrgUnitPagePermission;
import com.pg.repository.HqNotifyEnvConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgPagePermissionRepository;
import com.pg.repository.OrgUnitAssistantPagePermissionRepository;
import com.pg.repository.OrgUnitPagePermissionRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.ChatbotMerchantAdminConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
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

    public static final List<String> ASSISTANT_ROLE_TYPES =
            List.of("MANAGER", "OPERATOR", "SETTLEMENT", "TECH", ChatbotMerchantAdminConstants.ASSISTANT_ROLE_TYPE);

    /** 운영관리(/ops/*)에 있던 배포 문서 URL — 권한·북마크 호환용 */
    private static final Map<String, String> LEGACY_OPS_TO_DEPLOY_URL = Map.of(
            "/ops/integrationPlan", "/deploy/integrationPlan",
            "/ops/jpayWorkPlan", "/deploy/jpayWorkPlan",
            "/ops/merchantApiPolicy", "/deploy/merchantApiPolicy",
            "/ops/launchChecklist", "/deploy/launchChecklist"
    );

    /**
     * 담당자(ASSISTANT)별 메뉴 상한 — DB에 tb_org_unit_assistant_page_permission 행이 없을 때 적용.
     * 조직 개별 권한(ceiling)과 교집합되어 최종 접근이 결정됩니다.
     * <ul>
     *   <li>MANAGER — 전 메뉴 {@link #P_DELETE}</li>
     *   <li>OPERATOR — 사용자관리·정산관리 그룹 제외 전부</li>
     *   <li>SETTLEMENT — 업체관리·결제관리·정산관리·챗봇관리·분할관리</li>
     *   <li>TECH — 결제관리·통보관리</li>
     *   <li>CHATBOT_ADMIN — 업체관리·결제관리·챗봇관리·분할관리</li>
     * </ul>
     */
    public static String defaultAssistantFloorForCatalogItem(String assistantRoleType, PageMenuCatalog.PageMenuItem item) {
        if (item == null) {
            return P_NONE;
        }
        String role = trim(assistantRoleType).toUpperCase(Locale.ROOT);
        String g = item.parentGroup() != null ? item.parentGroup() : "";
        return defaultAssistantAllowsParentGroup(role, g) ? P_DELETE : P_NONE;
    }

    private static boolean defaultAssistantAllowsParentGroup(String roleUpper, String parentGroup) {
        String g = parentGroup != null ? parentGroup : "";
        if ("MANAGER".equals(roleUpper)) {
            return true;
        }
        if ("OPERATOR".equals(roleUpper)) {
            return !"사용자관리".equals(g) && !"정산관리".equals(g);
        }
        if ("SETTLEMENT".equals(roleUpper)) {
            return "업체관리".equals(g) || "결제관리".equals(g) || "정산관리".equals(g) || "챗봇관리".equals(g) || "분할관리".equals(g);
        }
        if ("TECH".equals(roleUpper)) {
            return "결제관리".equals(g) || "통보관리".equals(g);
        }
        if (ChatbotMerchantAdminConstants.ASSISTANT_ROLE_TYPE.equals(roleUpper)) {
            return "업체관리".equals(g) || "결제관리".equals(g) || "챗봇관리".equals(g) || "분할관리".equals(g);
        }
        return true;
    }

    private static Map<String, String> buildCodeOnlyDefaultAssistantRoleMap(String assistantRoleTypeUpper) {
        String role = assistantRoleTypeUpper != null ? assistantRoleTypeUpper.trim().toUpperCase(Locale.ROOT) : "";
        Map<String, String> m = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            m.put(item.pageUrl(), defaultAssistantFloorForCatalogItem(role, item));
        }
        return m;
    }

    /**
     * 조직 최종 권한(ceiling) 위에 담당자 역할 기본(및 저장된 오버라이드)을 합성합니다.
     */
    public Map<String, String> mergeAssistantRoleOverlay(Map<String, String> orgCeiling, String assistantRoleTypeUpper, long orgUnitId) {
        if (orgCeiling == null) {
            return new LinkedHashMap<>();
        }
        String art = assistantRoleTypeUpper != null ? assistantRoleTypeUpper.trim().toUpperCase(Locale.ROOT) : "";
        Map<String, String> byUrl = new HashMap<>();
        for (OrgUnitAssistantPagePermission r : orgUnitAssistantPagePermissionRepository
                .findByOrgUnitIdOrderByAssistantRoleTypeAscPageUrlAsc(orgUnitId)) {
            if (r.getAssistantRoleType() != null && art.equalsIgnoreCase(r.getAssistantRoleType().trim())
                    && r.getPageUrl() != null && !r.getPageUrl().isBlank()) {
                byUrl.put(r.getPageUrl().trim(), normalizePerm(r.getPermission()));
            }
        }
        AssistantMatrixStorage shell = readAssistantMatrixStorageFromDb();
        OrgUnit ou = orgUnitRepository.findById(orgUnitId).orElse(null);
        OrgLevel orgLevel = ou != null ? ou.getOrgLevel() : null;
        Map<String, String> roleDefault = resolveDefaultAssistantRoleMap(art, orgLevel, shell);
        Map<String, String> out = new LinkedHashMap<>();
        String orgLevelCode = orgLevel != null ? orgLevel.name() : "";
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            String url = item.pageUrl();
            String ceiling = normalizePerm(orgCeiling.getOrDefault(url, P_DELETE));
            String roleWant = byUrl.containsKey(url) ? byUrl.get(url) : roleDefault.getOrDefault(url, P_DELETE);
            if (P_NONE.equals(ceiling)) {
                out.put(url, P_NONE);
            } else {
                out.put(url, intersectPermission(ceiling, roleWant));
            }
        }
        enforceTabletExposureOnRoleMap(orgLevelCode, out);
        return out;
    }

    private final OrgPagePermissionRepository orgPagePermissionRepository;
    private final OrgUnitPagePermissionRepository orgUnitPagePermissionRepository;
    private final OrgUnitAssistantPagePermissionRepository orgUnitAssistantPagePermissionRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final AuthService authService;
    private final OrgUnitChangeAuditService orgUnitChangeAuditService;
    private final PayFollowPolicyService payFollowPolicyService;
    private final HqNotifyEnvConfigRepository hqNotifyEnvConfigRepository;
    private final OrgTabletMenuService orgTabletMenuService;

    private static final ObjectMapper ASSISTANT_MATRIX_JSON = new ObjectMapper();

    public OrgPagePermissionService(OrgPagePermissionRepository orgPagePermissionRepository,
                                      OrgUnitPagePermissionRepository orgUnitPagePermissionRepository,
                                      OrgUnitAssistantPagePermissionRepository orgUnitAssistantPagePermissionRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      MerchantProfileRepository merchantProfileRepository,
                                      AuthService authService,
                                      OrgUnitChangeAuditService orgUnitChangeAuditService,
                                      @Lazy PayFollowPolicyService payFollowPolicyService,
                                      HqNotifyEnvConfigRepository hqNotifyEnvConfigRepository,
                                      @Lazy OrgTabletMenuService orgTabletMenuService) {
        this.orgPagePermissionRepository = orgPagePermissionRepository;
        this.orgUnitPagePermissionRepository = orgUnitPagePermissionRepository;
        this.orgUnitAssistantPagePermissionRepository = orgUnitAssistantPagePermissionRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.authService = authService;
        this.orgUnitChangeAuditService = orgUnitChangeAuditService;
        this.payFollowPolicyService = payFollowPolicyService;
        this.hqNotifyEnvConfigRepository = hqNotifyEnvConfigRepository;
        this.orgTabletMenuService = orgTabletMenuService;
    }

    /** 태블릿설정에서 노출되지 않은 태블릿 전용 URL은 담당자 권한을 접근불가로 고정 */
    private void enforceTabletExposureOnRoleMap(String orgLevel, Map<String, String> roleMap) {
        if (roleMap == null || orgLevel == null || orgLevel.isBlank()) {
            return;
        }
        for (String url : OrgTabletMenuService.TABLET_MENU_URLS) {
            if (!orgTabletMenuService.isTabletUrlExposedForOrgLevel(orgLevel, url)) {
                roleMap.put(url, P_NONE);
            }
        }
    }

    /**
     * 본사 사용자설정 UI: 조직 단계(총본사~가맹점)별로 코드 기본 + HQ 저장값이 합성된 담당자 역할×URL 권한.
     */
    public Map<String, Map<String, Map<String, String>>> getHqAssistantDefaultMatrixByLevelResolvedForApi() {
        AssistantMatrixStorage shell = readAssistantMatrixStorageFromDb();
        Map<String, Map<String, Map<String, String>>> out = new LinkedHashMap<>();
        for (OrgLevel ol : OrgLevel.values()) {
            Map<String, Map<String, String>> roleMap = new LinkedHashMap<>();
            for (String role : ASSISTANT_ROLE_TYPES) {
                roleMap.put(role, new LinkedHashMap<>(resolveDefaultAssistantRoleMap(role, ol, shell)));
            }
            out.put(ol.name(), roleMap);
        }
        return out;
    }

    /** 사용자설정 일괄 적용 UI — {@link OrgLevel} 순서대로. */
    public List<Map<String, Object>> getAssistantOrgLevelsForApi() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OrgLevel lv : OrgLevel.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", lv.name());
            m.put("nameKo", lv.getNameKo());
            m.put("code", lv.getCode());
            rows.add(m);
        }
        return rows;
    }

    public List<Map<String, Object>> getAssistantMatrixCatalogForApi() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PageMenuCatalog.PageMenuItem it : PageMenuCatalog.items()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("pageUrl", it.pageUrl());
            r.put("menuId", it.menuId());
            r.put("menuNm", it.menuName());
            r.put("parentGroup", it.parentGroup());
            rows.add(r);
        }
        return rows;
    }

    /** 사용자설정 「태블릿모드」 매트릭스 행 — {@link OrgTabletMenuService#TABLET_MENU_URLS} 순서 */
    public List<Map<String, Object>> getAssistantTabletMatrixCatalogForApi() {
        Map<String, PageMenuCatalog.PageMenuItem> byUrl = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem it : PageMenuCatalog.items()) {
            byUrl.put(it.pageUrl(), it);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String url : OrgTabletMenuService.TABLET_MENU_URLS) {
            PageMenuCatalog.PageMenuItem it = byUrl.get(url);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("pageUrl", url);
            r.put("menuNm", it != null ? it.menuName() : url);
            r.put("parentGroup", it != null ? it.parentGroup() : "태블릿모드");
            if (it != null) {
                r.put("menuId", it.menuId());
            }
            rows.add(r);
        }
        return rows;
    }

    public String normalizeAssistantRoleDefaultMatrixToJson(Object raw) throws JsonProcessingException {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("assistantRoleDefaultMatrix 는 객체여야 합니다.");
        }
        Set<String> catalogUrls = PageMenuCatalog.items().stream()
                .map(PageMenuCatalog.PageMenuItem::pageUrl)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> validLevels = Arrays.stream(OrgLevel.values()).map(Enum::name).collect(Collectors.toCollection(LinkedHashSet::new));
        AssistantMatrixStorage cleaned = AssistantMatrixStorage.fromClient(root).sanitize(catalogUrls, ASSISTANT_ROLE_TYPES, validLevels);
        clampTabletMenusInAssistantStorage(cleaned);
        if (cleaned.isEmpty()) {
            return null;
        }
        return ASSISTANT_MATRIX_JSON.writeValueAsString(cleaned.toJsonRoot());
    }

    private AssistantMatrixStorage readAssistantMatrixStorageFromDb() {
        String json = hqNotifyEnvConfigRepository.findFirstByOrderByIdAsc()
                .map(HqNotifyEnvConfig::getAssistantRoleDefaultMatrixJson)
                .orElse(null);
        return AssistantMatrixStorage.parseDbJson(json);
    }

    private Map<String, String> resolveDefaultAssistantRoleMap(String assistantRoleTypeUpper, OrgLevel orgLevel,
                                                               AssistantMatrixStorage shell) {
        String role = assistantRoleTypeUpper != null ? assistantRoleTypeUpper.trim().toUpperCase(Locale.ROOT) : "";
        Map<String, String> out = new LinkedHashMap<>(buildCodeOnlyDefaultAssistantRoleMap(role));
        mergeRoleUrlOverlayInto(out, shell.global.get(role));
        if (orgLevel != null) {
            Map<String, Map<String, String>> lvl = shell.byLevel.get(orgLevel.name());
            if (lvl != null) {
                mergeRoleUrlOverlayInto(out, lvl.get(role));
            }
            enforceTabletExposureOnRoleMap(orgLevel.name(), out);
        }
        return out;
    }

    private static void mergeRoleUrlOverlayInto(Map<String, String> out, Map<String, String> overlay) {
        if (overlay == null || overlay.isEmpty() || out == null) {
            return;
        }
        for (Map.Entry<String, String> e : overlay.entrySet()) {
            String u = e.getKey() != null ? e.getKey().trim() : "";
            if (u.isEmpty() || !out.containsKey(u)) {
                continue;
            }
            out.put(u, normalizePerm(e.getValue()));
        }
    }

    /**
     * HQ 저장 JSON — 레거시(역할만 최상위) 또는 v2(global + byLevel).
     */
    private static final class AssistantMatrixStorage {
        final Map<String, Map<String, String>> global;
        /** OrgLevel.name() → role → url → perm */
        final Map<String, Map<String, Map<String, String>>> byLevel;

        private AssistantMatrixStorage(Map<String, Map<String, String>> global,
                                       Map<String, Map<String, Map<String, String>>> byLevel) {
            this.global = global != null ? global : new LinkedHashMap<>();
            this.byLevel = byLevel != null ? byLevel : new LinkedHashMap<>();
        }

        static AssistantMatrixStorage empty() {
            return new AssistantMatrixStorage(new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        boolean isEmpty() {
            if (hasUrlEntries(global)) {
                return false;
            }
            for (Map<String, Map<String, String>> m : byLevel.values()) {
                if (hasUrlEntries(m)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean hasUrlEntries(Map<String, Map<String, String>> roleMap) {
            if (roleMap == null) {
                return false;
            }
            for (Map<String, String> row : roleMap.values()) {
                if (row != null && !row.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        AssistantMatrixStorage sanitize(Set<String> catalogUrls, List<String> roles, Set<String> validLevels) {
            Map<String, Map<String, String>> g = sanitizeRoleMap(global, catalogUrls, roles);
            Map<String, Map<String, Map<String, String>>> bl = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Map<String, String>>> le : byLevel.entrySet()) {
                String lvl = le.getKey() != null ? le.getKey().trim() : "";
                if (!validLevels.contains(lvl)) {
                    continue;
                }
                Map<String, Map<String, String>> inner = new LinkedHashMap<>();
                if (le.getValue() != null) {
                    for (String role : roles) {
                        Map<String, String> row = le.getValue().get(role);
                        Map<String, String> cleanRow = new LinkedHashMap<>();
                        if (row != null) {
                            for (Map.Entry<String, String> pe : row.entrySet()) {
                                String url = pe.getKey() != null ? pe.getKey().trim() : "";
                                if (catalogUrls.contains(url)) {
                                    cleanRow.put(url, normalizePermStatic(pe.getValue()));
                                }
                            }
                        }
                        if (!cleanRow.isEmpty()) {
                            inner.put(role, cleanRow);
                        }
                    }
                }
                if (!inner.isEmpty()) {
                    bl.put(lvl, inner);
                }
            }
            return new AssistantMatrixStorage(g, bl);
        }

        private static Map<String, Map<String, String>> sanitizeRoleMap(Map<String, Map<String, String>> raw,
                                                                        Set<String> catalogUrls, List<String> roles) {
            Map<String, Map<String, String>> g = new LinkedHashMap<>();
            if (raw == null) {
                return g;
            }
            for (String role : roles) {
                Map<String, String> row = raw.get(role);
                Map<String, String> cleanRow = new LinkedHashMap<>();
                if (row != null) {
                    for (Map.Entry<String, String> pe : row.entrySet()) {
                        String url = pe.getKey() != null ? pe.getKey().trim() : "";
                        if (catalogUrls.contains(url)) {
                            cleanRow.put(url, normalizePermStatic(pe.getValue()));
                        }
                    }
                }
                if (!cleanRow.isEmpty()) {
                    g.put(role, cleanRow);
                }
            }
            return g;
        }

        Map<String, Object> toJsonRoot() {
            Map<String, Object> root = new LinkedHashMap<>();
            if (!global.isEmpty()) {
                root.put("global", new LinkedHashMap<>(global));
            }
            if (!byLevel.isEmpty()) {
                Map<String, Object> bl = new LinkedHashMap<>();
                for (Map.Entry<String, Map<String, Map<String, String>>> e : byLevel.entrySet()) {
                    Map<String, Object> inner = new LinkedHashMap<>();
                    for (Map.Entry<String, Map<String, String>> re : e.getValue().entrySet()) {
                        inner.put(re.getKey(), new LinkedHashMap<>(re.getValue()));
                    }
                    bl.put(e.getKey(), inner);
                }
                root.put("byLevel", bl);
            }
            root.put("v", 2);
            return root;
        }

        static AssistantMatrixStorage fromClient(Map<?, ?> root) {
            if (root == null) {
                return empty();
            }
            boolean v2 = root.containsKey("global") || root.containsKey("byLevel");
            if (v2) {
                Map<String, Map<String, String>> g = parseRoleUrlMapLayer(root.get("global"));
                Map<String, Map<String, Map<String, String>>> bl = new LinkedHashMap<>();
                Object blo = root.get("byLevel");
                if (blo instanceof Map<?, ?> blm) {
                    for (Map.Entry<?, ?> le : blm.entrySet()) {
                        String lvl = le.getKey() != null ? le.getKey().toString().trim() : "";
                        if (lvl.isEmpty()) {
                            continue;
                        }
                        if (le.getValue() instanceof Map<?, ?> rm) {
                            bl.put(lvl, parseRoleUrlMapLayer(rm));
                        }
                    }
                }
                return new AssistantMatrixStorage(g, bl);
            }
            // 레거시: 최상위 키가 역할명
            return new AssistantMatrixStorage(parseRoleUrlMapLayer(root), new LinkedHashMap<>());
        }

        static AssistantMatrixStorage parseDbJson(String json) {
            if (json == null || json.isBlank()) {
                return empty();
            }
            try {
                Map<String, Object> root = ASSISTANT_MATRIX_JSON.readValue(json, new TypeReference<Map<String, Object>>() {
                });
                return fromClient(root);
            } catch (Exception e) {
                return empty();
            }
        }

        private static Map<String, Map<String, String>> parseRoleUrlMapLayer(Object layerObj) {
            Map<String, Map<String, String>> out = new LinkedHashMap<>();
            if (!(layerObj instanceof Map<?, ?> layer)) {
                return out;
            }
            for (Map.Entry<?, ?> re : layer.entrySet()) {
                String role = re.getKey() != null ? re.getKey().toString().trim().toUpperCase(Locale.ROOT) : "";
                if (!ASSISTANT_ROLE_TYPES.contains(role)) {
                    continue;
                }
                if (!(re.getValue() instanceof Map<?, ?> rm)) {
                    continue;
                }
                Map<String, String> inner = new LinkedHashMap<>();
                for (Map.Entry<?, ?> pe : rm.entrySet()) {
                    String url = pe.getKey() != null ? pe.getKey().toString().trim() : "";
                    inner.put(url, normalizePermStatic(String.valueOf(pe.getValue())));
                }
                if (!inner.isEmpty()) {
                    out.put(role, inner);
                }
            }
            return out;
        }
    }

    /**
     * 로그인 사용자 최종 페이지 권한.
     * 조직에 연결된 계정(총본사·본사·총판·가맹 등)은 역할이 ADMIN 이어도 본사권한설정을 적용합니다.
     * 조직 미연결 ADMIN 만 null(제한 없음).
     */
    public Map<String, String> resolvePagePermissionsForUser(AppUser user) {
        if (user == null) return null;
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
        Map<String, String> layered = applyAssistantRoleOverlayIfNeeded(user, base, org);
        return elevateMerchantSplitPayMenusIfEligible(user, org,
                elevateMerchantChatbotAdminChatbotMenusIfEligible(user, org, layered));
    }

    /**
     * 가맹(MERCHANT)이고 챗봇결제 사용(Y)·업체 대표 또는 CHATBOT 권한그룹이면 챗봇관리 메뉴 최소 사용권한을 확보합니다(조직 권한이 NONE이던 경우도).
     */
    private Map<String, String> elevateMerchantChatbotAdminChatbotMenusIfEligible(AppUser user, Map<String, Object> org,
                                                                                   Map<String, String> permissions) {
        if (user == null || org == null || permissions == null) {
            return permissions;
        }
        if (!OrgLevel.MERCHANT.name().equalsIgnoreCase(trim(String.valueOf(org.getOrDefault("orgLevel", ""))))) {
            return permissions;
        }
        if (!ChatbotMerchantAdminConstants.merchantAdminWebMayUseChatbotFeatures(user)) {
            return permissions;
        }
        Object ouIdObj = org.get("orgUnitId");
        if (ouIdObj == null) {
            return permissions;
        }
        long ouId;
        try {
            ouId = Long.parseLong(ouIdObj.toString().trim());
        } catch (NumberFormatException e) {
            return permissions;
        }
        String chatbotYn = merchantProfileRepository.findByOrgUnitId(ouId)
                .map(MerchantProfile::getChatbotPaymentUseYn)
                .orElse("N");
        if (!"Y".equalsIgnoreCase(trim(chatbotYn))) {
            return permissions;
        }
        Map<String, String> out = new LinkedHashMap<>(permissions);
        String floor = P_DELETE;
        raisePermissionFloor(out, "/chatbot/productMng", floor);
        raisePermissionFloor(out, "/chatbot/chatbotKbMng", floor);
        return out;
    }

    /**
     * 가맹(MERCHANT)이고 분할결제 사용(Y)이면 분할관리·분할결제 관련 메뉴 최소 사용권한을 확보합니다.
     */
    private Map<String, String> elevateMerchantSplitPayMenusIfEligible(AppUser user, Map<String, Object> org,
                                                                      Map<String, String> permissions) {
        if (user == null || org == null || permissions == null) {
            return permissions;
        }
        if (!OrgLevel.MERCHANT.name().equalsIgnoreCase(trim(String.valueOf(org.getOrDefault("orgLevel", ""))))) {
            return permissions;
        }
        Object ouIdObj = org.get("orgUnitId");
        if (ouIdObj == null) {
            return permissions;
        }
        long ouId;
        try {
            ouId = Long.parseLong(ouIdObj.toString().trim());
        } catch (NumberFormatException e) {
            return permissions;
        }
        String splitYn = merchantProfileRepository.findByOrgUnitId(ouId)
                .map(MerchantProfile::getSplitPayEnabledYn)
                .orElse("N");
        if (!"Y".equalsIgnoreCase(trim(splitYn))) {
            return permissions;
        }
        Map<String, String> out = new LinkedHashMap<>(permissions);
        String floor = P_DELETE;
        raisePermissionFloor(out, "/calc/splitPayList", floor);
        raisePermissionFloor(out, "/pay/splitPay", floor);
        raisePermissionFloor(out, "/splitpay/progressMng", floor);
        raisePermissionFloor(out, "/splitpay/mailMng", floor);
        return out;
    }

    private void raisePermissionFloor(Map<String, String> map, String pageUrl, String floorPerm) {
        if (map == null || pageUrl == null || pageUrl.isBlank()) {
            return;
        }
        String cur = normalizePerm(map.get(pageUrl));
        String fl = normalizePerm(floorPerm);
        map.put(pageUrl, permFromStrength(Math.max(strength(cur), strength(fl))));
    }

    /**
     * ASSISTANT 계정이면 담당자 역할별 기본 메뉴 상한(및 tb_org_unit_assistant_page_permission 저장값)을
     * 조직 최종 권한(ceiling)과 교집합하여 병합합니다. URL별 저장 행이 없으면 역할 기본만 적용합니다.
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
        return mergeAssistantRoleOverlay(orgEffective, art, ouId);
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
        if (OrgLevel.MERCHANT.name().equals(orgLevel) && rows.isEmpty()) {
            return defaultMerchantRestrictedMap();
        }
        Map<String, String> byUrl = new HashMap<>();
        for (OrgPagePermission r : rows) {
            if (r.getPageUrl() != null && r.getPermission() != null) {
                byUrl.put(r.getPageUrl().trim(), normalizePerm(r.getPermission()));
            }
        }
        mergeLegacyOpsDeployPermissions(byUrl);
        Map<String, String> out = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            String url = item.pageUrl();
            String p = byUrl.get(url);
            if (p == null && "/calc/unpaidMng".equals(url)) {
                p = defaultUnpaidMngPermissionForOrgLevel(orgLevel);
            }
            if (p == null && OrgLevel.MERCHANT.name().equals(orgLevel)) {
                if ("/system/noticeList".equals(url) || "/comp/myCompMng".equals(url)
                        || "/comp/merchantApiPortal".equals(url)) {
                    p = P_OBSERVER;
                }
            }
            if (p == null && !OrgLevel.MERCHANT.name().equals(orgLevel)
                    && "/comp/merchantApiPortal".equals(url)) {
                p = P_NONE;
            }
            out.put(url, p != null ? p : P_DELETE);
        }
        return out;
    }

    /**
     * 단계(tb_org_page_permission)에 행이 없을 때 미수금관리 URL 기본값.
     * 총본사·본사·총판은 등록·대손 등 쓰기 가능(DELETE), 그 외(지사·대리점·영업점 등)는 조회만(OBSERVER).
     * 저장된 단계/조직별 권한이 있으면 그 값이 우선합니다.
     */
    private static String defaultUnpaidMngPermissionForOrgLevel(String orgLevel) {
        if (orgLevel == null || orgLevel.isBlank()) {
            return P_OBSERVER;
        }
        String ol = orgLevel.trim().toUpperCase(Locale.ROOT);
        if (OrgLevel.HEADQUARTERS.name().equals(ol) || OrgLevel.REGIONAL.name().equals(ol) || OrgLevel.MASTER_DIST.name().equals(ol)) {
            return P_DELETE;
        }
        return P_OBSERVER;
    }

    /**
     * 미수금 수동 등록·대손·취소 등 쓰기 API — 「미수금관리」(/calc/unpaidMng) 화면 권한이 MODIFY 이상일 때만 허용.
     * ADMIN 은 항상 허용. 본사권한설정에서 해당 URL을 OBSERVER/NONE 으로 내리면 동일 계정은 API에서도 차단됩니다.
     */
    public boolean canManuallyManageMerchantReceivable(AppUser user) {
        if (user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        Map<String, String> m = resolvePagePermissionsForUser(user);
        if (m == null) {
            return false;
        }
        String raw = m.get("/calc/unpaidMng");
        String p = normalizePerm(raw != null ? raw : P_NONE);
        return P_MODIFY.equals(p) || P_DELETE.equals(p);
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
        mergeLegacyOpsDeployPermissions(byUrl);
        Map<String, String> out = new LinkedHashMap<>();
        for (PageMenuCatalog.PageMenuItem item : PageMenuCatalog.items()) {
            String url = item.pageUrl();
            String levelPerm = base.getOrDefault(url, P_DELETE);
            if (byUrl.containsKey(url)) {
                out.put(url, intersectPermission(levelPerm, byUrl.get(url)));
            } else {
                out.put(url, levelPerm);
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
        out.put("tabletExposedUrls", orgTabletMenuService.listExposedTabletUrlsForOrgLevel(level));
        return out;
    }

    private Map<String, Map<String, String>> buildAssistantMatrixMap(long orgUnitId) {
        OrgUnit ou = orgUnitRepository.findById(orgUnitId).orElse(null);
        String level = ou != null && ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "";
        Map<String, String> ceiling = effectiveMapForOrgUnit(orgUnitId, level);
        Map<String, Map<String, String>> assist = new LinkedHashMap<>();
        for (String role : ASSISTANT_ROLE_TYPES) {
            assist.put(role, mergeAssistantRoleOverlay(ceiling, role, orgUnitId));
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
        int oldAsstCount = orgUnitAssistantPagePermissionRepository
                .findByOrgUnitIdOrderByAssistantRoleTypeAscPageUrlAsc(orgUnitId).size();
        String level = ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "";
        Map<String, String> effective = effectiveMapForOrgUnit(orgUnitId, level);
        orgUnitAssistantPagePermissionRepository.deleteByOrgUnitId(orgUnitId);
        orgUnitAssistantPagePermissionRepository.flush();
        if (matrix == null) {
            int newAsstCount = orgUnitAssistantPagePermissionRepository
                    .findByOrgUnitIdOrderByAssistantRoleTypeAscPageUrlAsc(orgUnitId).size();
            logAssistantPermissionAudit(ou, oldAsstCount, newAsstCount);
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
                if (OrgTabletMenuService.isTabletCatalogUrl(url)
                        && !orgTabletMenuService.isTabletUrlExposedForOrgLevel(level, url)) {
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
        int newAsstCount = orgUnitAssistantPagePermissionRepository
                .findByOrgUnitIdOrderByAssistantRoleTypeAscPageUrlAsc(orgUnitId).size();
        logAssistantPermissionAudit(ou, oldAsstCount, newAsstCount);
    }

    private void logAssistantPermissionAudit(OrgUnit ou, int oldCnt, int newCnt) {
        String cid = ou.getCode() != null ? ou.getCode().trim() : "";
        String cnm = ou.getName() != null ? ou.getName().trim() : "";
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), cid, cnm, "[조직권한] 담당자별메뉴 오버라이드 건수",
                String.valueOf(oldCnt), String.valueOf(newCnt));
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
            // 단일 조직만 보이더라도 개별 조직·적용방식 패널은 표시 (편집은 canSaveOrgUnit=false 로 제한)
            caps.put("showOrgUnitPanel", true);
            String compId = String.valueOf(orgMap.getOrDefault("compId", "")).trim();
            List<Map<String, Object>> all = listAllOrgUnitsForPermissionAdmin();
            List<Map<String, Object>> one = all.stream()
                    .filter(m -> compId.equals(String.valueOf(m.getOrDefault("code", "")).trim()))
                    .toList();
            if (one.isEmpty()) {
                Object ouIdObj = orgMap.get("orgUnitId");
                if (ouIdObj != null) {
                    try {
                        long ouId = Long.parseLong(ouIdObj.toString().trim());
                        one = all.stream()
                                .filter(m -> {
                                    Object ido = m.get("id");
                                    if (ido == null) {
                                        return false;
                                    }
                                    try {
                                        return ouId == Long.parseLong(ido.toString().trim());
                                    } catch (NumberFormatException e) {
                                        return false;
                                    }
                                })
                                .toList();
                    } catch (NumberFormatException ignored) {
                        one = List.of();
                    }
                }
            }
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
        String oldModeRaw = ou.getPagePermissionMode() != null ? ou.getPagePermissionMode().trim() : MODE_LEVEL_DEFAULT;
        int oldPermCount = orgUnitPagePermissionRepository.findByOrgUnitIdOrderByPageUrlAsc(orgUnitId).size();
        String m = mode != null ? mode.trim().toUpperCase(Locale.ROOT) : MODE_LEVEL_DEFAULT;
        if (!MODE_CUSTOM.equals(m) && !MODE_LEVEL_DEFAULT.equals(m)) {
            m = MODE_LEVEL_DEFAULT;
        }
        ou.setPagePermissionMode(m);
        orgUnitRepository.save(ou);

        orgUnitPagePermissionRepository.deleteByOrgUnitId(orgUnitId);
        orgUnitPagePermissionRepository.flush();

        if (!MODE_CUSTOM.equals(m) || pages == null) {
            int newPermCount = orgUnitPagePermissionRepository.findByOrgUnitIdOrderByPageUrlAsc(orgUnitId).size();
            logOrgPermissionAudit(ou, oldModeRaw, m, oldPermCount, newPermCount);
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
        int newPermCount = orgUnitPagePermissionRepository.findByOrgUnitIdOrderByPageUrlAsc(orgUnitId).size();
        logOrgPermissionAudit(ou, oldModeRaw, m, oldPermCount, newPermCount);
    }

    private void logOrgPermissionAudit(OrgUnit ou, String oldModeRaw, String newModeRaw, int oldPermCount, int newPermCount) {
        String cid = ou.getCode() != null ? ou.getCode().trim() : "";
        String cnm = ou.getName() != null ? ou.getName().trim() : "";
        String oldKo = MODE_CUSTOM.equalsIgnoreCase(oldModeRaw) ? "개별 설정" : "단계 기본";
        String newKo = MODE_CUSTOM.equalsIgnoreCase(newModeRaw) ? "개별 설정" : "단계 기본";
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), cid, cnm, "[조직권한] 메뉴권한방식", oldKo, newKo);
        orgUnitChangeAuditService.appendIfChanged(ou.getId(), cid, cnm, "[조직권한] 개별메뉴 건수",
                String.valueOf(oldPermCount), String.valueOf(newPermCount));
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
        payload.put("payFollowLevelCaps", payFollowPolicyService.buildLevelCapsPayload());
        payload.put("tabletMenuExposureByLevel", orgTabletMenuService.buildTabletExposureByLevelForApi());
        return payload;
    }

    private void clampTabletMenusInAssistantStorage(AssistantMatrixStorage storage) {
        if (storage == null) {
            return;
        }
        for (Map.Entry<String, Map<String, Map<String, String>>> le : storage.byLevel.entrySet()) {
            String lvl = le.getKey();
            if (le.getValue() == null) {
                continue;
            }
            for (Map<String, String> roleMap : le.getValue().values()) {
                enforceTabletExposureOnRoleMap(lvl, roleMap);
            }
        }
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

    /** DB에 남아 있는 /ops/* 배포 문서 권한을 /deploy/* 로 병합 */
    private static void mergeLegacyOpsDeployPermissions(Map<String, String> byUrl) {
        if (byUrl == null || byUrl.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> legacy : LEGACY_OPS_TO_DEPLOY_URL.entrySet()) {
            String from = legacy.getKey();
            String to = legacy.getValue();
            if (!byUrl.containsKey(from)) {
                continue;
            }
            String perm = byUrl.remove(from);
            if (byUrl.containsKey(to)) {
                byUrl.put(to, permFromStrength(Math.max(strength(byUrl.get(to)), strength(perm))));
            } else {
                byUrl.put(to, perm);
            }
        }
    }
}
