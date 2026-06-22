package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.splitpay.SplitPayContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/pay/split")
public class ApiSplitPayController {

    private final SplitPayContractService splitPayContractService;

    public ApiSplitPayController(SplitPayContractService splitPayContractService) {
        this.splitPayContractService = splitPayContractService;
    }

    @GetMapping("/merchant-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> merchantConfig(@RequestParam String compId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(splitPayContractService.merchantConfig(compId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> preview(@RequestBody Map<String, Object> body) {
        try {
            String compId = str(body, "compId");
            BigDecimal total = bd(body, "totalAmount");
            int count = intVal(body, "installmentCount", 0);
            String intervalType = str(body, "intervalType");
            Integer intervalValue = intObj(body, "intervalValue");
            LocalDate contractDate = body.get("contractDate") != null
                    ? LocalDate.parse(body.get("contractDate").toString().trim()) : null;
            return ResponseEntity.ok(ApiResponse.ok(
                    splitPayContractService.preview(compId, total, count, intervalType, intervalValue, contractDate)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID_REQUEST"));
        }
    }

    @PostMapping("/contracts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody Map<String, Object> body,
                                                                     HttpServletRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(splitPayContractService.createContract(
                    str(body, "compId"),
                    str(body, "customerEmail"),
                    str(body, "customerName"),
                    bd(body, "totalAmount"),
                    intVal(body, "installmentCount", 0),
                    str(body, "intervalType"),
                    intObj(body, "intervalValue"),
                    str(body, "currencyCode"),
                    request)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID_REQUEST"));
        }
    }

    @GetMapping("/installment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> installment(@RequestParam String token,
                                                                         HttpServletRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(splitPayContractService.installmentCheckoutContext(token, request)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail("결제 정보를 찾을 수 없습니다.", "NOT_FOUND"));
        }
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString().trim() : "";
    }

    private static BigDecimal bd(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || v.toString().isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(v.toString().trim());
    }

    private static int intVal(Map<String, Object> body, String key, int def) {
        Object v = body.get(key);
        if (v == null || v.toString().isBlank()) {
            return def;
        }
        return Integer.parseInt(v.toString().trim());
    }

    private static Integer intObj(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || v.toString().isBlank()) {
            return null;
        }
        return Integer.parseInt(v.toString().trim());
    }
}
