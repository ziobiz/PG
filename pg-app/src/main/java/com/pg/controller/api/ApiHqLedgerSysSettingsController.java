package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.service.AuthService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.HqOperationalDataResetService;
import com.pg.service.HqPayNotifyDayPurgeService;
import com.pg.service.HqSettlementDataResetService;
import com.pg.service.PayFollowEmailVoidService;
import com.pg.service.PayCardPolicyService;
import com.pg.service.PayFollowPolicyService;
import com.pg.service.settlement.SettlementVoidRefundFeeCorrectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;

/**
 * 본사설정 — 전산설정관리 (NOTI 시스템/환경설정: 시간·동기화, 자동화 메일, 결제 후속조치)
 */
@RestController
@RequestMapping(value = "/api/hq/ledgerSysSettings", produces = "application/json")
public class ApiHqLedgerSysSettingsController {

    private final HqLedgerSysSettingsService service;
    private final HqOperationalDataResetService operationalDataResetService;
    private final HqSettlementDataResetService settlementDataResetService;
    private final HqPayNotifyDayPurgeService payNotifyDayPurgeService;
    private final AuthService authService;
    private final PayFollowEmailVoidService payFollowEmailVoidService;
    private final PayFollowPolicyService payFollowPolicyService;
    private final PayCardPolicyService payCardPolicyService;
    private final SettlementVoidRefundFeeCorrectionService voidRefundFeeCorrectionService;

