package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.PgAgencyCostPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 본사설정 — 대행수수료설정 (ICOPAY↔PG 계약 수수료·정산 주기)
 */
@RestController
@RequestMapping(value = "/api/hq/pgAgencyCostPolicy", produces = "application/json")
public class ApiHqPgAgencyCostPolicyController {

    private final PgAgencyCostPolicyService service;

    public ApiHqPgAgencyCostPolicyController(PgAgencyCostPolicyService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> bootstrap() {
        return ResponseEntity.ok(ApiResponse.ok(service.bootstrap()));
    }

    @GetMapping("/{pgCd}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@PathVariable String pgCd) {
        return ResponseEntity.ok(ApiResponse.ok(service.getByPgCd(pgCd)));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(service.save(body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }
}
