package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.LoginRequest;
import com.pg.api.dto.LoginResponse;
import com.pg.entity.AppUser;
import com.pg.repository.UserRepository;
import com.pg.service.AuthService;
import com.pg.service.OrgPagePermissionService;
import com.pg.service.PayFollowPolicyService;
import com.pg.service.UserOtpEnrollmentService;
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
    private final PayFollowPolicyService payFollowPolicyService;
    private final UserRepository userRepository;
    private final UserOtpEnrollmentService userOtpEnrollmentService;

    public ApiAuthController(AuthService authService, OrgPagePermissionService orgPagePermissionService,
                             PayFollowPolicyService payFollowPolicyService,
                             UserRepository userRepository,
                             UserOtpEnrollmentService userOtpEnrollmentService) {
        this.authService = authService;
        this.orgPagePermissionService = orgPagePermissionService;
        this.payFollowPolicyService = payFollowPolicyService;
        this.userRepository = userRepository;
        this.userOtpEnrollmentService = userOtpEnrollmentService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest req) {
        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            return ResponseEntity.ok(ApiResponse.fail("아이디와 비밀번호를 입력하세요.", "INVALID_INPUT"));
        }
        String ch = req.getClientHost() != null ? req.getClientHost().trim() : null;
        return authService.login(req.getUsername().trim(), req.getPassword(), ch)
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
            AppUser fresh = userRepository.findById(u.getId()).orElse(u);
            user.put("userId", fresh.getUsername());
            user.put("userNm", fresh.getName() != null ? fresh.getName() : fresh.getUsername());
            user.put("role", fresh.getRole());
            user.put("otpRegisteredYn", "Y".equalsIgnoreCase(fresh.getOtpRegisteredYn()) ? "Y" : "N");
            user.put("mustSetupOtp", authService.requiresOtpEnrollment(fresh));
            Map<String, Object> org = authService.getOrgInfo(fresh.getUsername());
            if (org != null) {
                user.put("orgUnitId", org.get("orgUnitId"));
                user.put("compId", org.get("compId"));
                user.put("compNm", org.get("compNm"));
                user.put("orgLevel", org.get("orgLevel"));
            }
            user.put("pagePermissions", orgPagePermissionService.resolvePagePermissionsForUser(fresh));
            user.put("canWriteNotice", orgPagePermissionService.canWriteNotice(fresh));
            var pfa = payFollowPolicyService.allowedActionsForViewer(fresh);
            user.put("payFollowAllowed", pfa);
            Map<String, Object> pfn = new HashMap<>();
            pfn.put("showForceRefundMenu", Boolean.TRUE.equals(pfa.get("FORCE_REFUND")));
            user.put("payFollowNav", pfn);
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

    /** Google OTP 구성 1단계: 인증번호를 지정 운영 메일로만 발송 */
    @PostMapping("/otp/enroll/request-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> otpEnrollRequestEmail() {
        AppUser actor = currentAppUser();
        if (actor == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        try {
            userOtpEnrollmentService.requestEmailVerificationCode(actor);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "message", "인증번호를 발송했습니다.",
                    "sentTo", UserOtpEnrollmentService.OTP_SETUP_EMAIL_TO)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 이메일 인증 후 TOTP 시크릿 발급(앱에 등록) */
    @PostMapping("/otp/enroll/verify-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> otpEnrollVerifyEmail(@RequestBody Map<String, Object> body) {
        AppUser actor = currentAppUser();
        if (actor == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        String code = body != null && body.get("emailCode") != null ? String.valueOf(body.get("emailCode")) : "";
        try {
            Map<String, Object> out = userOtpEnrollmentService.verifyEmailCodeAndIssueSecret(actor, code);
            return ResponseEntity.ok(ApiResponse.ok(out));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** Google OTP 앱에서 생성한 6자리로 등록 확정 */
    @PostMapping("/otp/enroll/activate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> otpEnrollActivate(@RequestBody Map<String, Object> body) {
        AppUser actor = currentAppUser();
        if (actor == null) {
            return ResponseEntity.ok(ApiResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED"));
        }
        String totp = body != null && body.get("totpCode") != null ? String.valueOf(body.get("totpCode")) : "";
        try {
            userOtpEnrollmentService.activatePendingSecret(actor, totp);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "Google OTP 등록이 완료되었습니다.")));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private static AppUser currentAppUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            return u;
        }
        return null;
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
