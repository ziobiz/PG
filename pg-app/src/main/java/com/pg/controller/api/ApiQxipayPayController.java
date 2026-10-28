package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.OrgServiceUseService;
import com.pg.service.QxipayPaymentService;
import com.pg.urlpay.UrlPayPublicCheckoutService;
import com.pg.urlpay.UrlPaySaleDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** QXIPAY 공개 결제 API — sale·checkout-context. */
@RestController
@RequestMapping("/api/pay/qxipay")
public class ApiQxipayPayController {

    private final QxipayPaymentService qxipayPaymentService;
    private final UrlPaySaleDispatcher urlPaySaleDispatcher;
    private final UrlPayPublicCheckoutService urlPayPublicCheckoutService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;

    public ApiQxipayPayController(QxipayPaymentService qxipayPaymentService,
                                  UrlPaySaleDispatcher urlPaySaleDispatcher,
                                  UrlPayPublicCheckoutService urlPayPublicCheckoutService,
                                  OrgUnitRepository orgUnitRepository,
                                  OrgServiceUseService orgServiceUseService) {
        this.qxipayPaymentService = qxipayPaymentService;
        this.urlPaySaleDispatcher = urlPaySaleDispatcher;
        this.urlPayPublicCheckoutService = urlPayPublicCheckoutService;
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
    }

    @GetMapping(value = "/checkout-context", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
        Long orgUnitId = resolveOrg(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        try {
            Map<String, Object> data = urlPayPublicCheckoutService.buildCheckoutContext(orgUnitId, request, false);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "URL_PAY_ROUTE_NOT_CONFIGURED"));
        }
    }

    @PostMapping(value = "/sale", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> sale(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long orgUnitId = resolveOrg(null, body != null ? str(body.get("compId")) : null);
        if (orgUnitId == null && body != null && body.get("merchantId") != null) {
            try {
                orgUnitId = Long.parseLong(String.valueOf(body.get("merchantId")).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        String clientIp = request.getRemoteAddr();
        Map<String, Object> result = urlPaySaleDispatcher.executeSale(orgUnitId, body, request, clientIp);
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        if (ok) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = result.get("data") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : result;
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        return ResponseEntity.ok(ApiResponse.fail(
                String.valueOf(result.getOrDefault("message", "결제 실패")),
                String.valueOf(result.getOrDefault("errorCode", "SALE_FAILED"))));
    }

    @GetMapping(value = "/noti-urls", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> notiUrls() {
        return ResponseEntity.ok(ApiResponse.ok(qxipayPaymentService.publicNotiIngressUrls()));
    }

    private Long resolveOrg(Long merchantId, String compId) {
        if (merchantId != null) {
            return merchantId;
        }
        if (compId != null && !compId.isBlank()) {
            return orgUnitRepository.findByCode(compId.trim()).map(o -> o.getId()).orElse(null);
        }
        return null;
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }
}
