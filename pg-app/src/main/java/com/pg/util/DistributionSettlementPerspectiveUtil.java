package com.pg.util;

import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.OrgLevel;
import com.pg.entity.SettlementRun;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * 정산 실행 행의 유통 단계별 분배 스냅샷(또는 배분 설정)으로 로그인 조직 시점 금액을 산출합니다.
 */
public final class DistributionSettlementPerspectiveUtil {

    private static final OrgLevel[] TIERS = {
            OrgLevel.HEADQUARTERS,
            OrgLevel.REGIONAL,
            OrgLevel.MASTER_DIST,
            OrgLevel.BRANCH,
            OrgLevel.AGENCY,
            OrgLevel.SALES_OFFICE
    };

    private DistributionSettlementPerspectiveUtil() {
    }

    public record TierFeeMap(Map<OrgLevel, BigDecimal> tierFees) {
        public BigDecimal get(OrgLevel level) {
            if (level == null || tierFees == null) {
                return BigDecimal.ZERO;
            }
            return tierFees.getOrDefault(level, BigDecimal.ZERO);
        }
    }

    public record ViewerSlice(
            BigDecimal merchantTotalFee,
            BigDecimal upstreamDistFee,
            BigDecimal ownTierDistFee,
            BigDecimal downstreamDistFee,
            BigDecimal merchantPayAmt,
            BigDecimal passThroughDistFee
    ) {}

    public static TierFeeMap tierFeesFromRun(SettlementRun run,
                                             DistributionFeeConfig cfg,
                                             FeeListRoundingPolicy rp) {
        EnumMap<OrgLevel, BigDecimal> map = new EnumMap<>(OrgLevel.class);
        for (OrgLevel l : TIERS) {
            map.put(l, BigDecimal.ZERO);
        }
        if (run == null || rp == null) {
            return new TierFeeMap(map);
        }
        if (run.getDistHqFeeAmt() != null) {
            map.put(OrgLevel.HEADQUARTERS, nz(run.getDistHqFeeAmt()));
            map.put(OrgLevel.REGIONAL, nz(run.getDistRegionalFeeAmt()));
            map.put(OrgLevel.MASTER_DIST, nz(run.getDistMasterFeeAmt()));
            map.put(OrgLevel.BRANCH, nz(run.getDistBranchFeeAmt()));
            map.put(OrgLevel.AGENCY, nz(run.getDistAgencyFeeAmt()));
            map.put(OrgLevel.SALES_OFFICE, nz(run.getDistSalesOfficeFeeAmt()));
            return new TierFeeMap(map);
        }
        BigDecimal base = run.getPayAmt() != null ? run.getPayAmt() : BigDecimal.ZERO;
        if (cfg == null) {
            return new TierFeeMap(map);
        }
        map.put(OrgLevel.HEADQUARTERS, pctFee(base, cfg.getHqRate(), rp));
        map.put(OrgLevel.REGIONAL, pctFee(base, cfg.getRegionalRate(), rp));
        map.put(OrgLevel.MASTER_DIST, pctFee(base, cfg.getMasterRate(), rp));
        map.put(OrgLevel.BRANCH, pctFee(base, cfg.getBranchRate(), rp));
        map.put(OrgLevel.AGENCY, pctFee(base, cfg.getAgencyRate(), rp));
        map.put(OrgLevel.SALES_OFFICE, pctFee(base, cfg.getSalesOfficeRate(), rp));
        return new TierFeeMap(map);
    }

    public static ViewerSlice viewerSlice(TierFeeMap tiers,
                                          OrgLevel viewerLevel,
                                          SettlementRun run) {
        BigDecimal merchantFee = run != null && run.getTotalFee() != null ? run.getTotalFee() : BigDecimal.ZERO;
        BigDecimal payAmt = run != null && run.getPayAmt() != null ? run.getPayAmt() : BigDecimal.ZERO;
        if (viewerLevel == null || viewerLevel == OrgLevel.MERCHANT || tiers == null) {
            return new ViewerSlice(merchantFee, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, payAmt, merchantFee);
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
        BigDecimal distSum = upstream.add(own).add(downstream);
        BigDecimal passThrough = distSum.subtract(upstream);
        return new ViewerSlice(merchantFee, upstream, own, downstream, payAmt, passThrough);
    }

    private static BigDecimal pctFee(BigDecimal base, BigDecimal ratePct, FeeListRoundingPolicy rp) {
        if (base == null || ratePct == null || ratePct.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return FeeListRoundingPolicy.round(
                base.multiply(ratePct).divide(BigDecimal.valueOf(100), 16, RoundingMode.HALF_UP), rp);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
