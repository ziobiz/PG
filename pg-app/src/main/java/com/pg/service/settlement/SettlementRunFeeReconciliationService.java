package com.pg.service.settlement;

import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementSetting;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.SettlementCalcService;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayDisplayCurrency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 정산 실행 행의 저장 {@link SettlementRun#getTotalFee()} 와
 * 수수료내역용 {@link FeeListTxnBreakdownCalculator} 건별 합(보조)을 비교합니다.
 * 월 이용료·집계 라운딩·통화별 표시 등으로 차이가 날 수 있어 소프트 검증입니다.
 */
@Service
public class SettlementRunFeeReconciliationService {

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator;
    private final SettlementCalcService settlementCalcService;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    public SettlementRunFeeReconciliationService(
            PgTrnsctnRepository pgTrnsctnRepository,
            FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator,
            SettlementCalcService settlementCalcService,
            OrgUnitRepository orgUnitRepository,
            SettlementSettingRepository settlementSettingRepository,
            HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.feeListTxnBreakdownCalculator = feeListTxnBreakdownCalculator;
        this.settlementCalcService = settlementCalcService;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    public Map<String, Object> reconcile(SettlementRun run) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();
        notes.add("건당·차지백 월집계 등은 정산 집계와 표시 단위가 달라 소액 차이가 날 수 있습니다. 정산수수료·송금수수료는 실행당 1회입니다.");
        if (run == null || run.getMerchantId() == null || run.getMerchantId().isBlank()) {
            out.put("skipped", true);
            out.put("reason", "no_merchant");
            out.put("notes", notes);
            return out;
        }
        String mid = run.getMerchantId().trim();
        CommissionPolicy pol = settlementCalcService.getPolicy(mid);
        if (pol == null) {
            pol = new CommissionPolicy();
            pol.setScope("DEFAULT");
            pol.setPayRate(new BigDecimal("2.5"));
        }

        var ledgerSys = hqLedgerSysSettingsService.getOrCreate();
        FeeListRoundingPolicy ledgerRp = FeeCurrencyRoundResolver.from(ledgerSys)
                .forCurrency(PayDisplayCurrency.alphaFromSettings(ledgerSys));
        FeeCurrencyRoundResolver feeResolver = FeeCurrencyRoundResolver.from(ledgerSys);

        SettlementSetting feeVatSs = orgUnitRepository.findByCode(mid)
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                .orElse(null);

        Map<String, Long> monthCbCountCache = new LinkedHashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new LinkedHashMap<>();

        List<PgTrnsctn> txs = pgTrnsctnRepository.findForSettlement(mid, run.resolvePeriodStartAt(), run.resolvePeriodEndAt());

        boolean legacyRun = run.getSettlementBatchFeeAmt() == null && run.getRemittanceFeeAmt() == null;
        BigDecimal sumLines;
        BigDecimal runFee;
        if (legacyRun) {
            BigDecimal acc = BigDecimal.ZERO;
            for (PgTrnsctn t : txs) {
                String rowCur = t.getCurType() != null && !t.getCurType().isBlank() ? t.getCurType().trim() : "KRW";
                FeeListRoundingPolicy rowRp = feeResolver.forCurrency(rowCur);
                FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                        t, mid, pol, monthCbCountCache, tiersByPolicyId, feeVatSs, rowRp);
                BigDecimal rowTotal = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), rowRp);
                BigDecimal rowSettle = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.settlementPerTxFee()), rowRp);
                acc = acc.add(rowTotal).add(rowSettle);
            }
            sumLines = FeeListRoundingPolicy.round(acc, ledgerRp);
            runFee = FeeListRoundingPolicy.round(run.getTotalFee() != null ? run.getTotalFee() : BigDecimal.ZERO, ledgerRp);
        } else {
            BigDecimal sumTxnFees = BigDecimal.ZERO;
            for (PgTrnsctn t : txs) {
                String rowCur = t.getCurType() != null && !t.getCurType().isBlank() ? t.getCurType().trim() : "KRW";
                FeeListRoundingPolicy rowRp = feeResolver.forCurrency(rowCur);
                FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                        t, mid, pol, monthCbCountCache, tiersByPolicyId, feeVatSs, rowRp);
                BigDecimal rowTotal = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), rowRp);
                sumTxnFees = sumTxnFees.add(rowTotal);
            }
            sumTxnFees = FeeListRoundingPolicy.round(sumTxnFees, ledgerRp);
            BigDecimal policySettleOnce = FeeListRoundingPolicy.round(
                    pol.getFeeSettlementPerTx() != null ? pol.getFeeSettlementPerTx() : BigDecimal.ZERO, ledgerRp);
            BigDecimal policyRemitOnce = FeeListRoundingPolicy.round(
                    pol.getRemittanceTransferFee() != null ? pol.getRemittanceTransferFee() : BigDecimal.ZERO, ledgerRp);
            sumLines = FeeListRoundingPolicy.round(sumTxnFees.add(policySettleOnce).add(policyRemitOnce), ledgerRp);
            BigDecimal runTxnFee = FeeListRoundingPolicy.round(run.getTotalFee() != null ? run.getTotalFee() : BigDecimal.ZERO, ledgerRp);
            BigDecimal runSettle = FeeListRoundingPolicy.round(run.getSettlementBatchFeeAmt(), ledgerRp);
            BigDecimal runRemit = FeeListRoundingPolicy.round(run.getRemittanceFeeAmt(), ledgerRp);
            runFee = FeeListRoundingPolicy.round(runTxnFee.add(runSettle).add(runRemit), ledgerRp);
        }
        BigDecimal diff = runFee.subtract(sumLines).abs();

        int scale = Math.max(0, ledgerRp.decimalPlaces());
        BigDecimal unit = BigDecimal.ONE.scaleByPowerOfTen(-scale);
        BigDecimal eps = unit.multiply(BigDecimal.valueOf(Math.max(2, 1 + txs.size() / 20)));

        boolean usageExplains = false;
        BigDecimal usageRate = pol.getUsageRate() != null ? pol.getUsageRate() : BigDecimal.ZERO;
        if (usageRate.signum() > 0) {
            BigDecimal usageRounded = FeeListRoundingPolicy.round(usageRate, ledgerRp);
            if (diff.subtract(usageRounded).abs().compareTo(eps) <= 0) {
                usageExplains = true;
                notes.add("차이가 월 이용료(usage) 근처입니다. 집계 거래수수료(total_fee)에는 포함되나 건별 합에는 없습니다.");
            }
        }

        boolean ok = diff.compareTo(eps) <= 0 || usageExplains;
        out.put("skipped", false);
        out.put("txnCount", txs.size());
        out.put("runTotalFee", runFee);
        out.put("detailFeeSum", sumLines);
        out.put("diffAbs", diff);
        out.put("tolerance", eps);
        out.put("ok", ok);
        out.put("usageLikelyExplainsDiff", usageExplains);
        out.put("notes", notes);
        return out;
    }
}
