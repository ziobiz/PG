package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import com.pg.splitpay.SplitPayEmailSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/splitpay/emailSettings", produces = "application/json")
public class ApiSplitPayEmailSettingsController {

    private final SplitPayEmailSettingsService emailSettingsService;
    private final AuthService authService;

    public ApiSplitPayEmailSettingsController(SplitPayEmailSettingsService emailSettingsService,
                                              AuthService authService) {
        this.emailSettingsService = emailSettingsService;
        this.authService = authService;
    }

    private boolean mayEdit(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        return org != null && "HEADQUARTERS".equals(String.valueOf(org.getOrDefault("orgLevel", ""))
                .trim().toUpperCase(Locale.ROOT));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        return ResponseEntity.ok(ApiResponse.ok(emailSettingsService.loadAll()));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(Authentication authentication,
                                                                  @RequestBody Map<String, Object> body) {
        if (!mayEdit(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(emailSettingsService.saveFromBody(body)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> test(Authentication authentication,
                                                                 @RequestBody Map<String, Object> body) {
        if (!mayEdit(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "총본사(HEADQUARTERS) 또는 시스템 관리자만 테스트 발송할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            String phase = body != null && body.get("phase") != null ? body.get("phase").toString() : "D0";
            String locale = body != null && body.get("locale") != null ? body.get("locale").toString() : "KOR";
            String testTo = body != null && body.get("testRecipientEmail") != null
                    ? body.get("testRecipientEmail").toString() : "";
            String actor = null;
            if (authentication != null && authentication.getPrincipal() instanceof AppUser u) {
                actor = u.getUsername();
            }
            emailSettingsService.sendTestMail(phase, locale, testTo, actor);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("sent", true)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }
}
