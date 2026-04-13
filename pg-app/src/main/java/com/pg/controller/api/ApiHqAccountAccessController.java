package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.HqAccountAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 본사설정 — 계정·업체 접근 허용 (ziobiz/NOTI 계정관리 대응)
 */
@RestController
@RequestMapping(value = "/api/hq/accountAccess", produces = "application/json")
public class ApiHqAccountAccessController {

    private final HqAccountAccessService hqAccountAccessService;

    public ApiHqAccountAccessController(HqAccountAccessService hqAccountAccessService) {
        this.hqAccountAccessService = hqAccountAccessService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(hqAccountAccessService.listAll()));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> add(@RequestBody Map<String, Object> body) {
        try {
            String username = body.get("username") != null ? body.get("username").toString() : "";
            String compCode = body.get("compCode") != null ? body.get("compCode").toString() : "";
            hqAccountAccessService.add(username, compCode);
            return ResponseEntity.ok(ApiResponse.ok(hqAccountAccessService.listAll()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        try {
            String username = body.get("username") != null ? body.get("username").toString() : "";
            String compCode = body.get("compCode") != null ? body.get("compCode").toString() : "";
            hqAccountAccessService.update(id, username, compCode);
            return ResponseEntity.ok(ApiResponse.ok(hqAccountAccessService.listAll()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable long id) {
        try {
            hqAccountAccessService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(hqAccountAccessService.listAll()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }
}
