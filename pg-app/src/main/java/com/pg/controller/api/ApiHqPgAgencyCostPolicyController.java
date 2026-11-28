package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.PgAgencyCostPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 연동·배포 — 결제대행사 등록 (근원원가)
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

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @GetMapping("/{pgCd}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@PathVariable String pgCd) {
        return ResponseEntity.ok(ApiResponse.ok(service.getByPgCd(pgCd)));
    }

    @GetMapping("/history/{policyId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> history(@PathVariable Long policyId) {
        return ResponseEntity.ok(ApiResponse.ok(service.history(policyId)));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(service.save(body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@RequestBody Map<String, Object> body) {
        try {
            Long id = null;
            Object raw = body != null ? body.get("id") : null;
            if (raw != null && !String.valueOf(raw).isBlank()) {
                try {
                    id = Long.parseLong(String.valueOf(raw).trim());
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("삭제할 원가 정책이 없습니다.");
                }
            }
            return ResponseEntity.ok(ApiResponse.ok(service.delete(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }
}
