package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import com.pg.service.JpayPortalAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

/**
 * 본사설정 — 결제대행사로직: JPAY 포털 통합내역 총판별 계정
 */
@RestController
@RequestMapping(value = "/api/hq/jpayPortalAccount", produces = "application/json")
public class ApiHqJpayPortalAccountController {

    private final JpayPortalAccountService service;
    private final AuthService authService;

    public ApiHqJpayPortalAccountController(JpayPortalAccountService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    private boolean mayManage(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        return org != null && "HEADQUARTERS".equals(String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(Authentication authentication) {
        if (!mayManage(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(service.listForAdmin()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(Authentication authentication,
                                                               @RequestBody Map<String, Object> body) {
        if (!mayManage(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(service.save(body)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "저장 실패", "ERROR"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication,
                                                    @RequestBody Map<String, Object> body) {
        if (!mayManage(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 삭제할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            Object idObj = body != null ? body.get("id") : null;
            if (idObj == null) {
                return ResponseEntity.ok(ApiResponse.fail("id가 필요합니다.", "VALIDATION"));
            }
            service.delete(Long.parseLong(idObj.toString().trim()));
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "삭제 실패", "ERROR"));
        }
    }
}
