package com.pg.merchantdeploy;

import com.pg.integration.pg.PgVendor;
import com.pg.service.ChillPayService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.urlpay.IcipayBuyerContactUtil;
import com.pg.urlpay.NeutralCheckoutRoute;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * PG 무관 통합 가맹 REDIRECT checkout — 운영 WEB PG({@link ChillPayService#resolveUrlPayOperationalPgCd})에 따라
 * ChillPay/JPAY prepare 로 위임합니다.
 */
@Service
public class MerchantUnifiedRedirectCheckoutService {

    private final ChillPayService chillPayService;
    private final MerchantChillpayRedirectCheckoutService chillpayRedirectCheckoutService;
    private final MerchantJpayRedirectCheckoutService jpayRedirectCheckoutService;
    private final MerchantEximbayInlineCheckoutService eximbayInlineCheckoutService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final MerchantChatbotProductService productService;
    private final MerchantApiIntegrationChannelService integrationChannelService;

    public MerchantUnifiedRedirectCheckoutService(ChillPayService chillPayService,
                                                  MerchantChillpayRedirectCheckoutService chillpayRedirectCheckoutService,
                                                  MerchantJpayRedirectCheckoutService jpayRedirectCheckoutService,
                                                  MerchantEximbayInlineCheckoutService eximbayInlineCheckoutService,
                                                  MerchantInlineCheckoutTokenService tokenService,
                                                  MerchantChatbotProductService productService,
                                                  MerchantApiIntegrationChannelService integrationChannelService) {
        this.chillPayService = chillPayService;
        this.chillpayRedirectCheckoutService = chillpayRedirectCheckoutService;
        this.jpayRedirectCheckoutService = jpayRedirectCheckoutService;
        this.eximbayInlineCheckoutService = eximbayInlineCheckoutService;
        this.tokenService = tokenService;
        this.productService = productService;
        this.integrationChannelService = integrationChannelService;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        Optional<String> redirectDeny = integrationChannelService.denyMessage(orgUnitId,
                MerchantApiIntegrationChannelService.Channel.API_BROKER_REDIRECT);
        if (redirectDeny.isPresent()) {
            return fail(redirectDeny.get(), MerchantApiIntegrationChannelService.CODE_INTEGRATION_CHANNEL_DISABLED);
        }
        Optional<Map<String, Object>> returnUrlReject = MerchantRedirectCheckoutPrepareUtil.rejectMerchantReturnUrlsInBody(body);
        if (returnUrlReject.isPresent()) {
            return returnUrlReject.get();
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
            result = jpayRedirectCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isEximbayFamily(opPg)) {
            // Eximbay 는 호스티드 결제창 — 동일 세션 준비로 중립 결제창 payUrl 을 반환
            result = eximbayInlineCheckoutService.prepare(orgUnitId, enriched, request);
        } else if (PgVendor.isChillPayFamily(opPg)) {
            result = chillpayRedirectCheckoutService.prepare(orgUnitId, enriched, request);
        } else {
            return fail("지원하지 않는 결제 구성입니다.", "PG_NOT_SUPPORTED");
        }

        patchUnifiedPrepareResponse(result, request, opPg);
        return result;
    }

    public Map<String, Object> readSession(String token) {
        return tokenService.parseValid(token)
                .map(session -> {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("success", true);
                    out.put("data", session.toPublicMap());
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
            return jpayRedirectCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isEximbayFamily(opPg)) {
            return eximbayInlineCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        if (PgVendor.isChillPayFamily(opPg)) {
            return chillpayRedirectCheckoutService.orderStatus(orgUnitId, orderNo);
        }
        return fail("지원하지 않는 결제 구성입니다.", "PG_NOT_SUPPORTED");
    }

    @SuppressWarnings("unchecked")
    private void patchUnifiedPrepareResponse(Map<String, Object> result, HttpServletRequest request, String opPg) {
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
        // 가맹점·구매자에게 PG(ChillPay/JPAY/Eximbay)를 노출하지 않는 중립 결제창 경로로 통일
        if (!compId.isBlank()) {
            data.put("payUrl", NeutralCheckoutRoute.buildPayUrl(base, compId,
                    str(data.get("sessionToken")), str(data.get("langCode")), false));
        }
        data.put("redirectCheckoutPrepareUrl",
                base + "/api/middleware/v1/merchant/checkout/redirect/prepare");
        // 실제 PG 는 응답에 노출하지 않는다 — 항상 ICOPAY (operationalPgCd 미노출)
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("integrationMode", "REDIRECT_UNIFIED");
        data.put("redirectUsageHint",
                "ICOPAY 통합 REDIRECT prepare: buyer(email·phone·countryIso2) 필수. "
                        + "returnUrl/cancelUrl은 body에 넣지 않음 — 브라우저 복귀는 NOTI Result 경유. "
                        + "결제망은 ICOPAY가 자동 선택·처리합니다.");
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
