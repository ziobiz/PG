package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.entity.MerchantDefaultProduct;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantDefaultProductRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayDirectCreditRecordService;
import com.pg.service.ChillPayService;
import com.pg.service.OrgServiceUseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 결제 API - ChillPay DirectCredit 연동.
 */
@RestController
@RequestMapping(value = "/api/pay", produces = "application/json")
public class ApiPayController {

    private final ChillPayService chillPayService;
    private final ChillPayDirectCreditRecordService chillPayDirectCreditRecordService;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantDefaultProductRepository merchantDefaultProductRepository;
    private final OrgServiceUseService orgServiceUseService;

    public ApiPayController(ChillPayService chillPayService,
                            ChillPayDirectCreditRecordService chillPayDirectCreditRecordService,
                            OrgUnitRepository orgUnitRepository,
                            MerchantDefaultProductRepository merchantDefaultProductRepository,
                            OrgServiceUseService orgServiceUseService) {
        this.chillPayService = chillPayService;
        this.chillPayDirectCreditRecordService = chillPayDirectCreditRecordService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.orgServiceUseService = orgServiceUseService;
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
        if (orgUnitId != null && !orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "서비스가 중지된 업체입니다. (미사용 또는 상위 조직 미사용)", "ORG_DISABLED"));
        }
        return ResponseEntity.ok(ApiResponse.ok(chillPayService.getConfigForFrontend(orgUnitId)));
    }

    /**
     * 공개 URL 결제 페이지(pay.html)용: 가맹점 표시명·기본 상품·금액(JPY 정수),
     * 본사 설정 기준 {@code urlPayFlow}(INLINE/REDIRECT), {@code urlPayFormMode}(FULL/SIMPLE), ChillPay URL 안내 필드.
     */
    @GetMapping("/chillpay/checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chillpayCheckoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId) {
        Long orgUnitId = resolveMerchantOrgUnitId(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "서비스가 중지된 업체입니다. (미사용 또는 상위 조직 미사용)", "ORG_DISABLED"));
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("compId", ou.get().getCode());
        data.put("merchantName", ou.get().getName());
        Optional<MerchantDefaultProduct> dp = merchantDefaultProductRepository.findByOrgUnitId(orgUnitId);
        if (dp.isPresent()) {
            MerchantDefaultProduct p = dp.get();
            if (p.getProductName() != null && !p.getProductName().isBlank()) {
                data.put("defaultProductName", p.getProductName().trim());
            }
            if (p.getDefaultAmount() != null) {
                data.put("defaultAmountYen", p.getDefaultAmount().longValue());
            }
        }
        data.putAll(chillPayService.getUrlPayPresentationForCheckout(orgUnitId));
        return ResponseEntity.ok(ApiResponse.ok(data));
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

        String orderNo = str(body, "orderNo");
        String custEmail = str(body, "custEmail");
        String customerId = str(body, "customerId");
        if (customerId == null || customerId.isEmpty()) {
            customerId = (custEmail != null && !custEmail.isEmpty()) ? custEmail : "guest";
        }
        String phoneNumber = str(body, "phoneNumber");
        String description = buildInlineDescription(body);

        String langCode = str(body, "langCode");

        String ipAddress = getClientIp(request);

        if (merchantOrgUnitId != null && !orgServiceUseService.isOrgServiceActive(merchantOrgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "서비스가 중지된 업체입니다. (미사용 또는 상위 조직 미사용)", "ORG_DISABLED"));
        }

        try {
            ChillPayDirectCreditResponse res = chillPayService.requestPayment(
                    orderNo, customerId, amount, directCreditToken,
                    phoneNumber, description, ipAddress, custEmail,
                    merchantOrgUnitId, langCode
            );
            int routeNo = chillPayService.resolveEffectiveRouteNo(merchantOrgUnitId);
            chillPayDirectCreditRecordService.recordAfterDirectCreditResponse(
                    merchantOrgUnitId, res, amount, orderNo, customerId, routeNo);
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

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) {
            return null;
        }
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * DirectCredit API 본문(Description)은 Table 1.3 필드만 서명에 포함.
     * 청구지·구매자 성명 등은 Description 끝에 구분자로 부가(매뉴얼 필드 외 메타).
     */
    private static String buildInlineDescription(Map<String, Object> body) {
        String item = str(body, "item");
        String desc = str(body, "description");
        String base = (item != null && !item.isEmpty()) ? item : (desc != null ? desc : "");
        String compId = str(body, "compId");
        String fn = str(body, "firstName");
        String ln = str(body, "lastName");
        String zip = str(body, "zipCode");
        String country = str(body, "country");
        String city = str(body, "city");
        String addr = str(body, "addressLine");
        StringBuilder meta = new StringBuilder();
        if (compId != null && !compId.isEmpty()) {
            meta.append("icopayCompId=").append(compId).append(";");
        }
        if (fn != null || ln != null) {
            meta.append("name=").append(fn != null ? fn : "").append(" ").append(ln != null ? ln : "").append(";");
        }
        if (zip != null) {
            meta.append("zip=").append(zip).append(";");
        }
        if (country != null) {
            meta.append("cty=").append(country).append(";");
        }
        if (city != null) {
            meta.append("city=").append(city).append(";");
        }
        if (addr != null) {
            meta.append("addr=").append(addr).append(";");
        }
        if (meta.length() == 0) {
            return base;
        }
        if (base.isEmpty()) {
            return meta.toString();
        }
        return base + " | " + meta;
    }
}
