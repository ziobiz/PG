package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.UrlPayCheckoutFieldPreset;
import com.pg.service.UrlPayCheckoutFieldPresetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 본사 — 결제창 구매자 입력 필드 프리셋(기본형·N형).
 */
@RestController
@RequestMapping(value = "/api/hq/urlPayCheckoutFieldPresets", produces = "application/json")
public class ApiHqUrlPayCheckoutFieldPresetController {

    private final UrlPayCheckoutFieldPresetService service;

    public ApiHqUrlPayCheckoutFieldPresetController(UrlPayCheckoutFieldPresetService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        List<Map<String, Object>> list = service.listAll().stream().map(service::toMap).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> mutate(@RequestBody Map<String, Object> body) {
        String action = body != null && body.get("action") != null
                ? body.get("action").toString().trim().toUpperCase() : "";
        try {
            if ("CREATE".equals(action) || "ADD".equals(action)) {
                UrlPayCheckoutFieldPreset created = service.createNext();
                Map<String, Object> data = new LinkedHashMap<>(service.toMap(created));
                data.put("message", "프리셋이 추가되었습니다.");
                return ResponseEntity.ok(ApiResponse.ok(data));
            }
            if ("UPDATE".equals(action) || "SAVE".equals(action)) {
                Long id = parseId(body != null ? body.get("id") : null);
                UrlPayCheckoutFieldPreset updated = service.update(id, body);
                Map<String, Object> data = new LinkedHashMap<>(service.toMap(updated));
                data.put("message", "프리셋이 저장되었습니다.");
                return ResponseEntity.ok(ApiResponse.ok(data));
            }
            if ("DELETE".equals(action) || "REMOVE".equals(action)) {
                Long id = parseId(body != null ? body.get("id") : null);
                service.delete(id);
                return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "프리셋이 삭제되었습니다.", "id", id)));
            }
            return ResponseEntity.ok(ApiResponse.fail("action은 CREATE|UPDATE|DELETE 중 하나여야 합니다.", "VALIDATION"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private static Long parseId(Object raw) {
        if (raw == null || raw.toString().isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
