package com.pg.service.settlement;

import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.SettlementRun;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayDisplayCurrency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 가맹 정산 실행의 <strong>최종 지급액</strong>(미수·환수 반영 후)을 기준으로
 * {@link com.pg.entity.DistributionFeeConfig} 요율에 따른 유통 단계별 분배액을 {@link SettlementRun}에 저장합니다.
 * 유통망정산내역은 실행 행을 조직 단위로 합산할 때 이 스냅샷을 우선 사용합니다.
 */
@Component
public class DistributionFeeSnapshotApplier {

    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;

    public DistributionFeeSnapshotApplier(
            DistributionFeeConfigRepository distributionFeeConfigRepository,
            HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository) {
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
    }

    public void stampOrgDistributionFees(SettlementRun run) {
        if (run == null) {
            return;
        }
        String mid = run.getMerchantId();
        if (mid == null || mid.isBlank()) {
            return;
        }
        FeeListRoundingPolicy rp = hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(s -> FeeCurrencyRoundResolver.from(s).forCurrency(PayDisplayCurrency.alphaFromSettings(s)))
                .orElseGet(FeeListRoundingPolicy::defaults);
        BigDecimal base = run.getPayAmt() != null ? run.getPayAmt() : BigDecimal.ZERO;
        DistributionFeeConfig cfg = distributionFeeConfigRepository.findByCompId(mid.trim()).orElse(null);
        run.setDistHqFeeAmt(pctFeeBd(base, cfg != null ? cfg.getHqRate() : null, rp));
        run.setDistRegionalFeeAmt(pctFeeBd(base, cfg != null ? cfg.getRegionalRate() : null, rp));
        run.setDistMasterFeeAmt(pctFeeBd(base, cfg != null ? cfg.getMasterRate() : null, rp));
        run.setDistBranchFeeAmt(pctFeeBd(base, cfg != null ? cfg.getBranchRate() : null, rp));
        run.setDistAgencyFeeAmt(pctFeeBd(base, cfg != null ? cfg.getAgencyRate() : null, rp));
        run.setDistSalesOfficeFeeAmt(pctFeeBd(base, cfg != null ? cfg.getSalesOfficeRate() : null, rp));
    }

    private static BigDecimal pctFeeBd(BigDecimal base, BigDecimal ratePct, FeeListRoundingPolicy rp) {
        if (base == null || ratePct == null || rp == null || ratePct.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return FeeListRoundingPolicy.round(
                base.multiply(ratePct).divide(BigDecimal.valueOf(100), 16, RoundingMode.HALF_UP), rp);
    }
}
