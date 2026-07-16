package com.pg.merchantdeploy;

import com.pg.entity.MerchantIlkSubscription;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantIlkSubscriptionRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.IlkPaymentService;
import com.pg.service.JpaySubscriptionConfigService;
import com.pg.service.JpaySubscriptionPlanUtil;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.OrgServiceUseService;
import com.pg.service.UrlPayCheckoutCurrencyService;
import com.pg.urlpay.CheckoutFailI18n;
import com.pg.urlpay.NeutralCheckoutRoute;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 가맹점 API — ILK 구독(CIT+3DS 초회, MIT 스케줄). */
@Service
public class MerchantIlkSubscriptionCheckoutService {

    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final JpaySubscriptionConfigService subscriptionConfigService;
    private final IlkPaymentService ilkPaymentService;
    private final MerchantChatbotProductService productService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final MerchantIlkSubscriptionRepository subscriptionRepository;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;

    public MerchantIlkSubscriptionCheckoutService(OrgUnitRepository orgUnitRepository,
                                                  OrgServiceUseService orgServiceUseService,
                                                  JpaySubscriptionConfigService subscriptionConfigService,
                                                  IlkPaymentService ilkPaymentService,
                                                  MerchantChatbotProductService productService,
                                                  MerchantInlineCheckoutTokenService tokenService,
                                                  MerchantIlkSubscriptionRepository subscriptionRepository,
                                                  UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService) {
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.subscriptionConfigService = subscriptionConfigService;
        this.ilkPaymentService = ilkPaymentService;
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
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        OrgUnit ou = ouOpt.get();

        String orderNo = normalizeOrderNo(str(body.get("orderNo")));
        if (orderNo.isBlank()) {
            return CheckoutFailI18n.invalidOrderNo();
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CheckoutFailI18n.invalidAmount();
        }
        Map<String, Object> planBuilt = JpaySubscriptionPlanUtil.buildPlanJson(body,
                subscriptionConfigService.resolveHqDefaultsJson());
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

        touchSubscriptionMaster(orgUnitId, ou.getCode(), orderNo, planJson, amount, currency);

        String sessionToken = tokenService.issueSubscription(MerchantPgBrokerVendor.ILK, ou.getCode(), orderNo,
                amountPlain, currency, productName, planJson);
        Optional<MerchantInlineCheckoutTokenService.SessionPayload> parsed = tokenService.parseValid(
                sessionToken, MerchantPgBrokerVendor.ILK, MerchantInlineCheckoutTokenService.CHECKOUT_SUBSCRIPTION);
        if (parsed.isEmpty()) {
            return CheckoutFailI18n.sessionError();
        }
        MerchantInlineCheckoutTokenService.SessionPayload session = parsed.get();

        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        String payUrl = NeutralCheckoutRoute.buildSubscribeUrl(base, ou.getCode(), sessionToken, langCode, true);
        String embedScriptUrl = (base.isEmpty() ? "" : base) + "/v1/embed-checkout-subscribe/" + urlEnc(ou.getCode());

        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(session.toPublicMap());
        data.put("sessionToken", sessionToken);
        data.put("payUrl", payUrl);
        data.put("embedScriptUrl", embedScriptUrl);
        data.put("integrationMode", "INLINE");
        data.put("checkoutKind", MerchantInlineCheckoutTokenService.CHECKOUT_SUBSCRIPTION);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
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
                token, MerchantPgBrokerVendor.ILK, MerchantInlineCheckoutTokenService.CHECKOUT_SUBSCRIPTION);
        if (parsed.isEmpty()) {
            return CheckoutFailI18n.fail("INVALID_SESSION",
                    "세션이 유효하지 않거나 만료되었습니다.",
                    "The session is invalid or has expired.",
                    "セッションが無効か期限切れです。",
                    "会话无效或已过期。",
                    "เซสชันไม่ถูกต้องหรือหมดอายุ");
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
            return CheckoutFailI18n.invalidOrderNo();
        }
        Optional<MerchantIlkSubscription> sub = subscriptionRepository.findByCompIdAndSubscriptionNo(
                ouOpt.get().getCode(), orderNo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", ouOpt.get().getCode());
        data.put("orderNo", orderNo);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        if (sub.isEmpty()) {
            data.put("found", false);
            data.put("status", "NOT_FOUND");
        } else {
            MerchantIlkSubscription s = sub.get();
            data.put("found", true);
            data.put("status", s.getStatus());
            data.put("chargeCount", s.getChargeCount());
            data.put("firstAuthId", s.getFirstAuthId());
            data.put("nextChargeAt", s.getNextChargeAt() != null ? s.getNextChargeAt().toString() : null);
            data.put("cardLast4", s.getCardLast4());
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
            return CheckoutFailI18n.invalidOrderNo();
        }
        subscriptionRepository.findByCompIdAndSubscriptionNo(ouOpt.get().getCode(), orderNo).ifPresent(s -> {
            s.setStatus(MerchantIlkSubscription.STATUS_CANCELLED);
            s.setNextChargeAt(null);
            subscriptionRepository.save(s);
        });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("status", MerchantIlkSubscription.STATUS_CANCELLED);
        out.put("data", data);
        return out;
    }

