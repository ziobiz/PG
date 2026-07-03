package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.ops.OpsPayRiskFilterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/ops/riskFilter", produces = "application/json")
public class ApiOpsPayRiskFilterController {

    private final OpsPayRiskFilterService opsPayRiskFilterService;

    public ApiOpsPayRiskFilterController(OpsPayRiskFilterService opsPayRiskFilterService) {
        this.opsPayRiskFilterService = opsPayRiskFilterService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchRiskDiv,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> data = opsPayRiskFilterService.search(
                searchCompId, searchRiskDiv, searchFromDate, searchToDate, page, size);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/filterCodes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> filterCodes() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("options", opsPayRiskFilterService.filterCodeOptions());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
