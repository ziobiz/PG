package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.HqOperationalDataResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

/**
 * 본사설정 — 전산설정관리 (NOTI 시스템/환경설정: 시간·동기화, 자동화 메일, 결제 후속조치)
 */
@RestController
@RequestMapping(value = "/api/hq/ledgerSysSettings", produces = "application/json")
public class ApiHqLedgerSysSettingsController {

    private final HqLedgerSysSettingsService service;
    private final HqOperationalDataResetService operationalDataResetService;
    private final AuthService authService;

    public ApiHqLedgerSysSettingsController(HqLedgerSysSettingsService service,
                                            HqOperationalDataResetService operationalDataResetService,
                                            AuthService authService) {
        this.service = service;
        this.operationalDataResetService = operationalDataResetService;
        this.authService = authService;
    }

    private boolean canResetOperationalData(Authentication auth) {
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        return ResponseEntity.ok(ApiResponse.ok(service.toMap(service.getOrCreate())));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        try {
            var s = service.saveFromBody(body);
            return ResponseEntity.ok(ApiResponse.ok(service.toMap(s)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    /**
     * 등록 조직·가맹 프로필(tb_org_unit, tb_merchant_profile)만 남기고 운영 데이터를 삭제합니다.
     */
    @PostMapping("/resetOperationalData")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetOperationalData(Authentication authentication) {
        if (!canResetOperationalData(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 실행할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            operationalDataResetService.resetAllExceptRegisteredMerchants();
            return ResponseEntity.ok(ApiResponse.ok(Map.of("reset", true)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }
}
