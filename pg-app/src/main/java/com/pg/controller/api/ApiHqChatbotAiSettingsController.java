package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import com.pg.service.HqChatbotAiSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 본사설정 — AI설정 (챗봇 연동용 LLM 키·모델·프롬프트). JSON 스키마는 Stock AI 페이지와 호환 가능한 필드명을 사용합니다.
 */
@RestController
@RequestMapping(value = "/api/hq/chatbotAiSettings", produces = "application/json")
public class ApiHqChatbotAiSettingsController {

    private final HqChatbotAiSettingsService service;
    private final AuthService authService;

    public ApiHqChatbotAiSettingsController(HqChatbotAiSettingsService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    private Map<String, Object> orgOf(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return null;
        }
        return authService.getOrgInfo(u.getUsername());
    }

    private boolean mayAccess(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return false;
        }
        return HqChatbotAiSettingsService.mayEditHqAiSettings(u, orgOf(auth));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(Authentication auth) {
        if (!mayAccess(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(service.toMaskedMap(service.getOrCreate())));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(Authentication auth, @RequestBody Map<String, Object> body) {
        if (!mayAccess(auth)) {
            return ResponseEntity.ok(ApiResponse.fail("총본사(HEADQUARTERS) 또는 시스템 관리자만 저장할 수 있습니다.", "FORBIDDEN"));
        }
        try {
            var saved = service.saveFromBody(body);
            return ResponseEntity.ok(ApiResponse.ok(service.toMaskedMap(saved)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }
}
