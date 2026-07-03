package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.ops.OpsDistributionSettlementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** 운영관리 — 유통망정산(조직 시점 정산 실행). */
@RestController
@RequestMapping("/api/ops/distributionSettlement")
public class ApiOpsDistributionSettlementController {

    private final OpsDistributionSettlementService opsDistributionSettlementService;

    public ApiOpsDistributionSettlementController(OpsDistributionSettlementService opsDistributionSettlementService) {
        this.opsDistributionSettlementService = opsDistributionSettlementService;
    }

    @GetMapping("/access")
    public ResponseEntity<ApiResponse<Map<String, Object>>> access(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(opsDistributionSettlementService.accessMeta(authentication)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(opsDistributionSettlementService.list(
                authentication, searchFromDate, searchToDate, searchCompId, searchCompNm,
                searchFieldType, searchKeyword, searchOrderDir, page, size)));
    }
}
