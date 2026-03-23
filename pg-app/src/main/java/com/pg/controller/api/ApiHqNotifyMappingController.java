package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.HqNotifyMappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 본사설정 — 노티매핑 (PG CALLBACK/RESULT → 전산 화면·필드)
 */
@RestController
@RequestMapping(value = "/api/hq/notifyMapping", produces = "application/json")
public class ApiHqNotifyMappingController {

    private final HqNotifyMappingService hqNotifyMappingService;

    public ApiHqNotifyMappingController(HqNotifyMappingService hqNotifyMappingService) {
        this.hqNotifyMappingService = hqNotifyMappingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        var c = hqNotifyMappingService.getOrCreate();
        return ResponseEntity.ok(ApiResponse.ok(hqNotifyMappingService.toMap(c)));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        try {
            var c = hqNotifyMappingService.saveFromBody(body);
            return ResponseEntity.ok(ApiResponse.ok(hqNotifyMappingService.toMap(c)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }
}
