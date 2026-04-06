package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.entity.MerchantDefaultProduct;
import com.pg.entity.OrgBranding;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantDefaultProductRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayDirectCreditRecordService;
import com.pg.service.ChillPayService;
import com.pg.service.OrgServiceUseService;
import com.pg.service.PaymentCurrencyScaleService;
import com.pg.service.UrlPayCardCopyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final PaymentCurrencyScaleService paymentCurrencyScaleService;
    private final UrlPayCardCopyService urlPayCardCopyService;
    private final OrgBrandingRepository orgBrandingRepository;

    public ApiPayController(ChillPayService chillPayService,
                            ChillPayDirectCreditRecordService chillPayDirectCreditRecordService,
                            OrgUnitRepository orgUnitRepository,
                            MerchantDefaultProductRepository merchantDefaultProductRepository,
                            MerchantProfileRepository merchantProfileRepository,
                            OrgServiceUseService orgServiceUseService,
                            PaymentCurrencyScaleService paymentCurrencyScaleService,
                            UrlPayCardCopyService urlPayCardCopyService,
                            OrgBrandingRepository orgBrandingRepository) {
        this.chillPayService = chillPayService;
        this.chillPayDirectCreditRecordService = chillPayDirectCreditRecordService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.paymentCurrencyScaleService = paymentCurrencyScaleService;
        this.urlPayCardCopyService = urlPayCardCopyService;
        this.orgBrandingRepository = orgBrandingRepository;
    }

    private Long resolveMerchantOrgUnitId(Long merchantId, String compId) {
        if (merchantId != null) return merchantId;
        if (compId != null && !compId.isEmpty()) {
            return orgUnitRepository.findByCode(compId.trim()).map(o -> o.getId()).orElse(null);
        }
        return null;
    }

    /**
     * ChillPay 결제 페이지용 설정 (CCD·DirectCredit·리다이렉트 URL 등).
     * 가맹점 운영 ChillPay 바인딩의 {@code pg_cd}와 동일한 PG사 API 연동({@code tb_pg_agency}) 행을 따름.
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
        try {
            return ResponseEntity.ok(ApiResponse.ok(chillPayService.getConfigForFrontend(orgUnitId)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CHILLPAY_ROUTE_NOT_CONFIGURED"));
        }
    }

    /**
     * 공개 URL 결제 페이지(pay.html)용: 가맹점 표시명·기본 상품·금액 등과,
     * 본사 {@code urlPayFlow}/{@code urlPayFormMode} 플래그 및 ChillPay 연동 URL(가맹점 결제대행사 {@code pg_cd}의 PG사 API 연동 행 기준).
     */
    @GetMapping("/chillpay/checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chillpayCheckoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
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
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("clientIp", getClientIp(request));
            data.putAll(chillPayService.getUrlPayPresentationForCheckout(orgUnitId));
            // 본사 URL결제 프레젠테이션 이후에 덮어씀 — 향후 맵에 동일 키가 생겨도 가맹점 표시·기본상품이 유지되도록
            data.put("compId", ou.get().getCode());
            data.put("merchantName", ou.get().getName());
            merchantProfileRepository.findByOrgUnitId(orgUnitId).ifPresent(mp -> {
                String bc = mp.getBaseCurrency();
                if (bc != null && !bc.isBlank()) {
                    String first = bc.split(",")[0].trim();
                    if (!first.isEmpty()) {
                        data.put("checkoutCurrencyCode", first);
                    }
                }
            });
            Optional<MerchantDefaultProduct> dp = merchantDefaultProductRepository.findByOrgUnitId(orgUnitId);
            if (dp.isPresent()) {
                MerchantDefaultProduct p = dp.get();
                if (p.getProductName() != null && !p.getProductName().isBlank()) {
                    data.put("defaultProductName", p.getProductName().trim());
                }
                if (p.getDefaultAmount() != null) {
                    long amt = p.getDefaultAmount().longValue();
                    data.put("defaultAmountYen", amt);
                    data.put("defaultCheckoutAmount", amt);
                }
            }
            String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
            Object checkoutCurObj = data.get("checkoutCurrencyCode");
            String checkoutCur = checkoutCurObj instanceof String ? (String) checkoutCurObj : null;
            String scaleMode = paymentCurrencyScaleService.resolveModeForUi(opPg,
                    checkoutCur != null && !checkoutCur.isBlank() ? checkoutCur : "");
            data.put("urlPayAmountScaleMode", scaleMode);
            urlPayCardCopyService.resolveActiveCopyByPg(opPg).ifPresent(copy -> data.put("urlPayCardCopy", copy));
            resolveCheckoutHeaderLogoUrl(orgUnitId).ifPresent(u -> data.put("checkoutHeaderLogoUrl", u));
            data.put("urlPayResultPageUrl", chillPayService.resolveUrlPayResultAbsolute(request, ou.get().getCode()));
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CHILLPAY_ROUTE_NOT_CONFIGURED"));
        }
    }

    /**
     * 공개 {@code pay-result.html}용: 활성 결제구문 행의 성공/실패 결과 문구(다국어 맵).
     * 비어 있으면 페이지 기본 문구를 씁니다.
     */
    @GetMapping("/chillpay/url-result-copy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> urlResultCopy(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId) {
        Map<String, Object> data = new LinkedHashMap<>();
        Long orgUnitId = resolveMerchantOrgUnitId(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        urlPayCardCopyService.resolveActiveCopyByPg(opPg).ifPresent(copy -> {
            putResultCopyField(data, copy, UrlPayCardCopyService.KEY_RESULT_SUCCESS_MAIN);
            putResultCopyField(data, copy, UrlPayCardCopyService.KEY_RESULT_SUCCESS_FOOT);
            putResultCopyField(data, copy, UrlPayCardCopyService.KEY_RESULT_FAIL_MAIN);
            putResultCopyField(data, copy, UrlPayCardCopyService.KEY_RESULT_FAIL_FOOT);
        });
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @SuppressWarnings("unchecked")
    private static void putResultCopyField(Map<String, Object> target, Map<String, Object> copy, String key) {
        Object v = copy.get(key);
        if (v instanceof Map && !((Map<?, ?>) v).isEmpty()) {
            target.put(key, v);
        }
    }

    /**
     * 가맹점 상위 체인에서 첫 {@link OrgLevel#MASTER_DIST} 조직의 로고 URL.
     */
    private Optional<String> resolveCheckoutHeaderLogoUrl(Long merchantOrgUnitId) {
        Long cur = merchantOrgUnitId;
        while (cur != null) {
            Optional<OrgUnit> opt = orgUnitRepository.findById(cur);
            if (opt.isEmpty()) {
                break;
            }
            OrgUnit u = opt.get();
            if (u.getOrgLevel() == OrgLevel.MASTER_DIST) {
                return orgBrandingRepository.findByOrgUnitId(u.getId())
                        .map(OrgBranding::getLogoImageUrl)
                        .filter(s -> s != null && !s.isBlank())
                        .map(String::trim);
            }
            cur = u.getParentId();
        }
        return Optional.empty();
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

        /* ziobiz/NOTI /admin/test-pay/submit 과 동일 토큰 키 변형 */
        String directCreditToken = firstNonBlankStr(body,
                "directCreditToken", "PaymentCreditToken", "paymentCreditToken", "paymentCredittoken");
        if (directCreditToken == null || directCreditToken.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "PaymentCreditToken(DirectCreditToken)이 필요합니다. CCD 인라인·MID·API Key를 확인하세요.",
                    "INVALID_TOKEN"));
        }

        BigDecimal displayAmount = parsePayAmount(body.get("amount"));
        if (displayAmount == null || displayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.ok(ApiResponse.fail("유효한 결제 금액을 입력하세요.", "INVALID_AMOUNT"));
        }

        String orderNo = normalizeChillPayOrderNo(str(body, "orderNo"));
        String custEmail = str(body, "custEmail");
        String fn = str(body, "firstName");
        String ln = str(body, "lastName");
        String payerName = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
        boolean urlPayCcdInline = Boolean.TRUE.equals(body.get("urlPayCcdInline"))
                || "true".equalsIgnoreCase(String.valueOf(body.get("urlPayCcdInline")));
        if (payerName.isEmpty() && !urlPayCcdInline) {
            return ResponseEntity.ok(ApiResponse.fail("결제자 성명(이름·성)을 입력하세요.", "INVALID_PAYER_NAME"));
        }

        String customerId = str(body, "customerId");
        if (customerId == null || customerId.isEmpty()) {
            customerId = (custEmail != null && !custEmail.isEmpty()) ? custEmail
                    : (!orderNo.isEmpty() ? orderNo : "guest");
        }
        String phoneNumber = str(body, "phoneNumber");
        String description = buildInlineDescription(body);

        String langCode = str(body, "langCode");
        String checkoutCurrencyCode = str(body, "currency");

        String ipAddress = getClientIp(request);

        if (merchantOrgUnitId != null && !orgServiceUseService.isOrgServiceActive(merchantOrgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "서비스가 중지된 업체입니다. (미사용 또는 상위 조직 미사용)", "ORG_DISABLED"));
        }

        String opPg = merchantOrgUnitId != null ? chillPayService.resolveUrlPayOperationalPgCd(merchantOrgUnitId) : "";
        BigDecimal pgAmount = paymentCurrencyScaleService.toPgAmount(displayAmount, opPg, checkoutCurrencyCode);
        if (pgAmount == null || pgAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.ok(ApiResponse.fail("유효한 결제 금액을 입력하세요.", "INVALID_AMOUNT"));
        }

        String browserReturnUrl = enrichPayResultReturnUrlWithOrderNo(
                chillPayService.resolveUrlPayResultAbsolute(request, str(body, "compId")), orderNo);
        try {
            ChillPayService.ChillPayDirectPaymentResult payResult = chillPayService.requestPayment(
                    orderNo, customerId, pgAmount, directCreditToken,
                    phoneNumber, description, ipAddress, custEmail,
                    merchantOrgUnitId, langCode, checkoutCurrencyCode,
                    browserReturnUrl
            );
            ChillPayDirectCreditResponse res = payResult.response();
            String urlPayMode = str(body, "urlPayIntegrationMode");
            if (urlPayMode == null || urlPayMode.isBlank()) {
                urlPayMode = "INLINE";
            }
            long recordAmt = pgAmount.setScale(0, RoundingMode.HALF_UP).longValue();
            chillPayDirectCreditRecordService.recordAfterDirectCreditResponse(
                    merchantOrgUnitId, res, recordAmt, orderNo, customerId, payResult.routeUsed(),
                    urlPayMode, payerName.isEmpty() ? null : payerName);
            return ResponseEntity.ok(ApiResponse.ok(res));
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "결제 요청 처리 중 오류가 발생했습니다.";
            String code = msg.startsWith("ChillPay 루트(Route)") ? "CHILLPAY_ROUTE_NOT_CONFIGURED" : "PAYMENT_ERROR";
            return ResponseEntity.ok(ApiResponse.fail(msg, code));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(
                    e.getMessage() != null ? e.getMessage() : "결제 요청 처리 중 오류가 발생했습니다.",
                    "PAYMENT_ERROR"
            ));
        }
    }

    /**
     * ChillPay ReturnUrl 에 주문번호를 붙여 복귀 시 {@code pay-result.html} 가 URL 만으로도 주문번호를 표시할 수 있게 합니다.
     * (거래번호는 승인 직전 응답에만 오는 경우가 많아 프론트 {@code sessionStorage} 로 보완합니다.)
     */
    private static String enrichPayResultReturnUrlWithOrderNo(String base, String orderNo) {
        if (base == null || base.isBlank()) {
            return base;
        }
        if (orderNo == null || orderNo.isBlank()) {
            return base;
        }
        try {
            String enc = URLEncoder.encode(orderNo.trim(), StandardCharsets.UTF_8);
            return base + (base.contains("?") ? "&" : "?") + "OrderNo=" + enc;
        } catch (Exception e) {
            return base;
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

    private static String firstNonBlankStr(Map<String, Object> body, String... keys) {
        for (String k : keys) {
            String v = str(body, k);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /**
     * ChillPay DirectCredit 매뉴얼·NOTI 테스트 페이지: OrderNo 최대 20자.
     * 초과·허용 외 문자는 제거 후 20자로 자름. 비었으면 {@code O}{@code System.currentTimeMillis()} (14자).
     */
    private static String normalizeChillPayOrderNo(String orderNo) {
        String s = orderNo != null ? orderNo.trim() : "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '-' || ch == '_') {
                sb.append(ch);
            }
        }
        String cleaned = sb.toString();
        if (cleaned.isEmpty()) {
            cleaned = "O" + System.currentTimeMillis();
        }
        return cleaned.length() <= 20 ? cleaned : cleaned.substring(0, 20);
    }

    private static BigDecimal parsePayAmount(Object amountObj) {
        if (amountObj == null) {
            return null;
        }
        if (amountObj instanceof BigDecimal bd) {
            return bd;
        }
        if (amountObj instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            String s = amountObj.toString().trim();
            if (s.isEmpty()) {
                return null;
            }
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
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
