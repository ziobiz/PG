package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.LoginRequest;
import com.pg.api.dto.LoginResponse;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import com.pg.service.OrgPagePermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiAuthController {

    private final AuthService authService;
    private final OrgPagePermissionService orgPagePermissionService;

    public ApiAuthController(AuthService authService, OrgPagePermissionService orgPagePermissionService) {
        this.authService = authService;
        this.orgPagePermissionService = orgPagePermissionService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest req) {
        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            return ResponseEntity.ok(ApiResponse.fail("아이디와 비밀번호를 입력하세요.", "INVALID_INPUT"));
        }
        return authService.login(req.getUsername().trim(), req.getPassword())
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.fail("아이디 또는 비밀번호가 올바르지 않습니다.", "AUTH_FAIL")));
    }

    /** 현재 로그인 사용자 정보 (업체정보조회 필터·권한 판단용) */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> user = new HashMap<>();
        user.put("ok", true);
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            user.put("userId", u.getUsername());
            user.put("userNm", u.getName() != null ? u.getName() : u.getUsername());
            user.put("role", u.getRole());
            Map<String, Object> org = authService.getOrgInfo(u.getUsername());
            if (org != null) {
                user.put("orgUnitId", org.get("orgUnitId"));
                user.put("compId", org.get("compId"));
                user.put("compNm", org.get("compNm"));
                user.put("orgLevel", org.get("orgLevel"));
            }
            user.put("pagePermissions", orgPagePermissionService.resolvePagePermissionsForUser(u));
        }
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            username = u.getUsername();
        } else if (auth != null) {
            username = auth.getName();
        }
        String currentPassword = body.get("currentPassword") != null ? String.valueOf(body.get("currentPassword")) : "";
        String newPassword = body.get("newPassword") != null ? String.valueOf(body.get("newPassword")) : "";
        String confirmPassword = body.get("confirmPassword") != null ? String.valueOf(body.get("confirmPassword")) : "";
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.ok(ApiResponse.fail("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.", "VALIDATION"));
        }
        try {
            authService.changeOwnPassword(username, currentPassword, newPassword);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "비밀번호가 변경되었습니다.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/change-name")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeName(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            username = u.getUsername();
        } else if (auth != null) {
            username = auth.getName();
        }
        String newName = body.get("newName") != null ? String.valueOf(body.get("newName")) : "";
        try {
            authService.changeOwnName(username, newName);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "이름이 변경되었습니다.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }
}
