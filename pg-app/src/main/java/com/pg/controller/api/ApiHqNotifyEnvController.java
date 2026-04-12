package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.HqNotifyEnvService;
import com.pg.service.HqNotifyTargetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 본사설정 — 전산노티 수신 URL 등 (NOTI 환경설정 대응). 결제 후속조치 스위치는 전산설정관리에서 편집·동기화.
 */
@RestController
@RequestMapping(value = "/api/hq/notifyEnv", produces = "application/json")
public class ApiHqNotifyEnvController {

    private final HqNotifyEnvService hqNotifyEnvService;
    private final HqNotifyTargetService hqNotifyTargetService;

    public ApiHqNotifyEnvController(HqNotifyEnvService hqNotifyEnvService, HqNotifyTargetService hqNotifyTargetService) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.hqNotifyTargetService = hqNotifyTargetService;
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

    @GetMapping("/targets")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> targets() {
        return ResponseEntity.ok(ApiResponse.ok(hqNotifyTargetService.list()));
    }

    @GetMapping("/targets/masterDistOptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> masterDistOptions() {
        return ResponseEntity.ok(ApiResponse.ok(hqNotifyTargetService.listMasterDistNotifyLinkOptions()));
    }

    @PostMapping("/targets/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTarget(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        try {
            String targetName = body.get("targetName") != null ? String.valueOf(body.get("targetName")) : "";
            Long boundOrgUnitId = parseOptionalLong(body.get("boundOrgUnitId"));
            if (boundOrgUnitId == null) {
                boundOrgUnitId = parseOptionalLong(body.get("orgUnitId"));
            }
            return ResponseEntity.ok(ApiResponse.ok(hqNotifyTargetService.createPair(targetName, boundOrgUnitId, req)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/targets/bindBoundOrg")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bindBoundOrg(@RequestBody Map<String, Object> body) {
        try {
            Object raw = body.get("targetIds");
            if (!(raw instanceof List<?> list) || list.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail("targetIds가 필요합니다.", "VALIDATION"));
            }
            List<Long> ids = new ArrayList<>();
            for (Object o : list) {
                Long v = parseOptionalLong(o);
                if (v == null) {
                    throw new IllegalArgumentException("targetIds에 유효하지 않은 값이 있습니다.");
                }
                ids.add(v);
            }
            Long bid = parseOptionalLong(body.get("boundOrgUnitId"));
            hqNotifyTargetService.bindBoundOrgToTargets(ids, bid);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "연결되었습니다.")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private static Long parseOptionalLong(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @DeleteMapping("/targets/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteTarget(@PathVariable Long id) {
        try {
            hqNotifyTargetService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "삭제되었습니다.")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }
}
