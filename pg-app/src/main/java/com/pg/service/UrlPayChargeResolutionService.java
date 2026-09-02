package com.pg.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * URL·API·챗봇·분할결제 공통 청구 금액·통화 해석 (전 PG).
 * <ul>
 *   <li>표시 금액(JPY 등)은 가맹/구매자 계약 — prepare·화면에서 고정</li>
 *   <li>실결제는 라우팅된 운영 PG의 금액 모드로 확정
 *       ({@link UrlPayDisplayFxService#MODE_DISPLAY_FX_THB} → FX, 일반 → 표시통화 1:1)</li>
 *   <li>혼용 가맹: UI는 DP 통일, V/M→일반 PG는 JPY, DP PG는 THB 등</li>
 * </ul>
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
        String modeForPg = chillPayService.resolveUrlPayPricingModeForPg(merchantOrgUnitId, opPg);
        boolean merchantDp = chillPayService.merchantAllowsDisplayFx(merchantOrgUnitId);
        String pricingReq = str(body, "urlPayPricingMode");
        boolean requestDp = UrlPayDisplayFxService.MODE_DISPLAY_FX_THB.equalsIgnoreCase(
                pricingReq != null ? pricingReq : "");
        boolean routedDp = UrlPayDisplayFxService.MODE_DISPLAY_FX_THB.equalsIgnoreCase(modeForPg);

        BigDecimal dispAmt = parsePayAmount(body.get("displayAmount"));
        if (dispAmt == null) {
            dispAmt = parsePayAmount(body.get("amount"));
        }
        String dispCur = str(body, "displayCurrency");
        if (dispCur.isEmpty()) {
            dispCur = str(body, "currency");
        }

        /* DP UI 요청 또는 라우팅 PG가 DP — 표시금액 기준 */
        if (requestDp || routedDp) {
            if (!merchantDp && !routedDp) {
                throw new IllegalArgumentException("DISPLAY_FX_NOT_ALLOWED");
            }
            if (!urlPayDisplayFxService.isHqFeatureEnabled() && routedDp) {
                throw new IllegalArgumentException("DISPLAY_FX_HQ_DISABLED");
            }
            if (dispAmt == null || dispAmt.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("INVALID_AMOUNT");
            }
            String shopperCur = !dispCur.isBlank()
                    ? dispCur.trim().toUpperCase(Locale.ROOT)
                    : urlPayCheckoutCurrencyService.resolveCheckoutCurrency(merchantOrgUnitId, null);

            if (routedDp) {
                if (!urlPayDisplayFxService.isHqFeatureEnabled()) {
                    throw new IllegalArgumentException("DISPLAY_FX_HQ_DISABLED");
                }
                String compId = str(body, "compId");
                String fxTok = str(body, "fxQuoteToken");
                if (fxTok.isBlank()) {
                    throw new IllegalArgumentException("INVALID_FX_QUOTE");
                }
                String quoteCur = !dispCur.isBlank() ? dispCur : shopperCur;
                UrlPayDisplayFxService.FxComputedSettlement fx = urlPayDisplayFxService.computeSettlementFromQuote(
                        compId, quoteCur, dispAmt, fxTok, opPg);
                return new ResolvedCharge(fx.amount(), fx.settlementCurrency(), dispAmt, shopperCur);
            }
            /* 혼용: UI는 DP·이 PG는 일반 — 표시통화로 1:1 실결제 */
            String settleCur = shopperCur != null && !shopperCur.isBlank()
                    ? shopperCur
                    : urlPayCheckoutCurrencyService.resolveCheckoutCurrency(merchantOrgUnitId, dispCur);
            BigDecimal pgAmount = paymentCurrencyScaleService.toPgAmount(dispAmt, opPg, settleCur);
            if (pgAmount == null || pgAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("INVALID_AMOUNT");
            }
            return new ResolvedCharge(pgAmount, settleCur, dispAmt, settleCur);
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
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static BigDecimal parsePayAmount(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            if (raw instanceof BigDecimal bd) {
                return bd;
            }
            if (raw instanceof Number n) {
                return BigDecimal.valueOf(n.doubleValue());
            }
            String s = String.valueOf(raw).trim().replace(",", "");
            if (s.isEmpty()) {
                return null;
            }
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }
}
