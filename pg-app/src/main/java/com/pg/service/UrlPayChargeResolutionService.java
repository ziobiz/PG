package com.pg.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * URL 결제(ChillPay·JPAY 등) 청구 금액·통화 해석.
 * 본사 URL결제설정의 {@link UrlPayDisplayFxService#MODE_DISPLAY_FX_THB} 와
 * {@link PaymentCurrencyScaleService} 결제통화 로직을 PG사와 무관하게 동일 적용합니다.
 */
@Service
public class UrlPayChargeResolutionService {

    public record ResolvedCharge(
            BigDecimal pgAmount,
            String settlementCurrency,
            BigDecimal shopperDisplayAmount,
            String shopperDisplayCurrency) {
    }

    private final ChillPayService chillPayService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;
    private final PaymentCurrencyScaleService paymentCurrencyScaleService;

    public UrlPayChargeResolutionService(ChillPayService chillPayService,
                                         UrlPayDisplayFxService urlPayDisplayFxService,
                                         UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService,
                                         PaymentCurrencyScaleService paymentCurrencyScaleService) {
        this.chillPayService = chillPayService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
        this.paymentCurrencyScaleService = paymentCurrencyScaleService;
    }

    /**
     * @param body {@code urlPayPricingMode}, {@code amount}, {@code displayAmount}, {@code displayCurrency},
     *             {@code fxQuoteToken}, {@code currency}, {@code compId}
     */
    public ResolvedCharge resolve(Long merchantOrgUnitId, Map<String, Object> body) {
        if (merchantOrgUnitId == null) {
            throw new IllegalArgumentException("NOT_FOUND");
        }
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(merchantOrgUnitId);
        return resolve(merchantOrgUnitId, body, opPg);
    }

    public ResolvedCharge resolve(Long merchantOrgUnitId, Map<String, Object> body, String operationalPgCd) {
        if (merchantOrgUnitId == null) {
            throw new IllegalArgumentException("NOT_FOUND");
        }
        String opPg = operationalPgCd != null ? operationalPgCd.trim() : "";
        if (opPg.isEmpty()) {
            opPg = chillPayService.resolveUrlPayOperationalPgCd(merchantOrgUnitId);
        }
        String pricingReq = str(body, "urlPayPricingMode");
        if (UrlPayDisplayFxService.MODE_DISPLAY_FX_THB.equalsIgnoreCase(pricingReq != null ? pricingReq : "")) {
            if (!UrlPayDisplayFxService.MODE_DISPLAY_FX_THB.equals(
                    chillPayService.resolveUrlPayPricingMode(merchantOrgUnitId))) {
                throw new IllegalArgumentException("DISPLAY_FX_NOT_ALLOWED");
            }
            if (!urlPayDisplayFxService.isHqFeatureEnabled()) {
                throw new IllegalArgumentException("DISPLAY_FX_HQ_DISABLED");
            }
            BigDecimal dispAmt = parsePayAmount(body.get("displayAmount"));
            if (dispAmt == null) {
                dispAmt = parsePayAmount(body.get("amount"));
            }
            if (dispAmt == null || dispAmt.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("INVALID_AMOUNT");
            }
            String compId = str(body, "compId");
            String fxTok = str(body, "fxQuoteToken");
            String dispCur = str(body, "displayCurrency");
            UrlPayDisplayFxService.FxComputedSettlement fx = urlPayDisplayFxService.computeSettlementFromQuote(
                    compId, dispCur, dispAmt, fxTok, opPg);
            String shopperCur = dispCur != null && !dispCur.isBlank()
                    ? dispCur.trim().toUpperCase(Locale.ROOT) : null;
            return new ResolvedCharge(fx.amount(), fx.settlementCurrency(), dispAmt, shopperCur);
        }
        BigDecimal displayAmount = parsePayAmount(body.get("amount"));
        if (displayAmount == null || displayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
        String bodyCur = str(body, "currency");
        String checkoutCurrencyCode = urlPayCheckoutCurrencyService.resolveCheckoutCurrency(
                merchantOrgUnitId, bodyCur);
        BigDecimal pgAmount = paymentCurrencyScaleService.toPgAmount(displayAmount, opPg, checkoutCurrencyCode);
        if (pgAmount == null || pgAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
        return new ResolvedCharge(pgAmount, checkoutCurrencyCode, null, null);
    }

    public static String failMessageForCode(String code) {
        if (code == null) {
            return "결제 금액을 확인할 수 없습니다.";
        }
        return switch (code) {
            case "NOT_FOUND" -> "가맹점을 찾을 수 없습니다.";
            case "DISPLAY_FX_NOT_ALLOWED" -> "표시통화 URL 결제가 아닌 가맹점입니다.";
            case "DISPLAY_FX_HQ_DISABLED" -> "본사 표시통화 설정이 비활성입니다.";
            case "INVALID_AMOUNT" -> "유효한 결제 금액을 입력하세요.";
            case "INVALID_FX_QUOTE" -> "환율 견적이 유효하지 않거나 만료되었습니다. 페이지를 새로고침한 뒤 다시 시도하세요.";
            default -> {
                if (code.startsWith("DISPLAY_FX") || code.contains("FX")) {
                    yield "환율 견적이 유효하지 않거나 만료되었습니다. 페이지를 새로고침한 뒤 다시 시도하세요.";
                }
                yield "결제 금액을 확인할 수 없습니다.";
            }
        };
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || key == null) {
            return "";
        }
        Object v = body.get(key);
        return v != null ? v.toString().trim() : "";
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
}
