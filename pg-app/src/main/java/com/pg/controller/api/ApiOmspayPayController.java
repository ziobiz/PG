package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.MerchantProfile;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.service.OmspayPaymentService;
import com.pg.service.OmspaySaleRecordService;
import com.pg.service.OrgServiceUseService;
import com.pg.urlpay.UrlPayPublicCheckoutService;
import com.pg.urlpay.UrlPaySaleDispatcher;
import com.pg.urlpay.UrlPayVendorCapabilityRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/** OMSPAY 공개 결제 API — start(리다이렉트)·checkout-context·status. */
@RestController
@RequestMapping("/api/pay/omspay")
public class ApiOmspayPayController {

    private final OmspayPaymentService omspayPaymentService;
    private final OmspaySaleRecordService omspaySaleRecordService;
    private final UrlPaySaleDispatcher urlPaySaleDispatcher;
    private final UrlPayPublicCheckoutService urlPayPublicCheckoutService;
    private final ChillPayService chillPayService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final MerchantProfileRepository merchantProfileRepository;

    public ApiOmspayPayController(OmspayPaymentService omspayPaymentService,
                                  OmspaySaleRecordService omspaySaleRecordService,
                                  UrlPaySaleDispatcher urlPaySaleDispatcher,
                                  UrlPayPublicCheckoutService urlPayPublicCheckoutService,
                                  ChillPayService chillPayService,
                                  OrgUnitRepository orgUnitRepository,
                                  OrgServiceUseService orgServiceUseService,
                                  MerchantProfileRepository merchantProfileRepository) {
        this.omspayPaymentService = omspayPaymentService;
        this.omspaySaleRecordService = omspaySaleRecordService;
        this.urlPaySaleDispatcher = urlPaySaleDispatcher;
        this.urlPayPublicCheckoutService = urlPayPublicCheckoutService;
        this.chillPayService = chillPayService;
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
            Map<String, Object> data = urlPayPublicCheckoutService.buildCheckoutContext(orgUnitId, request, false);
            /* ElementPay 등 DP PG가 기본 운영이어도 OMSPAY 전용 창은 OMSPAY 바인딩·금액모드를 씀 */
            String omsPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                    orgUnitId, PgVendor::isOmspayFamily);
            if (omsPg == null || omsPg.isBlank()) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "이 가맹점은 OMSPAY URL 결제 운영 바인딩이 없습니다.",
                        "OMSPAY_URL_NOT_CONFIGURED"));
            }
            String curOp = String.valueOf(data.getOrDefault("urlPayOperationalPgCd", "")).trim();
            if (!omsPg.equalsIgnoreCase(curOp)) {
                data.put("urlPayOperationalPgCd", omsPg);
                data.put("urlPayInlineWidgetKind",
                        UrlPayVendorCapabilityRegistry.resolveInlineWidgetKindLegacy(omsPg));
                urlPayPublicCheckoutService.enrichPresentation(data, orgUnitId);
            }
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "URL_PAY_ROUTE_NOT_CONFIGURED"));
        }
    }

    @PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> start(
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
        ResponseEntity<ApiResponse<Map<String, Object>>> gate = gateOrgAndWebPayment(orgUnitId);
        if (gate != null) {
            return gate;
        }
        Map<String, Object> result = urlPaySaleDispatcher.executeSale(orgUnitId, body, request, request.getRemoteAddr());
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        if (ok) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = result.get("data") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : result;
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        return ResponseEntity.ok(ApiResponse.fail(
                String.valueOf(result.getOrDefault("message", "결제 시작 실패")),
                String.valueOf(result.getOrDefault("errorCode", "START_FAILED"))));
    }

    @GetMapping(value = "/noti-urls", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> notiUrls() {
        return ResponseEntity.ok(ApiResponse.ok(omspayPaymentService.publicNotiIngressUrls()));
    }

    /** 구매자 return 화면 폴링 — 웹훅 반영 후 PAID/PENDING/FAIL */
    @GetMapping(value = "/result-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> resultStatus(
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long merchantId) {
        Long orgUnitId = resolveOrg(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        Map<String, Object> result = omspayPaymentService.buyerResultStatus(orgUnitId, orderNo);
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        if (ok) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = result.get("data") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : result;
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        return ResponseEntity.ok(ApiResponse.fail(
                String.valueOf(result.getOrDefault("message", "상태 조회 실패")),
                String.valueOf(result.getOrDefault("errorCode", "STATUS_FAILED"))));
    }

    /**
     * NOTI OMSPAY ingress {@code icopayOrderLookupUrl} —
     * {@code GET .../order-lookup?order={order}} (SO-… 또는 가맹 orderNo).
     * 응답 헤더 {@code X-Icopay-Comp-Id} + JSON {@code compId}.
     */
    @GetMapping(value = "/order-lookup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> orderLookup(
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String orderNo) {
        String q = order != null && !order.isBlank() ? order.trim()
                : (orderNo != null ? orderNo.trim() : "");
        Map<String, Object> m = omspaySaleRecordService.lookupCompIdForNoti(q);
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

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }
}