    @Transactional
    public void activateAfterFirstCharge(String compId, String subscriptionNo, String authId,
                                         String cardBrand, String cardLast4,
                                         String cardTokenEnc, String expMonthEnc, String expYearEnc,
                                         int intervalDays) {
        if (compId == null || subscriptionNo == null) {
            return;
        }
        subscriptionRepository.findByCompIdAndSubscriptionNo(compId, subscriptionNo).ifPresent(s -> {
            s.setStatus(MerchantIlkSubscription.STATUS_ACTIVE);
            s.setFirstAuthId(authId);
            s.setCardBrand(cardBrand);
            s.setCardLast4(cardLast4);
            s.setCardTokenEnc(cardTokenEnc);
            s.setCardExpMonthEnc(expMonthEnc);
            s.setCardExpYearEnc(expYearEnc);
            s.setChargeCount(s.getChargeCount() == null ? 1 : Math.max(1, s.getChargeCount()));
            s.setLastChargeAt(LocalDateTime.now());
            int days = intervalDays > 0 ? intervalDays : 30;
            s.setNextChargeAt(LocalDateTime.now().plusDays(days));
            subscriptionRepository.save(s);
        });
    }

    @Transactional
    protected void touchSubscriptionMaster(Long orgUnitId, String compId, String orderNo,
                                           String planJson, BigDecimal amount, String currency) {
        Optional<MerchantIlkSubscription> ex = subscriptionRepository.findByCompIdAndSubscriptionNo(compId, orderNo);
        MerchantIlkSubscription s = ex.orElseGet(MerchantIlkSubscription::new);
        if (s.getId() == null) {
            s.setOrgUnitId(orgUnitId);
            s.setCompId(compId);
            s.setSubscriptionNo(orderNo);
            s.setStatus(MerchantIlkSubscription.STATUS_PENDING);
            s.setChargeCount(0);
            s.setFirstOrderNo(orderNo);
        }
        s.setPlanJson(planJson);
        s.setAmount(amount);
        s.setCurrency(currency);
        subscriptionRepository.save(s);
    }

    private Map<String, Object> gateMerchant(Long orgUnitId) {
        if (orgUnitId == null) {
            return CheckoutFailI18n.merchantNotFound();
        }
        if (orgUnitRepository.findById(orgUnitId).isEmpty()) {
            return CheckoutFailI18n.merchantNotFound();
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED");
        }
        if (!ilkPaymentService.hasOperationalSubscriptionBinding(orgUnitId)) {
            return CheckoutFailI18n.subscriptionPgMissing();
        }
        return null;
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
            return new BigDecimal(o.toString().trim().replace(",", ""));
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
        return s.trim().replaceAll("/+$", "");
    }
}
