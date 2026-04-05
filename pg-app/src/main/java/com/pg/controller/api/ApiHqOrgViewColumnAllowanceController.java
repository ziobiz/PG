package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.service.HqViewCustomColumnService;
import com.pg.service.OrgViewColumnAllowanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/hq/orgViewColumnAllowance", produces = "application/json")
public class ApiHqOrgViewColumnAllowanceController {

    private final OrgViewColumnAllowanceService allowanceService;
    private final HqViewCustomColumnService hqViewCustomColumnService;

    public ApiHqOrgViewColumnAllowanceController(OrgViewColumnAllowanceService allowanceService,
                                                HqViewCustomColumnService hqViewCustomColumnService) {
        this.allowanceService = allowanceService;
        this.hqViewCustomColumnService = hqViewCustomColumnService;
    }

    @GetMapping("/regionalBranches")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> regionalBranches() {
        AppUser actor = tryActor();
        if (actor == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "AUTH"));
        }
        if (!allowanceService.canManageOrgViewAllowance(actor)) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(allowanceService.listRegionalBranches()));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(@RequestParam String regionalOrgCode) {
        try {
            AppUser actor = tryActor();
            if (actor == null) {
                return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "AUTH"));
            }
            return ResponseEntity.ok(ApiResponse.ok(allowanceService.listAllowancesByRegional(regionalOrgCode, actor)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(
            @RequestParam String regionalOrgCode,
            @RequestParam String pageUrl,
            @RequestParam(required = false) String viewerScope) {
        AppUser actor = tryActor();
        if (actor == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "AUTH"));
        }
        if (!allowanceService.canManageOrgViewAllowance(actor)) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(allowanceService.getAllowanceRow(regionalOrgCode, pageUrl, viewerScope)));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = tryActor();
            if (actor == null) {
                return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "AUTH"));
            }
            String regional = body.get("regionalOrgCode") != null ? String.valueOf(body.get("regionalOrgCode")) : "";
            String pageUrl = body.get("pageUrl") != null ? String.valueOf(body.get("pageUrl")) : "";
            String viewerScope = body.get("viewerScope") != null ? String.valueOf(body.get("viewerScope")) : "";
            String allowedKeysJson = body.get("allowedKeysJson") != null ? String.valueOf(body.get("allowedKeysJson")) : "[]";
            return ResponseEntity.ok(ApiResponse.ok(allowanceService.saveAllowance(regional, pageUrl, viewerScope, allowedKeysJson, actor)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/customColumns")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> customColumnsList(@RequestParam String pageUrl) {
        try {
            AppUser actor = tryActor();
            if (actor == null) {
                return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "AUTH"));
            }
            return ResponseEntity.ok(ApiResponse.ok(hqViewCustomColumnService.list(pageUrl, actor)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/customColumns/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> customColumnsAdd(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = tryActor();
            if (actor == null) {
                return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "AUTH"));
            }
            String pageUrl = body.get("pageUrl") != null ? String.valueOf(body.get("pageUrl")) : "";
            String displayName = body.get("displayName") != null ? String.valueOf(body.get("displayName")) : "";
            return ResponseEntity.ok(ApiResponse.ok(hqViewCustomColumnService.add(pageUrl, displayName, actor)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/customColumns/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> customColumnsUpdate(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = tryActor();
            if (actor == null) {
                return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "AUTH"));
            }
            long id = body.get("id") == null ? 0L : Long.parseLong(String.valueOf(body.get("id")));
            String displayName = body.get("displayName") != null ? String.valueOf(body.get("displayName")) : "";
            return ResponseEntity.ok(ApiResponse.ok(hqViewCustomColumnService.update(id, displayName, actor)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/customColumns/delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> customColumnsDelete(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = tryActor();
            if (actor == null) {
                return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "AUTH"));
            }
            long id = body.get("id") == null ? 0L : Long.parseLong(String.valueOf(body.get("id")));
            hqViewCustomColumnService.delete(id, actor);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("ok", true)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = tryActor();
            if (actor == null) {
                return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "AUTH"));
            }
            String regional = body.get("regionalOrgCode") != null ? String.valueOf(body.get("regionalOrgCode")) : "";
            String pageUrl = body.get("pageUrl") != null ? String.valueOf(body.get("pageUrl")) : "";
            String viewerScope = body.get("viewerScope") != null ? String.valueOf(body.get("viewerScope")) : "";
            return ResponseEntity.ok(ApiResponse.ok(allowanceService.deleteAllowance(regional, pageUrl, viewerScope, actor)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private static AppUser tryActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser user) return user;
        return null;
    }
}
