package com.pg.service.settlement;

import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.MerchantReceivable;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementSetting;
import com.pg.repository.MerchantReceivableRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.CommissionService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.util.CommissionExtraFeeUtil;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.MerchantFeeVatUtil;
import com.pg.util.PayDisplayCurrency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 무효·환불 수수료 산식 버그(이중 과금 반영 오류)로 저장된 {@link SettlementRun} 금액을
 * 수수료내역과 동일한 {@link FeeListTxnBreakdownCalculator} 로 재산출해 보정합니다.
 * <p>거래·담보·환수 FIFO·미수금 FIFO는 재실행하지 않고, {@code total_fee}·{@code pay_amt} 및
 * 연동 미수금(자동 지급부족)만 델타로 조정합니다.</p>
 */
@Service
public class SettlementVoidRefundFeeCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(SettlementVoidRefundFeeCorrectionService.class);

    private static final Set<String> AFFECTED_STATUSES = Set.of(
            "21", "22", "30", "31", "40", "41", "42");

    private final SettlementRunRepository settlementRunRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator;
    private final CommissionService commissionService;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final DistributionFeeSnapshotApplier distributionFeeSnapshotApplier;
    private final MerchantReceivableRepository merchantReceivableRepository;
    private final SplitPayTxnFeeResolver splitPayTxnFeeResolver;

    public SettlementVoidRefundFeeCorrectionService(
            SettlementRunRepository settlementRunRepository,
            PgTrnsctnRepository pgTrnsctnRepository,
            FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator,
            CommissionService commissionService,
            OrgUnitRepository orgUnitRepository,
            SettlementSettingRepository settlementSettingRepository,
            HqLedgerSysSettingsService hqLedgerSysSettingsService,
            DistributionFeeSnapshotApplier distributionFeeSnapshotApplier,
            MerchantReceivableRepository merchantReceivableRepository,
            SplitPayTxnFeeResolver splitPayTxnFeeResolver) {
        this.settlementRunRepository = settlementRunRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.feeListTxnBreakdownCalculator = feeListTxnBreakdownCalculator;
        this.commissionService = commissionService;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.distributionFeeSnapshotApplier = distributionFeeSnapshotApplier;
        this.merchantReceivableRepository = merchantReceivableRepository;
        this.splitPayTxnFeeResolver = splitPayTxnFeeResolver;
    }

    public record CorrectionRow(
            long runId,
            String merchantId,
            LocalDate calcDt,
            BigDecimal oldTotalFee,
            BigDecimal newTotalFee,
            BigDecimal oldPayAmt,
            BigDecimal newPayAmt,
            boolean skipped,
            String note
    ) {}

    /**
     * @param calcDtFrom inclusive (null → 2020-01-01)
     * @param calcDtTo   inclusive (null → today)
     * @param merchantId optional filter
     * @param dryRun     true면 DB 미반영
     */
    @Transactional
    public Map<String, Object> correctStoredRuns(
            LocalDate calcDtFrom,
            LocalDate calcDtTo,
            String merchantId,
            boolean dryRun) {
        LocalDate from = calcDtFrom != null ? calcDtFrom : LocalDate.of(2020, 1, 1);
        LocalDate to = calcDtTo != null ? calcDtTo : LocalDate.now();
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("calcDtTo는 calcDtFrom 이후여야 합니다.");
        }
        String midFilter = merchantId != null ? merchantId.trim() : "";

        var ledgerSys = hqLedgerSysSettingsService.getOrCreate();
        FeeCurrencyRoundResolver feeResolver = FeeCurrencyRoundResolver.from(ledgerSys);
        FeeListRoundingPolicy ledgerRp = feeResolver.forCurrency(PayDisplayCurrency.alphaFromSettings(ledgerSys));

        List<SettlementRun> runs = settlementRunRepository.findByCalcDtBetweenOrderByMerchantId(from, to);
        List<CorrectionRow> changed = new ArrayList<>();
        List<CorrectionRow> skipped = new ArrayList<>();
        int scanned = 0;
        int updated = 0;

        for (SettlementRun run : runs) {
            if (run.getId() == null || run.getMerchantId() == null || run.getMerchantId().isBlank()) {
                continue;
            }
            String mid = run.getMerchantId().trim();
            if (!midFilter.isEmpty() && !mid.equalsIgnoreCase(midFilter)) {
                continue;
            }
            scanned++;
            List<PgTrnsctn> txList = pgTrnsctnRepository.findForSettlement(
                    mid, run.resolvePeriodStartAt(), run.resolvePeriodEndAt());
            if (!batchHasAffectedStatus(txList)) {
                skipped.add(new CorrectionRow(run.getId(), mid, run.getCalcDt(),
                        run.getTotalFee(), run.getTotalFee(), run.getPayAmt(), run.getPayAmt(),
                        true, "무효·환불 거래 없음"));
                continue;
            }
            Optional<CorrectionRow> rowOpt = buildCorrection(run, txList, feeResolver, ledgerRp, dryRun);
            if (rowOpt.isEmpty()) {
                skipped.add(new CorrectionRow(run.getId(), mid, run.getCalcDt(),
                        run.getTotalFee(), run.getTotalFee(), run.getPayAmt(), run.getPayAmt(),
                        true, "산출 불가"));
                continue;
            }
            CorrectionRow row = rowOpt.get();
            if (isZeroDelta(row.oldTotalFee(), row.newTotalFee()) && isZeroDelta(row.oldPayAmt(), row.newPayAmt())) {
                skipped.add(new CorrectionRow(row.runId(), row.merchantId(), row.calcDt(),
                        row.oldTotalFee(), row.newTotalFee(), row.oldPayAmt(), row.newPayAmt(),
                        true, "변경 없음"));
                continue;
            }
            changed.add(row);
            if (!dryRun) {
                updated++;
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dryRun", dryRun);
        out.put("calcDtFrom", from.toString());
        out.put("calcDtTo", to.toString());
        out.put("merchantFilter", midFilter.isEmpty() ? null : midFilter);
        out.put("scannedRuns", scanned);
        out.put("updatedRuns", updated);
        out.put("changedCount", changed.size());
        out.put("skippedCount", skipped.size());
        out.put("changed", changed.stream().limit(500).map(this::rowToMap).toList());
        if (skipped.size() > 200) {
            out.put("skippedSample", skipped.stream().limit(200).map(this::rowToMap).toList());
            out.put("skippedTruncated", true);
        } else {
            out.put("skipped", skipped.stream().map(this::rowToMap).toList());
        }
        out.put("note", "거래·담보·환수 FIFO는 재실행하지 않습니다. total_fee·pay_amt·유통분배 스냅샷·자동지급부족 미수금만 조정합니다.");
        log.info("void/refund fee correction dryRun={} scanned={} changed={} updated={}", dryRun, scanned, changed.size(), updated);
        return out;
    }

    private Optional<CorrectionRow> buildCorrection(
            SettlementRun run,
            List<PgTrnsctn> txList,
            FeeCurrencyRoundResolver feeResolver,
            FeeListRoundingPolicy ledgerRp,
            boolean dryRun) {
        String mid = run.getMerchantId().trim();
        CommissionPolicy policy = commissionService.resolveCommissionPolicyForSettlement(mid);
        if (policy == null) {
            policy = new CommissionPolicy();
            policy.setScope("DEFAULT");
            policy.setPayRate(BigDecimal.ZERO);
        }
        SettlementSetting feeVatSs = orgUnitRepository.findByCode(mid)
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                .orElse(null);

        BigDecimal newSumTxnFee = sumTxnFees(mid, txList, policy, feeVatSs, feeResolver, ledgerRp);
        BigDecimal feeExtraFix = CommissionExtraFeeUtil.sumFixedForSettlementRounded(policy, ledgerRp);
        BigDecimal feeUsage = inferUsageFeeOnRun(run, policy, ledgerRp, newSumTxnFee, feeExtraFix);
        BigDecimal newTotalFee = FeeListRoundingPolicy.round(newSumTxnFee.add(feeUsage).add(feeExtraFix), ledgerRp);

        BigDecimal oldTotalFee = nz(run.getTotalFee());
        BigDecimal batchFee = nz(run.getSettlementBatchFeeAmt());
        BigDecimal oldVat = MerchantFeeVatUtil.vatOnFeeAmount(oldTotalFee.add(batchFee), feeVatSs, ledgerRp.decimalPlaces());
        BigDecimal newVat = MerchantFeeVatUtil.vatOnFeeAmount(newTotalFee.add(batchFee), feeVatSs, ledgerRp.decimalPlaces());
        oldVat = FeeListRoundingPolicy.round(oldVat, ledgerRp);
        newVat = FeeListRoundingPolicy.round(newVat, ledgerRp);

        BigDecimal feeDelta = newTotalFee.subtract(oldTotalFee);
        BigDecimal vatDelta = newVat.subtract(oldVat);
        BigDecimal oldPay = nz(run.getPayAmt());
        BigDecimal newPay = FeeListRoundingPolicy.round(oldPay.subtract(feeDelta).subtract(vatDelta), ledgerRp);

        if (!dryRun) {
            run.setTotalFee(newTotalFee);
            run.setPayAmt(newPay);
            distributionFeeSnapshotApplier.stampOrgDistributionFees(run);
            settlementRunRepository.save(run);
            syncAutoDeficitReceivable(run, newPay, ledgerRp);
        }
        return Optional.of(new CorrectionRow(
                run.getId(), mid, run.getCalcDt(),
                oldTotalFee, newTotalFee, oldPay, newPay, false, null));
    }

    private BigDecimal sumTxnFees(
            String merchantId,
            List<PgTrnsctn> txList,
            CommissionPolicy policy,
            SettlementSetting feeVatSs,
            FeeCurrencyRoundResolver feeResolver,
            FeeListRoundingPolicy ledgerRp) {
        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        SplitPayTxnFeeResolver.InstallmentCache splitPayCache = splitPayTxnFeeResolver.buildCache(txList);
        BigDecimal acc = BigDecimal.ZERO;
        for (PgTrnsctn t : txList) {
            String rowCur = t.getCurType() != null && !t.getCurType().isBlank() ? t.getCurType().trim() : "KRW";
            FeeListRoundingPolicy rowRp = feeResolver.forCurrency(rowCur);
            FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                    t, merchantId, policy, monthCbCountCache, tiersByPolicyId, feeVatSs, rowRp, splitPayCache);
            acc = acc.add(FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), rowRp));
        }
        return FeeListRoundingPolicy.round(acc, ledgerRp);
    }

    /** 기존 실행에 월간 이용료가 포함됐으면 보정 후에도 동일하게 유지 */
    private BigDecimal inferUsageFeeOnRun(
            SettlementRun run,
            CommissionPolicy policy,
            FeeListRoundingPolicy lr,
            BigDecimal newSumTxnFee,
            BigDecimal feeExtraFix) {
        BigDecimal usageRate = policy.getUsageRate() != null ? policy.getUsageRate() : BigDecimal.ZERO;
        if (usageRate.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal oldTotal = nz(run.getTotalFee());
        BigDecimal withoutUsage = newSumTxnFee.add(feeExtraFix);
        BigDecimal impliedUsage = oldTotal.subtract(withoutUsage);
        if (impliedUsage.compareTo(usageRate.multiply(new BigDecimal("0.5"))) >= 0) {
            return FeeListRoundingPolicy.round(usageRate, lr);
        }
        return BigDecimal.ZERO;
    }

    private void syncAutoDeficitReceivable(SettlementRun run, BigDecimal newPay, FeeListRoundingPolicy rp) {
        if (newPay.signum() >= 0) {
            return;
        }
        String mid = run.getMerchantId().trim();
        String slotMemo = SettlementArrearsService.buildAutoDeficitSlotMemo(run, mid);
        Optional<MerchantReceivable> opt = merchantReceivableRepository.findByMerchantIdAndReasonCodeAndMemo(
                mid, SettlementArrearsService.REASON_AUTO_SETTLEMENT_DEFICIT, slotMemo);
        BigDecimal debt = FeeListRoundingPolicy.round(newPay.abs(), rp);
        if (opt.isEmpty()) {
            return;
        }
        MerchantReceivable r = opt.get();
        BigDecimal applied = nz(r.getAppliedAmount());
        BigDecimal newRemaining = debt.subtract(applied).max(BigDecimal.ZERO);
        r.setTotalAmount(debt);
        r.setRemainingAmount(newRemaining);
        if (newRemaining.signum() == 0) {
            r.setStatus("CLOSED");
        } else if (applied.signum() > 0) {
            r.setStatus("PARTIAL");
        } else {
            r.setStatus("PENDING");
        }
        merchantReceivableRepository.save(r);
    }

    private static boolean batchHasAffectedStatus(List<PgTrnsctn> txList) {
        if (txList == null || txList.isEmpty()) {
            return false;
        }
        for (PgTrnsctn t : txList) {
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            if (AFFECTED_STATUSES.contains(st)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isZeroDelta(BigDecimal a, BigDecimal b) {
        return nz(a).subtract(nz(b)).abs().compareTo(new BigDecimal("0.00000001")) <= 0;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private Map<String, Object> rowToMap(CorrectionRow row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("runId", row.runId());
        m.put("merchantId", row.merchantId());
        m.put("calcDt", row.calcDt() != null ? row.calcDt().toString() : "");
        m.put("oldTotalFee", row.oldTotalFee());
        m.put("newTotalFee", row.newTotalFee());
        m.put("oldPayAmt", row.oldPayAmt());
        m.put("newPayAmt", row.newPayAmt());
        m.put("skipped", row.skipped());
        if (row.note() != null) {
            m.put("note", row.note());
        }
        return m;
    }
}
