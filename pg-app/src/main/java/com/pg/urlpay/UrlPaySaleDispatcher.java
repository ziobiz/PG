package com.pg.urlpay;

import com.pg.entity.MerchantPgBinding;
import com.pg.service.ChillPayService;
import com.pg.service.ElementPayPaymentService;
import com.pg.service.EximbayPaymentService;
import com.pg.service.IlkPaymentService;
import com.pg.service.JpayPaymentService;
import com.pg.service.MerchantPgBindingRouterService;
import com.pg.service.UrlPayChargeResolutionService;
import com.pg.util.CardBrandScopeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 통합 URL 결제 승인 라우팅 — 운영 PG의 {@link UrlPaySaleChannel} 에 따라 어댑터로 위임합니다.
 */
@Service
public class UrlPaySaleDispatcher {

    private final ChillPayService chillPayService;
    private final JpayPaymentService jpayPaymentService;
    private final EximbayPaymentService eximbayPaymentService;
    private final ElementPayPaymentService elementPayPaymentService;
    private final IlkPaymentService ilkPaymentService;
    private final UrlPayVendorCapabilityRegistry capabilityRegistry;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final MerchantPgBindingRouterService pgBindingRouter;

    public UrlPaySaleDispatcher(ChillPayService chillPayService,
                                JpayPaymentService jpayPaymentService,
                                EximbayPaymentService eximbayPaymentService,
                                ElementPayPaymentService elementPayPaymentService,
                                IlkPaymentService ilkPaymentService,
                                UrlPayVendorCapabilityRegistry capabilityRegistry,
                                UrlPayChargeResolutionService urlPayChargeResolutionService,
                                MerchantPgBindingRouterService pgBindingRouter) {
        this.chillPayService = chillPayService;
        this.jpayPaymentService = jpayPaymentService;
        this.eximbayPaymentService = eximbayPaymentService;
        this.elementPayPaymentService = elementPayPaymentService;
        this.ilkPaymentService = ilkPaymentService;
        this.capabilityRegistry = capabilityRegistry;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.pgBindingRouter = pgBindingRouter;
    }

    /**
     * 금액·통화 해석 후 PG 승인 API 호출.
     *
     * @return PG 어댑터 결과 맵({@code success}, …) 또는 실패 맵
     */
    public Map<String, Object> executeSale(Long orgUnitId,
                                           Map<String, Object> body,
                                           HttpServletRequest request,
                                           String clientIp) {
        String cardBrand = firstNonBlank(body, "cardBrand", "payCardBrand");
        String currency = firstNonBlank(body, "currency", "displayCurrency");
        MerchantPgBindingRouterService.RoutingHint hint =
                MerchantPgBindingRouterService.RoutingHint.standard(cardBrand, currency);
        Optional<MerchantPgBinding> binding = pgBindingRouter.resolveOperationalBinding(orgUnitId, hint);
        if (binding.isEmpty()) {
            if (pgBindingRouter.isMultiPgRoutingEnabled()
                    && !CardBrandScopeUtil.toScopeLetter(cardBrand).isEmpty()) {
                return fail("이 카드 브랜드에 해당하는 운영 결제대행사가 없습니다. 가맹 결제대행사 설정의 카드브랜드를 확인하세요.",
                        "CARD_BRAND_PG_NOT_CONFIGURED");
            }
            return fail("URL 결제를 처리할 결제대행사(운영·연동용도 URL결제)가 없습니다.", "URL_PAYMENT_PG_MISSING");
        }
        String opPg = binding.get().getPgCd() != null ? binding.get().getPgCd().trim() : "";
        UrlPayVendorCapability cap = capabilityRegistry.resolve(opPg);
        try {
            UrlPayChargeResolutionService.ResolvedCharge charge =
                    urlPayChargeResolutionService.resolve(orgUnitId, body, opPg);
            body.put("amount", charge.pgAmount().stripTrailingZeros().toPlainString());
            body.put("currency", charge.settlementCurrency());
            if (charge.shopperDisplayAmount() != null) {
                body.put("shopperDisplayAmount", charge.shopperDisplayAmount().stripTrailingZeros().toPlainString());
            }
            if (charge.shopperDisplayCurrency() != null && !charge.shopperDisplayCurrency().isBlank()) {
                body.put("shopperDisplayCurrency", charge.shopperDisplayCurrency());
            }
        } catch (IllegalArgumentException ex) {
            return fail(UrlPayChargeResolutionService.failMessageForCode(ex.getMessage()), ex.getMessage());
        }
        return switch (cap.saleChannel()) {
            case JPAY_INLINE_SALE -> jpayPaymentService.executeDirectSale(orgUnitId, body, request, clientIp);
            case EXIMBAY_READY_SALE -> eximbayPaymentService.executeReady(orgUnitId, body, request, clientIp);
            case ELEMENTPAY_INIT_PAYMENT -> elementPayPaymentService.executeInitPayment(orgUnitId, body, request, clientIp);
            case ILK_INLINE_SALE -> ilkPaymentService.executeSale(orgUnitId, body, request, clientIp);
            case CHILLPAY_DIRECT_CREDIT -> fail(
                    "ChillPay URL 결제는 POST /api/pay/chillpay/direct-credit 를 사용하세요(CCD 토큰 필요).",
                    "USE_CHILLPAY_DIRECT_CREDIT");
            case NOT_REGISTERED -> fail(
                    "이 PG(" + cap.operationalPgCd() + ")의 URL 결제 승인 어댑터가 아직 등록되지 않았습니다.",
                    "URL_PAY_SALE_NOT_REGISTERED");
        };
    }

    public UrlPayVendorCapability resolveCapability(Long orgUnitId, String cardBrand, String currency) {
        String opPg = pgBindingRouter.resolveOperationalPgCd(
                orgUnitId, MerchantPgBindingRouterService.RoutingHint.standard(cardBrand, currency));
        if (opPg.isEmpty()) {
            opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        }
        return capabilityRegistry.resolve(opPg);
    }

    public UrlPayVendorCapability resolveCapability(Long orgUnitId) {
        return resolveCapability(orgUnitId, null, null);
    }

    private static String firstNonBlank(Map<String, Object> body, String... keys) {
        if (body == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            Object v = body.get(key);
            if (v != null && !v.toString().isBlank()) {
                return v.toString().trim();
            }
        }
        return "";
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code != null ? code : "URL_PAY_SALE_FAILED");
        return out;
    }
}
