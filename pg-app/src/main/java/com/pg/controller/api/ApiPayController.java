package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantDefaultProduct;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantDefaultProductRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.urlpay.CheckoutHeaderLogoResolver;
import com.pg.merchantdeploy.MerchantApiResponseMapper;
import com.pg.merchantdeploy.MerchantInlineCheckoutTokenService;
import com.pg.merchantdeploy.MerchantOperationalPgGuard;
import com.pg.merchantdeploy.MerchantOperationalPgGuardI18n;
import com.pg.merchantdeploy.MerchantPgBrokerVendor;
import com.pg.service.ChillPayDirectCreditRecordService;
import com.pg.service.ChillPayService;
import com.pg.service.EximbayPaymentService;
import com.pg.service.ElementPayPaymentService;
import com.pg.service.JpayPaymentService;
import com.pg.service.MerchantCreditTokenService;
import com.pg.service.MerchantPgBindingRouterService;
import com.pg.service.OrgServiceUseService;
import com.pg.service.PayCardPolicyService;
import com.pg.service.PayContactRememberPolicyService;
import com.pg.service.PayPresaleRiskFilterService;
import com.pg.service.PaymentCurrencyScaleService;
import com.pg.integration.pg.PgVendor;
import com.pg.util.PayPresaleRiskFilterCodes;
import com.pg.service.UrlPayCardCopyService;
import com.pg.service.UrlPayChargeResolutionService;
import com.pg.service.UrlPayCheckoutCurrencyService;
import com.pg.service.UrlPayDisplayFxService;
import com.pg.urlpay.UrlPayCardExpiryModeService;
import com.pg.urlpay.UrlPayCheckoutModeUtil;
import com.pg.urlpay.UrlPayPublicCheckoutService;
import com.pg.urlpay.UrlPaySaleDispatcher;
import com.pg.urlpay.UrlPayVendorCapability;
import com.pg.util.ChillPayDirectCreditUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 결제 API — ChillPay DirectCredit, JPAY {@code pay_index}, URL 결제 공통 플랫폼({@code /api/pay/url/*}) 등.
 * <p>본사 URL결제설정·다통화·다국어는 PG 무관({@link com.pg.urlpay})이며, PG별 차이는
 * {@link com.pg.urlpay.UrlPayVendorCapabilityRegistry} 에 등록합니다.
 */
@RestController
@RequestMapping(value = "/api/pay", produces = "application/json")
public class ApiPayController {

    private final ChillPayService chillPayService;
    private final JpayPaymentService jpayPaymentService;
    private final EximbayPaymentService eximbayPaymentService;
    private final ElementPayPaymentService elementPayPaymentService;
    private final ChillPayDirectCreditRecordService chillPayDirectCreditRecordService;
    private final OrgUnitRepository orgUnitRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantDefaultProductRepository merchantDefaultProductRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final PaymentCurrencyScaleService paymentCurrencyScaleService;
    private final UrlPayCardCopyService urlPayCardCopyService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;
    private final MerchantCreditTokenService merchantCreditTokenService;
    private final MerchantInlineCheckoutTokenService merchantInlineCheckoutTokenService;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final UrlPayPublicCheckoutService urlPayPublicCheckoutService;
    private final UrlPaySaleDispatcher urlPaySaleDispatcher;
    private final CheckoutHeaderLogoResolver checkoutHeaderLogoResolver;
    private final MerchantOperationalPgGuard operationalPgGuard;
    private final PayCardPolicyService payCardPolicyService;
    private final PayPresaleRiskFilterService payPresaleRiskFilterService;
    private final UrlPayCardExpiryModeService urlPayCardExpiryModeService;
    private final PayContactRememberPolicyService payContactRememberPolicyService;
    private final MerchantPgBindingRouterService pgBindingRouter;

    public ApiPayController(ChillPayService chillPayService,
                            JpayPaymentService jpayPaymentService,
                            EximbayPaymentService eximbayPaymentService,
                            ElementPayPaymentService elementPayPaymentService,
                            ChillPayDirectCreditRecordService chillPayDirectCreditRecordService,
                            OrgUnitRepository orgUnitRepository,
                            HqApiConfigRepository hqApiConfigRepository,
                            MerchantDefaultProductRepository merchantDefaultProductRepository,
                            MerchantProfileRepository merchantProfileRepository,
                            OrgServiceUseService orgServiceUseService,
                            PaymentCurrencyScaleService paymentCurrencyScaleService,
                            UrlPayCardCopyService urlPayCardCopyService,
                            UrlPayDisplayFxService urlPayDisplayFxService,
                            UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService,
                            MerchantCreditTokenService merchantCreditTokenService,
                            MerchantInlineCheckoutTokenService merchantInlineCheckoutTokenService,
                            UrlPayChargeResolutionService urlPayChargeResolutionService,
                            UrlPayPublicCheckoutService urlPayPublicCheckoutService,
                            UrlPaySaleDispatcher urlPaySaleDispatcher,
                            CheckoutHeaderLogoResolver checkoutHeaderLogoResolver,
                            MerchantOperationalPgGuard operationalPgGuard,
                            PayCardPolicyService payCardPolicyService,
                            PayPresaleRiskFilterService payPresaleRiskFilterService,
                            UrlPayCardExpiryModeService urlPayCardExpiryModeService,
                            PayContactRememberPolicyService payContactRememberPolicyService,
                            MerchantPgBindingRouterService pgBindingRouter) {
        this.chillPayService = chillPayService;
        this.jpayPaymentService = jpayPaymentService;
        this.eximbayPaymentService = eximbayPaymentService;
        this.elementPayPaymentService = elementPayPaymentService;
        this.chillPayDirectCreditRecordService = chillPayDirectCreditRecordService;
        this.orgUnitRepository = orgUnitRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.paymentCurrencyScaleService = paymentCurrencyScaleService;
        this.urlPayCardCopyService = urlPayCardCopyService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
        this.merchantCreditTokenService = merchantCreditTokenService;
        this.merchantInlineCheckoutTokenService = merchantInlineCheckoutTokenService;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.urlPayPublicCheckoutService = urlPayPublicCheckoutService;
        this.urlPaySaleDispatcher = urlPaySaleDispatcher;
        this.checkoutHeaderLogoResolver = checkoutHeaderLogoResolver;
        this.operationalPgGuard = operationalPgGuard;
        this.payCardPolicyService = payCardPolicyService;
        this.payPresaleRiskFilterService = payPresaleRiskFilterService;
        this.urlPayCardExpiryModeService = urlPayCardExpiryModeService;
        this.payContactRememberPolicyService = payContactRememberPolicyService;
        this.pgBindingRouter = pgBindingRouter;
    }

    private <T> ResponseEntity<ApiResponse<T>> vendorMismatchIfAny(Long orgUnitId,
                                                                    String requestedVendorScope,
                                                                    boolean repayScope) {
        return vendorMismatchIfAny(orgUnitId, requestedVendorScope, repayScope, null, null);
    }

    private <T> ResponseEntity<ApiResponse<T>> vendorMismatchIfAnyFromBody(Long orgUnitId,
                                                                              String requestedVendorScope,
                                                                              boolean repayScope,
                                                                              Map<String, Object> body) {
        Map<String, Object> safe = body != null ? body : Map.of();
        return vendorMismatchIfAny(orgUnitId, requestedVendorScope, repayScope,
                firstNonBlankStr(safe, "cardBrand", "payCardBrand"),
                firstNonBlankStr(safe, "currency", "displayCurrency"));
    }

    private <T> ResponseEntity<ApiResponse<T>> vendorMismatchIfAny(Long orgUnitId,
                                                                    String requestedVendorScope,
                                                                    boolean repayScope,
                                                                    String cardBrand,
                                                                    String currency) {
        if (orgUnitId == null) {
            return null;
        }
        Optional<Map<String, Object>> deny = operationalPgGuard.denyIfUrlPayVendorMismatch(
                orgUnitId, requestedVendorScope, repayScope, cardBrand, currency);
        if (deny.isEmpty()) {
            return null;
        }
        Map<String, Object> d = deny.get();
        @SuppressWarnings("unchecked")
        Map<String, String> messages = (Map<String, String>) d.get("messages");
        return ResponseEntity.ok(ApiResponse.failI18n(
                d.get("message") != null ? d.get("message").toString() : "PG vendor mismatch",
                d.get("errorCode") != null ? d.get("errorCode").toString() : MerchantOperationalPgGuard.ERROR_CODE,
                d.get("messageKey") != null ? d.get("messageKey").toString()
                        : MerchantOperationalPgGuardI18n.KEY_PG_VENDOR_MISMATCH,
                messages));
    }

    private static boolean isUrlPayRepayVariant(String raw) {
        return UrlPayCheckoutModeUtil.isUrlPayRepayVariantParam(raw);
    }

    private String merchantUrlPayCheckoutMode(Long orgUnitId) {
        if (orgUnitId == null) {
            return UrlPayCheckoutModeUtil.STANDARD;
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> UrlPayCheckoutModeUtil.normalize(mp.getUrlPayCheckoutMode()))
                .orElse(UrlPayCheckoutModeUtil.STANDARD);
    }

    private boolean resolveEffectiveUrlPayRepay(String urlPayVariantParam, Long orgUnitId) {
        return UrlPayCheckoutModeUtil.resolveEffectiveRepay(urlPayVariantParam, merchantUrlPayCheckoutMode(orgUnitId));
    }

    private Long resolveMerchantOrgUnitId(Long merchantId, String compId) {
        if (merchantId != null) return merchantId;
        if (compId != null && !compId.isEmpty()) {
            return orgUnitRepository.findByCode(compId.trim()).map(o -> o.getId()).orElse(null);
        }
        return null;
    }

    /**
     * PG 무관 URL 공개 결제 checkout-context (통합·ChillPay·JPAY 하위 호환 공통 구현).
     */
    private ResponseEntity<ApiResponse<Map<String, Object>>> urlPayCheckoutContextInternal(
            Long orgUnitId,
            boolean repay,
            HttpServletRequest request) {
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        Optional<MerchantProfile> profCtx = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (profCtx.isPresent() && !orgServiceUseService.isWebPaymentActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    OrgServiceUseService.MSG_WEB_PAYMENT_DISABLED, "WEB_PAYMENT_DISABLED"));
        }
        if (repay) {
            if (!chillPayService.isUrlPayRepayEnabledAtHq()) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "본사 설정에서 URL 재결제 기능이 꺼져 있습니다.", "URL_PAY_REPAY_DISABLED"));
            }
            if (chillPayService.findOperationalWebBindingForUrlPayRepay(orgUnitId).isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "URL 재결제를 처리할 결제대행사(운영·연동용도 URL재결제)가 없습니다.", "URL_PAY_REPAY_PG_MISSING"));
            }
        } else if (chillPayService.findOperationalWebBindingForUrlPay(orgUnitId).isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "URL 결제를 처리할 결제대행사(운영·연동용도 URL결제)가 없습니다.", "URL_PAYMENT_PG_MISSING"));
        }
        try {
            Map<String, Object> data = urlPayPublicCheckoutService.buildCheckoutContext(orgUnitId, request, repay);
            data.put("clientIp", getClientIp(request));
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "URL_PAY_ROUTE_NOT_CONFIGURED"));
        }
    }

    /**
     * 결제창 카드번호 사전 검증 — 비활성카드(마스킹)·실패 쿨다운·BIN 등 JPAY 호출 전 차단.
     */
    @PostMapping("/url/card-policy-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> urlPayCardPolicyCheck(
            @RequestBody Map<String, Object> body) {
        return cardPolicyCheckInternal(body);
    }

    /** {@link #urlPayCardPolicyCheck} 하위 호환 */
    @PostMapping("/jpay/card-policy-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> jpayCardPolicyCheck(
            @RequestBody Map<String, Object> body) {
        return cardPolicyCheckInternal(body);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> cardPolicyCheckInternal(Map<String, Object> body) {
        Map<String, Object> safe = body != null ? body : Map.of();
        Long merchantIdVal = null;
        Object mid = safe.get("merchantId");
        if (mid != null && !mid.toString().isEmpty()) {
            try {
                merchantIdVal = Long.parseLong(mid.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        Long orgUnitId = resolveMerchantOrgUnitId(merchantIdVal, str(safe, "compId"));
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        String brand = firstNonBlankStr(safe, "cardBrand", "payCardBrand");
        String currency = firstNonBlankStr(safe, "currency", "displayCurrency");
        String pg = pgBindingRouter.resolveOperationalPgCd(
                orgUnitId, MerchantPgBindingRouterService.RoutingHint.standard(brand, currency));
        if (pg == null || pg.isBlank()) {
            pg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        }
        String pan = firstNonBlankStr(safe, "pan", "payCardno", "cardno");
        String lang = firstNonBlankStr(safe, "lang", "language");
        String holderName = firstNonBlankStr(safe, "holderName", "customerNm");
        if (holderName == null || holderName.isBlank()) {
            holderName = joinPayerName(firstNonBlankStr(safe, "payFirstname", "firstname"),
                    firstNonBlankStr(safe, "payLastname", "lastname"));
        }
        Map<String, Object> result = payCardPolicyService.validateForSale(pg, pan, brand, lang, orgUnitId, holderName);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * URL 결제 공통 checkout-context — 운영 PG에 맞는 {@code urlPayCapabilities} 포함.
     */
    @GetMapping("/url/checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> urlPayCheckoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            @RequestParam(required = false, name = "urlPayVariant") String urlPayVariant,
            HttpServletRequest request) {
        Long orgUnitId = resolveMerchantOrgUnitId(merchantId, compId);
        boolean repay = resolveEffectiveUrlPayRepay(urlPayVariant, orgUnitId);
        return urlPayCheckoutContextInternal(orgUnitId, repay, request);
    }

    /** {@link #urlPayCheckoutContext} 와 동일 — 표시통화 견적(PG 무관). */
    @GetMapping("/url/display-fx-quote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> urlPayDisplayFxQuote(
            @RequestParam String compId,
            @RequestParam(name = "displayCurrency", required = false) String displayCurrency) {
        return chillpayDisplayFxQuote(compId, displayCurrency);
    }

    @PostMapping("/url/resolve-route")
    public ResponseEntity<ApiResponse<Map<String, Object>>> urlPayResolveRoute(
            @RequestBody Map<String, Object> body) {
        Map<String, Object> safe = body != null ? body : Map.of();
        Long merchantIdVal = null;
        Object mid = safe.get("merchantId");
        if (mid != null && !mid.toString().isEmpty()) {
            try {
                merchantIdVal = Long.parseLong(mid.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        Long orgUnitId = resolveMerchantOrgUnitId(merchantIdVal, str(safe, "compId"));
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        String cardBrand = firstNonBlankStr(safe, "cardBrand", "payCardBrand");
        String currency = firstNonBlankStr(safe, "currency", "displayCurrency");
        boolean repay = "Y".equalsIgnoreCase(str(safe, "repay"))
                || UrlPayCheckoutModeUtil.REPAY.equalsIgnoreCase(str(safe, "urlPayVariant"));
        MerchantPgBindingRouterService.RoutingHint hint = repay
                ? MerchantPgBindingRouterService.RoutingHint.repay(cardBrand, currency)
                : MerchantPgBindingRouterService.RoutingHint.standard(cardBrand, currency);
        var binding = pgBindingRouter.resolveOperationalBinding(orgUnitId, hint);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("multiPgRoutingEnabled", pgBindingRouter.isMultiPgRoutingEnabled());
        out.put("multiPgRoutingMode", pgBindingRouter.resolveMultiPgRoutingMode());
        if (binding.isEmpty()) {
            out.put("resolved", false);
            out.put("errorCode", pgBindingRouter.isMultiPgRoutingEnabled()
                    ? "MULTI_PG_ROUTE_NOT_CONFIGURED" : "URL_PAYMENT_PG_MISSING");
            return ResponseEntity.ok(ApiResponse.ok(out));
        }
        String opPg = binding.get().getPgCd() != null ? binding.get().getPgCd().trim() : "";
        UrlPayVendorCapability cap = urlPaySaleDispatcher.resolveCapability(orgUnitId, cardBrand, currency);
        out.put("resolved", true);
        out.put("urlPayOperationalPgCd", opPg);
        out.put("cardBrandScope", binding.get().getCardBrandScope());
        out.put("currencyScope", binding.get().getCurrencyScope());
        out.put("urlPayCapabilities", cap.toMap());
        out.put("urlPayCheckoutPagePath", cap.checkoutPagePath());
        out.put("urlPayInlineWidgetKind", cap.inlineWidgetKind());
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    /**
     * URL 결제 공통 승인 — 운영 PG {@link com.pg.urlpay.UrlPaySaleChannel} 로 라우팅.
     */
    @PostMapping("/url/sale")
    public ResponseEntity<ApiResponse<Map<String, Object>>> urlPaySale(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long merchantIdVal = null;
        Object mid = body.get("merchantId");
        if (mid != null && !mid.toString().isEmpty()) {
            try {
                merchantIdVal = Long.parseLong(mid.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        Long orgUnitId = resolveMerchantOrgUnitId(merchantIdVal, str(body, "compId"));
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        Map<String, Object> result = urlPaySaleDispatcher.executeSale(orgUnitId, body, request, getClientIp(request));
        Object ok = result.get("success");
        if (ok instanceof Boolean && !(Boolean) ok) {
            return ResponseEntity.ok(MerchantApiResponseMapper.failFromResultMap(
                    result, "URL 결제 요청 실패", "URL_PAY_SALE_FAILED"));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * JPAY {@code pay_index} 서버 사이드 호출(샌드박스·운영). 본문은 {@link JpayPaymentService#executeDirectSale} 필드 규약을 따릅니다.
     * 공개 엔드포인트 — {@code compId} 또는 {@code merchantId}(org_unit.id)로 가맹점을 식별합니다.
     */
    @PostMapping("/jpay/sale")
    public ResponseEntity<ApiResponse<Map<String, Object>>> jpaySale(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long merchantIdVal = null;
        Object mid = body.get("merchantId");
        if (mid != null && !mid.toString().isEmpty()) {
            try {
                merchantIdVal = Long.parseLong(mid.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        Long orgUnitId = resolveMerchantOrgUnitId(merchantIdVal, str(body, "compId"));
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다. compId 또는 merchantId를 넣으세요.", "NOT_FOUND"));
        }
        if (body.containsKey("subscriptionPlan") || body.containsKey("subscription_plan")
                || "Subscription".equalsIgnoreCase(str(body, "pay_type"))
                || "SUBSCRIPTION".equalsIgnoreCase(str(body, "checkoutKind"))) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "JPAY 구독은 /api/pay/jpay/subscribe 또는 subscription/prepare API를 사용하세요.", "SUBSCRIPTION_USE_DEDICATED_API"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        ResponseEntity<ApiResponse<Map<String, Object>>> vendorBlock = vendorMismatchIfAnyFromBody(
                orgUnitId, MerchantPgBrokerVendor.JPAY, false, body);
        if (vendorBlock != null) {
            return vendorBlock;
        }
        Map<String, Object> result = urlPaySaleDispatcher.executeSale(orgUnitId, body, request, getClientIp(request));
        Object ok = result.get("success");
        if (ok instanceof Boolean && !(Boolean) ok) {
            return ResponseEntity.ok(MerchantApiResponseMapper.failFromResultMap(
                    result, "JPAY 요청 실패", "JPAY_ERROR"));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** JPAY 결제 페이지 — {@link #urlPayCheckoutContext} 와 동일(하위 호환 경로). */
    @GetMapping("/jpay/checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> jpayCheckoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
        return urlPayCheckoutContext(merchantId, compId, null, request);
    }

    /**
     * JPAY 구독 인라인 페이지({@code jpay-subscribe.html})용 컨텍스트.
     */
    @GetMapping("/jpay/subscribe-checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> jpaySubscribeCheckoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
        Long orgUnitId = resolveMerchantOrgUnitId(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        if (!jpayPaymentService.hasOperationalSubscriptionBinding(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail("JPAY API 구독(운영) 바인딩이 없습니다.", "SUBSCRIPTION_PG_MISSING"));
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", ou.get().getCode());
        data.put("merchantName", ou.get().getName());
        data.put("pgVendor", "JPAY");
        data.put("checkoutKind", "SUBSCRIPTION");
        data.put("integrationMode", "INLINE");
        data.put("checkoutCurrencyCode", urlPayCheckoutCurrencyService.resolveCheckoutCurrency(orgUnitId, null));
        data.put("clientIp", getClientIp(request));
        checkoutHeaderLogoResolver.applyToCheckoutMap(data, orgUnitId);
        urlPayCardExpiryModeService.putEffectiveIntoMap(data, orgUnitId);
        data.put("checkoutContactRememberEnabled", payContactRememberPolicyService.isEnabledForOrgUnit(orgUnitId));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * JPAY 구독 Sale — 세션 토큰({@code checkoutKind=SUBSCRIPTION}) 필수.
     */
    @PostMapping("/jpay/subscribe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> jpaySubscribe(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String sessionToken = str(body, "sessionToken");
        if (sessionToken.isBlank()) {
            sessionToken = str(body, "session");
        }
        if (sessionToken.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("sessionToken(세션)이 필요합니다.", "INVALID_SESSION"));
        }
        var parsed = merchantInlineCheckoutTokenService.parseValid(
                sessionToken, MerchantPgBrokerVendor.JPAY,
                MerchantInlineCheckoutTokenService.CHECKOUT_SUBSCRIPTION);
        if (parsed.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("세션이 유효하지 않거나 만료되었습니다.", "INVALID_SESSION"));
        }
        var session = parsed.get();
        Long orgUnitId = resolveMerchantOrgUnitId(null, session.compId());
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        String orderNo = str(body, "orderNo");
        if (orderNo.isBlank()) {
            orderNo = session.orderNo();
        }
        if (!orderNo.equals(session.orderNo())) {
            return ResponseEntity.ok(ApiResponse.fail("세션 orderNo와 요청 orderNo가 일치하지 않습니다.", "SESSION_MISMATCH"));
        }
        String planJson = session.subscriptionPlanJson();
        if (planJson == null || planJson.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("구독 plan 정보가 없습니다.", "SUBSCRIPTION_PLAN_REQUIRED"));
        }
        body.put("compId", session.compId());
        body.put("orderNo", orderNo);
        body.put("txnOrigin", "SUBSCRIPTION");
        if (body.get("amount") == null || str(body, "amount").isBlank()) {
            body.put("amount", session.amountPlain());
        }
        if (body.get("currency") == null || str(body, "currency").isBlank()) {
            body.put("currency", session.currency());
        }
        Map<String, Object> result = jpayPaymentService.executeSubscriptionSale(
                orgUnitId, body, request, getClientIp(request), planJson);
        Object ok = result.get("success");
        if (ok instanceof Boolean && !(Boolean) ok) {
            String msg = result.get("message") != null ? result.get("message").toString() : "JPAY 구독 요청 실패";
            String code = result.get("errorCode") != null ? result.get("errorCode").toString().trim() : "JPAY_ERROR";
            return ResponseEntity.ok(ApiResponse.fail(msg, code.isEmpty() ? "JPAY_ERROR" : code));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** Eximbay 결제 페이지({@code eximbay-pay.html}) — {@link #urlPayCheckoutContext} 와 동일(하위 호환 경로). */
    @GetMapping("/eximbay/checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eximbayCheckoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
        return urlPayCheckoutContext(merchantId, compId, null, request);
    }

    /** ElementPay 결제 페이지 — {@link #urlPayCheckoutContext} 와 동일. */
    @GetMapping("/elementpay/checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> elementpayCheckoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
        return urlPayCheckoutContext(merchantId, compId, null, request);
    }

    /**
     * ElementPay INLINE 결제 결과 폴링 — getStatus 요약({@code PAID}/{@code FAILED}/{@code PENDING}).
     */
    @GetMapping("/url/elementpay-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> elementpayInlineStatus(
            @RequestParam(required = false) String compId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String paymentId) {
        Long orgUnitId = resolveMerchantOrgUnitId(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!elementPayPaymentService.hasOperationalWebBinding(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail("ElementPay 운영 바인딩이 없습니다.", "ELEMENTPAY_PG_MISSING"));
        }
        Map<String, Object> res = elementPayPaymentService.queryInlineStatus(orgUnitId, paymentId, orderNo);
        if (!Boolean.TRUE.equals(res.get("success"))) {
            return ResponseEntity.ok(ApiResponse.fail(
                    res.get("message") != null ? res.get("message").toString() : "status failed",
                    res.get("errorCode") != null ? res.get("errorCode").toString() : "ELEMENTPAY_STATUS_FAILED"));
        }
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    /**
     * Eximbay 결과(SDK/return_url) 쿼리스트링 위·변조 검증 — {@code /v1/payments/verify} 위임.
     * 프론트가 결과 표시 전 무결성을 확인하는 보조 엔드포인트(서버 측 확정은 status_url 웹훅이 담당).
     */
    @PostMapping("/eximbay/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eximbayVerify(
            @RequestBody Map<String, Object> body) {
        String data = str(body, "data");
        if (data.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("검증할 결과 데이터(data)가 없습니다.", "INVALID_DATA"));
        }
        String mid = str(body, "mid");
        Map<String, Object> res = eximbayPaymentService.verify(mid, data);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    /**
     * Eximbay 구독 인라인 페이지({@code eximbay-subscribe.html})용 컨텍스트.
     */
    @GetMapping("/eximbay/subscribe-checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eximbaySubscribeCheckoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            HttpServletRequest request) {
        Long orgUnitId = resolveMerchantOrgUnitId(merchantId, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        if (!eximbayPaymentService.hasOperationalWebBinding(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail("Eximbay 운영(WEB) 바인딩이 없습니다.", "SUBSCRIPTION_PG_MISSING"));
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", ou.get().getCode());
        data.put("merchantName", ou.get().getName());
        data.put("pgVendor", "EXIMBAY");
        data.put("checkoutKind", "SUBSCRIPTION");
        data.put("integrationMode", "INLINE");
        data.put("checkoutCurrencyCode", urlPayCheckoutCurrencyService.resolveCheckoutCurrency(orgUnitId, null));
        data.put("clientIp", getClientIp(request));
        checkoutHeaderLogoResolver.applyToCheckoutMap(data, orgUnitId);
        urlPayCardExpiryModeService.putEffectiveIntoMap(data, orgUnitId);
        data.put("checkoutContactRememberEnabled", payContactRememberPolicyService.isEnabledForOrgUnit(orgUnitId));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * Eximbay 구독(정기결제) 등록 — {@code tokenbilling(token_creation)} + {@code recurring}.
     * 최초 결제창에서 토큰을 만들고 1차 결제를 수행하며, 이후 회차는 Eximbay 가 자동 청구합니다.
     */
    @PostMapping("/eximbay/subscribe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eximbaySubscribe(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long merchantIdVal = null;
        Object mid = body.get("merchantId");
        if (mid != null && !mid.toString().isEmpty()) {
            try {
                merchantIdVal = Long.parseLong(mid.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        Long orgUnitId = resolveMerchantOrgUnitId(merchantIdVal, str(body, "compId"));
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        if (!eximbayPaymentService.hasOperationalWebBinding(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail("Eximbay 운영(WEB) 바인딩이 없습니다.", "SUBSCRIPTION_PG_MISSING"));
        }
        Map<String, Object> plan = extractSubscriptionPlan(body);
        body.put("txnOrigin", "SUBSCRIPTION");
        Map<String, Object> result = eximbayPaymentService.executeSubscriptionReady(
                orgUnitId, body, request, getClientIp(request), plan);
        Object ok = result.get("success");
        if (ok instanceof Boolean && !(Boolean) ok) {
            return ResponseEntity.ok(MerchantApiResponseMapper.failFromResultMap(
                    result, "Eximbay 구독 요청 실패", "EXIMBAY_ERROR"));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractSubscriptionPlan(Map<String, Object> body) {
        Object raw = body.get("subscriptionPlan");
        if (raw == null) {
            raw = body.get("subscription_plan");
        }
        if (raw instanceof Map) {
            return (Map<String, Object>) raw;
        }
        Map<String, Object> plan = new LinkedHashMap<>();
        for (String k : new String[]{"recurring_amount", "recurring_start_date", "recurring_interval",
                "remind_email_interval"}) {
            Object v = body.get(k);
            if (v != null && !v.toString().isBlank()) {
                plan.put(k, v.toString().trim());
            }
        }
        return plan;
    }

    /**
     * ChillPay 결제 페이지용 설정 (CCD·DirectCredit·리다이렉트 URL 등).
     * 가맹점 운영 ChillPay 바인딩의 {@code pg_cd}와 동일한 PG사 API 연동({@code tb_pg_agency}) 행을 따름.
     */
    @GetMapping("/chillpay/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chillpayConfig(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            @RequestParam(required = false, name = "urlPayVariant") String urlPayVariant) {
        Long orgUnitId = resolveMerchantOrgUnitId(merchantId, compId);
        boolean repay = resolveEffectiveUrlPayRepay(urlPayVariant, orgUnitId);
        if (orgUnitId != null && !orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        if (orgUnitId != null) {
            ResponseEntity<ApiResponse<Map<String, Object>>> vendorBlock = vendorMismatchIfAny(
                    orgUnitId, MerchantPgBrokerVendor.CHILLPAY, repay);
            if (vendorBlock != null) {
                return vendorBlock;
            }
            Optional<MerchantProfile> profCfg = merchantProfileRepository.findByOrgUnitId(orgUnitId);
            if (profCfg.isPresent()) {
                String wpy = profCfg.get().getWebPaymentUseYn();
                if (wpy != null && "N".equalsIgnoreCase(wpy.trim())) {
                    return ResponseEntity.ok(ApiResponse.fail(
                            OrgServiceUseService.MSG_WEB_PAYMENT_DISABLED, "WEB_PAYMENT_DISABLED"));
                }
            }
            if (repay) {
                if (!chillPayService.isUrlPayRepayEnabledAtHq()) {
                    return ResponseEntity.ok(ApiResponse.fail(
                            "본사 설정에서 URL 재결제 기능이 꺼져 있습니다.", "URL_PAY_REPAY_DISABLED"));
                }
                if (chillPayService.findOperationalWebBindingForUrlPayRepay(orgUnitId).isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.fail(
                            "URL 재결제를 처리할 결제대행사(운영·연동용도 URL재결제)가 없습니다.", "URL_PAY_REPAY_PG_MISSING"));
                }
            } else if (chillPayService.findOperationalWebBindingForUrlPay(orgUnitId).isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "URL 결제를 처리할 결제대행사(운영·연동용도 URL결제)가 없습니다.", "URL_PAYMENT_PG_MISSING"));
            }
        }
        try {
            Map<String, Object> cfg = repay
                    ? chillPayService.getConfigForFrontendUrlPayRepay(orgUnitId)
                    : chillPayService.getConfigForFrontend(orgUnitId);
            return ResponseEntity.ok(ApiResponse.ok(cfg));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "CHILLPAY_ROUTE_NOT_CONFIGURED"));
        }
    }

    /** ChillPay 결제 페이지 — {@link #urlPayCheckoutContext} 와 동일(하위 호환 경로). */
    @GetMapping("/chillpay/checkout-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chillpayCheckoutContext(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String compId,
            @RequestParam(required = false, name = "urlPayVariant") String urlPayVariant,
            HttpServletRequest request) {
        return urlPayCheckoutContext(merchantId, compId, urlPayVariant, request);
    }

    /**
     * URL 결제 표시통화(JPY·USD·KRW·THB 등)→실결제 통화 견적(BOT 일평균 또는 1:1 + 본사 마진). 서명 토큰은 결제 요청 시 재검증됩니다.
     */
    @GetMapping("/chillpay/display-fx-quote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chillpayDisplayFxQuote(
            @RequestParam String compId,
            @RequestParam(name = "displayCurrency", required = false) String displayCurrency) {
        Long orgUnitId = resolveMerchantOrgUnitId(null, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        ResponseEntity<ApiResponse<Map<String, Object>>> vendorBlock = vendorMismatchIfAny(
                orgUnitId, MerchantPgBrokerVendor.CHILLPAY, false);
        if (vendorBlock != null) {
            return vendorBlock;
        }
        if (!UrlPayDisplayFxService.MODE_DISPLAY_FX_THB.equals(chillPayService.resolveUrlPayPricingMode(orgUnitId))) {
            return ResponseEntity.ok(ApiResponse.fail("이 가맹점 URL 결제는 표시통화(THB정산) 모드가 아닙니다.", "DISPLAY_FX_NOT_CONFIGURED"));
        }
        if (!urlPayDisplayFxService.isHqFeatureEnabled()) {
            return ResponseEntity.ok(ApiResponse.fail("본사 「URL 표시통화(THB정산)」 설정이 꺼져 있거나 비어 있습니다.", "DISPLAY_FX_HQ_DISABLED"));
        }
        String cur = displayCurrency != null && !displayCurrency.isBlank() ? displayCurrency.trim() : "JPY";
        String opPgQ = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        Optional<UrlPayDisplayFxService.QuoteResult> q = urlPayDisplayFxService.buildQuote(compId.trim(), cur, opPgQ);
        if (q.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "환율 견적을 만들 수 없습니다. 본사설정 「URL결제설정」에서 해당 PG의 FX(자동: BOT API 키, 수동: THB/표시단위)와 DISPLAY 설정을 확인하세요.",
                    "DISPLAY_FX_RATE_UNAVAILABLE"));
        }
        UrlPayDisplayFxService.QuoteResult r = q.get();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("displayCurrency", r.displayCurrency());
        out.put("settlementCurrency", r.settlementCurrency());
        out.put("settlementPerUnit", r.settlementPerUnit().stripTrailingZeros().toPlainString());
        out.put("botPeriod", r.botPeriod());
        out.put("thbPerUnit", r.settlementPerUnit().stripTrailingZeros().toPlainString());
        out.put("marginRate", r.marginRate().stripTrailingZeros().toPlainString());
        out.put("expEpochSec", r.expEpochSec());
        out.put("fxQuoteToken", r.quoteToken());
        out.put("rateDescription", r.rateDescription());
        String set = r.settlementCurrency();
        String scaleNote = ("JPY".equals(set) || "KRW".equals(set)) ? "정수 반올림" : "소수 둘째";
        out.put("formulaNote", "청구 " + set + "(" + scaleNote + ") = 표시금액 × settlementPerUnit × (1+margin). 자동은 BOT 일평균을 THB 경유로 환산합니다.");
        return ResponseEntity.ok(ApiResponse.ok(out));
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
     * URL 재결제 — 저장된 CreditToken 목록(Card Select UI용 MerchantSecurityCheck 포함).
     */
    @GetMapping("/chillpay/saved-cards")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chillpaySavedCards(
            @RequestParam String compId,
            @RequestParam String customerId) {
        Long orgUnitId = resolveMerchantOrgUnitId(null, compId);
        if (orgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (!chillPayService.isUrlPayRepayEnabledAtHq()) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "본사 설정에서 URL 재결제 기능이 꺼져 있습니다.", "URL_PAY_REPAY_DISABLED"));
        }
        if (chillPayService.findOperationalWebBindingForUrlPayRepay(orgUnitId).isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "URL 재결제를 처리할 결제대행사(운영·연동용도 URL재결제)가 없습니다.", "URL_PAY_REPAY_PG_MISSING"));
        }
        String pgCd = chillPayService.resolveUrlPayRepayOperationalPgCd(orgUnitId);
        String cust = MerchantCreditTokenService.normalizeCustomerId(null, null, customerId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("compId", compId.trim());
        out.put("customerId", cust);
        out.put("pgCd", pgCd);
        out.put("cards", merchantCreditTokenService.listForCardSelect(orgUnitId, pgCd, cust));
        return ResponseEntity.ok(ApiResponse.ok(out));
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
        boolean repayVariant = resolveEffectiveUrlPayRepay(str(body, "urlPayVariant"), merchantOrgUnitId);

        /* ziobiz/NOTI /admin/test-pay/submit 과 동일 토큰 키 변형 */
        String directCreditToken = firstNonBlankStr(body,
                "directCreditToken", "PaymentCreditToken", "paymentCreditToken", "paymentCredittoken");
        if (directCreditToken == null || directCreditToken.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "PaymentCreditToken(DirectCreditToken)이 필요합니다. CCD 인라인·MID·API Key를 확인하세요.",
                    "INVALID_TOKEN"));
        }

        String orderNo = ChillPayDirectCreditUtil.normalizeOrderNo(str(body, "orderNo"));
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

        String ipAddress = getClientIp(request);

        if (merchantOrgUnitId != null && !orgServiceUseService.isOrgServiceActive(merchantOrgUnitId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED"));
        }
        if (merchantOrgUnitId != null) {
            ResponseEntity<ApiResponse<ChillPayDirectCreditResponse>> vendorBlock = vendorMismatchIfAny(
                    merchantOrgUnitId, MerchantPgBrokerVendor.CHILLPAY, repayVariant);
            if (vendorBlock != null) {
                return vendorBlock;
            }
        }

        String opPg = merchantOrgUnitId != null
                ? (repayVariant
                    ? chillPayService.resolveUrlPayRepayOperationalPgCd(merchantOrgUnitId)
                    : chillPayService.resolveUrlPayOperationalPgCd(merchantOrgUnitId))
                : "";
        if (repayVariant && merchantOrgUnitId != null) {
            if (!chillPayService.isUrlPayRepayEnabledAtHq()) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "본사 설정에서 URL 재결제 기능이 꺼져 있습니다.", "URL_PAY_REPAY_DISABLED"));
            }
            if (chillPayService.findOperationalWebBindingForUrlPayRepay(merchantOrgUnitId).isEmpty()) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "URL 재결제를 처리할 결제대행사(운영·연동용도 URL재결제)가 없습니다.", "URL_PAY_REPAY_PG_MISSING"));
            }
        }
        String checkoutCurrencyCode;
        BigDecimal pgAmount;
        BigDecimal shopperDisplayAmountOut = null;
        String shopperDisplayCurrencyOut = null;
        if (merchantOrgUnitId == null) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        try {
            UrlPayChargeResolutionService.ResolvedCharge charge =
                    urlPayChargeResolutionService.resolve(merchantOrgUnitId, body, opPg);
            checkoutCurrencyCode = charge.settlementCurrency();
            pgAmount = charge.pgAmount();
            shopperDisplayAmountOut = charge.shopperDisplayAmount();
            shopperDisplayCurrencyOut = charge.shopperDisplayCurrency();
        } catch (IllegalArgumentException ex) {
            String code = ex.getMessage() != null ? ex.getMessage() : "INVALID_AMOUNT";
            return ResponseEntity.ok(ApiResponse.fail(
                    UrlPayChargeResolutionService.failMessageForCode(code), code));
        }
        if (pgAmount == null || pgAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.ok(ApiResponse.fail("유효한 결제 금액을 입력하세요.", "INVALID_AMOUNT"));
        }

        Optional<OrgUnit> ouPresale = orgUnitRepository.findById(merchantOrgUnitId);
        String merchantCode = ouPresale.map(OrgUnit::getCode).orElse("");
        Map<String, Object> presaleBody = new LinkedHashMap<>(body != null ? body : Map.of());
        presaleBody.put("_payerClientIp", ipAddress);
        if (fn != null) presaleBody.put("firstName", fn);
        if (ln != null) presaleBody.put("lastName", ln);
        if (custEmail != null) presaleBody.put("custEmail", custEmail);
        if (phoneNumber != null) presaleBody.put("phoneNumber", phoneNumber);
        if (langCode != null) presaleBody.put("langCode", langCode);
        Optional<PayPresaleRiskFilterService.PresaleRiskBlock> presaleRisk =
                payPresaleRiskFilterService.evaluate(merchantOrgUnitId, merchantCode, PgVendor.CHILLPAY, presaleBody);
        if (presaleRisk.isPresent()) {
            PayPresaleRiskFilterService.PresaleRiskBlock block = presaleRisk.get();
            String txnOriginPresale = normalizeTxnOrigin(str(body, "txnOrigin"));
            chillPayDirectCreditRecordService.recordPresalePending(
                    merchantOrgUnitId, orderNo, pgAmount, checkoutCurrencyCode,
                    custEmail, phoneNumber, payerName, txnOriginPresale,
                    shopperDisplayAmountOut, shopperDisplayCurrencyOut, langCode);
            String trnId = chillPayDirectCreditRecordService.applyPresaleRiskCancel(
                    merchantCode, orderNo, txnOriginPresale, block);
            payPresaleRiskFilterService.recordEvent(
                    merchantOrgUnitId, merchantCode, orderNo, trnId, PgVendor.CHILLPAY, block);
            return ResponseEntity.ok(ApiResponse.failI18n(
                    block.message(), PayPresaleRiskFilterCodes.ERROR_CODE, block.filterCode(), block.messages()));
        }

        String browserReturnUrl = enrichPayResultReturnUrlWithOrderNo(
                chillPayService.resolveUrlPayResultAbsolute(request, str(body, "compId")), orderNo);
        String saveCard = str(body, "saveCard");
        if (saveCard == null) {
            saveCard = str(body, "rememberCard");
        }
        String creditToken = firstNonBlankStr(body, "creditToken", "CreditToken");
        String tokenType = str(body, "tokenType");
        if (tokenType == null && creditToken != null && !creditToken.isEmpty()) {
            tokenType = "CT";
        }
        ChillPayService.UrlPayBindingScope bindScope = repayVariant
                ? ChillPayService.UrlPayBindingScope.REPAY
                : ChillPayService.UrlPayBindingScope.STANDARD;
        try {
            ChillPayService.ChillPayDirectPaymentResult payResult = chillPayService.requestPayment(
                    orderNo, customerId, pgAmount, directCreditToken,
                    phoneNumber, description, ipAddress, custEmail,
                    merchantOrgUnitId, langCode, checkoutCurrencyCode,
                    browserReturnUrl, saveCard, creditToken, tokenType, bindScope
            );
            ChillPayDirectCreditResponse res = payResult.response();
            if (repayVariant && res != null && res.getData() != null
                    && "Paid".equalsIgnoreCase(String.valueOf(res.getData().getPaymentStatus()))) {
                if (creditToken != null && !creditToken.isBlank()) {
                    merchantCreditTokenService.markUsed(merchantOrgUnitId, opPg, customerId, creditToken);
                }
            }
            if (repayVariant && saveCard != null && "Y".equalsIgnoreCase(saveCard)
                    && creditToken != null && !creditToken.isBlank()) {
                merchantCreditTokenService.upsertToken(merchantOrgUnitId, opPg, customerId, creditToken, null, null);
            }
            String urlPayMode = str(body, "urlPayIntegrationMode");
            if (urlPayMode == null || urlPayMode.isBlank()) {
                urlPayMode = "INLINE";
            }
            long recordAmt = chillPayWireAmountLong(pgAmount, checkoutCurrencyCode);
            String txnOrigin = normalizeTxnOrigin(str(body, "txnOrigin"));
            chillPayDirectCreditRecordService.recordAfterDirectCreditResponse(
                    merchantOrgUnitId, res, recordAmt, orderNo, customerId, payResult.routeUsed(),
                    urlPayMode, payerName.isEmpty() ? null : payerName, checkoutCurrencyCode,
                    shopperDisplayAmountOut, shopperDisplayCurrencyOut, txnOrigin, langCode);
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

    /** URL 결제 기본 ORIGIN(URL). 챗봇·가맹 API 진입 시 프론트에서 CHATBOT/MERCHANT_API 전달 */
    private static String normalizeTxnOrigin(String raw) {
        if (raw == null || raw.isBlank()) {
            return "URL";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("CHATBOT".equals(u)) {
            return "CHATBOT";
        }
        if ("MERCHANT_API".equals(u)) {
            return "MERCHANT_API";
        }
        return "URL";
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

    private static String joinPayerName(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        if (f.isEmpty()) {
            return l;
        }
        if (l.isEmpty()) {
            return f;
        }
        return f + " " + l;
    }

    /** ChillPay 요청 본문 Amount 정수(392·410=주단위 정수, 그 외=주단위×100) — 적재용 보조 */
    private static long chillPayWireAmountLong(BigDecimal majorUnits, String checkoutCurrencyCode) {
        if (majorUnits == null) {
            return 0;
        }
        String num = ChillPayService.toChillPayCurrencyNumeric(checkoutCurrencyCode);
        if ("392".equals(num) || "410".equals(num)) {
            return majorUnits.setScale(0, RoundingMode.HALF_UP).longValue();
        }
        return majorUnits.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();
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
