package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.MerchantProfile;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.OrgServiceUseService;
import com.pg.service.QbangPaymentService;
import com.pg.urlpay.CheckoutHeaderLogoResolver;
import com.pg.urlpay.QrPaymentMeansService;
import com.pg.urlpay.UrlPayPublicCheckoutService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/** QBANG QR 공개 결제 API. */
@RestController
@RequestMapping("/api/pay/qbang")
public class ApiQbangPayController {

    private final QbangPaymentService qbangPaymentService;
    private final UrlPayPublicCheckoutService urlPayPublicCheckoutService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final MerchantProfileRepository merchantProfileRepository;
    private final QrPaymentMeansService qrPaymentMeansService;
    private final CheckoutHeaderLogoResolver checkoutHeaderLogoResolver;

    public ApiQbangPayController(QbangPaymentService qbangPaymentService,
                                 UrlPayPublicCheckoutService urlPayPublicCheckoutService,
                                 OrgUnitRepository orgUnitRepository,
                                 OrgServiceUseService orgServiceUseService,
                                 MerchantProfileRepository merchantProfileRepository,
                                 QrPaymentMeansService qrPaymentMeansService,
                                 CheckoutHeaderLogoResolver checkoutHeaderLogoResolver) {
        this.qbangPaymentService = qbangPaymentService;
        this.urlPayPublicCheckoutService = urlPayPublicCheckoutService;
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.merchantProfileRepository = merchantProfileRepository;
        this.qrPaymentMeansService = qrPaymentMeansService;
        this.checkoutHeaderLogoResolver = checkoutHeaderLogoResolver;
    }

    @GetMapping(value = "/checkout-context", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) String m,
            HttpServletRequest request) {
        Long orgUnitId = resolveOrg(merchantId, firstNonBlank(compId, m));
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        ResponseEntity<ApiResponse<Map<String, Object>>> gate = gateOrgAndWebPayment(orgUnitId);
        if (gate != null) {
            return gate;
        }
        if (!qrPaymentMeansService.qrMeansAllowed(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail("QR 결제를 사용할 수 없습니다.", "QR_MEANS_DISABLED"));
        }
        try {
            Map<String, Object> data = urlPayPublicCheckoutService.buildCheckoutContext(orgUnitId, request, false);
            checkoutHeaderLogoResolver.applyQrToCheckoutMap(data, orgUnitId);
            data.put("payMeans", "QR");
            data.put("qrWallet", "QBANG");
            data.put("qbangConfigured", qbangPaymentService.resolveBinding(orgUnitId, "").isPresent());
            data.putAll(qbangPaymentService.publicNotiIngressUrls());
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
        if (!qrPaymentMeansService.qrMeansAllowed(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail("QR 결제를 사용할 수 없습니다.", "QR_MEANS_DISABLED"));
        }
        if (body != null) {
            body.put("payMeans", "QR");
            body.put("qrWallet", "QBANG");
        }
        Map<String, Object> result = qbangPaymentService.startQr(
                orgUnitId, body, request, request.getRemoteAddr());
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        if (ok) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = result.get("data") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : result;
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        return ResponseEntity.ok(ApiResponse.fail(
                String.valueOf(result.getOrDefault("message", "QR 시작 실패")),
                String.valueOf(result.getOrDefault("errorCode", "START_FAILED"))));
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam String transactionNo,
            @RequestParam(required = false) String orderNo) {
        Long orgUnitId = resolveOrg(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        Map<String, Object> result = qbangPaymentService.pollAndMaybeConfirm(orgUnitId, orderNo, transactionNo);
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

    @GetMapping(value = "/noti-urls", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> notiUrls() {
        return ResponseEntity.ok(ApiResponse.ok(qbangPaymentService.publicNotiIngressUrls()));
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

    private static String str(Object o) {
        return o != null ? String.valueOf(o).trim() : "";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }
}
