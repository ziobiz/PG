package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.HqRiskCardPolicy;
import com.pg.service.HqRiskCardPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 본사설정 — 리스크설정(카드 실패 쿨다운·자동 비활성 트리거) */
@RestController
@RequestMapping(value = "/api/hq/riskCardPolicy", produces = "application/json")
public class ApiHqRiskCardPolicyController {

    private final HqRiskCardPolicyService riskCardPolicyService;

    public ApiHqRiskCardPolicyController(HqRiskCardPolicyService riskCardPolicyService) {
        this.riskCardPolicyService = riskCardPolicyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        HqRiskCardPolicy row = riskCardPolicyService.getOrCreate();
        Map<String, Object> data = new LinkedHashMap<>(riskCardPolicyService.toMap(row));
        data.put("merchantRows", riskCardPolicyService.listActiveMerchantRows());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        HqRiskCardPolicy row = riskCardPolicyService.save(body != null ? body : Map.of());
        Map<String, Object> data = new LinkedHashMap<>(riskCardPolicyService.toMap(row));
        data.put("merchantRows", riskCardPolicyService.listActiveMerchantRows());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/merchantRows")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> merchantRows() {
        return ResponseEntity.ok(ApiResponse.ok(riskCardPolicyService.listActiveMerchantRows()));
    }
}
