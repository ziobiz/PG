package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.HqNotifyEnvService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 본사설정 — 전산노티 수신 URL·결제 후속(자동무효 등) 환경 (NOTI 환경설정 대응)
 */
@RestController
@RequestMapping(value = "/api/hq/notifyEnv", produces = "application/json")
public class ApiHqNotifyEnvController {

    private final HqNotifyEnvService hqNotifyEnvService;

    public ApiHqNotifyEnvController(HqNotifyEnvService hqNotifyEnvService) {
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(HttpServletRequest req) {
        var c = hqNotifyEnvService.getOrCreate();
        return ResponseEntity.ok(ApiResponse.ok(hqNotifyEnvService.toMap(c, req)));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        try {
            var c = hqNotifyEnvService.saveFromBody(body);
            return ResponseEntity.ok(ApiResponse.ok(hqNotifyEnvService.toMap(c, req)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    @PostMapping("/regenerateToken")
    public ResponseEntity<ApiResponse<Map<String, Object>>> regenerateToken(HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(hqNotifyEnvService.regenerateToken(req)));
    }
}
