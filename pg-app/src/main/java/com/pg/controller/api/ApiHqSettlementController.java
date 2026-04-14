package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.entity.HqSettlementCycleDef;
import com.pg.entity.MasterDistSettlementCycleConfig;
import com.pg.service.AuthService;
import com.pg.service.HqSettlementCycleAdminService;
import com.pg.service.MasterDistSettlementCycleConfigService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 본사설정 — 정산관리설정(정산주기 관리·정산일정 요약).
 */
@RestController
@RequestMapping(value = "/api/hq/settlement", produces = "application/json")
public class ApiHqSettlementController {

    private final HqSettlementCycleAdminService hqSettlementCycleAdminService;
    private final MasterDistSettlementCycleConfigService masterDistSettlementCycleConfigService;
    private final AuthService authService;

    public ApiHqSettlementController(HqSettlementCycleAdminService hqSettlementCycleAdminService,
                                     MasterDistSettlementCycleConfigService masterDistSettlementCycleConfigService,
                                     AuthService authService) {
        this.hqSettlementCycleAdminService = hqSettlementCycleAdminService;
        this.masterDistSettlementCycleConfigService = masterDistSettlementCycleConfigService;
        this.authService = authService;
    }

    private boolean canManageSettlementSettings(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        return org != null && "HEADQUARTERS".equals(String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT));
    }

    /** 가맹·검색 화면 정산주기 셀렉트용(로그인 사용자) */
    @GetMapping("/cycleOptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> cycleOptions() {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.listActiveSelectOptions()));
    }

    /** 총판별 가맹 정산주기 설정 등 — 병합 표준 전체(미사용 N 포함), 순서·코드표시는 정산주기관리 병합 표와 동일 */
    @GetMapping("/cycleOptionsCatalog")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> cycleOptionsCatalog() {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.listCatalogSelectOptions()));
    }

    /**
     * 가맹 조직 트리 임의 노드 기준(권장: 가맹 {@code fromOrgUnitId}): 총판 허용 슬롯만(슬롯 순),
     * 본사 직속·총판 미설정이면 본사 필수 정산주기만(활성 Y).
     * {@code parentOrgUnitId} 는 하위 호환용 별칭({@code fromOrgUnitId} 가 없을 때만 사용).
     * {@code ensureCycleCode} 가 목록에 없으면(과거 저장값) 병합 카탈로그 행을 한 줄 덧붙이고 {@code orphanSavedCycleYn}=Y.
     */
    @GetMapping("/cycleOptionsScoped")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cycleOptionsScoped(
            @RequestParam(required = false) Long parentOrgUnitId,
            @RequestParam(required = false) Long fromOrgUnitId,
            @RequestParam(required = false) String ensureCycleCode) {
        Long start = fromOrgUnitId != null ? fromOrgUnitId : parentOrgUnitId;
        return ResponseEntity.ok(ApiResponse.ok(
                masterDistSettlementCycleConfigService.buildScopedCycleOptionsForMerchantParent(start, ensureCycleCode)));
    }

    @GetMapping("/masterDistOrgOptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> masterDistOrgOptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(masterDistSettlementCycleConfigService.listMasterDistOrgOptions()));
    }

    @GetMapping("/masterDistCalcCycleConfig")
    public ResponseEntity<ApiResponse<Map<String, Object>>> masterDistCalcCycleGet(
            @RequestParam long orgUnitId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        return masterDistSettlementCycleConfigService.findByMasterDistOrgId(orgUnitId)
                .map(c -> ResponseEntity.ok(ApiResponse.ok(masterDistSettlementCycleConfigService.toApiMap(c))))
                .orElseGet(() -> {
                    Map<String, Object> empty = new LinkedHashMap<>();
                    empty.put("orgUnitId", orgUnitId);
                    List<String> slots = new ArrayList<>(10);
                    for (int i = 0; i < 10; i++) {
                        slots.add(null);
                    }
                    empty.put("slots", slots);
                    empty.put("defaultSlot", 0);
                    return ResponseEntity.ok(ApiResponse.ok(empty));
                });
    }

    @PostMapping("/masterDistCalcCycleConfig")
    public ResponseEntity<ApiResponse<Map<String, Object>>> masterDistCalcCycleSave(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            if (body == null || body.get("orgUnitId") == null) {
                return ResponseEntity.ok(ApiResponse.fail("orgUnitId가 필요합니다.", "VALIDATION"));
            }
            long orgUnitId = parseRequestLong(body.get("orgUnitId"), "orgUnitId");
            int defaultSlot = parseRequestInt(body.get("defaultSlot"), 0, "defaultSlot");
            @SuppressWarnings("unchecked")
            List<Object> rawSlots = body.get("slots") instanceof List<?> l ? (List<Object>) l : new ArrayList<>();
            List<String> slots = new ArrayList<>();
            for (Object o : rawSlots) {
                slots.add(o != null ? String.valueOf(o).trim() : "");
            }
            MasterDistSettlementCycleConfig saved =
                    masterDistSettlementCycleConfigService.saveForMasterDist(orgUnitId, slots, defaultSlot);
            return ResponseEntity.ok(ApiResponse.ok(masterDistSettlementCycleConfigService.toApiMap(saved)));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = "총판 정산주기 저장 중 오류가 발생했습니다. (" + e.getClass().getSimpleName() + ")";
            }
            return ResponseEntity.ok(ApiResponse.fail(msg, "VALIDATION"));
        }
    }

    /** JSON Map 값이 Integer/Long/Double 등일 때 안전하게 long 변환(예: 11.0 문자열 방지). */
    private static long parseRequestLong(Object raw, String fieldName) {
        if (raw == null) {
            throw new IllegalArgumentException(fieldName + "가 필요합니다.");
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "가 비어 있습니다.");
        }
        try {
            if (s.contains("e") || s.contains("E") || s.indexOf('.') >= 0) {
                return (long) Double.parseDouble(s);
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 숫자 형식이 올바르지 않습니다: " + s);
        }
    }

    private static int parseRequestInt(Object raw, int defaultVal, String fieldName) {
        if (raw == null) {
            return defaultVal;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return defaultVal;
        }
        try {
            if (s.contains("e") || s.contains("E") || s.indexOf('.') >= 0) {
                return (int) Double.parseDouble(s);
            }
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 숫자 형식이 올바르지 않습니다: " + s);
        }
    }

    @GetMapping("/cycleDefs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> cycleDefs() {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.listMergedDefinitions()));
    }

    @GetMapping("/schedulePreview")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> schedulePreview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.schedulePreview(fromDate, toDate)));
    }

    @GetMapping("/merchantAutoCounts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> merchantAutoCounts() {
        return ResponseEntity.ok(ApiResponse.ok(hqSettlementCycleAdminService.autoMerchantCountByCycle()));
    }

    @PostMapping("/cycleDefs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCycleDef(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 등록할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            String family = body != null && body.get("family") != null ? String.valueOf(body.get("family")) : "";
            Integer offset = parseIntOrNull(body != null ? body.get("offset") : null);
            String wkKey = body != null && body.get("wkKey") != null ? String.valueOf(body.get("wkKey")) : "";
            String displayLabel = body != null && body.get("displayLabel") != null ? String.valueOf(body.get("displayLabel")) : "";
            String description = body != null && body.get("description") != null ? String.valueOf(body.get("description")) : "";
            Integer sortParsed = parseIntOrNull(body != null ? body.get("sortOrder") : null);
            int sort = sortParsed != null ? sortParsed : 100;
            String activeYn = body != null && body.get("activeYn") != null ? String.valueOf(body.get("activeYn")) : "Y";
            HqSettlementCycleDef saved = hqSettlementCycleAdminService.createCustom(family, offset, wkKey, displayLabel, description, sort, activeYn);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", saved.getId());
            m.put("cycleCode", saved.getCycleCode());
            return ResponseEntity.ok(ApiResponse.ok(m));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PutMapping("/cycleDefs/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCycleDef(@PathVariable long id,
                                                                           @RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 수정할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            String displayLabel = body != null && body.get("displayLabel") != null ? String.valueOf(body.get("displayLabel")) : null;
            String description = body != null && body.get("description") != null ? String.valueOf(body.get("description")) : null;
            Integer sortOrder = parseIntOrNull(body != null ? body.get("sortOrder") : null);
            String activeYn = body != null && body.get("activeYn") != null ? String.valueOf(body.get("activeYn")) : null;
            hqSettlementCycleAdminService.updateRow(id, displayLabel, description, sortOrder, activeYn);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /**
     * 표준 정산주기 코드 중 DB에 없는 행만 일괄 INSERT(내장 목록과 동일한 표시·설명·순서·사용=Y).
     */
    @PostMapping("/cycleDefs/seedMissing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedMissingCycleDefs() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 실행할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            int inserted = hqSettlementCycleAdminService.seedMissingStandardCycleDefs();
            return ResponseEntity.ok(ApiResponse.ok(Map.of("inserted", inserted)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @DeleteMapping("/cycleDefs/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCycleDef(@PathVariable long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!canManageSettlementSettings(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 삭제할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            hqSettlementCycleAdminService.deleteRow(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private static Integer parseIntOrNull(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s, 10);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
