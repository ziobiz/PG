package com.pg.urlpay;

import com.pg.service.ChillPayService;
import com.pg.service.JpayPaymentService;
import com.pg.service.UrlPayChargeResolutionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 통합 URL 결제 승인 라우팅 — 운영 PG의 {@link UrlPaySaleChannel} 에 따라 어댑터로 위임합니다.
 */
@Service
public class UrlPaySaleDispatcher {

    private final ChillPayService chillPayService;
    private final JpayPaymentService jpayPaymentService;
    private final UrlPayVendorCapabilityRegistry capabilityRegistry;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;

    public UrlPaySaleDispatcher(ChillPayService chillPayService,
                                JpayPaymentService jpayPaymentService,
                                UrlPayVendorCapabilityRegistry capabilityRegistry,
                                UrlPayChargeResolutionService urlPayChargeResolutionService) {
        this.chillPayService = chillPayService;
        this.jpayPaymentService = jpayPaymentService;
        this.capabilityRegistry = capabilityRegistry;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
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
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
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
            case CHILLPAY_DIRECT_CREDIT -> fail(
                    "ChillPay URL 결제는 POST /api/pay/chillpay/direct-credit 를 사용하세요(CCD 토큰 필요).",
                    "USE_CHILLPAY_DIRECT_CREDIT");
            case NOT_REGISTERED -> fail(
                    "이 PG(" + cap.operationalPgCd() + ")의 URL 결제 승인 어댑터가 아직 등록되지 않았습니다.",
                    "URL_PAY_SALE_NOT_REGISTERED");
        };
    }

    public UrlPayVendorCapability resolveCapability(Long orgUnitId) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        return capabilityRegistry.resolve(opPg);
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code != null ? code : "URL_PAY_SALE_FAILED");
        return out;
    }
}
