package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PayListSearchRequest;
import com.pg.service.ops.OpsCostAuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** 검수관리 — 원가검수(대사 우선 · 결제내역 보강). */
@RestController
@RequestMapping("/api/ops/costAudit")
public class ApiOpsCostAuditController {

    private final OpsCostAuditService opsCostAuditService;

    public ApiOpsCostAuditController(OpsCostAuditService opsCostAuditService) {
        this.opsCostAuditService = opsCostAuditService;
    }

    @GetMapping("/access")
    public ResponseEntity<ApiResponse<Map<String, Object>>> access(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(opsCostAuditService.accessMeta(authentication)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchOrderNo,
            @RequestParam(required = false) String searchPayDivCd,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(required = false) String searchAcquirer,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchIncludeZeroCount) {
        PayListSearchRequest payTemplate = new PayListSearchRequest();
        if (searchFieldType != null) {
            payTemplate.setSearchFieldType(searchFieldType);
        }
        if (searchKeyword != null) {
            payTemplate.setSearchKeyword(searchKeyword);
        }
        boolean includeZero = searchIncludeZeroCount != null
                && ("true".equalsIgnoreCase(searchIncludeZeroCount.trim())
                || "1".equals(searchIncludeZeroCount.trim())
                || "Y".equalsIgnoreCase(searchIncludeZeroCount.trim()));
        return ResponseEntity.ok(ApiResponse.ok(opsCostAuditService.list(
                authentication, searchFromDate, searchToDate, searchKeyword, searchOrderNo,
                searchPayDivCd, searchOrderDir, searchAcquirer, payTemplate, includeZero)));
    }
}
