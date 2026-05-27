package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.ops.OpsVerifyReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 운영관리 — 검증 리포트 (ChillPay 통합내역 ↔ NOTI).
 */
@RestController
@RequestMapping("/api/ops/verifyReport")
public class ApiOpsVerifyReportController {

    private final OpsVerifyReportService opsVerifyReportService;

    public ApiOpsVerifyReportController(OpsVerifyReportService opsVerifyReportService) {
        this.opsVerifyReportService = opsVerifyReportService;
    }

    @GetMapping(value = "/access", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> access(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(opsVerifyReportService.accessMeta(authentication)));
    }

    @GetMapping(value = "/daily", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> daily(
            Authentication authentication,
            @RequestParam Map<String, String> params) {
        try {
            Map<String, Object> payload = opsVerifyReportService.buildVerifyReport(params, authentication);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "VALIDATION";
            return ResponseEntity.ok(ApiResponse.fail(msg, "VALIDATION"));
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "FORBIDDEN";
            return ResponseEntity.ok(ApiResponse.fail(msg, "FORBIDDEN"));
        }
    }

    /** 검증 리포트 — 상태 불일치 건 NOTI 결제내역을 통합(ChillPay) 상태에 맞춤 */
    @PostMapping(value = "/syncStatus", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncStatus(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        try {
            String approvalNo = body != null ? body.get("approvalNo") : null;
            String day = body != null ? body.get("day") : null;
            Map<String, Object> payload = opsVerifyReportService.syncNotiStatusFromChill(approvalNo, day, authentication);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "VALIDATION";
            return ResponseEntity.ok(ApiResponse.fail(msg, "VALIDATION"));
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "FORBIDDEN";
            return ResponseEntity.ok(ApiResponse.fail(msg, "FORBIDDEN"));
        }
    }

    /** 검증 리포트 — 선택 일자 상태 불일치 건 일괄 맞춤 */
    @PostMapping(value = "/syncStatusBatch", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncStatusBatch(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        try {
            Map<String, Object> payload = opsVerifyReportService.syncStatusMismatchesForDay(body, authentication);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "VALIDATION";
            return ResponseEntity.ok(ApiResponse.fail(msg, "VALIDATION"));
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "FORBIDDEN";
            return ResponseEntity.ok(ApiResponse.fail(msg, "FORBIDDEN"));
        }
    }
}
