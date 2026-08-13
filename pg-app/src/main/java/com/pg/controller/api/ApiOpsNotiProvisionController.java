package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.noti.NotiProvisionException;
import com.pg.service.ops.OpsNotiProvisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 운영관리 — NOTI Provision (노티생성: JPAY · ElementPay). */
@RestController
@RequestMapping(value = "/api/ops/notiProvision", produces = "application/json")
public class ApiOpsNotiProvisionController {

    private final OpsNotiProvisionService opsNotiProvisionService;

    public ApiOpsNotiProvisionController(OpsNotiProvisionService opsNotiProvisionService) {
        this.opsNotiProvisionService = opsNotiProvisionService;
    }

    @GetMapping("/access")
    public ResponseEntity<ApiResponse<Map<String, Object>>> access(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(opsNotiProvisionService.accessMeta(authentication)));
    }

    @GetMapping("/context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> context(
            Authentication authentication,
            @RequestParam String compId,
            @RequestParam(required = false) String adminLang) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    opsNotiProvisionService.merchantContext(authentication, compId, adminLang)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    @GetMapping("/checkMerchantId")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkMerchantId(
            Authentication authentication,
            @RequestParam String merchantId,
            @RequestParam(required = false) String compId,
            @RequestParam(required = false, defaultValue = "jpay") String pgKind) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    opsNotiProvisionService.checkMerchantId(authentication, merchantId, compId, pgKind)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/nextSlot")
    public ResponseEntity<ApiResponse<Map<String, Object>>> nextSlot(
            Authentication authentication,
            @RequestParam(defaultValue = "JPY") String baseCurrency) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    opsNotiProvisionService.nextAutoSlot(authentication, baseCurrency)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/checkSlot")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSlot(
            Authentication authentication,
            @RequestParam Integer slotNo,
            @RequestParam(required = false) String merchantId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    opsNotiProvisionService.checkJpaySlot(authentication, slotNo, merchantId)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            Authentication authentication,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    opsNotiProvisionService.list(authentication, searchCompId, page, size)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(
            Authentication authentication,
            @RequestParam String compId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(opsNotiProvisionService.merchantStatus(authentication, compId)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "ERROR"));
        }
    }

    @PostMapping("/provision")
    public ResponseEntity<ApiResponse<Map<String, Object>>> provision(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(opsNotiProvisionService.provision(authentication, body)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @GetMapping("/log/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logDetail(
            Authentication authentication,
            @RequestParam Long id,
            @RequestParam(required = false) String adminLang) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    opsNotiProvisionService.logDetail(authentication, id, adminLang)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/log/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateLog(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(opsNotiProvisionService.updateLog(authentication, body)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/log/delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteLog(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(opsNotiProvisionService.deleteLog(authentication, body)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/log/deleteBulk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteLogs(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(opsNotiProvisionService.deleteLogs(authentication, body)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "FORBIDDEN"));
        } catch (NotiProvisionException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }
}
