package com.pg.merchantdeploy;

import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.splitpay.SplitPayCheckoutModeGuard;
import com.pg.urlpay.MobileCheckoutModeService;
import com.pg.urlpay.IcipayBuyerContactUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * PG 무관 통합 가맹 인라인 checkout — 운영 WEB PG({@link ChillPayService#resolveUrlPayOperationalPgCd})에 따라
 * ChillPay/JPAY prepare 로 위임합니다.
 */
@Service
public class MerchantUnifiedInlineCheckoutService {

    private final ChillPayService chillPayService;
    private final MerchantInlineCheckoutService chillpayInlineCheckoutService;
    private final MerchantJpayInlineCheckoutService jpayInlineCheckoutService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final MerchantChatbotProductService productService;
    private final MerchantApiIntegrationChannelService integrationChannelService;

    private final SplitPayCheckoutModeGuard splitPayCheckoutModeGuard;
    private final MobileCheckoutModeService mobileCheckoutModeService;
    private final OrgUnitRepository orgUnitRepository;

    public MerchantUnifiedInlineCheckoutService(ChillPayService chillPayService,
                                                MerchantInlineCheckoutService chillpayInlineCheckoutService,
                                                MerchantJpayInlineCheckoutService jpayInlineCheckoutService,
                                                MerchantInlineCheckoutTokenService tokenService,
                                                MerchantChatbotProductService productService,
                                                MerchantApiIntegrationChannelService integrationChannelService,
                                                SplitPayCheckoutModeGuard splitPayCheckoutModeGuard,
                                                MobileCheckoutModeService mobileCheckoutModeService,
                                                OrgUnitRepository orgUnitRepository) {
        this.chillPayService = chillPayService;
        this.chillpayInlineCheckoutService = chillpayInlineCheckoutService;
        this.jpayInlineCheckoutService = jpayInlineCheckoutService;
        this.tokenService = tokenService;
        this.productService = productService;
        this.integrationChannelService = integrationChannelService;
        this.splitPayCheckoutModeGuard = splitPayCheckoutModeGuard;
        this.mobileCheckoutModeService = mobileCheckoutModeService;
        this.orgUnitRepository = orgUnitRepository;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        Optional<Map<String, Object>> splitDeny = splitPayCheckoutModeGuard.denyInlineOneShotPrepare(orgUnitId);
        if (splitDeny.isPresent()) {
            return splitDeny.get();
        }
        Optional<String> inlineDeny = integrationChannelService.denyMessage(orgUnitId,
                MerchantApiIntegrationChannelService.Channel.API_BROKER_INLINE);
        if (inlineDeny.isPresent()) {
            return fail(inlineDeny.get(), MerchantApiIntegrationChannelService.CODE_INTEGRATION_CHANNEL_DISABLED);
        }
        Map<String, String> buyer;
        try {
            buyer = IcipayBuyerContactUtil.extractAndValidateRequired(body);
        } catch (IllegalArgumentException ex) {
            return fail(ex.getMessage(), "BUYER_REQUIRED");
        }

        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (opPg == null || opPg.isBlank()) {
            return fail("URL 결제 운영 PG가 설정되지 않았습니다.", "URL_PAYMENT_PG_MISSING");
        }

        Map<String, Object> enriched = new LinkedHashMap<>(body != null ? body : Map.of());
        enriched.put("buyerPrefill", IcipayBuyerContactUtil.toPublicMap(buyer));

        Map<String, Object> result;
        if (PgVendor.isJpayFamily(opPg)) {
            result = jpayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isChillPayFamily(opPg)) {
            result = chillpayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else {
            return fail("지원하지 않는 URL 결제 PG: " + opPg, "PG_NOT_SUPPORTED");
        }

        patchUnifiedPrepareResponse(result, request, opPg, orgUnitId);
        return result;
    }

    public Map<String, Object> readSession(String token, HttpServletRequest request) {
        return tokenService.parseValid(token)
                .map(session -> {
                    Map<String, Object> data = new LinkedHashMap<>(session.toPublicMap());
                    data.put("sessionToken", token);
                    orgUnitRepository.findByCode(session.compId()).ifPresent(ou ->
                            mobileCheckoutModeService.enrichInlineSession(data, ou.getId(), request));
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("success", true);
                    out.put("data", data);
                    return out;
                })
                .orElseGet(() -> fail("세션이 유효하지 않거나 만료되었습니다.", "INVALID_SESSION"));
    }

    public Map<String, Object> orderStatus(Long orgUnitId, String orderNo) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (opPg == null || opPg.isBlank()) {
            return fail("URL 결제 운영 PG가 설정되지 않았습니다.", "URL_PAYMENT_PG_MISSING");
        }
        if (PgVendor.isJpayFamily(opPg)) {
            return jpayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isChillPayFamily(opPg)) {
            return chillpayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        return fail("지원하지 않는 URL 결제 PG: " + opPg, "PG_NOT_SUPPORTED");
    }

    @SuppressWarnings("unchecked")
    private void patchUnifiedPrepareResponse(Map<String, Object> result, HttpServletRequest request,
                                             String opPg, Long orgUnitId) {
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
        String embedPath = "/v1/embed-checkout/" + compId;
        data.put("embedScriptUrl", base.isEmpty() ? embedPath : base + embedPath);
        data.put("inlineCheckoutPrepareUrl",
                base + "/api/middleware/v1/merchant/checkout/prepare");
        data.put("operationalPgCd", opPg);
        data.put("integrationMode", "INLINE_UNIFIED");
        if (orgUnitId != null) {
            mobileCheckoutModeService.putEffectiveIntoMap(data, orgUnitId);
        }
        data.put("embedUsageHint",
                "통합 prepare: buyer(email·phone·countryIso2) 필수. sessionToken → /v1/embed-checkout/{compId} "
                        + "(운영 PG 자동 분기). 레거시 /chillpay·/jpay 경로도 호환됩니다.");
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
