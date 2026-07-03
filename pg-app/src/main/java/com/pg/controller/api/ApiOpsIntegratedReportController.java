package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.ops.OpsIntegratedReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 운영관리 — 통합 리포트 API.
 */
@RestController
@RequestMapping("/api/ops/integratedReport")
public class ApiOpsIntegratedReportController {

    private final OpsIntegratedReportService opsIntegratedReportService;

    public ApiOpsIntegratedReportController(OpsIntegratedReportService opsIntegratedReportService) {
        this.opsIntegratedReportService = opsIntegratedReportService;
    }

    @GetMapping(value = "/access", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> access(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(opsIntegratedReportService.accessMeta(authentication)));
    }

    /**
     * 일자별 통합 집계(적재일 created_at). 하단 상세는 클라이언트가 동일 조건으로 payList 를 해당 일만 조회합니다.
     */
    @GetMapping(value = "/daily", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> daily(
            Authentication authentication,
            @RequestParam Map<String, String> params) {
        try {
            Map<String, Object> payload = opsIntegratedReportService.dailyReport(params, authentication);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "VALIDATION";
            return ResponseEntity.ok(ApiResponse.fail(msg, "VALIDATION"));
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "FORBIDDEN";
            return ResponseEntity.ok(ApiResponse.fail(msg, "FORBIDDEN"));
        }
    }

    /** 조직구분 선택 시 검색어(조직) 드롭다운 — 로그인 범위 내 해당 단계 조직만 */
    @GetMapping(value = "/orgUnits", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> orgUnits(
            Authentication authentication,
            @RequestParam(name = "searchOrgLevel", required = false) String searchOrgLevel) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    opsIntegratedReportService.orgUnitsForSearch(searchOrgLevel, authentication)));
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "FORBIDDEN";
            return ResponseEntity.ok(ApiResponse.fail(msg, "FORBIDDEN"));
        }
    }
}
