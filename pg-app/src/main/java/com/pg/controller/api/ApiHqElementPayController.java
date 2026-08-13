package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.ElementPayPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 본사 — ElementPay getMethods 진단·cardServiceAlias 안내용.
 */
@RestController
@RequestMapping(value = "/api/hq", produces = "application/json")
public class ApiHqElementPayController {

    private final ElementPayPaymentService elementPayPaymentService;

    public ApiHqElementPayController(ElementPayPaymentService elementPayPaymentService) {
        this.elementPayPaymentService = elementPayPaymentService;
    }

    @PostMapping("/pgApiMng/elementpayMethods")
    public ResponseEntity<ApiResponse<Map<String, Object>>> elementpayMethods(@RequestBody Map<String, Object> body) {
        Long id = null;
        if (body != null && body.get("id") != null) {
            try {
                id = Long.valueOf(String.valueOf(body.get("id")).trim());
            } catch (NumberFormatException ignored) {
                id = null;
            }
        }
        Map<String, Object> result = elementPayPaymentService.listPaymentMethods(id);
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        if (!ok) {
            return ResponseEntity.ok(ApiResponse.fail(
                    String.valueOf(result.getOrDefault("message", "ElementPay methods failed")),
                    String.valueOf(result.getOrDefault("errorCode", "ELEMENTPAY_METHODS_FAILED"))));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
