package com.pg.middleware.merchant;

import com.pg.api.ApiResponse;
import com.pg.entity.OrgUnit;
import com.pg.merchantdeploy.MerchantApiResponseMapper;
import com.pg.merchantdeploy.MerchantBrokerAccessVerifier;
import com.pg.merchantdeploy.MerchantChillpayRedirectCheckoutService;
import com.pg.merchantdeploy.MerchantPgBrokerVendor;
import com.pg.repository.OrgUnitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹점용 ChillPay REDIRECT 결제 API — ICOPAY {@code pay.html} 페이지로 브라우저 리다이렉트.
 */
@RestController
@RequestMapping("/api/middleware/v1/merchant/chillpay/redirect-checkout")
public class IcipayMerchantChillpayRedirectCheckoutController {

    private final MerchantChillpayRedirectCheckoutService redirectCheckoutService;
    private final MerchantBrokerAccessVerifier brokerAccessVerifier;
    private final OrgUnitRepository orgUnitRepository;

    public IcipayMerchantChillpayRedirectCheckoutController(MerchantChillpayRedirectCheckoutService redirectCheckoutService,
                                                            MerchantBrokerAccessVerifier brokerAccessVerifier,
                                                            OrgUnitRepository orgUnitRepository) {
        this.redirectCheckoutService = redirectCheckoutService;
        this.brokerAccessVerifier = brokerAccessVerifier;
        this.orgUnitRepository = orgUnitRepository;
    }

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> prepare(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verifyMerchantApi(request, body != null ? body : Map.of(),
                    MerchantPgBrokerVendor.CHILLPAY);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        Long orgUnitId = resolveOrgUnitId(body);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId 또는 merchantId가 필요합니다.", "NOT_FOUND"));
        }
        Map<String, Object> result = redirectCheckoutService.prepare(orgUnitId, body != null ? body : Map.of(), request);
        return MerchantApiResponseMapper.mapServiceResult(result);
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> session(@RequestParam("token") String token) {
        Map<String, Object> result = redirectCheckoutService.readSession(token);
        return MerchantApiResponseMapper.mapServiceResult(result);
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam String orderNo,
            HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("compId", compId);
        body.put("merchantId", merchantId);
        body.put("orderNo", orderNo);
        try {
            brokerAccessVerifier.verifyMerchantApi(request, body, MerchantPgBrokerVendor.CHILLPAY);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        Long orgUnitId = resolveOrgUnitId(body);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId 또는 merchantId가 필요합니다.", "NOT_FOUND"));
        }
        Map<String, Object> result = redirectCheckoutService.orderStatus(orgUnitId, orderNo);
        return MerchantApiResponseMapper.mapServiceResult(result);
    }

    private Long resolveOrgUnitId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object mid = body.get("merchantId");
        if (mid != null && !mid.toString().isBlank()) {
            try {
                return Long.parseLong(mid.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        String compId = body.get("compId") != null ? body.get("compId").toString().trim() : "";
        if (compId.isEmpty()) {
            return null;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(compId);
        return ou.map(OrgUnit::getId).orElse(null);
    }
}
