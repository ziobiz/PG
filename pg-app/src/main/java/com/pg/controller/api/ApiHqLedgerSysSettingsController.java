package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.HqLedgerSysSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 본사설정 — 전산설정관리 (NOTI 시스템/환경설정: 시간·동기화, 자동화 메일)
 */
@RestController
@RequestMapping(value = "/api/hq/ledgerSysSettings", produces = "application/json")
public class ApiHqLedgerSysSettingsController {

    private final HqLedgerSysSettingsService service;

    public ApiHqLedgerSysSettingsController(HqLedgerSysSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        return ResponseEntity.ok(ApiResponse.ok(service.toMap(service.getOrCreate())));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        try {
            var s = service.saveFromBody(body);
            return ResponseEntity.ok(ApiResponse.ok(service.toMap(s)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }
}
