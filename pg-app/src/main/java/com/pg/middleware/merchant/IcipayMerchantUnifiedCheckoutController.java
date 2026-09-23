package com.pg.middleware.merchant;

import com.pg.api.ApiResponse;
import com.pg.entity.OrgUnit;
import com.pg.merchantdeploy.MerchantApiResponseMapper;
import com.pg.merchantdeploy.MerchantBrokerAccessVerifier;
import com.pg.merchantdeploy.MerchantPgBrokerVendor;
import com.pg.merchantdeploy.MerchantSandboxCheckoutService;
import com.pg.merchantdeploy.MerchantUnifiedInlineCheckoutService;
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
 * ICOPAY 통합 가맹 인라인 checkout API.
 * Sandbox 브로커 시크릿이면 시뮬 승인(실PG·NOTI 미호출).
 */
@RestController
@RequestMapping("/api/middleware/v1/merchant/checkout")
public class IcipayMerchantUnifiedCheckoutController {

    private final MerchantUnifiedInlineCheckoutService unifiedCheckoutService;
    private final MerchantSandboxCheckoutService sandboxCheckoutService;
    private final MerchantBrokerAccessVerifier brokerAccessVerifier;
    private final OrgUnitRepository orgUnitRepository;

    public IcipayMerchantUnifiedCheckoutController(MerchantUnifiedInlineCheckoutService unifiedCheckoutService,
                                                   MerchantSandboxCheckoutService sandboxCheckoutService,
                                                   MerchantBrokerAccessVerifier brokerAccessVerifier,
                                                   OrgUnitRepository orgUnitRepository) {
        this.unifiedCheckoutService = unifiedCheckoutService;
        this.sandboxCheckoutService = sandboxCheckoutService;
        this.brokerAccessVerifier = brokerAccessVerifier;
        this.orgUnitRepository = orgUnitRepository;
    }

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> prepare(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verifyMerchantApi(request, body != null ? body : Map.of(),
                    MerchantPgBrokerVendor.ALL);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        Long orgUnitId = resolveOrgUnitId(body);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId 또는 merchantId가 필요합니다.", "NOT_FOUND"));
        }
        Map<String, Object> result;
        if (MerchantBrokerAccessVerifier.isSandboxRequest(request)) {
            result = sandboxCheckoutService.prepare(orgUnitId, body != null ? body : Map.of());
        } else {
            result = unifiedCheckoutService.prepare(orgUnitId, body != null ? body : Map.of(), request);
        }
        return MerchantApiResponseMapper.mapServiceResult(result);
    }

    /** 샌드박스 전용: prepare 후 시뮬 승인(또는 forceFail). Live 시크릿으로는 거부. */
    @PostMapping({"/complete", "/sandbox/complete"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> complete(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            brokerAccessVerifier.verifyMerchantApi(request, body != null ? body : Map.of(),
                    MerchantPgBrokerVendor.ALL);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        if (!MerchantBrokerAccessVerifier.isSandboxRequest(request)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "샌드박스 시크릿으로만 complete 할 수 있습니다.", "SANDBOX_ONLY"));
        }
        Long orgUnitId = resolveOrgUnitId(body);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId 또는 merchantId가 필요합니다.", "NOT_FOUND"));
        }
        Map<String, Object> result = sandboxCheckoutService.complete(orgUnitId, body != null ? body : Map.of());
        return MerchantApiResponseMapper.mapServiceResult(result);
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> session(
            @RequestParam("token") String token,
            HttpServletRequest request) {
        Map<String, Object> result = unifiedCheckoutService.readSession(token, request);
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
            brokerAccessVerifier.verifyMerchantApi(request, body, MerchantPgBrokerVendor.ALL);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(e.getMessage(), "BROKER_AUTH"));
        }
        Long orgUnitId = resolveOrgUnitId(body);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId 또는 merchantId가 필요합니다.", "NOT_FOUND"));
        }
        Map<String, Object> result;
        if (MerchantBrokerAccessVerifier.isSandboxRequest(request)) {
            result = sandboxCheckoutService.orderStatus(orgUnitId, orderNo);
        } else {
            result = unifiedCheckoutService.orderStatus(orgUnitId, orderNo);
        }
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
        String c = body.get("compId") != null ? body.get("compId").toString().trim() : "";
        if (c.isEmpty()) {
            return null;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(c);
        return ou.map(OrgUnit::getId).orElse(null);
    }
}
