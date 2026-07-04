package com.pg.merchantdeploy;

import com.pg.entity.MerchantPgBinding;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.JpaySubscriptionConfigService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.urlpay.NeutralCheckoutRoute;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * PG 무관 통합 가맹 구독(정기결제) checkout — 운영 구독 PG에 따라 내부 서비스로 위임.
 * 가맹점 응답·URL에는 ICOPAY만 노출한다.
 */
@Service
public class MerchantUnifiedSubscriptionCheckoutService {

    private final JpaySubscriptionConfigService subscriptionConfigService;
    private final MerchantJpaySubscriptionCheckoutService jpaySubscriptionCheckoutService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final MerchantChatbotProductService productService;
    private final OrgUnitRepository orgUnitRepository;

    public MerchantUnifiedSubscriptionCheckoutService(JpaySubscriptionConfigService subscriptionConfigService,
                                                        MerchantJpaySubscriptionCheckoutService jpaySubscriptionCheckoutService,
                                                        MerchantInlineCheckoutTokenService tokenService,
                                                        MerchantChatbotProductService productService,
                                                        OrgUnitRepository orgUnitRepository) {
        this.subscriptionConfigService = subscriptionConfigService;
        this.jpaySubscriptionCheckoutService = jpaySubscriptionCheckoutService;
        this.tokenService = tokenService;
        this.productService = productService;
        this.orgUnitRepository = orgUnitRepository;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        String opPg = resolveOperationalSubscriptionPgCd(orgUnitId);
        if (opPg.isBlank()) {
            return fail("구독(정기결제) 운영 바인딩이 없습니다.", "SUBSCRIPTION_PG_MISSING");
        }
        Map<String, Object> result;
        if (PgVendor.isJpayFamily(opPg)) {
            result = jpaySubscriptionCheckoutService.prepare(orgUnitId, body, request);
        } else {
            return fail("지원하지 않는 구독 결제 구성입니다.", "SUBSCRIPTION_PG_NOT_SUPPORTED");
        }
        patchUnifiedPrepareResponse(result, request, orgUnitId);
        return result;
    }

    public Map<String, Object> readSession(String token, HttpServletRequest request) {
        return tokenService.parseValid(token, null, MerchantInlineCheckoutTokenService.CHECKOUT_SUBSCRIPTION)
                .map(session -> {
                    Map<String, Object> data = new LinkedHashMap<>(session.toPublicMap());
                    data.put("sessionToken", token);
                    data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
                    orgUnitRepository.findByCode(session.compId()).ifPresent(ou ->
                            enrichSubscribeSessionPayUrl(data, ou.getId(), request));
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("success", true);
                    out.put("data", data);
                    return out;
                })
                .orElseGet(() -> fail("세션이 유효하지 않거나 만료되었습니다.", "INVALID_SESSION"));
    }

    public Map<String, Object> subscriptionStatus(Long orgUnitId, String orderNo) {
        String opPg = resolveOperationalSubscriptionPgCd(orgUnitId);
        if (opPg.isBlank()) {
            return fail("구독(정기결제) 운영 바인딩이 없습니다.", "SUBSCRIPTION_PG_MISSING");
        }
        Map<String, Object> result;
        if (PgVendor.isJpayFamily(opPg)) {
            result = jpaySubscriptionCheckoutService.subscriptionStatus(orgUnitId, orderNo);
        } else {
            return fail("지원하지 않는 구독 결제 구성입니다.", "SUBSCRIPTION_PG_NOT_SUPPORTED");
        }
        neutralizeResultData(result);
        return result;
    }

    public Map<String, Object> cancel(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        String opPg = resolveOperationalSubscriptionPgCd(orgUnitId);
        if (opPg.isBlank()) {
            return fail("구독(정기결제) 운영 바인딩이 없습니다.", "SUBSCRIPTION_PG_MISSING");
        }
        Map<String, Object> result;
        if (PgVendor.isJpayFamily(opPg)) {
            result = jpaySubscriptionCheckoutService.cancel(orgUnitId, body, request);
        } else {
            return fail("지원하지 않는 구독 결제 구성입니다.", "SUBSCRIPTION_PG_NOT_SUPPORTED");
        }
        neutralizeResultData(result);
        return result;
    }

    private String resolveOperationalSubscriptionPgCd(Long orgUnitId) {
        if (orgUnitId == null) {
            return "";
        }
        Optional<MerchantPgBinding> bind = subscriptionConfigService.findOperationalSubscriptionBinding(orgUnitId);
        if (bind.isEmpty()) {
            return "";
        }
        String pgCd = bind.get().getPgCd();
        return pgCd != null ? pgCd.trim() : "";
    }

    @SuppressWarnings("unchecked")
    private void patchUnifiedPrepareResponse(Map<String, Object> result, HttpServletRequest request, Long orgUnitId) {
        Object ok = result.get("success");
        if (!(ok instanceof Boolean) || !(Boolean) ok) {
            return;
        }
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            return;
        }
        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        String compId = str(data.get("compId"));
        if (compId.isBlank()) {
            return;
        }
        String sessionToken = str(data.get("sessionToken"));
        String langCode = str(data.get("langCode"));
        data.put("embedScriptUrl", base.isEmpty()
                ? "/v1/embed-checkout-subscribe/" + compId
                : base + "/v1/embed-checkout-subscribe/" + compId);
        data.put("subscriptionPrepareUrl",
                base + "/api/middleware/v1/merchant/checkout/subscription/prepare");
        data.put("payUrl", NeutralCheckoutRoute.buildSubscribeUrl(base, compId, sessionToken, langCode, true));
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("integrationMode", "SUBSCRIPTION_UNIFIED");
        data.put("embedUsageHint",
                "ICOPAY 통합 구독 prepare: subscriptionPlan 필수. sessionToken → /v1/embed-checkout-subscribe/{compId}. "
                        + "결제망은 ICOPAY가 자동 선택·처리합니다.");
    }

    private void enrichSubscribeSessionPayUrl(Map<String, Object> data, Long orgUnitId, HttpServletRequest request) {
        String compId = str(data.get("compId"));
        if (compId.isBlank()) {
            return;
        }
        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        data.put("payUrl", NeutralCheckoutRoute.buildSubscribeUrl(base, compId,
                str(data.get("sessionToken")), str(data.get("langCode")), true));
    }

    @SuppressWarnings("unchecked")
    private static void neutralizeResultData(Map<String, Object> result) {
        Object ok = result.get("success");
        if (!(ok instanceof Boolean) || !(Boolean) ok) {
            return;
        }
        Object dataObj = result.get("data");
        if (dataObj instanceof Map<?, ?> data) {
            if (data.containsKey("pgVendor")) {
                ((Map<String, Object>) data).put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
            }
        }
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
