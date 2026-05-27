package com.pg.middleware.pg;

import com.pg.api.ApiResponse;
import com.pg.controller.api.ApiPayController;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.merchantdeploy.MerchantBrokerAccessVerifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * <strong>PG 미들웨어(ICOPAY)</strong> — 가맹점·파트너가 연동하는 ChillPay 공개 API 베이스.
 * 구현은 기존 {@code /api/pay/chillpay/*} 와 동일하며, 문서·운영에서 이 경로를 “브로커” 표준으로 안내할 수 있습니다.
 * 가맹점에 브로커 시크릿이 발급·강제된 경우 {@link MerchantBrokerAccessVerifier} 가 요청을 검증합니다.
 */
@RestController
@RequestMapping("/api/middleware/v1/pg/chillpay")
public class IcipayPgBrokerChillPayController {

    private final ApiPayController apiPayController;
    private final MerchantBrokerAccessVerifier brokerAccessVerifier;

    public IcipayPgBrokerChillPayController(ApiPayController apiPayController,
                                            MerchantBrokerAccessVerifier brokerAccessVerifier) {
        this.apiPayController = apiPayController;
        this.brokerAccessVerifier = brokerAccessVerifier;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> config(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verify(request, Map.of());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        return apiPayController.chillpayConfig(merchantId, compId, null);
    }

    @GetMapping("/checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verify(request, Map.of());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        return apiPayController.chillpayCheckoutContext(merchantId, compId, null, request);
    }

    @GetMapping("/display-fx-quote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> displayFxQuote(
            @RequestParam String compId,
            @RequestParam(name = "displayCurrency", required = false) String displayCurrency,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verify(request, Map.of());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        return apiPayController.chillpayDisplayFxQuote(compId, displayCurrency);
    }

    @GetMapping("/url-result-copy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> urlResultCopy(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verify(request, Map.of());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        return apiPayController.urlResultCopy(merchantId, compId);
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<ChillPayDirectCreditResponse>> request(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verify(request, body != null ? body : Map.of());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        return apiPayController.chillpayRequest(body, request);
    }
}
