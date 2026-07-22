package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.service.HqBulkOpsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 본사설정 — 리스크설정 일괄운영관리 */
@RestController
@RequestMapping(value = "/api/hq/bulkOps", produces = "application/json")
public class ApiHqBulkOpsController {

    private final HqBulkOpsService hqBulkOpsService;

    public ApiHqBulkOpsController(HqBulkOpsService hqBulkOpsService) {
        this.hqBulkOpsService = hqBulkOpsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        return ResponseEntity.ok(ApiResponse.ok(hqBulkOpsService.snapshotForApi()));
    }

    /** 모든로그인제한 — 조직 단계별 업체코드·명칭 드롭다운 옵션 */
    @GetMapping("/login/orgOptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> loginOrgOptions(
            @RequestParam(required = false) String orgLevel,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(hqBulkOpsService.listLoginOrgOptions(orgLevel, q)));
    }

    @PostMapping("/orgUse/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyOrgUse(@RequestBody Map<String, Object> body) {
        String action = body != null && body.get("action") != null ? String.valueOf(body.get("action")) : "";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orgUse", hqBulkOpsService.applyOrgUseAction(action, currentUsername()));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/urlPay/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyUrlPay(@RequestBody Map<String, Object> body) {
        String action = body != null && body.get("action") != null ? String.valueOf(body.get("action")) : "";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("urlPay", hqBulkOpsService.applyUrlPayAction(action, currentUsername()));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/login/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveLogin(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                hqBulkOpsService.saveLoginRestriction(body != null ? body : Map.of(), currentUsername())));
    }

    @PostMapping("/login/delete")
    public ResponseEntity<ApiResponse<Object>> deleteLogin(@RequestBody Map<String, Object> body) {
        Long id = null;
        if (body != null && body.get("id") != null) {
            try {
                id = Long.parseLong(String.valueOf(body.get("id")));
            } catch (NumberFormatException ignored) {
                /* ignore */
            }
        }
        hqBulkOpsService.deleteLoginRestriction(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/login/release")
    public ResponseEntity<ApiResponse<Map<String, Object>>> releaseLogin(@RequestBody Map<String, Object> body) {
        Long id = null;
        if (body != null && body.get("id") != null) {
            id = Long.parseLong(String.valueOf(body.get("id")));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                hqBulkOpsService.releaseLoginRestriction(id, currentUsername())));
    }

    @PostMapping("/login/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyLogin(@RequestBody Map<String, Object> body) {
        Long id = body != null && body.get("id") != null ? Long.parseLong(String.valueOf(body.get("id"))) : null;
        String action = body != null && body.get("action") != null ? String.valueOf(body.get("action")) : "";
        return ResponseEntity.ok(ApiResponse.ok(
                hqBulkOpsService.applyLoginRestrictionAction(id, action, currentUsername())));
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            return u.getUsername();
        }
        return auth != null ? auth.getName() : "";
    }
}
