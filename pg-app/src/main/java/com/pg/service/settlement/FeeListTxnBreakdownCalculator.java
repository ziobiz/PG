package com.pg.service.settlement;

import com.pg.entity.ChargebackFeePolicy;
import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementSetting;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.VoidRefundSettlementModeResolutionService;
import com.pg.util.ChargebackTierResolver;
import com.pg.util.CommissionExtraFeeUtil;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.MerchantFeeVatUtil;
import com.pg.util.PercentDecimalHelper;
import com.pg.util.VoidRefundSettlementModeUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 수수료내역·환수금액 산정 등에서 공유하는 거래 1건 수수료 분해.
 * 승인(10)에는 건당(perTx)과 결제% 등이 붙고, 실패(99/F0)에는 실패 고정만, 취소(20)에는 취소 고정만 붙는다.
 * 무효·환불 등은 순매출 반영 모드(GENERAL/REVENUE/HYBRID)에 따라 결제측 부가(건당·% 등)를 합산할지 정한다.
 */
@Component
public class FeeListTxnBreakdownCalculator {

    private static final List<String> CHARGEBACK_STATUSES = List.of("31");

    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final VoidRefundSettlementModeResolutionService voidRefundSettlementModeResolutionService;

    public FeeListTxnBreakdownCalculator(ChargebackFeePolicyRepository chargebackFeePolicyRepository,
                                         PgTrnsctnRepository pgTrnsctnRepository,
                                         VoidRefundSettlementModeResolutionService voidRefundSettlementModeResolutionService) {
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.voidRefundSettlementModeResolutionService = voidRefundSettlementModeResolutionService;
    }

    public record FeeListTxnBreakdown(
            double remittanceTransferFee,
            double usdtTransferFeeUsd,
            double perTxFee,
            double usageFee,
            double failFee,
            double cancelFee,
            double voidFee,
            double manualVoidFee,
            double refundFee,
            double payFee,
            double usdtFee,
            double fxFee,
            double fee3dsFee,
            double settlementPerTxFee,
            double chargebackFee,
            double extraFee1,
            double extraFee2,
            double extraFee3,
            double extraFee4,
            double rollingHoldEst,
            String rollingPctPlain,
            int rollingDays,
            double successFeesSeparate,
            double totalFee,
            BigDecimal feeVatBd
    ) {}

