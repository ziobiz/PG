package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.ElementPayClientApiService;
import com.pg.service.HqRemitAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 운영관리 — 송금관리. ElementPay 반자동 송금 요청과 송금 기록 조회.
 */
@RestController
@RequestMapping("/api/ops/remittance")
public class ApiOpsRemittanceController {

    private final ElementPayClientApiService elementPayClientApiService;
    private final HqRemitAccountService hqRemitAccountService;

    public ApiOpsRemittanceController(
            ElementPayClientApiService elementPayClientApiService,
            HqRemitAccountService hqRemitAccountService) {
        this.elementPayClientApiService = elementPayClientApiService;
        this.hqRemitAccountService = hqRemitAccountService;
    }

    @GetMapping(value = "/accounts", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> accounts() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(hqRemitAccountService.list(currentUser())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ELEMENTPAY"));
        }
    }

    @PostMapping(value = "/accounts", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAccount(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(hqRemitAccountService.create(currentUser(), body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ELEMENTPAY"));
        }
    }

    @PostMapping(value = "/accounts/{id}/default", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> defaultAccount(@PathVariable long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(hqRemitAccountService.markDefault(currentUser(), id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ELEMENTPAY"));
        }
    }

    @GetMapping(value = "/balance", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> balance() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(elementPayClientApiService.balanceReport(currentUser())));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ELEMENTPAY"));
        }
    }

    @GetMapping(value = "/transfers", produces = "application/json")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> transfers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    elementPayClientApiService.listTransfers(currentUser(), page, size)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ELEMENTPAY"));
        }
    }

    @GetMapping(value = "/recipients", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recipients() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(elementPayClientApiService.listRecipients(currentUser())));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ELEMENTPAY"));
        }
    }

    @GetMapping(value = "/banks", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> banks() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(elementPayClientApiService.listBanks(currentUser())));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ELEMENTPAY"));
        }
    }

    @PostMapping(value = "/transfers", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(elementPayClientApiService.createTransfer(currentUser(), body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ELEMENTPAY"));
        }
    }

    private static Authentication currentUser() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
