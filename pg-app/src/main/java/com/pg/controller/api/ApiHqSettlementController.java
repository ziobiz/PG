package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.entity.HqSettlementCycleDef;
import com.pg.service.AuthService;
import com.pg.service.HqSettlementCycleAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private final AuthService authService;

    public ApiHqSettlementController(HqSettlementCycleAdminService hqSettlementCycleAdminService,
                                     AuthService authService) {
        this.hqSettlementCycleAdminService = hqSettlementCycleAdminService;
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
