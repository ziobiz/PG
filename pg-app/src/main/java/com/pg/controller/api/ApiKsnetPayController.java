package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.MerchantProfile;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.KsnetPaymentService;
import com.pg.service.KsnetSaleRecordService;
import com.pg.service.OrgServiceUseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * KSNET(해외 USD) 공개 결제 API — checkout-context · approve(payKey→서버 승인) · cancel(결제창 닫힘) · result-status.
 * 결제 시작(requestPay 파라미터)은 통합 {@code POST /api/pay/url/sale} 을 쓴다.
 * 응답은 ICOPAY 중립(PG명 미노출).
 */
@RestController
@RequestMapping("/api/pay/ksnet")
public class ApiKsnetPayController {

    private final KsnetPaymentService ksnetPaymentService;
    private final KsnetSaleRecordService ksnetSaleRecordService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final MerchantProfileRepository merchantProfileRepository;

    public ApiKsnetPayController(KsnetPaymentService ksnetPaymentService,
                                 KsnetSaleRecordService ksnetSaleRecordService,
                                 OrgUnitRepository orgUnitRepository,
                                 OrgServiceUseService orgServiceUseService,
                                 MerchantProfileRepository merchantProfileRepository) {
        this.ksnetPaymentService = ksnetPaymentService;
        this.ksnetSaleRecordService = ksnetSaleRecordService;
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
            return ResponseEntity.ok(ApiResponse.ok(ksnetPaymentService.checkoutContext(orgUnitId, request)));
        } catch (IllegalStateException e) {
            if ("KSNET_URL_NOT_CONFIGURED".equals(e.getMessage())) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "이 가맹점은 해외 USD 결제 운영 바인딩이 없습니다.", "URL_PAY_ROUTE_NOT_CONFIGURED"));
            }
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "URL_PAY_ROUTE_NOT_CONFIGURED"));
        }
    }

    /**
     * requestPay 성공(payKey) 후 서버 승인. 금액은 서버 대기거래에서 산출 — 클라이언트 금액은 받지 않는다.
     * body: {@code orderNo}, {@code payKey}(또는 commConId), {@code compId}(선택), {@code deviceType}(선택)
     */
    @PostMapping(value = "/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String orderNo = str(body, "orderNo", "order_no");
        String payKey = str(body, "payKey", "commConId", "pay_key");
        String compId = str(body, "compId", "m");
        String device = str(body, "deviceType");
        boolean mobile = "MOBILE".equalsIgnoreCase(device) || "M".equalsIgnoreCase(device);
        Map<String, Object> result = ksnetPaymentService.executeApprove(
                compId, orderNo, payKey, mobile, request.getRemoteAddr());
        return wrap(result, "승인 실패", "KSNET_APPROVE_FAILED");
    }

    /** 구매자가 결제창을 닫음·인증 실패 — 대기 거래만 실패 처리(금전 이동 없음). 환불은 결제내역에서 수행. */
    @PostMapping(value = "/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancel(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = ksnetPaymentService.reportAuthAborted(
                str(body, "compId", "m"), str(body, "orderNo", "order_no"), str(body, "reason"));
        return wrap(result, "취소 처리 실패", "KSNET_CANCEL_FAILED");
    }

    /** 구매자 return 화면 폴링. */
    @GetMapping(value = "/result-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> resultStatus(
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long merchantId) {
        Long orgUnitId = resolveOrg(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        return wrap(ksnetPaymentService.buyerResultStatus(orgUnitId, orderNo), "상태 조회 실패", "STATUS_FAILED");
    }

    /** NOTI ingress {@code icopayOrderLookupUrl} — {@code GET .../order-lookup?order=} (헤더 X-Icopay-Comp-Id). */
    @GetMapping(value = "/order-lookup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> orderLookup(
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String orderNo) {
        String q = order != null && !order.isBlank() ? order.trim() : (orderNo != null ? orderNo.trim() : "");
        Map<String, Object> m = ksnetSaleRecordService.lookupCompIdForNoti(q);
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
            Map<String, Object> data = result.get("data") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : result;
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        return ResponseEntity.ok(ApiResponse.fail(
                String.valueOf(result.getOrDefault("message", defMsg)),
                String.valueOf(result.getOrDefault("errorCode", defCode))));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> gateOrgAndWebPayment(Long orgUnitId) {
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (prof.isPresent() && !orgServiceUseService.isWebPaymentActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    OrgServiceUseService.MSG_WEB_PAYMENT_DISABLED, "WEB_PAYMENT_DISABLED"));
        }
        return null;
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

    private static String str(Map<String, Object> body, String... keys) {
        if (body == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            Object v = body.get(k);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v).trim();
            }
        }
        return "";
    }
}
