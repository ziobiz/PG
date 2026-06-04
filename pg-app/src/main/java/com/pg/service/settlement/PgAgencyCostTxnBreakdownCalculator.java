package com.pg.service.settlement;

import com.pg.entity.ChargebackFeePolicy;
import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.PgAgencyCostPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.ChargebackTierResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PercentDecimalHelper;
import com.pg.util.PgAgencyCostExtraFeeUtil;
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
 * 대행거래내역 — PG 계약 원가({@link PgAgencyCostPolicy}) 기준 건별 수수료 분해(수수료내역 산식과 동형, VAT·가맹 정산 제외).
 */
@Component
public class PgAgencyCostTxnBreakdownCalculator {

    private static final List<String> CHARGEBACK_STATUSES = List.of("31");

    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;

    public PgAgencyCostTxnBreakdownCalculator(ChargebackFeePolicyRepository chargebackFeePolicyRepository,
                                              PgTrnsctnRepository pgTrnsctnRepository) {
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    public record AgencyCostTxnBreakdown(
            double perTxFee,
            double failFee,
            double cancelFee,
            double voidFee,
            double manualVoidFee,
            double refundFee,
            double payFee,
            double usdtFee,
            double fxFee,
            double fee3dsFee,
            double chargebackFee,
            double extraFee1,
            double extraFee2,
            double extraFee3,
            double extraFee4,
            double rollingHoldEst,
            String rollingPctPlain,
            int rollingDays,
            double successFeesSeparate,
            double totalAgencyFee
    ) {}

    public AgencyCostTxnBreakdown compute(PgTrnsctn t,
                                          String compId,
                                          PgAgencyCostPolicy pol,
                                          Map<String, Long> monthCbCountCache,
                                          Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
                                          FeeListRoundingPolicy rp) {
        BigDecimal amountBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        int feeScale = rp.decimalPlaces();
        RoundingMode feeRm = rp.roundMode();
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        String voidMode = pol.getVoidSettlementMode();
        String manualVoidMode = pol.getManualVoidSettlementMode();
        String refundMode = pol.getRefundSettlementMode();
        String forceRefundMode = pol.getForceRefundSettlementMode();

        double perTxFee = nz(pol.getPerTxFee()).doubleValue();
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
                chargebackFee = resolveChargebackFee(t, compId, pol, monthCbCountCache, tiersByPolicyId);
            }
            if (amountBd.signum() > 0) {
                payFee = pctOf(amountBd, pol.getPayRate(), feeScale, feeRm);
                usdtFee = pctOf(amountBd, pol.getFeeUsdt(), feeScale, feeRm);
                fxFee = pctOf(amountBd, pol.getFeeFx(), feeScale, feeRm);
                fee3dsFee = FeeListRoundingPolicy.round(nz(pol.getFee3dsRate()), rp).doubleValue();
                extraFee1 = PgAgencyCostExtraFeeUtil.pctSlotAmountOnApproved(pol, 1, amountBd, feeScale, feeRm).doubleValue();
                extraFee2 = PgAgencyCostExtraFeeUtil.pctSlotAmountOnApproved(pol, 2, amountBd, feeScale, feeRm).doubleValue();
                extraFee3 = PgAgencyCostExtraFeeUtil.pctSlotAmountOnApproved(pol, 3, amountBd, feeScale, feeRm).doubleValue();
                extraFee4 = PgAgencyCostExtraFeeUtil.pctSlotAmountOnApproved(pol, 4, amountBd, feeScale, feeRm).doubleValue();
            }
            successFeesSeparate = perTxFee + payFee + usdtFee + fxFee + fee3dsFee
                    + extraFee1 + extraFee2 + extraFee3 + extraFee4;
        } else if ("10".equals(st)) {
            rollingHoldEst = amountBd.signum() > 0
                    ? pctOf(amountBd, pol.getRollingPct(), feeScale, feeRm) : 0d;
            if (amountBd.signum() > 0) {
                payFee = pctOf(amountBd, pol.getPayRate(), feeScale, feeRm);
                usdtFee = pctOf(amountBd, pol.getFeeUsdt(), feeScale, feeRm);
                fxFee = pctOf(amountBd, pol.getFeeFx(), feeScale, feeRm);
                fee3dsFee = FeeListRoundingPolicy.round(nz(pol.getFee3dsRate()), rp).doubleValue();
                extraFee1 = PgAgencyCostExtraFeeUtil.pctSlotAmountOnApproved(pol, 1, amountBd, feeScale, feeRm).doubleValue();
                extraFee2 = PgAgencyCostExtraFeeUtil.pctSlotAmountOnApproved(pol, 2, amountBd, feeScale, feeRm).doubleValue();
                extraFee3 = PgAgencyCostExtraFeeUtil.pctSlotAmountOnApproved(pol, 3, amountBd, feeScale, feeRm).doubleValue();
                extraFee4 = PgAgencyCostExtraFeeUtil.pctSlotAmountOnApproved(pol, 4, amountBd, feeScale, feeRm).doubleValue();
            }
        }

        double totalFee;
        if ("10".equals(st)) {
            totalFee = Math.max(0d, perTxFee + failFee + cancelFee + voidFee + manualVoidFee + refundFee
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
        return new AgencyCostTxnBreakdown(perTxFee, failFee, cancelFee, voidFee, manualVoidFee, refundFee,
                payFee, usdtFee, fxFee, fee3dsFee, chargebackFee,
                extraFee1, extraFee2, extraFee3, extraFee4, rollingHoldEst, rollingPctPlain, rollingDays,
                successFeesSeparate, totalFeeBd.doubleValue());
    }

    private double resolveChargebackFee(PgTrnsctn t,
                                        String compId,
                                        PgAgencyCostPolicy pol,
                                        Map<String, Long> monthCbCountCache,
                                        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId) {
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

    private static double pctOf(BigDecimal amount, BigDecimal rate, int scale, RoundingMode rm) {
        return amount.multiply(nz(rate)).divide(BigDecimal.valueOf(100), scale, rm).doubleValue();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
