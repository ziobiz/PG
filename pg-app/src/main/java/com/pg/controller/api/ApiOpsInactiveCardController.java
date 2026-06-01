package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.ops.OpsInactiveCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 운영관리 — 비활성카드등록(블랙리스트). */
@RestController
@RequestMapping(value = "/api/ops/inactiveCard", produces = "application/json")
public class ApiOpsInactiveCardController {

    private final OpsInactiveCardService opsInactiveCardService;

    public ApiOpsInactiveCardController(OpsInactiveCardService opsInactiveCardService) {
        this.opsInactiveCardService = opsInactiveCardService;
    }

    @GetMapping("/access")
    public ResponseEntity<ApiResponse<Map<String, Object>>> access(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(opsInactiveCardService.accessMeta(authentication)));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            Authentication authentication,
            @RequestParam(required = false) String searchActiveYn,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageResult<Map<String, Object>> pr = opsInactiveCardService.list(
                    authentication, searchActiveYn, page, size);
            return ResponseEntity.ok(ApiResponse.ok(pr));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> row = opsInactiveCardService.register(authentication, body != null ? body : Map.of());
            return ResponseEntity.ok(ApiResponse.ok(row));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Map<String, Object>>> release(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> row = opsInactiveCardService.release(authentication, body != null ? body : Map.of());
            return ResponseEntity.ok(ApiResponse.ok(row));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }
}
