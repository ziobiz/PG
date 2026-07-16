package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.MerchantIlkSubscription;
import com.pg.merchantdeploy.MerchantApiResponseMapper;
import com.pg.repository.MerchantIlkSubscriptionRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.IlkPaymentService;
import com.pg.service.OrgServiceUseService;
import com.pg.urlpay.CheckoutFailI18n;
import com.pg.urlpay.UrlPayPublicCheckoutService;
import com.pg.urlpay.UrlPaySaleDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/** ILK 전용 공개 결제 API — Front Noti·sale·checkout-context. */
@RestController
@RequestMapping("/api/pay/ilk")
public class ApiIlkPayController {

    private final IlkPaymentService ilkPaymentService;
    private final UrlPaySaleDispatcher urlPaySaleDispatcher;
    private final UrlPayPublicCheckoutService urlPayPublicCheckoutService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final MerchantIlkSubscriptionRepository subscriptionRepository;

    public ApiIlkPayController(IlkPaymentService ilkPaymentService,
                               UrlPaySaleDispatcher urlPaySaleDispatcher,
                               UrlPayPublicCheckoutService urlPayPublicCheckoutService,
                               OrgUnitRepository orgUnitRepository,
                               OrgServiceUseService orgServiceUseService,
                               MerchantIlkSubscriptionRepository subscriptionRepository) {
        this.ilkPaymentService = ilkPaymentService;
        this.urlPaySaleDispatcher = urlPaySaleDispatcher;
        this.urlPayPublicCheckoutService = urlPayPublicCheckoutService;
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.subscriptionRepository = subscriptionRepository;
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
        Long orgUnitId = resolveOrg(null, str(body, "compId"));
        if (orgUnitId == null) {
            Object mid = body.get("merchantId");
            if (mid != null && !mid.toString().isBlank()) {
                try {
                    orgUnitId = Long.parseLong(mid.toString().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (orgUnitId == null) {
            Map<String, Object> nf = CheckoutFailI18n.merchantNotFound();
            return ResponseEntity.ok(MerchantApiResponseMapper.failFromResultMap(nf, "not found", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        boolean subscription = "SUBSCRIPTION".equalsIgnoreCase(str(body, "checkoutKind"))
                || body.containsKey("subscriptionPlan");
        Map<String, Object> result = subscription
                ? ilkPaymentService.executeSubscriptionFirstCharge(orgUnitId, body, request, clientIp(request))
                : urlPaySaleDispatcher.executeSale(orgUnitId, body, request, clientIp(request));
        Object ok = result.get("success");
        if (ok instanceof Boolean && !(Boolean) ok) {
            return ResponseEntity.ok(MerchantApiResponseMapper.failFromResultMap(
                    result, "Payment failed", "PAYMENT_FAILED"));
        }
        if (subscription && Boolean.TRUE.equals(result.get("success"))
                && !Boolean.TRUE.equals(result.get("needs3ds"))) {
            activateSubscription(str(body, "compId"), str(body, "orderNo"),
                    String.valueOf(result.getOrDefault("id", "")));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** ILK Front Noti — ACS 완료 후 form POST. */
    @PostMapping(value = "/front-notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> frontNotify(@RequestParam MultiValueMap<String, String> form) {
        String orderNo = first(form, "clientReferenceInformation.code");
        String status = first(form, "status");
        String id = first(form, "id");
        boolean success = "SUCCESS".equalsIgnoreCase(status);
        if (success && !orderNo.isBlank()) {
            Optional<MerchantIlkSubscription> sub = subscriptionRepository.findFirstBySubscriptionNoOrderByIdDesc(orderNo);
            if (sub.isEmpty()) {
                sub = subscriptionRepository.findFirstByFirstOrderNoOrderByIdDesc(orderNo);
            }
            sub.ifPresent(s -> {
                s.setStatus(MerchantIlkSubscription.STATUS_ACTIVE);
                s.setFirstAuthId(id);
                s.setChargeCount(s.getChargeCount() == null || s.getChargeCount() < 1 ? 1 : s.getChargeCount());
                s.setLastChargeAt(LocalDateTime.now());
                if (s.getNextChargeAt() == null) {
                    s.setNextChargeAt(LocalDateTime.now().plusDays(30));
                }
                subscriptionRepository.save(s);
            });
        }
        String html = """
                <!DOCTYPE html><html><head><meta charset="UTF-8"><title>ICOPAY</title></head>
                <body><script>
                try {
                  var payload = { type: 'ILK_FRONT_NOTI', orderNo: %s, status: %s, id: %s, success: %s };
                  if (window.opener && !window.opener.closed) {
                    window.opener.postMessage(payload, '*');
                  }
                } catch (e) {}
                try { window.close(); } catch (e2) {}
                document.write('<p>Payment authentication finished. You may close this window.</p>');
                </script></body></html>
                """.formatted(jsStr(orderNo), jsStr(status), jsStr(id), success);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    private void activateSubscription(String compId, String orderNo, String authId) {
        if (compId.isBlank() || orderNo.isBlank()) {
            return;
        }
        subscriptionRepository.findByCompIdAndSubscriptionNo(compId, orderNo).ifPresent(s -> {
            s.setStatus(MerchantIlkSubscription.STATUS_ACTIVE);
            s.setFirstAuthId(authId);
            s.setChargeCount(1);
            s.setLastChargeAt(LocalDateTime.now());
            s.setNextChargeAt(LocalDateTime.now().plusDays(30));
            subscriptionRepository.save(s);
        });
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

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
    }

    private static String first(MultiValueMap<String, String> form, String key) {
        if (form == null || key == null) {
            return "";
        }
        String v = form.getFirst(key);
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        for (String k : form.keySet()) {
            if (k != null && k.equalsIgnoreCase(key)) {
                String x = form.getFirst(k);
                return x != null ? x.trim() : "";
            }
        }
        return "";
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || key == null) {
            return "";
        }
        Object v = body.get(key);
        return v != null ? v.toString().trim() : "";
    }

    private static String jsStr(String s) {
        if (s == null) {
            return "''";
        }
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
