package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 결제 API - ChillPay DirectCredit 연동.
 */
@RestController
@RequestMapping(value = "/api/pay", produces = "application/json")
public class ApiPayController {

    private final ChillPayService chillPayService;
    private final OrgUnitRepository orgUnitRepository;

    public ApiPayController(ChillPayService chillPayService, OrgUnitRepository orgUnitRepository) {
        this.chillPayService = chillPayService;
        this.orgUnitRepository = orgUnitRepository;
    }

    private Long resolveMerchantOrgUnitId(Long merchantId, String compId) {
        if (merchantId != null) return merchantId;
        if (compId != null && !compId.isEmpty()) {
            return orgUnitRepository.findByCode(compId.trim()).map(o -> o.getId()).orElse(null);
        }
        return null;
    }

    /**
     * ChillPay 결제 페이지용 설정 (CCD 스크립트 URL, Merchant Code, 호스티드 결제 CDN URL, v2 Payment API URL 등).
     * 프론트엔드에서 ChillPay CCD 스크립트 로드·리다이렉트 연동 안내에 사용.
     */
    @GetMapping("/chillpay/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chillpayConfig(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId) {
        Long orgUnitId = resolveMerchantOrgUnitId(merchantId, compId);
        return ResponseEntity.ok(ApiResponse.ok(chillPayService.getConfigForFrontend(orgUnitId)));
    }

    /**
     * ChillPay DirectCredit 결제 요청.
     * CCD 스크립트에서 발급받은 DirectCreditToken과 주문 정보를 받아 ChillPay API 호출.
     */
    @PostMapping("/chillpay/request")
    public ResponseEntity<ApiResponse<ChillPayDirectCreditResponse>> chillpayRequest(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Long merchantIdVal = null;
        Object mid = body.get("merchantId");
        if (mid != null && !mid.toString().isEmpty()) {
            try { merchantIdVal = Long.parseLong(mid.toString()); } catch (NumberFormatException ignored) {}
        }
        Long merchantOrgUnitId = resolveMerchantOrgUnitId(merchantIdVal, (String) body.get("compId"));

        String directCreditToken = (String) body.get("directCreditToken");
        if (directCreditToken == null || directCreditToken.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("DirectCreditToken이 필요합니다.", "INVALID_TOKEN"));
        }

        Object amountObj = body.get("amount");
        Long amount = null;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).longValue();
        } else if (amountObj instanceof String) {
            try {
                amount = Long.parseLong((String) amountObj);
            } catch (NumberFormatException ignored) {}
        }
        if (amount == null || amount <= 0) {
            return ResponseEntity.ok(ApiResponse.fail("유효한 결제 금액을 입력하세요.", "INVALID_AMOUNT"));
        }

        String orderNo = (String) body.get("orderNo");
        String customerId = body.get("customerId") != null ? body.get("customerId").toString() : "guest";
        String phoneNumber = (String) body.get("phoneNumber");
        String description = (String) body.get("description");
        String custEmail = (String) body.get("custEmail");

        String ipAddress = getClientIp(request);

        try {
            ChillPayDirectCreditResponse res = chillPayService.requestPayment(
                    orderNo, customerId, amount, directCreditToken,
                    phoneNumber, description, ipAddress, custEmail,
                    merchantOrgUnitId
            );
            return ResponseEntity.ok(ApiResponse.ok(res));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(
                    e.getMessage() != null ? e.getMessage() : "결제 요청 처리 중 오류가 발생했습니다.",
                    "PAYMENT_ERROR"
            ));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
