package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.HqOperationalDataResetService;
import com.pg.service.HqSettlementDataResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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
    private final HqSettlementDataResetService settlementDataResetService;
    private final AuthService authService;

    public ApiHqLedgerSysSettingsController(HqLedgerSysSettingsService service,
                                            HqOperationalDataResetService operationalDataResetService,
                                            HqSettlementDataResetService settlementDataResetService,
                                            AuthService authService) {
        this.service = service;
        this.operationalDataResetService = operationalDataResetService;
        this.settlementDataResetService = settlementDataResetService;
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

    /** 헬로 타임라인(사용 여부·유지 분)만 저장 — 전산설정 나머지는 그대로 둡니다. */
    @PostMapping("/saveHelloTimeline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveHelloTimeline(@RequestBody Map<String, Object> body) {
        try {
            var s = service.saveHelloTimelineFromBody(body);
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

    /**
     * 정산 운영 데이터만 삭제합니다. 수수료내역·거래 마스터·본사 정산 설정·통합정산(외부)은 유지합니다.
     * 본문 {@code scope}: {@code ALL}(기본)·{@code RUNS}(실행+연동 일괄)·{@code RECEIVABLES}·{@code RECOVERY}·{@code ROLLING}·{@code DEDUCTIONS}.
     */
    @PostMapping("/resetSettlementData")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetSettlementData(
            Authentication authentication,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!canResetOperationalData(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 실행할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            String scopeRaw = body != null && body.get("scope") != null ? String.valueOf(body.get("scope")).trim() : "ALL";
            HqSettlementDataResetService.Scope scope = HqSettlementDataResetService.parseScope(scopeRaw);
            Map<String, Object> result = new LinkedHashMap<>(settlementDataResetService.reset(scope));
            result.put("reset", true);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }
}
