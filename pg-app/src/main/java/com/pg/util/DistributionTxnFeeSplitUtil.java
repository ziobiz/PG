package com.pg.util;

import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.OrgLevel;
import com.pg.service.settlement.FeeListTxnBreakdownCalculator.FeeListTxnBreakdown;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * 수수료내역 건별 총수수료를 본사설정 수수료설정(유통 배분) 기준 6단계로 분해합니다.
 * 배분 설정({@link DistributionFeeConfig})의 결제%·건당 구간과 건별 breakdown을 맞춥니다.
 */
public final class DistributionTxnFeeSplitUtil {

    private static final OrgLevel[] TIERS = {
            OrgLevel.HEADQUARTERS,
            OrgLevel.REGIONAL,
            OrgLevel.MASTER_DIST,
            OrgLevel.BRANCH,
            OrgLevel.AGENCY,
            OrgLevel.SALES_OFFICE
    };

    private DistributionTxnFeeSplitUtil() {
    }

    public record TierFeeMap(Map<OrgLevel, BigDecimal> tierFees) {
        public BigDecimal get(OrgLevel level) {
            if (level == null || tierFees == null) {
                return BigDecimal.ZERO;
            }
            return tierFees.getOrDefault(level, BigDecimal.ZERO);
        }
    }

    /** 로그인 조직 시점에서의 수수료 구간 합계 */
    public record ViewerSlice(
            BigDecimal merchantTotalFee,
            BigDecimal upstreamFee,
            BigDecimal ownTierFee,
            BigDecimal downstreamFee,
            BigDecimal passThroughFee
    ) {}

    public static TierFeeMap splitTxnFees(DistributionFeeConfig cfg,
                                          FeeListTxnBreakdown br,
                                          BigDecimal txnAmt,
                                          FeeListRoundingPolicy rp) {
        EnumMap<OrgLevel, BigDecimal> map = new EnumMap<>(OrgLevel.class);
        for (OrgLevel l : TIERS) {
            map.put(l, BigDecimal.ZERO);
        }
        if (cfg == null || br == null || rp == null) {
            return new TierFeeMap(map);
        }
        BigDecimal[] perTx = tierPerTxArray(cfg);
        BigDecimal[] payRate = tierPayRateArray(cfg);

        double perTxSum = br.perTxFee() + br.settlementPerTxFee();
        if (perTxSum > 0) {
            addArrayToTiers(map, perTx, rp);
        }

        double pctSum = br.usageFee() + br.payFee() + br.splitPayPctFee();
        BigDecimal baseAmt = txnAmt != null ? txnAmt : BigDecimal.ZERO;
        if (pctSum > 0 && baseAmt.signum() > 0) {
            for (int i = 0; i < TIERS.length; i++) {
                if (payRate[i].signum() > 0) {
                    BigDecimal slice = FeeListRoundingPolicy.round(
                            baseAmt.multiply(payRate[i]).divide(BigDecimal.valueOf(100), 16, RoundingMode.HALF_UP),
                            rp);
                    map.merge(TIERS[i], slice, BigDecimal::add);
                }
            }
        }

        double hqOnly = br.usdtFee() + br.fxFee() + br.fee3dsFee() + br.remittanceTransferFee()
                + br.usdtTransferFeeUsd() + br.chargebackFee()
                + br.extraFee1() + br.extraFee2() + br.extraFee3() + br.extraFee4();
        if (hqOnly > 0) {
            map.merge(OrgLevel.HEADQUARTERS,
                    FeeListRoundingPolicy.round(BigDecimal.valueOf(hqOnly), rp),
                    BigDecimal::add);
        }

        double proportional = br.failFee() + br.cancelFee() + br.voidFee() + br.manualVoidFee()
                + br.refundFee() + br.splitPayFixedFee();
        if (proportional > 0) {
            distributeProportional(map, BigDecimal.valueOf(proportional), perTx, payRate, baseAmt, rp);
        }

        reconcileToTotal(map, br.totalFee(), rp);
        return new TierFeeMap(map);
    }

