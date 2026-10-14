package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.crypto.CryptoAdminService;
import com.pg.crypto.CryptoGasFeeService;
import com.pg.crypto.CryptoPayoutService;
import com.pg.entity.AppUser;
import com.pg.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 본사 크립토 설정 · 입금내역 · 출금 큐. */
@RestController
@RequestMapping(value = "/api/hq/crypto", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHqCryptoController {

    private final CryptoAdminService adminService;
    private final CryptoPayoutService payoutService;
    private final CryptoGasFeeService gasFeeService;
    private final AuthService authService;

    public ApiHqCryptoController(CryptoAdminService adminService, CryptoPayoutService payoutService,
                                 CryptoGasFeeService gasFeeService, AuthService authService) {
        this.adminService = adminService;
        this.payoutService = payoutService;
        this.gasFeeService = gasFeeService;
        this.authService = authService;
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settings() {
        Map<String, Object> data = new java.util.LinkedHashMap<>(adminService.hqSettings());
        data.put("gasFees", gasFeeService.listAll());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** 네트워크별 가스피(전 가맹 동일). 조직% 배분과 별개. */
    @GetMapping("/gas-fees")
    public ResponseEntity<ApiResponse<Map<String, Object>>> gasFees() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("list", gasFeeService.listAll())));
    }

    @PostMapping("/gas-fees/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveGasFee(@RequestBody Map<String, Object> body) {
        try {
            denyIfMerchantMutates();
            requireTotp(body);
            return ResponseEntity.ok(ApiResponse.ok(gasFeeService.saveOne(body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "OTP_REQUIRED"));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/gas-fees/save-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveGasFeesAll(@RequestBody Map<String, Object> body) {
        try {
            denyIfMerchantMutates();
            requireTotp(body);
            List<Map<String, Object>> rows = body != null && body.get("list") instanceof List
                    ? (List<Map<String, Object>>) body.get("list") : List.of();
            return ResponseEntity.ok(ApiResponse.ok(gasFeeService.saveAll(rows)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "OTP_REQUIRED"));
        }
    }

    @PostMapping("/custody/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveCustody(@RequestBody Map<String, Object> body) {
        try {
            denyIfMerchantMutates();
            requireTotp(body);
            return ResponseEntity.ok(ApiResponse.ok(adminService.saveCustody(body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "OTP_REQUIRED"));
        }
    }

    @PostMapping("/custody/delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCustody(@RequestBody Map<String, Object> body) {
        try {
            denyIfMerchantMutates();
            requireTotp(body);
            Long id = body != null && body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
            return ResponseEntity.ok(ApiResponse.ok(adminService.deleteCustody(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "OTP_REQUIRED"));
        }
    }

    @PostMapping("/custody/set-primary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setCustodyPrimary(@RequestBody Map<String, Object> body) {
        try {
            denyIfMerchantMutates();
            requireTotp(body);
            Long id = body != null && body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
            return ResponseEntity.ok(ApiResponse.ok(adminService.setCustodyPrimary(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "OTP_REQUIRED"));
        }
    }

    @GetMapping("/merchant-configs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> merchantConfigs() {
        try {
            denyIfMerchantMutates();
            return ResponseEntity.ok(ApiResponse.ok(Map.of("list", adminService.listMerchantConfigs())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        }
    }

    @GetMapping("/merchant-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> merchantConfig(@RequestParam("compId") String compId) {
        try {
            denyIfMerchantReadsOther(compId);
            return ResponseEntity.ok(ApiResponse.ok(adminService.merchantConfig(compId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "NOT_FOUND"));
        }
    }

    @PostMapping("/merchant-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveMerchantConfig(@RequestBody Map<String, Object> body) {
        try {
            denyIfMerchantMutates();
            return ResponseEntity.ok(ApiResponse.ok(adminService.saveMerchantConfig(body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        }
    }

    /** 가맹 로그인은 본인 업체코드만 조회. */
    private void denyIfMerchantReadsOther(String compId) {
        Map<String, Object> org = currentOrgInfo();
        if (org == null) return;
        String level = org.get("orgLevel") != null ? String.valueOf(org.get("orgLevel")) : "";
        if (!"MERCHANT".equalsIgnoreCase(level)) return;
        String mine = org.get("compId") != null ? String.valueOf(org.get("compId")).trim() : "";
        if (mine.isEmpty() || !mine.equalsIgnoreCase(String.valueOf(compId == null ? "" : compId).trim())) {
            throw new IllegalArgumentException("본인 가맹 크립토 설정만 조회할 수 있습니다.");
        }
    }

    /** 가맹 로그인은 크립토 설정 저장 불가(조회·QR만). */
    private void denyIfMerchantMutates() {
        Map<String, Object> org = currentOrgInfo();
        if (org == null) return;
        String level = org.get("orgLevel") != null ? String.valueOf(org.get("orgLevel")) : "";
        if ("MERCHANT".equalsIgnoreCase(level)) {
            throw new IllegalArgumentException("가맹점은 크립토 설정을 변경할 수 없습니다. 본사·관리자에게 요청하세요.");
        }
    }

    private Map<String, Object> currentOrgInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) return null;
        try {
            return authService.getOrgInfo(user.getUsername());
        } catch (Exception e) {
            return null;
        }
    }

    /** 대표수탁지갑 추가·수정·삭제 시 Google OTP 필수. */
    private void requireTotp(Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String totp = "";
        if (body != null) {
            if (body.get("totpCode") != null) totp = String.valueOf(body.get("totpCode")).trim();
            if (totp.isEmpty() && body.get("otp") != null) totp = String.valueOf(body.get("otp")).trim();
        }
        authService.verifyTotpOrThrow(user, totp);
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
