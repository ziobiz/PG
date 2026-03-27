package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import com.pg.service.UserListService;
import com.pg.service.UserViewSettingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(value = "/api/user", produces = "application/json")
public class ApiUserController {

    private final UserListService userListService;
    private final UserViewSettingService userViewSettingService;
    private final AuthService authService;

    public ApiUserController(UserListService userListService, UserViewSettingService userViewSettingService,
                             AuthService authService) {
        this.userListService = userListService;
        this.userViewSettingService = userViewSettingService;
        this.authService = authService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            @RequestParam(required = false) String searchUserId,
            @RequestParam(required = false) String searchUserNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchUseStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String scopeCompCode = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                Map<String, Object> org = authService.getOrgInfo(u.getUsername());
                scopeCompCode = org != null && org.get("compId") != null ? String.valueOf(org.get("compId")) : null;
            }
        }
        PageResult<Map<String, Object>> result = userListService.searchScoped(
                searchUserId, searchUserNm, searchCompId, searchUseStatus, page, size, scopeCompCode);
        AppUser actor = currentUser();
        Map<String, Object> cap = userListService.managementCapability(actor);
        String canManage = String.valueOf(cap.getOrDefault("canManageUsers", "N"));
        String canReset = String.valueOf(cap.getOrDefault("canResetPassword", "N"));
        for (Map<String, Object> row : result.getList()) {
            row.put("canManageUsers", canManage);
            row.put("canResetPassword", canReset);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/capability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> capability() {
        AppUser actor = currentUser();
        return ResponseEntity.ok(ApiResponse.ok(userListService.managementCapability(actor)));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> add(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = currentUser();
            String scopeCompCode = resolveScopeCompCode(actor);
            Set<String> allowed = userListService.resolveAllowedCompCodes(scopeCompCode);
            userListService.createUserScoped(
                    actor,
                    allowed,
                    scopeCompCode,
                    str(body, "userId"),
                    str(body, "userNm"),
                    str(body, "password"),
                    str(body, "mobile"),
                    str(body, "compId"),
                    str(body, "role"),
                    str(body, "userType"),
                    str(body, "assistantRoleType"),
                    str(body, "parentUsername")
            );
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "사용자가 등록되었습니다.")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = currentUser();
            String scopeCompCode = resolveScopeCompCode(actor);
            Set<String> allowed = userListService.resolveAllowedCompCodes(scopeCompCode);
            Long id = body.get("id") == null ? null : Long.parseLong(String.valueOf(body.get("id")));
            if (id == null) throw new IllegalArgumentException("수정할 사용자 ID가 필요합니다.");
            userListService.updateUserScoped(actor, allowed, id,
                    str(body, "mobile"),
                    str(body, "userStatus"),
                    str(body, "inactiveReason"),
                    str(body, "assistantRoleType"));
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "저장되었습니다.")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/resetOtp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetOtp(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = currentUser();
            String scopeCompCode = resolveScopeCompCode(actor);
            Set<String> allowed = userListService.resolveAllowedCompCodes(scopeCompCode);
            Long id = body.get("id") == null ? null : Long.parseLong(String.valueOf(body.get("id")));
            if (id == null) throw new IllegalArgumentException("초기화할 사용자 ID가 필요합니다.");
            userListService.resetOtpScoped(actor, allowed, id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "OTP 등록이 초기화되었습니다.")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = currentUser();
            String scopeCompCode = resolveScopeCompCode(actor);
            Set<String> allowed = userListService.resolveAllowedCompCodes(scopeCompCode);
            Long id = body.get("id") == null ? null : Long.parseLong(String.valueOf(body.get("id")));
            if (id == null) throw new IllegalArgumentException("삭제할 사용자 ID가 필요합니다.");
            userListService.deleteUserScoped(actor, allowed, id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "사용자가 삭제되었습니다.")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(@RequestBody Map<String, Object> body) {
        try {
            AppUser actor = currentUser();
            String scopeCompCode = resolveScopeCompCode(actor);
            Set<String> allowed = userListService.resolveAllowedCompCodes(scopeCompCode);
            Long id = body.get("id") == null ? null : Long.parseLong(String.valueOf(body.get("id")));
            if (id == null) throw new IllegalArgumentException("초기화할 사용자 ID가 필요합니다.");
            Map<String, Object> data = new LinkedHashMap<>(userListService.resetPasswordScoped(actor, allowed, id));
            data.put("message", "비밀번호가 임시 비밀번호로 초기화되었습니다.");
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/menuOrderMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> menuOrderMng(
            @RequestParam(required = false) String searchMenuId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    @GetMapping("/viewSetting")
    public ResponseEntity<ApiResponse<Map<String, Object>>> viewSetting(
            @RequestParam String pageUrl) {
        String username = currentUsername();
        return ResponseEntity.ok(ApiResponse.ok(userViewSettingService.get(username, pageUrl)));
    }

    @PostMapping("/viewSetting/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveViewSetting(@RequestBody Map<String, Object> body) {
        try {
            String username = currentUsername();
            String pageUrl = body.get("pageUrl") != null ? String.valueOf(body.get("pageUrl")) : "";
            String selectedKeysJson = body.get("selectedKeysJson") != null ? String.valueOf(body.get("selectedKeysJson")) : "[]";
            return ResponseEntity.ok(ApiResponse.ok(userViewSettingService.save(username, pageUrl, selectedKeysJson)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser user && user.getUsername() != null) {
            return user.getUsername();
        }
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "";
    }

    private static AppUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser user) return user;
        throw new IllegalArgumentException("로그인 사용자 정보를 확인할 수 없습니다.");
    }

    private String resolveScopeCompCode(AppUser actor) {
        if (actor == null) return null;
        if ("ADMIN".equalsIgnoreCase(actor.getRole())) return null;
        Map<String, Object> org = authService.getOrgInfo(actor.getUsername());
        return org != null && org.get("compId") != null ? String.valueOf(org.get("compId")) : null;
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }
}