    public ApiHqLedgerSysSettingsController(HqLedgerSysSettingsService service,
                                            HqOperationalDataResetService operationalDataResetService,
                                            HqSettlementDataResetService settlementDataResetService,
                                            HqPayNotifyDayPurgeService payNotifyDayPurgeService,
                                            AuthService authService,
                                            PayFollowEmailVoidService payFollowEmailVoidService,
                                            PayFollowPolicyService payFollowPolicyService,
                                            PayCardPolicyService payCardPolicyService,
                                            SettlementVoidRefundFeeCorrectionService voidRefundFeeCorrectionService) {
        this.service = service;
        this.operationalDataResetService = operationalDataResetService;
        this.settlementDataResetService = settlementDataResetService;
        this.payNotifyDayPurgeService = payNotifyDayPurgeService;
        this.authService = authService;
        this.payFollowEmailVoidService = payFollowEmailVoidService;
        this.payFollowPolicyService = payFollowPolicyService;
        this.payCardPolicyService = payCardPolicyService;
        this.voidRefundFeeCorrectionService = voidRefundFeeCorrectionService;
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

    /** 본사권한설정의 조직 단계별 후속조치 상한과 동일 — 총본사·ADMIN 만 저장 */
    private boolean maySavePayFollowLevelCaps(Authentication auth) {
        return canResetOperationalData(auth);
    }

    private Map<String, Object> toMapWithPayFollowCaps(HqLedgerSysSettings s) {
        Map<String, Object> m = service.toMap(s);
        m.put("payFollowLevelCaps", payFollowPolicyService.buildLevelCapsPayload());
        m.put("payCardBlockPrefixes", payCardPolicyService.listBlockPrefixesForAdmin());
        return m;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        return ResponseEntity.ok(ApiResponse.ok(toMapWithPayFollowCaps(service.getOrCreate())));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        try {
            var s = service.saveFromBody(body);
            return ResponseEntity.ok(ApiResponse.ok(toMapWithPayFollowCaps(s)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    /** 헬로 타임라인(사용 여부·유지 분)만 저장 — 전산설정 나머지는 그대로 둡니다. */
    @PostMapping("/saveHelloTimeline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveHelloTimeline(@RequestBody Map<String, Object> body) {
        try {
            var s = service.saveHelloTimelineFromBody(body);
            return ResponseEntity.ok(ApiResponse.ok(toMapWithPayFollowCaps(s)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    /**
     * 조직 단계별(총본사~가맹점) 결제 후속조치 기능 상한 저장. {@code payFollowLevelCaps} 키는 본사권한설정 저장과 동일 형식.
     */
    @PostMapping("/savePayFollowLevelCaps")
    public ResponseEntity<ApiResponse<Map<String, Object>>> savePayFollowLevelCaps(
            Authentication authentication,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!maySavePayFollowLevelCaps(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            Object raw = body != null ? body.get("payFollowLevelCaps") : null;
            if (!(raw instanceof Map<?, ?> payFollowMap) || payFollowMap.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail("payFollowLevelCaps 항목이 필요합니다.", "VALIDATION"));
            }
            payFollowPolicyService.saveLevelCapsFromClient(payFollowMap);
            return ResponseEntity.ok(ApiResponse.ok(toMapWithPayFollowCaps(service.getOrCreate())));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "저장 실패", "ERROR"));
        }
    }

    /**
     * 이메일무효(VOID) 템플릿·SMTP로 테스트 수신처에 샘플 본문 메일을 발송합니다. 결과는 운영관리 메일로그에 남습니다.
     */
    @PostMapping("/payCardBlockPrefix")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addPayCardBlockPrefix(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        if (!canResetOperationalData(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 등록할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            String pg = body.get("pgVendor") != null ? body.get("pgVendor").toString() : "";
            String prefix = body.get("prefixDigits") != null ? body.get("prefixDigits").toString() : "";
            String remark = body.get("remark") != null ? body.get("remark").toString() : "";
            var row = payCardPolicyService.addBlockPrefix(pg, prefix, remark);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("id", row.getId())));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    @PostMapping("/payCardBlockPrefix/delete")
    public ResponseEntity<ApiResponse<Void>> deletePayCardBlockPrefix(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        if (!canResetOperationalData(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 삭제할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            Object idObj = body.get("id");
            if (idObj == null) {
                return ResponseEntity.ok(ApiResponse.fail("id가 필요합니다.", "VALIDATION"));
            }
            payCardPolicyService.deleteBlockPrefix(Long.parseLong(idObj.toString().trim()));
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    @PostMapping("/testVoidEmail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testVoidEmail(Authentication authentication,
                                                                          @RequestBody(required = false) Map<String, Object> body) {
        try {
            String testTo = "";
            if (body != null && body.get("testRecipientEmail") != null) {
                testTo = String.valueOf(body.get("testRecipientEmail")).trim();
            }
            String actor = null;
            if (authentication != null && authentication.getPrincipal() instanceof AppUser u) {
                actor = u.getUsername();
            }
            payFollowEmailVoidService.sendVoidTestMail(testTo, actor);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("sent", true)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
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

    /**
     * 특정 일자 결제내역·(선택) 노티수령정보 삭제 — NOTI 재전송 후 재처리용.
     * 본문: {@code date}(YYYY-MM-DD, 필수), {@code merchantId}(선택), {@code purgeInbound}(기본 true), {@code confirm:true}(필수).
     */
    @PostMapping("/purgePayAndNotifyForDay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purgePayAndNotifyForDay(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        if (!canResetOperationalData(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 실행할 수 있습니다.", "FORBIDDEN"));
        }
        if (body == null || body.get("confirm") == null || !Boolean.TRUE.equals(body.get("confirm"))) {
            return ResponseEntity.ok(ApiResponse.fail("confirm:true 가 필요합니다.", "VALIDATION"));
        }
        try {
            Object dateObj = body.get("date");
            if (dateObj == null || dateObj.toString().isBlank()) {
                return ResponseEntity.ok(ApiResponse.fail("date(YYYY-MM-DD)가 필요합니다.", "VALIDATION"));
            }
            LocalDate date = LocalDate.parse(dateObj.toString().trim());
            String merchantId = body.get("merchantId") != null ? body.get("merchantId").toString() : null;
            boolean purgeInbound = body.get("purgeInbound") == null || Boolean.parseBoolean(body.get("purgeInbound").toString());
            Map<String, Object> result = new LinkedHashMap<>(payNotifyDayPurgeService.purgeForDay(date, merchantId, purgeInbound));
            result.put("purged", true);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    /**
     * 무효·환불 수수료 산식 보정 — 저장된 정산 실행({@code tb_settlement_run}) 재산출.
     * 본문: {@code dryRun}(기본 true), {@code confirm:true}(실반영 시 필수),
     * {@code calcDtFrom}·{@code calcDtTo}(YYYY-MM-DD, 선택), {@code merchantId}(선택).
     */
    @PostMapping("/correctVoidRefundSettlementFees")
    public ResponseEntity<ApiResponse<Map<String, Object>>> correctVoidRefundSettlementFees(
            Authentication authentication,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!canResetOperationalData(authentication)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 실행할 수 있습니다.", "FORBIDDEN"));
        }
        boolean dryRun = body == null || body.get("dryRun") == null || Boolean.parseBoolean(String.valueOf(body.get("dryRun")));
        if (!dryRun && (body == null || body.get("confirm") == null || !Boolean.TRUE.equals(body.get("confirm")))) {
            return ResponseEntity.ok(ApiResponse.fail("실반영 시 confirm:true 가 필요합니다. 먼저 dryRun:true 로 결과를 확인하세요.", "VALIDATION"));
        }
        try {
            LocalDate from = null;
            LocalDate to = null;
            if (body != null) {
                if (body.get("calcDtFrom") != null && !String.valueOf(body.get("calcDtFrom")).isBlank()) {
                    from = LocalDate.parse(String.valueOf(body.get("calcDtFrom")).trim());
                }
                if (body.get("calcDtTo") != null && !String.valueOf(body.get("calcDtTo")).isBlank()) {
                    to = LocalDate.parse(String.valueOf(body.get("calcDtTo")).trim());
                }
            }
            String merchantId = body != null && body.get("merchantId") != null
                    ? String.valueOf(body.get("merchantId")).trim() : null;
            Map<String, Object> result = voidRefundFeeCorrectionService.correctStoredRuns(from, to, merchantId, dryRun);
            result.put("corrected", !dryRun);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }
}
