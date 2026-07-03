package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.ops.OpsDistributionTxnListService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** 운영관리 — 유통망내역(조직 시점 수수료 분해). */
@RestController
@RequestMapping("/api/ops/distributionTxnList")
public class ApiOpsDistributionTxnListController {

    private final OpsDistributionTxnListService opsDistributionTxnListService;

    public ApiOpsDistributionTxnListController(OpsDistributionTxnListService opsDistributionTxnListService) {
        this.opsDistributionTxnListService = opsDistributionTxnListService;
    }

    @GetMapping("/access")
    public ResponseEntity<ApiResponse<Map<String, Object>>> access(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(opsDistributionTxnListService.accessMeta(authentication)));
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
            @RequestParam(required = false) String searchStatusGroup,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(opsDistributionTxnListService.list(
                authentication, searchFromDate, searchToDate, searchCompId, searchCompNm,
                searchFieldType, searchKeyword, searchStatusGroup, searchOrderDir, page, size)));
    }
}
