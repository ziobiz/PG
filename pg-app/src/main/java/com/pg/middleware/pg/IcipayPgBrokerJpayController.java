package com.pg.middleware.pg;

import com.pg.api.ApiResponse;
import com.pg.controller.api.ApiPayController;
import com.pg.merchantdeploy.MerchantBrokerAccessVerifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * PG 미들웨어(ICOPAY) — JPAY 브로커 엔드포인트.
 */
@RestController
@RequestMapping("/api/middleware/v1/pg/jpay")
public class IcipayPgBrokerJpayController {

    private final ApiPayController apiPayController;
    private final MerchantBrokerAccessVerifier brokerAccessVerifier;

    public IcipayPgBrokerJpayController(ApiPayController apiPayController,
                                        MerchantBrokerAccessVerifier brokerAccessVerifier) {
        this.apiPayController = apiPayController;
        this.brokerAccessVerifier = brokerAccessVerifier;
    }

    @PostMapping("/sale")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sale(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verify(request, body != null ? body : Map.of());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        return apiPayController.jpaySale(body, request);
    }
}