    public static ViewerSlice viewerSlice(TierFeeMap tiers, OrgLevel viewerLevel, BigDecimal merchantTotalFee) {
        BigDecimal total = merchantTotalFee != null ? merchantTotalFee : BigDecimal.ZERO;
        if (viewerLevel == null || viewerLevel == OrgLevel.MERCHANT || tiers == null) {
            return new ViewerSlice(total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, total);
        }
        int viewerCode = viewerLevel.getCode();
        BigDecimal upstream = BigDecimal.ZERO;
        BigDecimal own = BigDecimal.ZERO;
        BigDecimal downstream = BigDecimal.ZERO;
        for (OrgLevel l : TIERS) {
            BigDecimal v = tiers.get(l);
            if (l.getCode() < viewerCode) {
                upstream = upstream.add(v);
            } else if (l.getCode() == viewerCode) {
                own = own.add(v);
            } else {
                downstream = downstream.add(v);
            }
        }
        BigDecimal passThrough = total.subtract(upstream);
        return new ViewerSlice(total, upstream, own, downstream, passThrough);
    }

    public static BigDecimal tierFeeForLevel(TierFeeMap tiers, OrgLevel level) {
        return tiers != null ? tiers.get(level) : BigDecimal.ZERO;
    }

    private static BigDecimal[] tierPerTxArray(DistributionFeeConfig cfg) {
        return new BigDecimal[] {
                nz(cfg.getHqPerTxFee()),
                nz(cfg.getRegionalPerTxFee()),
                nz(cfg.getMasterPerTxFee()),
                nz(cfg.getBranchPerTxFee()),
                nz(cfg.getAgencyPerTxFee()),
                nz(cfg.getSalesOfficePerTxFee())
        };
    }

    private static BigDecimal[] tierPayRateArray(DistributionFeeConfig cfg) {
        return new BigDecimal[] {
                nz(cfg.getHqRate()),
                nz(cfg.getRegionalRate()),
                nz(cfg.getMasterRate()),
                nz(cfg.getBranchRate()),
                nz(cfg.getAgencyRate()),
                nz(cfg.getSalesOfficeRate())
        };
    }

    private static void addArrayToTiers(EnumMap<OrgLevel, BigDecimal> map, BigDecimal[] perTx, FeeListRoundingPolicy rp) {
        for (int i = 0; i < TIERS.length; i++) {
            if (perTx[i].signum() > 0) {
                map.merge(TIERS[i], FeeListRoundingPolicy.round(perTx[i], rp), BigDecimal::add);
            }
        }
    }

    private static void distributeProportional(EnumMap<OrgLevel, BigDecimal> map,
                                               BigDecimal amount,
                                               BigDecimal[] perTx,
                                               BigDecimal[] payRate,
                                               BigDecimal txnAmt,
                                               FeeListRoundingPolicy rp) {
        BigDecimal[] weights = new BigDecimal[TIERS.length];
        BigDecimal weightSum = BigDecimal.ZERO;
        for (int i = 0; i < TIERS.length; i++) {
            BigDecimal w = perTx[i];
            if (txnAmt != null && txnAmt.signum() > 0 && payRate[i].signum() > 0) {
                w = w.add(txnAmt.multiply(payRate[i]).divide(BigDecimal.valueOf(100), 16, RoundingMode.HALF_UP));
            }
            weights[i] = w.max(BigDecimal.ZERO);
            weightSum = weightSum.add(weights[i]);
        }
        if (weightSum.signum() == 0) {
            map.merge(OrgLevel.HEADQUARTERS, FeeListRoundingPolicy.round(amount, rp), BigDecimal::add);
            return;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        int lastIdx = -1;
        for (int i = 0; i < TIERS.length; i++) {
            if (weights[i].signum() <= 0) {
                continue;
            }
            lastIdx = i;
            BigDecimal slice = FeeListRoundingPolicy.round(
                    amount.multiply(weights[i]).divide(weightSum, 16, RoundingMode.HALF_UP), rp);
            map.merge(TIERS[i], slice, BigDecimal::add);
            allocated = allocated.add(slice);
        }
        if (lastIdx >= 0) {
            BigDecimal delta = FeeListRoundingPolicy.round(amount, rp).subtract(allocated);
            if (delta.signum() != 0) {
                map.merge(TIERS[lastIdx], delta, BigDecimal::add);
            }
        }
    }

    private static void reconcileToTotal(EnumMap<OrgLevel, BigDecimal> map, double totalFee, FeeListRoundingPolicy rp) {
        BigDecimal target = FeeListRoundingPolicy.round(BigDecimal.valueOf(totalFee), rp);
        BigDecimal sum = BigDecimal.ZERO;
        for (OrgLevel l : TIERS) {
            sum = sum.add(map.getOrDefault(l, BigDecimal.ZERO));
        }
        BigDecimal delta = target.subtract(sum);
        if (delta.signum() != 0) {
            map.merge(OrgLevel.HEADQUARTERS, delta, BigDecimal::add);
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
