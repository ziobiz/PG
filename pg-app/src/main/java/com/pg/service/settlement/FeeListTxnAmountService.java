package com.pg.service.settlement;

import com.pg.api.dto.PayListRowContext;
import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementSetting;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayListStatusBarBuckets;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 수수료내역·통합 리포트 상세 등에서 동일한 건별 총수수료·부가세·정산액을 계산합니다.
 * {@link FeeListTxnBreakdownCalculator} + {@link com.pg.controller.api.ApiSettlementController} 의
 * {@code buildFeeListRowMap} 차감·지급 규칙과 동일합니다.
 */
@Service
public class FeeListTxnAmountService {

    private final FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator;

    public FeeListTxnAmountService(FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator) {
        this.feeListTxnBreakdownCalculator = feeListTxnBreakdownCalculator;
    }

    public record FeeListTxnAmounts(
            BigDecimal totalFee,
            BigDecimal feeVat,
            BigDecimal expectedPayout,
            BigDecimal settlementAmt,
            BigDecimal rollingHoldEst
    ) {}

    public FeeListTxnAmounts compute(PgTrnsctn t,
                                     PayListRowContext payCtx,
                                     CommissionPolicy pol,
                                     String payCurKey,
                                     FeeCurrencyRoundResolver feeResolver,
                                     Map<String, Long> monthCbCountCache,
                                     Map<Long, List<ChargebackFeeTier>> tiersByPolicyId) {
        if (t == null || pol == null || feeResolver == null) {
            return zeroAmounts(feeResolver);
        }
        String compId = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        SettlementSetting feeVatSs = payCtx != null ? payCtx.getSettlement() : null;
        String cur = payCurKey != null && !payCurKey.isBlank() ? payCurKey.trim()
                : (t.getCurType() != null && !t.getCurType().isBlank() ? t.getCurType().trim() : "KRW");
        FeeListRoundingPolicy feeListRp = feeResolver.forCurrency(cur);
        BigDecimal amountBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                t, compId, pol, monthCbCountCache, tiersByPolicyId, feeVatSs, feeListRp);
        String stRow = t.getStatus() != null ? t.getStatus().trim() : "";
        BigDecimal totalFeeMag = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), feeListRp);
        BigDecimal feeVatMag = FeeListRoundingPolicy.round(br.feeVatBd(), feeListRp);
        final BigDecimal totalFeeBd;
        final BigDecimal feeVatOut;
        final BigDecimal expectedPayoutBd;
        if ("10".equals(stRow)) {
            totalFeeBd = totalFeeMag;
            feeVatOut = feeVatMag;
            expectedPayoutBd = FeeListRoundingPolicy.round(amountBd.subtract(totalFeeBd).subtract(feeVatOut), feeListRp);
        } else if (isFeeListMerchantDeductionStatus(stRow)) {
            expectedPayoutBd = FeeListRoundingPolicy.round(BigDecimal.ZERO, feeListRp);
            totalFeeBd = totalFeeMag;
            feeVatOut = feeVatMag;
        } else {
            totalFeeBd = totalFeeMag;
            feeVatOut = feeVatMag;
            expectedPayoutBd = FeeListRoundingPolicy.round(amountBd.subtract(totalFeeBd).subtract(feeVatOut), feeListRp);
        }
        BigDecimal rollingHoldEstBd = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.rollingHoldEst()), feeListRp);
        final BigDecimal settlementAmtBd;
        if ("10".equals(stRow)) {
            settlementAmtBd = FeeListRoundingPolicy.round(expectedPayoutBd.subtract(rollingHoldEstBd), feeListRp);
        } else if (isFeeListMerchantDeductionStatus(stRow)) {
            settlementAmtBd = FeeListRoundingPolicy.round(totalFeeMag.add(feeVatMag).negate(), feeListRp);
        } else {
            settlementAmtBd = FeeListRoundingPolicy.round(expectedPayoutBd.subtract(rollingHoldEstBd), feeListRp);
        }
        return new FeeListTxnAmounts(totalFeeBd, feeVatOut, expectedPayoutBd, settlementAmtBd, rollingHoldEstBd);
    }

    private static FeeListTxnAmounts zeroAmounts(FeeCurrencyRoundResolver feeResolver) {
        FeeListRoundingPolicy rp = feeResolver != null ? feeResolver.fallback() : FeeListRoundingPolicy.defaults();
        BigDecimal z = FeeListRoundingPolicy.round(BigDecimal.ZERO, rp);
        return new FeeListTxnAmounts(z, z, z, z, z);
    }

    /**
     * 통합 리포트 상태별(성공·취소·실패 …) 금액 = 건수 × 정책 건당(고정) 수수료.
     * 성공=건당(perTx), 실패=failFee, 취소=cancelRate, 무효=voidFeePerTx, 이메일무효=manualVoidFeePerTx,
     * 환불=refundRate, 강제환불=chargebackFeePerTx.
     */
    public static BigDecimal policyFixedFeeForStatusBucket(String bucket, CommissionPolicy pol) {
        if (pol == null || bucket == null || bucket.isBlank()) {
            return BigDecimal.ZERO;
        }
        return switch (bucket) {
            case PayListStatusBarBuckets.SUCCESS -> nz(pol.getPerTxFee());
            case PayListStatusBarBuckets.FAIL -> nz(pol.getFailFee());
            case PayListStatusBarBuckets.CANCEL -> nz(pol.getCancelRate());
            case PayListStatusBarBuckets.VOID -> nz(pol.getVoidFeePerTx());
            case PayListStatusBarBuckets.EMAIL_VOID -> nz(pol.getManualVoidFeePerTx());
            case PayListStatusBarBuckets.REFUND -> nz(pol.getRefundRate());
            case PayListStatusBarBuckets.FORCE_REFUND -> nz(pol.getChargebackFeePerTx());
            default -> BigDecimal.ZERO;
        };
    }

    public static BigDecimal roundPolicyFixedFeeForBucket(String bucket,
                                                          CommissionPolicy pol,
                                                          FeeCurrencyRoundResolver feeResolver) {
        if (feeResolver == null) {
            return policyFixedFeeForStatusBucket(bucket, pol);
        }
        String cur = pol != null && pol.getCurrencyCode() != null && !pol.getCurrencyCode().isBlank()
                ? pol.getCurrencyCode().trim().toUpperCase(java.util.Locale.ROOT) : "KRW";
        FeeListRoundingPolicy rp = feeResolver.forCurrency(cur);
        return FeeListRoundingPolicy.round(policyFixedFeeForStatusBucket(bucket, pol), rp);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** 수수료내역 가맹점 차감 행 — 취소·무효·환불·실패 등 */
    public static boolean isFeeListMerchantDeductionStatus(String st) {
        if (st == null || st.isBlank()) {
            return false;
        }
        return switch (st.trim()) {
            case "20", "21", "22", "30", "31", "40", "41", "42", "F0", "99" -> true;
            default -> false;
        };
    }
}
