package com.pg.merchantdeploy;

import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.splitpay.SplitPayCheckoutModeGuard;
import com.pg.urlpay.MobileCheckoutModeService;
import com.pg.urlpay.IcipayBuyerContactUtil;
import com.pg.urlpay.NeutralCheckoutRoute;
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
    private final MerchantEximbayInlineCheckoutService eximbayInlineCheckoutService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final MerchantChatbotProductService productService;
    private final MerchantApiIntegrationChannelService integrationChannelService;

    private final SplitPayCheckoutModeGuard splitPayCheckoutModeGuard;
    private final MobileCheckoutModeService mobileCheckoutModeService;
    private final OrgUnitRepository orgUnitRepository;

    public MerchantUnifiedInlineCheckoutService(ChillPayService chillPayService,
                                                MerchantInlineCheckoutService chillpayInlineCheckoutService,
                                                MerchantJpayInlineCheckoutService jpayInlineCheckoutService,
                                                MerchantEximbayInlineCheckoutService eximbayInlineCheckoutService,
                                                MerchantInlineCheckoutTokenService tokenService,
                                                MerchantChatbotProductService productService,
                                                MerchantApiIntegrationChannelService integrationChannelService,
                                                SplitPayCheckoutModeGuard splitPayCheckoutModeGuard,
                                                MobileCheckoutModeService mobileCheckoutModeService,
                                                OrgUnitRepository orgUnitRepository) {
        this.chillPayService = chillPayService;
        this.chillpayInlineCheckoutService = chillpayInlineCheckoutService;
        this.jpayInlineCheckoutService = jpayInlineCheckoutService;
        this.eximbayInlineCheckoutService = eximbayInlineCheckoutService;
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
        } else if (PgVendor.isEximbayFamily(opPg)) {
            result = eximbayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isChillPayFamily(opPg)) {
            result = chillpayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else {
            return fail("지원하지 않는 결제 구성입니다.", "PG_NOT_SUPPORTED");
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
        if (PgVendor.isEximbayFamily(opPg)) {
            return eximbayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isChillPayFamily(opPg)) {
            return chillpayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        return fail("지원하지 않는 결제 구성입니다.", "PG_NOT_SUPPORTED");
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
        // 가맹점·구매자에게 PG(ChillPay/JPAY/Eximbay)를 노출하지 않는 중립 결제창 경로로 통일
        String sessionToken = str(data.get("sessionToken"));
        data.put("payUrl", NeutralCheckoutRoute.buildPayUrl(base, compId, sessionToken, str(data.get("langCode")), true));
        // 실제 PG 는 응답에 노출하지 않는다 — 항상 ICOPAY (operationalPgCd 미노출)
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("integrationMode", "INLINE_UNIFIED");
        if (orgUnitId != null) {
            mobileCheckoutModeService.putEffectiveIntoMap(data, orgUnitId);
        }
        data.put("embedUsageHint",
                "ICOPAY 통합 prepare: buyer(email·phone·countryIso2) 필수. sessionToken → /v1/embed-checkout/{compId}. "
                        + "결제망은 ICOPAY가 자동 선택·처리하며, 가맹점 연동은 결제 대행사 변경과 무관하게 그대로 유지됩니다.");
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
