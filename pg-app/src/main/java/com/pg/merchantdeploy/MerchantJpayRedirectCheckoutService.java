package com.pg.merchantdeploy;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.JpayPaymentService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.OrgServiceUseService;
import com.pg.service.UrlPayCheckoutCurrencyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹점 API — JPAY 리다이렉트 결제 페이지({@code jpay-pay.html}) 세션 준비.
 */
@Service
public class MerchantJpayRedirectCheckoutService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final JpayPaymentService jpayPaymentService;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantChatbotProductService productService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final MerchantJpayInlineCheckoutService inlineCheckoutService;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;
    private final MerchantApiIntegrationChannelService integrationChannelService;

    public MerchantJpayRedirectCheckoutService(OrgUnitRepository orgUnitRepository,
                                               MerchantProfileRepository merchantProfileRepository,
                                               OrgServiceUseService orgServiceUseService,
                                               JpayPaymentService jpayPaymentService,
                                               HqApiConfigRepository hqApiConfigRepository,
                                               MerchantChatbotProductService productService,
                                               MerchantInlineCheckoutTokenService tokenService,
                                               MerchantJpayInlineCheckoutService inlineCheckoutService,
                                               UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService,
                                               MerchantApiIntegrationChannelService integrationChannelService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.jpayPaymentService = jpayPaymentService;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.productService = productService;
        this.tokenService = tokenService;
        this.inlineCheckoutService = inlineCheckoutService;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
        this.integrationChannelService = integrationChannelService;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        if (orgUnitId == null) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (body != null && (body.containsKey("subscriptionPlan") || body.containsKey("subscription_plan")
                || "SUBSCRIPTION".equalsIgnoreCase(str(body.get("checkoutKind"))))) {
            return fail("JPAY 구독 가입은 /api/middleware/v1/merchant/jpay/subscription/prepare 를 사용하세요.", "SUBSCRIPTION_USE_DEDICATED_API");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        OrgUnit ou = ouOpt.get();
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return fail("서비스가 중지된 업체입니다.", "ORG_DISABLED");
        }
        Optional<MerchantProfile> profOpt = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (profOpt.isPresent()) {
            String wpy = profOpt.get().getWebPaymentUseYn();
            if (wpy != null && "N".equalsIgnoreCase(wpy.trim())) {
                return fail("이 가맹점은 웹결제(URL 결제)가 미사용으로 설정되어 있습니다.", "WEB_PAYMENT_DISABLED");
            }
        }
        if (!jpayPaymentService.hasOperationalWebBinding(orgUnitId)) {
            return fail("JPAY URL 결제(운영) 바인딩이 없습니다.", "URL_PAYMENT_PG_MISSING");
        }
        Optional<String> redirectDeny = integrationChannelService.denyMessage(orgUnitId,
                MerchantApiIntegrationChannelService.Channel.API_BROKER_REDIRECT);
        if (redirectDeny.isPresent()) {
            return fail(redirectDeny.get(), MerchantApiIntegrationChannelService.CODE_INTEGRATION_CHANNEL_DISABLED);
        }

        String returnUrl = str(body != null ? body.get("returnUrl") : null);
        if (returnUrl.isBlank()) {
            return fail("returnUrl(HTTPS)이 필요합니다.", "INVALID_RETURN_URL");
        }
        if (!isHttpsUrl(returnUrl)) {
            return fail("returnUrl은 HTTPS URL이어야 합니다.", "INVALID_RETURN_URL");
        }
        String cancelUrl = str(body != null ? body.get("cancelUrl") : null);
        if (!cancelUrl.isBlank() && !isHttpsUrl(cancelUrl)) {
            return fail("cancelUrl은 HTTPS URL이어야 합니다.", "INVALID_CANCEL_URL");
        }

        String orderNo = normalizeOrderNo(str(body.get("orderNo")));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("유효한 amount가 필요합니다.", "INVALID_AMOUNT");
        }
        String amountPlain = amount.stripTrailingZeros().toPlainString();
        String currency = urlPayCheckoutCurrencyService.resolveCheckoutCurrency(
                orgUnitId, str(body.get("currency")));
        String productName = clamp(str(body.get("productName")), 500);
        if (productName.isBlank()) {
            productName = clamp(str(body.get("item")), 500);
        }
        String langCode = MerchantCheckoutLangUtil.fromBody(body);

        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        String fieldMode = com.pg.urlpay.JpayCheckoutFieldModeUtil.resolve(
                profOpt.map(MerchantProfile::getJpayCheckoutFieldMode).orElse(null),
                hq != null ? hq.getJpayCheckoutFieldMode() : null);
        String buyerPrefillJson;
        try {
            buyerPrefillJson = com.pg.urlpay.IcipayBuyerContactUtil.resolvePrefillJsonFromBodyOptional(body);
            if (buyerPrefillJson == null) {
                buyerPrefillJson = com.pg.urlpay.JpayBuyerPrefillUtil.resolvePrefillJsonForPrepare(body, fieldMode);
            }
        } catch (IllegalArgumentException ex) {
            return fail(ex.getMessage(), "BUYER_PREFILL_INVALID");
        }

        String sessionToken = buyerPrefillJson != null && !buyerPrefillJson.isBlank()
                ? tokenService.issueWithBuyerPrefill(MerchantPgBrokerVendor.JPAY, ou.getCode(), orderNo,
                amountPlain, currency, productName, buyerPrefillJson)
                : tokenService.issue(MerchantPgBrokerVendor.JPAY, ou.getCode(), orderNo,
                amountPlain, currency, productName);
        Optional<MerchantInlineCheckoutTokenService.SessionPayload> parsed =
                tokenService.parseValid(sessionToken, MerchantPgBrokerVendor.JPAY);
        if (parsed.isEmpty()) {
            return fail("세션 토큰 생성에 실패했습니다.", "SESSION_ERROR");
        }
        MerchantInlineCheckoutTokenService.SessionPayload session = parsed.get();

        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        String payPath = buildJpayPayPath(ou.getCode(), sessionToken, orderNo, amountPlain, currency, productName,
                langCode, returnUrl, cancelUrl);
        String payUrl = base.isEmpty() ? payPath : base + payPath;

        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(session.toPublicMap());
        data.put("sessionToken", sessionToken);
        data.put("payUrl", payUrl);
        data.put("returnUrl", returnUrl);
        if (!cancelUrl.isBlank()) {
            data.put("cancelUrl", cancelUrl);
        }
        data.put("redirectCheckoutPrepareUrl",
                trimSlash(productService.resolvePublicCustomerSiteBase(request))
                        + "/api/middleware/v1/merchant/jpay/redirect-checkout/prepare");
        data.put("integrationMode", "REDIRECT");
        data.put("pgVendor", MerchantPgBrokerVendor.JPAY);
        if (langCode != null && !langCode.isBlank()) {
            data.put("langCode", langCode);
        }
        data.put("redirectUsageHint",
                "가맹점 서버에서 prepare 호출 후 payUrl로 브라우저를 리다이렉트하세요. "
                        + "결제 완료·취소 시 returnUrl·cancelUrl로 복귀합니다.");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public Map<String, Object> readSession(String token) {
        Optional<MerchantInlineCheckoutTokenService.SessionPayload> parsed =
                tokenService.parseValid(token, MerchantPgBrokerVendor.JPAY);
        if (parsed.isEmpty()) {
            return fail("세션이 유효하지 않거나 만료되었습니다.", "INVALID_SESSION");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", parsed.get().toPublicMap());
        return out;
    }

    public Map<String, Object> orderStatus(Long orgUnitId, String orderNoRaw) {
        return inlineCheckoutService.orderStatus(orgUnitId, orderNoRaw);
    }

    private static String buildJpayPayPath(String compCode, String sessionToken, String orderNo,
                                           String amountPlain, String currency, String productName, String langCode,
                                           String returnUrl, String cancelUrl) {
        StringBuilder q = new StringBuilder();
        q.append("/jpay-pay/").append(urlEnc(compCode));
        q.append("?entry=merchant_api");
        q.append("&session=").append(urlEnc(sessionToken));
        q.append("&orderNo=").append(urlEnc(orderNo));
        q.append("&amount=").append(urlEnc(amountPlain));
        q.append("&currency=").append(urlEnc(currency));
        if (productName != null && !productName.isBlank()) {
            q.append("&item=").append(urlEnc(productName));
        }
        if (langCode != null && !langCode.isBlank()) {
            q.append("&lang=").append(urlEnc(langCode));
        }
        q.append("&returnUrl=").append(urlEnc(returnUrl));
        if (cancelUrl != null && !cancelUrl.isBlank()) {
            q.append("&cancelUrl=").append(urlEnc(cancelUrl));
        }
        return q.toString();
    }

    private static boolean isHttpsUrl(String url) {
        if (url == null) {
            return false;
        }
        String t = url.trim();
        return t.regionMatches(true, 0, "https://", 0, 8) && t.length() > 8;
    }

    private static String normalizeOrderNo(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static BigDecimal parseAmount(Object o) {
        if (o == null) {
            return null;
        }
        try {
            String s = o.toString().trim().replace(",", "");
            if (s.isEmpty()) {
                return null;
            }
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String clamp(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String urlEnc(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
