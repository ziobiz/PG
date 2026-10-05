package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.crypto.CryptoAdminService;
import com.pg.crypto.CryptoPayoutService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 본사 크립토 설정 · 입금내역 · 출금 큐. */
@RestController
@RequestMapping(value = "/api/hq/crypto", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHqCryptoController {

    private final CryptoAdminService adminService;
    private final CryptoPayoutService payoutService;

    public ApiHqCryptoController(CryptoAdminService adminService, CryptoPayoutService payoutService) {
        this.adminService = adminService;
        this.payoutService = payoutService;
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settings() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.hqSettings()));
    }

    @PostMapping("/custody/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveCustody(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(adminService.saveCustody(body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        }
    }

    @GetMapping("/merchant-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> merchantConfig(@RequestParam("compId") String compId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(adminService.merchantConfig(compId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "NOT_FOUND"));
        }
    }

    @PostMapping("/merchant-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveMerchantConfig(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(adminService.saveMerchantConfig(body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        }
    }

    @GetMapping("/deposits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deposits(
            @RequestParam(value = "searchCompId", required = false) String searchCompId,
            @RequestParam(value = "searchStatus", required = false) String searchStatus,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.searchDeposits(searchCompId, searchStatus, page, size)));
    }

    @PostMapping("/deposits/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmDeposit(@RequestBody Map<String, Object> body) {
        try {
            Long id = Long.valueOf(body.get("id").toString());
            String tx = body.get("depositTxHash") != null ? body.get("depositTxHash").toString() : "";
            return ResponseEntity.ok(ApiResponse.ok(adminService.confirmDeposit(id, tx)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CONFIRM_FAILED"));
        }
    }

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payouts(
            @RequestParam(value = "searchCompId", required = false) String searchCompId,
            @RequestParam(value = "searchStatus", required = false) String searchStatus,
            @RequestParam(value = "searchPayoutMode", required = false) String searchPayoutMode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                payoutService.search(searchCompId, searchStatus, searchPayoutMode, page, size)));
    }

    @PostMapping("/payouts/manual-sent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> manualSent(@RequestBody Map<String, Object> body) {
        try {
            Long id = Long.valueOf(body.get("id").toString());
            String tx = body.get("payoutTxHash") != null ? body.get("payoutTxHash").toString() : "";
            return ResponseEntity.ok(ApiResponse.ok(payoutService.markManualSent(id, tx)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "MANUAL_FAILED"));
        }
    }

    @PostMapping("/payouts/auto-run")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoRun(
            @RequestBody(required = false) Map<String, Object> body) {
        int limit = 20;
        if (body != null && body.get("limit") != null) {
            try {
                limit = Integer.parseInt(body.get("limit").toString());
            } catch (NumberFormatException ignored) {
            }
        }
        int n = payoutService.processAutoReady(limit);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("processed", n)));
    }

    @PostMapping("/payouts/retry-auto")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retryAuto(@RequestBody Map<String, Object> body) {
        try {
            Long id = Long.valueOf(body.get("id").toString());
            return ResponseEntity.ok(ApiResponse.ok(payoutService.retryAuto(id)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "RETRY_FAILED"));
        }
    }
}
