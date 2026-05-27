package com.pg.merchantdeploy;

import com.pg.entity.MerchantJpaySubscription;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantJpaySubscriptionRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.JpayPaymentService;
import com.pg.service.JpaySubscriptionConfigService;
import com.pg.service.JpaySubscriptionPlanUtil;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.OrgServiceUseService;
import com.pg.service.UrlPayCheckoutCurrencyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 가맹점 API — JPAY 구독 인라인({@code jpay-subscribe.html}) 세션·해지. */
@Service
public class MerchantJpaySubscriptionCheckoutService {

    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final JpaySubscriptionConfigService subscriptionConfigService;
    private final JpayPaymentService jpayPaymentService;
    private final MerchantChatbotProductService productService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final MerchantJpaySubscriptionRepository subscriptionRepository;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;

    public MerchantJpaySubscriptionCheckoutService(OrgUnitRepository orgUnitRepository,
                                                   OrgServiceUseService orgServiceUseService,
                                                   JpaySubscriptionConfigService subscriptionConfigService,
                                                   JpayPaymentService jpayPaymentService,
                                                   MerchantChatbotProductService productService,
                                                   MerchantInlineCheckoutTokenService tokenService,
                                                   MerchantJpaySubscriptionRepository subscriptionRepository,
                                                   UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService) {
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.subscriptionConfigService = subscriptionConfigService;
        this.jpayPaymentService = jpayPaymentService;
        this.productService = productService;
        this.tokenService = tokenService;
        this.subscriptionRepository = subscriptionRepository;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> gate = gateMerchant(orgUnitId);
        if (gate != null) {
            return gate;
        }
        if (hasSubscriptionFields(body)) {
            /* ok */
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        OrgUnit ou = ouOpt.get();

        String orderNo = normalizeOrderNo(str(body.get("orderNo")));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("유효한 amount가 필요합니다.", "INVALID_AMOUNT");
        }
        Map<String, Object> planBuilt = JpaySubscriptionPlanUtil.buildPlanJson(body, subscriptionConfigService.resolveHqDefaultsJson());
        if (!Boolean.TRUE.equals(planBuilt.get("success"))) {
            return planBuilt;
        }
        String planJson = String.valueOf(planBuilt.get("planJson"));
        String amountPlain = amount.stripTrailingZeros().toPlainString();
        String currency = urlPayCheckoutCurrencyService.resolveCheckoutCurrency(orgUnitId, str(body.get("currency")));
        String productName = clamp(str(body.get("productName")), 500);
        if (productName.isBlank()) {
            productName = clamp(String.valueOf(planBuilt.getOrDefault("planName", "")), 500);
        }
        String langCode = MerchantCheckoutLangUtil.fromBody(body);

        touchSubscriptionMaster(orgUnitId, ou.getCode(), orderNo, planJson);

        String sessionToken = tokenService.issueSubscription(MerchantPgBrokerVendor.JPAY, ou.getCode(), orderNo,
                amountPlain, currency, productName, planJson);
        Optional<MerchantInlineCheckoutTokenService.SessionPayload> parsed = tokenService.parseValid(
                sessionToken, MerchantPgBrokerVendor.JPAY, MerchantInlineCheckoutTokenService.CHECKOUT_SUBSCRIPTION);
        if (parsed.isEmpty()) {
            return fail("세션 토큰 생성에 실패했습니다.", "SESSION_ERROR");
        }
        MerchantInlineCheckoutTokenService.SessionPayload session = parsed.get();

        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        String payPath = buildSubscribePath(ou.getCode(), sessionToken, orderNo, amountPlain, currency, productName, langCode);
        String payUrl = base.isEmpty() ? payPath : base + payPath;
        String embedScriptUrl = base.isEmpty()
                ? "/v1/embed-jpay-subscribe/" + urlEnc(ou.getCode())
                : base + "/v1/embed-jpay-subscribe/" + urlEnc(ou.getCode());

        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(session.toPublicMap());
        data.put("sessionToken", sessionToken);
        data.put("payUrl", payUrl);
        data.put("embedScriptUrl", embedScriptUrl);
        data.put("subscriptionPrepareUrl",
                trimSlash(productService.resolvePublicCustomerSiteBase(request))
                        + "/api/middleware/v1/merchant/jpay/subscription/prepare");
        data.put("integrationMode", "INLINE");
        data.put("checkoutKind", MerchantInlineCheckoutTokenService.CHECKOUT_SUBSCRIPTION);
        data.put("pgVendor", MerchantPgBrokerVendor.JPAY);
        if (langCode != null && !langCode.isBlank()) {
            data.put("langCode", langCode);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public Map<String, Object> readSession(String token) {
        Optional<MerchantInlineCheckoutTokenService.SessionPayload> parsed = tokenService.parseValid(
                token, MerchantPgBrokerVendor.JPAY, MerchantInlineCheckoutTokenService.CHECKOUT_SUBSCRIPTION);
        if (parsed.isEmpty()) {
            return fail("세션이 유효하지 않거나 만료되었습니다.", "INVALID_SESSION");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", parsed.get().toPublicMap());
        return out;
    }

    public Map<String, Object> subscriptionStatus(Long orgUnitId, String orderNoRaw) {
        Map<String, Object> gate = gateMerchant(orgUnitId);
        if (gate != null) {
            return gate;
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        String orderNo = normalizeOrderNo(orderNoRaw);
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        Optional<MerchantJpaySubscription> sub = subscriptionRepository.findByCompCodeAndCheckoutOrderNo(
                ouOpt.get().getCode(), orderNo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", ouOpt.get().getCode());
        data.put("orderNo", orderNo);
        data.put("pgVendor", MerchantPgBrokerVendor.JPAY);
        if (sub.isEmpty()) {
            data.put("found", false);
            data.put("status", "NOT_FOUND");
        } else {
            MerchantJpaySubscription s = sub.get();
            data.put("found", true);
            data.put("status", s.getStatus());
            data.put("periodCount", s.getPeriodCount());
            data.put("paymentTransactionId", s.getPaymentTransactionId());
            data.put("subscriptionPlanJson", s.getSubscriptionPlanJson());
            data.put("cancelledAt", s.getCancelledAt() != null ? s.getCancelledAt().toString() : null);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public Map<String, Object> cancel(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> gate = gateMerchant(orgUnitId);
        if (gate != null) {
            return gate;
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        String orderNo = normalizeOrderNo(str(body.get("orderNo")));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        Map<String, Object> cancelResult = jpayPaymentService.executeSubscriptionCancel(orgUnitId, orderNo, request);
        Object ok = cancelResult.get("success");
        if (!(ok instanceof Boolean) || !(Boolean) ok) {
            return cancelResult;
        }
        subscriptionRepository.findByCompCodeAndCheckoutOrderNo(ouOpt.get().getCode(), orderNo).ifPresent(s -> {
            s.setStatus(MerchantJpaySubscription.STATUS_CANCELLED);
            s.setCancelledAt(java.time.LocalDateTime.now());
            subscriptionRepository.save(s);
        });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", cancelResult);
        return out;
    }

    @Transactional
    protected void touchSubscriptionMaster(Long orgUnitId, String compCode, String orderNo, String planJson) {
        Optional<MerchantJpaySubscription> ex = subscriptionRepository.findByCompCodeAndCheckoutOrderNo(compCode, orderNo);
        MerchantJpaySubscription s = ex.orElseGet(MerchantJpaySubscription::new);
        if (s.getId() == null) {
            s.setOrgUnitId(orgUnitId);
            s.setCompCode(compCode);
            s.setCheckoutOrderNo(orderNo);
            s.setStatus(MerchantJpaySubscription.STATUS_PENDING);
            s.setPeriodCount(0);
        }
        subscriptionConfigService.findOperationalSubscriptionBinding(orgUnitId).ifPresent(b ->
                s.setPgCd(b.getPgCd() != null ? b.getPgCd().trim() : "JPAY"));
        s.setSubscriptionPlanJson(planJson);
        subscriptionRepository.save(s);
    }

    private Map<String, Object> gateMerchant(Long orgUnitId) {
        if (orgUnitId == null) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (orgUnitRepository.findById(orgUnitId).isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return fail("서비스가 중지된 업체입니다.", "ORG_DISABLED");
        }
        if (!subscriptionConfigService.isMerchantSubscriptionEnabled(orgUnitId)) {
            return fail("JPAY API 구독이 비활성입니다. 본사·가맹 설정을 확인하세요.", "SUBSCRIPTION_DISABLED");
        }
        if (!subscriptionConfigService.isHqSubscriptionInlineEnabled()) {
            return fail("본사 JPAY 구독 INLINE 제공이 꺼져 있습니다.", "INLINE_NOT_ENABLED");
        }
        if (!jpayPaymentService.hasOperationalSubscriptionBinding(orgUnitId)) {
            return fail("JPAY API 구독(운영) 바인딩이 없습니다.", "SUBSCRIPTION_PG_MISSING");
        }
        return null;
    }

    private static boolean hasSubscriptionFields(Map<String, Object> body) {
        return body != null && (body.containsKey("subscriptionPlan") || body.containsKey("subscription_plan"));
    }

    private static String buildSubscribePath(String compCode, String sessionToken, String orderNo,
                                             String amountPlain, String currency, String productName, String langCode) {
        StringBuilder q = new StringBuilder();
        q.append("/jpay-subscribe/").append(urlEnc(compCode));
        q.append("?entry=merchant_api");
        q.append("&embed=1");
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
        return q.toString();
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