    public double resolveChargebackFee(
            PgTrnsctn t,
            String compId,
            CommissionPolicy pol,
            String st,
            Map<String, Long> monthCbCountCache,
            Map<Long, List<ChargebackFeeTier>> tiersByPolicyId) {
        if (!"31".equals(st)) {
            return 0d;
        }
        LocalDate cbDay = t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate() : LocalDate.now();
        YearMonth ymcb = YearMonth.from(cbDay);
        String ck = compId + "|" + ymcb;
        long monthCbCount = monthCbCountCache.computeIfAbsent(ck, k -> {
            LocalDateTime ms = ymcb.atDay(1).atStartOfDay();
            LocalDateTime me = ymcb.plusMonths(1).atDay(1).atStartOfDay();
            return pgTrnsctnRepository.countByMerchantIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    compId, CHARGEBACK_STATUSES, ms, me);
        });
        int mc = (int) Math.min(monthCbCount, Integer.MAX_VALUE);
        Long cpid = pol.getChargebackPolicyId();
        if (cpid != null) {
            List<ChargebackFeeTier> tiers = tiersByPolicyId.computeIfAbsent(cpid, id ->
                    chargebackFeePolicyRepository.findByIdWithTiers(id)
                            .map(ChargebackFeePolicy::getTiers)
                            .orElse(Collections.emptyList()));
            if (!tiers.isEmpty()) {
                return ChargebackTierResolver.feePerCaseForMonthlyCount(mc, tiers).doubleValue();
            }
            return nz(pol.getChargebackFeePerTx()).doubleValue();
        }
        return nz(pol.getChargebackFeePerTx()).doubleValue();
    }

    public FeeListTxnBreakdown computeFeeListTxnBreakdown(
            PgTrnsctn t,
            String compId,
            CommissionPolicy pol,
            Map<String, Long> monthCbCountCache,
            Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
            SettlementSetting merchantFeeVatSetting,
            FeeListRoundingPolicy rp) {
        BigDecimal amountBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        int feeScale = rp.decimalPlaces();
        RoundingMode feeRm = rp.roundMode();
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        String voidMode = voidRefundSettlementModeResolutionService.resolveVoidSettlementMode(compId);
        String manualVoidMode = voidRefundSettlementModeResolutionService.resolveManualVoidSettlementMode(compId);
        String refundMode = voidRefundSettlementModeResolutionService.resolveRefundSettlementMode(compId);
        String forceRefundMode = voidRefundSettlementModeResolutionService.resolveForceRefundSettlementMode(compId);
        double perTxFee = nz(pol.getPerTxFee()).doubleValue();
        double settlementPerTxFee = nz(pol.getFeeSettlementPerTx()).doubleValue();
        double usdtRemitUsd = nz(pol.getUsdtTransferFeeUsd()).doubleValue();
        double usageFee = 0d;
        double failFee = 0d;
        double cancelFee = 0d;
        double voidFee = 0d;
        double manualVoidFee = 0d;
        double refundFee = 0d;
        double chargebackFee = 0d;
        double payFee = 0d;
        double usdtFee = 0d;
        double fxFee = 0d;
        double fee3dsFee = 0d;
        double extraFee1 = 0d;
        double extraFee2 = 0d;
        double extraFee3 = 0d;
        double extraFee4 = 0d;
        double successFeesSeparate = 0d;
        String rollingPctPlain = PercentDecimalHelper.toPlainOneDecimal(nz(pol.getRollingPct()));
        int rollingDays = pol.getRollingDays() != null ? pol.getRollingDays() : 0;
        double rollingHoldEst = 0d;

        if ("F0".equals(st) || "99".equals(st)) {
            failFee = nz(pol.getFailFee()).doubleValue();
        } else if ("20".equals(st)) {
            cancelFee = nz(pol.getCancelRate()).doubleValue();
        } else if ("21".equals(st) || "22".equals(st) || "30".equals(st) || "31".equals(st)
                || "40".equals(st) || "41".equals(st) || "42".equals(st)) {
            if ("21".equals(st) || "40".equals(st)) {
                voidFee = nz(pol.getVoidFeePerTx()).doubleValue();
            } else if ("22".equals(st) || "41".equals(st)) {
                manualVoidFee = nz(pol.getManualVoidFeePerTx()).doubleValue();
            }
            if ("30".equals(st) || "42".equals(st)) {
                refundFee = nz(pol.getRefundRate()).doubleValue();
            } else if ("31".equals(st)) {
                chargebackFee = resolveChargebackFee(t, compId, pol, st, monthCbCountCache, tiersByPolicyId);
            }
            if (amountBd.signum() > 0) {
                payFee = amountBd.multiply(nz(pol.getPayRate())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                usdtFee = amountBd.multiply(nz(pol.getFeeUsdt())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                fxFee = amountBd.multiply(nz(pol.getFeeFx())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                fee3dsFee = FeeListRoundingPolicy.round(nz(pol.getFee3dsRate()), rp).doubleValue();
                extraFee1 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 1, amountBd, feeScale, feeRm).doubleValue();
                extraFee2 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 2, amountBd, feeScale, feeRm).doubleValue();
                extraFee3 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 3, amountBd, feeScale, feeRm).doubleValue();
                extraFee4 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 4, amountBd, feeScale, feeRm).doubleValue();
            }
            successFeesSeparate = perTxFee + payFee + usdtFee + fxFee + fee3dsFee
                    + extraFee1 + extraFee2 + extraFee3 + extraFee4;
        } else if ("10".equals(st)) {
            rollingHoldEst = amountBd.signum() > 0
                    ? amountBd.multiply(nz(pol.getRollingPct())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue()
                    : 0d;
            if (amountBd.signum() > 0) {
                payFee = amountBd.multiply(nz(pol.getPayRate())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                usdtFee = amountBd.multiply(nz(pol.getFeeUsdt())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                fxFee = amountBd.multiply(nz(pol.getFeeFx())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                fee3dsFee = FeeListRoundingPolicy.round(nz(pol.getFee3dsRate()), rp).doubleValue();
                extraFee1 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 1, amountBd, feeScale, feeRm).doubleValue();
                extraFee2 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 2, amountBd, feeScale, feeRm).doubleValue();
                extraFee3 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 3, amountBd, feeScale, feeRm).doubleValue();
                extraFee4 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 4, amountBd, feeScale, feeRm).doubleValue();
            }
        }

        /* 승인(10): 건당+결제% 등. 실패/취소: 해당 고정만. 무효·환불: 모드에 따라 결제측 부가(perTx·% 등) 포함 여부 */
        double totalFee;
        if ("10".equals(st)) {
            totalFee = Math.max(0d, perTxFee + usageFee + failFee + cancelFee + voidFee + manualVoidFee + refundFee
                    + payFee + usdtFee + fxFee + fee3dsFee + chargebackFee
                    + extraFee1 + extraFee2 + extraFee3 + extraFee4);
        } else if ("F0".equals(st) || "99".equals(st)) {
            totalFee = Math.max(0d, failFee);
        } else if ("20".equals(st)) {
            totalFee = Math.max(0d, cancelFee);
        } else if ("21".equals(st) || "40".equals(st)) {
            boolean addPaySide = VoidRefundSettlementModeUtil.subtractVoidAmountFromNet(voidMode);
            totalFee = Math.max(0d, voidFee + (addPaySide ? successFeesSeparate : 0d));
        } else if ("22".equals(st) || "41".equals(st)) {
            boolean addPaySide = VoidRefundSettlementModeUtil.subtractManualVoidAmountFromNet(manualVoidMode);
            totalFee = Math.max(0d, manualVoidFee + (addPaySide ? successFeesSeparate : 0d));
        } else if ("30".equals(st) || "42".equals(st)) {
            boolean addPaySide = VoidRefundSettlementModeUtil.subtractRefundAmountFromNet(refundMode);
            totalFee = Math.max(0d, refundFee + (addPaySide ? successFeesSeparate : 0d));
        } else if ("31".equals(st)) {
            boolean addPaySide = VoidRefundSettlementModeUtil.subtractForceRefundAmountFromNet(forceRefundMode);
            totalFee = Math.max(0d, chargebackFee + (addPaySide ? successFeesSeparate : 0d));
        } else {
            totalFee = 0d;
        }

        BigDecimal totalFeeBd = FeeListRoundingPolicy.round(BigDecimal.valueOf(totalFee), rp);
        totalFee = totalFeeBd.doubleValue();
        int vatScale = Math.max(0, feeScale);
        BigDecimal feeVatBd = FeeListRoundingPolicy.round(
                MerchantFeeVatUtil.vatOnFeeAmount(totalFeeBd, merchantFeeVatSetting, vatScale), rp);
        return new FeeListTxnBreakdown(0d, usdtRemitUsd, perTxFee, usageFee, failFee, cancelFee, voidFee, manualVoidFee, refundFee,
                payFee, usdtFee, fxFee, fee3dsFee, settlementPerTxFee, chargebackFee,
                extraFee1, extraFee2, extraFee3, extraFee4, rollingHoldEst, rollingPctPlain, rollingDays, successFeesSeparate, totalFee, feeVatBd);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
