package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.merchantdeploy.MerchantSandboxAdminService;
import com.pg.service.AuthService;
import com.pg.service.CompService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 가맹 샌드박스 내역·통보·설정 (관리자: 총본사·본사·ADMIN).
 */
@RestController
@RequestMapping(value = "/api/hq/merchant-sandbox", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHqMerchantSandboxController {

    private final MerchantSandboxAdminService sandboxAdminService;
    private final CompService compService;
    private final AuthService authService;

    public ApiHqMerchantSandboxController(MerchantSandboxAdminService sandboxAdminService,
                                          CompService compService,
                                          AuthService authService) {
        this.sandboxAdminService = sandboxAdminService;
        this.compService = compService;
        this.authService = authService;
    }

    @GetMapping("/txns")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> txns(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                sandboxAdminService.searchTxns(searchCompId, searchStatus, page, size)));
    }

    @GetMapping("/notifies")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> notifies(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                sandboxAdminService.searchNotifies(searchCompId, page, size)));
    }

    @PostMapping("/notify/resend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resend(@RequestBody Map<String, Object> body) {
        try {
            assertAdminEditor();
            Long logId = null;
            if (body != null && body.get("logId") != null) {
                logId = Long.parseLong(body.get("logId").toString().trim());
            }
            return ResponseEntity.ok(ApiResponse.ok(sandboxAdminService.resendNotify(logId)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settings(@RequestParam String compId) {
        try {
            assertCanViewComp(compId);
            return ResponseEntity.ok(ApiResponse.ok(sandboxAdminService.getSettings(compId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveSettings(@RequestBody Map<String, Object> body) {
        try {
            assertAdminEditor();
            String compId = str(body.get("compId"));
            assertCanViewComp(compId);
            return ResponseEntity.ok(ApiResponse.ok(sandboxAdminService.saveSettings(
                    compId,
                    str(body.get("sandboxUseYn")),
                    str(body.get("notifyUrlBackgroundSandbox")),
                    str(body.get("notifyUrlResultSandbox")))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/retain-days")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retainDays() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "sandboxRetainDays", sandboxAdminService.getRetainDays())));
    }

    @PostMapping("/retain-days")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveRetainDays(@RequestBody Map<String, Object> body) {
        try {
            assertAdminEditor();
            Integer days = null;
            if (body != null && body.get("sandboxRetainDays") != null) {
                days = Integer.parseInt(body.get("sandboxRetainDays").toString().trim());
            }
            return ResponseEntity.ok(ApiResponse.ok(sandboxAdminService.saveRetainDays(days)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "VALIDATION", "VALIDATION"));
        }
    }

    private void assertAdminEditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            throw new IllegalStateException("관리자만 수정할 수 있습니다.");
        }
        if (u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().trim())) {
            return;
        }
        String orgLevel = resolveOrgLevel(u);
        if (OrgLevel.HEADQUARTERS.name().equalsIgnoreCase(orgLevel)
                || OrgLevel.REGIONAL.name().equalsIgnoreCase(orgLevel)
                || "총본사".equals(orgLevel) || "본사".equals(orgLevel)) {
            return;
        }
        throw new IllegalStateException("샌드박스 설정은 총본사·본사 관리자만 수정할 수 있습니다.");
    }

    private String resolveOrgLevel(AppUser u) {
        if (u.getOrgUnitCode() != null && !u.getOrgUnitCode().isBlank()) {
            String n = compService.findOrgLevelNameByCompCode(u.getOrgUnitCode().trim());
            if (n != null && !n.isBlank()) {
                return n;
            }
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        if (org != null && org.get("orgLevel") != null) {
            return org.get("orgLevel").toString();
        }
        return "";
    }

    private void assertCanViewComp(String compId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                Map<String, Object> org = authService.getOrgInfo(u.getUsername());
                String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
                String target = compId != null ? compId.trim() : "";
                if (mine.isEmpty() || target.isEmpty() || !compService.isTargetUnderViewerOrg(mine, target)) {
                    throw new IllegalArgumentException("소속 업체 및 하위 가맹점만 조회할 수 있습니다.");
                }
            }
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
