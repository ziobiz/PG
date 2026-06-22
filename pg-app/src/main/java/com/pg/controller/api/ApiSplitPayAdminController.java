package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.splitpay.SplitPayAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/splitpay")
public class ApiSplitPayAdminController {

    private final SplitPayAdminService splitPayAdminService;

    public ApiSplitPayAdminController(SplitPayAdminService splitPayAdminService) {
        this.splitPayAdminService = splitPayAdminService;
    }

    @GetMapping("/progressList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> progressList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                splitPayAdminService.searchProgress(page, size, compId, contractNo, status,
                        searchFromDate, searchToDate, authentication)));
    }

    @GetMapping("/mailList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> mailList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                splitPayAdminService.searchMail(page, size, compId, contractNo, status,
                        searchFromDate, searchToDate, authentication)));
    }

    @PostMapping("/resendMail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendMail(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Object idObj = body != null ? body.get("installmentId") : null;
        if (idObj == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("installmentId가 필요합니다."));
        }
        long installmentId;
        try {
            installmentId = Long.parseLong(String.valueOf(idObj).trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("installmentId 형식이 올바르지 않습니다."));
        }
        String phase = body != null && body.get("phase") != null ? String.valueOf(body.get("phase")) : "D0";
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    splitPayAdminService.resendInstallmentMail(installmentId, phase, authentication)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
