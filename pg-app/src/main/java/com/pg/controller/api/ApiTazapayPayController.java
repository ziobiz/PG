package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.MerchantProfile;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.OrgServiceUseService;
import com.pg.service.TazapayPaymentService;
import com.pg.service.TazapaySaleRecordService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pay/tazapay")
public class ApiTazapayPayController {

    private final TazapayPaymentService tazapayPaymentService;
    private final TazapaySaleRecordService tazapaySaleRecordService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final MerchantProfileRepository merchantProfileRepository;

    public ApiTazapayPayController(TazapayPaymentService tazapayPaymentService,
                                   TazapaySaleRecordService tazapaySaleRecordService,
                                   OrgUnitRepository orgUnitRepository,
                                   OrgServiceUseService orgServiceUseService,
                                   MerchantProfileRepository merchantProfileRepository) {
        this.tazapayPaymentService = tazapayPaymentService;
        this.tazapaySaleRecordService = tazapaySaleRecordService;
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.merchantProfileRepository = merchantProfileRepository;
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
        ResponseEntity<ApiResponse<Map<String, Object>>> gate = gateOrgAndWebPayment(orgUnitId);
        if (gate != null) {
            return gate;
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(tazapayPaymentService.checkoutContext(orgUnitId, request)));
        } catch (IllegalStateException e) {
            if ("TAZAPAY_URL_NOT_CONFIGURED".equals(e.getMessage())) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "이 가맹점은 Tazapay URL 결제 운영 바인딩이 없습니다.", "URL_PAY_ROUTE_NOT_CONFIGURED"));
            }
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "URL_PAY_ROUTE_NOT_CONFIGURED"));
        }
    }

    @GetMapping(value = "/result-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> resultStatus(
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long merchantId) {
        Long orgUnitId = resolveOrg(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        return wrap(tazapayPaymentService.buyerResultStatus(orgUnitId, orderNo), "상태 조회 실패", "STATUS_FAILED");
    }

    @GetMapping(value = "/order-lookup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> orderLookup(
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String orderNo) {
        String q = order != null && !order.isBlank() ? order.trim() : (orderNo != null ? orderNo.trim() : "");
        Map<String, Object> m = tazapaySaleRecordService.lookupCompIdForNoti(q);
        HttpHeaders headers = new HttpHeaders();
        String compId = m.get("compId") != null ? String.valueOf(m.get("compId")).trim() : "";
        String on = m.get("orderNo") != null ? String.valueOf(m.get("orderNo")).trim() : "";
        if (!compId.isBlank()) {
            headers.add("X-Icopay-Comp-Id", compId);
        }
        if (!on.isBlank()) {
            headers.add("X-Icopay-Order-No", on);
        }
        if (!Boolean.TRUE.equals(m.get("success"))) {
            String code = m.get("errorCode") != null ? String.valueOf(m.get("errorCode")) : "NOT_FOUND";
            String msg = m.get("message") != null ? String.valueOf(m.get("message")) : "not found";
            return ResponseEntity.ok().headers(headers).body(ApiResponse.fail(msg, code));
        }
        return ResponseEntity.ok().headers(headers).body(ApiResponse.ok(m));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> wrap(Map<String, Object> result,
                                                                  String defMsg, String defCode) {
        if (Boolean.TRUE.equals(result.get("success"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = result.get("data") instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : result;
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        return ResponseEntity.ok(ApiResponse.fail(
                String.valueOf(result.getOrDefault("message", defMsg)),
                String.valueOf(result.getOrDefault("errorCode", defCode))));
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

    private ResponseEntity<ApiResponse<Map<String, Object>>> gateOrgAndWebPayment(Long orgUnitId) {
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (prof.isPresent() && !orgServiceUseService.isWebPaymentActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(OrgServiceUseService.MSG_WEB_PAYMENT_DISABLED, "WEB_PAYMENT_DISABLED"));
        }
        return null;
    }
}