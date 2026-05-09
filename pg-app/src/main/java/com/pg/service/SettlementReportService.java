package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementSetting;
import com.pg.entity.SettlementRun;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.settlement.FeeListTxnBreakdownCalculator;
import com.pg.service.settlement.SettlementRunTargetPeriodLabelService;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.MerchantFeeVatUtil;
import com.pg.util.PayDisplayCurrency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 정산시트(집계·실시·집계표) 리포트 — PDF 기준 비율·건당요금은 상수(추후 HQ 설정으로 이전 가능).
 */
@Service
public class SettlementReportService {

    /** 예치(롤링) 비율 — 시트 예: 10%, 180일 보관(표시용) */
    public static final BigDecimal DEPOSIT_PCT = new BigDecimal("0.10");
    /** Processing fee 합계 5.6% = PG 4.2% + 파일 0.1% + 온라인 1.3% */
    public static final BigDecimal PROC_PCT_TOTAL = new BigDecimal("0.056");
    public static final BigDecimal PROC_PCT_PG = new BigDecimal("0.042");
    public static final BigDecimal PROC_PCT_FILE = new BigDecimal("0.001");
    public static final BigDecimal PROC_PCT_ONLINE = new BigDecimal("0.013");
    /** 건당(원) — 시트 예시값, 통화 KRW 기준 */
    public static final long FEE_PER_APPROVE = 50L;
    public static final long FEE_PER_REFUND = 300L;
    /** 정산 건당(배치) 수수료 — 시트 예시 */
    public static final long FEE_SETTLEMENT_BATCH = 11_000L;

    private final OrgUnitRepository orgUnitRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final SettlementCalcService settlementCalcService;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;
    private final ReceivableRecoveryModeService receivableRecoveryModeService;
    private final SettlementSettingRepository settlementSettingRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator;
    private final SettlementRunTargetPeriodLabelService settlementRunTargetPeriodLabelService;

    public SettlementReportService(OrgUnitRepository orgUnitRepository,
                                   PgTrnsctnRepository pgTrnsctnRepository,
                                   SettlementCalcService settlementCalcService,
                                   CommissionPolicyRepository commissionPolicyRepository,
                                   HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository,
                                   ReceivableRecoveryModeService receivableRecoveryModeService,
                                   SettlementSettingRepository settlementSettingRepository,
                                   MerchantProfileRepository merchantProfileRepository,
                                   FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator,
                                   SettlementRunTargetPeriodLabelService settlementRunTargetPeriodLabelService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.settlementCalcService = settlementCalcService;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
        this.receivableRecoveryModeService = receivableRecoveryModeService;
        this.settlementSettingRepository = settlementSettingRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.feeListTxnBreakdownCalculator = feeListTxnBreakdownCalculator;
        this.settlementRunTargetPeriodLabelService = settlementRunTargetPeriodLabelService;
    }

    private FeeListRoundingPolicy settlementLedgerRoundPolicy() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(s -> FeeCurrencyRoundResolver.from(s).forCurrency(PayDisplayCurrency.alphaFromSettings(s)))
                .orElseGet(FeeListRoundingPolicy::defaults);
    }

    private static double settlementMoneyDouble(BigDecimal bd, FeeListRoundingPolicy rp) {
        if (bd == null) {
            return 0d;
        }
        return FeeListRoundingPolicy.round(bd, rp).doubleValue();
    }

    /** 가맹 정산·리포트 표시용 통화(수수료 정책 통화코드, 없으면 KRW) */
    private String resolveStatementCurrency(String compId) {
        if (compId == null || compId.isBlank()) {
            return "KRW";
        }
        return commissionPolicyRepository.findByScope(compId.trim())
                .map(CommissionPolicy::getCurrencyCode)
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().toUpperCase(Locale.ROOT))
                .orElse("KRW");
    }

    private FeeCurrencyRoundResolver feeCurrencyRoundResolver() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(FeeCurrencyRoundResolver::from)
                .orElseGet(() -> FeeCurrencyRoundResolver.from(null));
    }

    private record MdrPerTxnSplit(double mdrAmt, double perTxnAmt) {}

    /**
     * 수수료내역과 동일한 건별 분해: MDR(결제%·USDT·FX) 합 + 건당(그 외 수수료) 합.
     */
    private MdrPerTxnSplit computeMdrAndPerTxnFeeForRun(SettlementRun r, String compId) {
        if (compId == null || compId.isBlank() || r == null) {
            return new MdrPerTxnSplit(0d, 0d);
        }
        CommissionPolicy pol = resolveCommissionPolicyForMerchantRow(compId.trim());
        if (pol == null) {
            return new MdrPerTxnSplit(0d, 0d);
        }
        LocalDateTime fromDt = r.resolvePeriodStartAt();
        LocalDateTime toDt = r.resolvePeriodEndAt();
        List<PgTrnsctn> txs = pgTrnsctnRepository.findForSettlement(compId.trim(), fromDt, toDt);
        FeeCurrencyRoundResolver feeResolver = feeCurrencyRoundResolver();
        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        SettlementSetting feeVatSs = orgUnitRepository.findByCode(compId.trim())
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                .orElse(null);
        double mdrSum = 0d;
        double perSum = 0d;
        for (PgTrnsctn t : txs) {
            String rowCur = t.getCurType() != null && !t.getCurType().isBlank() ? t.getCurType().trim() : "KRW";
            FeeListRoundingPolicy feeListRp = feeResolver.forCurrency(rowCur);
            FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                    t, compId.trim(), pol, monthCbCountCache, tiersByPolicyId, feeVatSs, feeListRp);
            double mdrOne = br.payFee() + br.usdtFee() + br.fxFee();
            double nonPct = br.totalFee() - br.payFee() - br.usdtFee() - br.fxFee();
            if (nonPct < 0d) {
                nonPct = 0d;
            }
            mdrSum += mdrOne;
            perSum += nonPct;
        }
        return new MdrPerTxnSplit(mdrSum, perSum);
    }

    private double feeVatForRun(SettlementRun r, String compId) {
        FeeListRoundingPolicy ledgerRp = settlementLedgerRoundPolicy();
        SettlementSetting feeVatSs = orgUnitRepository.findByCode(compId != null ? compId.trim() : "")
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                .orElse(null);
        BigDecimal vatFeeBase = nz(r.getTotalFee());
        if (r.getSettlementBatchFeeAmt() != null) {
            vatFeeBase = vatFeeBase.add(nz(r.getSettlementBatchFeeAmt()));
        }
        BigDecimal feeVat = MerchantFeeVatUtil.vatOnFeeAmount(vatFeeBase, feeVatSs, ledgerRp.decimalPlaces());
        return settlementMoneyDouble(feeVat, ledgerRp);
    }

    /** 리포트 통화 열: 정책 통화(THB/KRW/USD/JPY 등 알파코드). */
    private String displayCurrencyForMerchant(String merchantId) {
        String c = resolveStatementCurrency(merchantId);
        if (c == null || c.isBlank()) {
            return "KRW";
        }
        return c.trim().toUpperCase(Locale.ROOT);
    }

    private Map<String, Object> baseAggregateRowFromRun(SettlementRun r, String compId, OrgUnit ou) {
        FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
        BigDecimal ap = nz(r.getApproveAmt());
        BigDecimal ca = nz(r.getCancelAmt());
        BigDecimal net = ap.subtract(ca);
        String cycleFb = settlementRunTargetPeriodLabelService.settlementCycleFallbackForMerchant(compId);
        MdrPerTxnSplit split = computeMdrAndPerTxnFeeForRun(r, compId);
        double mdrRounded = settlementMoneyDouble(BigDecimal.valueOf(split.mdrAmt()), rp);
        double perRounded = settlementMoneyDouble(BigDecimal.valueOf(split.perTxnAmt()), rp);

        Map<String, Object> m = new LinkedHashMap<>();
        if (r.getCalcDt() != null) {
            m.put("calcDt", r.getCalcDt().toString());
        } else {
            m.put("calcDt", "");
        }
        if (r.getId() != null) {
            m.put("settlementRunId", r.getId());
        }
        m.put("targetPeriodText", settlementRunTargetPeriodLabelService.buildSettlementTargetPeriodLabel(r, cycleFb));
        m.put("calcCycle", displayCalcCycleForExecuteRow(r, compId));
        m.put("compId", compId);
        m.put("compNm", ou != null ? ou.getName() : compId);
        m.put("curType", displayCurrencyForMerchant(compId));
        m.put("grossPay", settlementMoneyDouble(ap, rp));
        m.put("refundAmt", settlementMoneyDouble(ca, rp));
        m.put("netPay", settlementMoneyDouble(net, rp));
        m.put("rollingReserveAmt", settlementMoneyDouble(r.getRollingReserveAmt(), rp));
        m.put("mdrFeeAmt", mdrRounded);
        m.put("perTxnFeeAmt", perRounded);
        m.put("settlementBatchFee", r.getSettlementBatchFeeAmt() != null
                ? settlementMoneyDouble(r.getSettlementBatchFeeAmt(), rp) : null);
        m.put("feeVat", feeVatForRun(r, compId));
        m.putAll(remittanceFieldsForRun(r));
        m.put("payAmount", settlementMoneyDouble(r.getPayAmt(), rp));
        m.put("totalFee", settlementMoneyDouble(r.getTotalFee(), rp));
        m.put("settlementDueDt", r.getCalcDt() != null ? r.getCalcDt().toString() : "");
        m.put("settledYn", "CALCULATED".equalsIgnoreCase(String.valueOf(r.getStatus())) ? "Y" : "N");
        m.put("status", r.getStatus());
        m.put("txnCnt", r.getIncludedTxnCnt());
        return m;
    }

    /**
     * 정산실시 행·확정 리포트 상세용: 송금(정책통화)·USDT 송금 차감 표기 및 최종 지급액.
     * DB {@link SettlementRun#getRemittanceFeeAmt()}가 채워져 있으면 우선 사용합니다.
     */
    public Map<String, Object> remittanceFieldsForRun(SettlementRun r) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (r == null || r.getMerchantId() == null || r.getMerchantId().isBlank()) {
            m.put("remittanceFeeBank", null);
            m.put("remittanceFeeUsdt", null);
            m.put("finalPayAfterRemittance", null);
            m.put("remittanceFee", null);
            return m;
        }
        applyExecuteRemittanceAndFinalPay(m, r, r.getMerchantId().trim(), settlementLedgerRoundPolicy());
        return m;
    }

    private String resolveMerchantCalcCycleRaw(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return "";
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantId.trim());
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(merchantId.trim());
        }
        if (ou.isEmpty()) {
            return "";
        }
        return settlementSettingRepository.findByOrgUnitId(ou.get().getId())
                .map(ss -> ss.getCalcCycle() != null ? ss.getCalcCycle().trim() : "")
                .orElse("");
    }

    private String displayCalcCycleForExecuteRow(SettlementRun r, String merchantId) {
        if (r.getCalcCycleSnapshot() != null && !r.getCalcCycleSnapshot().isBlank()) {
            return r.getCalcCycleSnapshot().trim();
        }
        return resolveMerchantCalcCycleRaw(merchantId);
    }

    /** 정책통화가 USDT이거나 가맹 기준통화에 USDT가 포함되면 USDT 송금 건당 정책을 사용합니다. */
    private boolean merchantUsesUsdtRemittanceFee(String merchantId) {
        String stmt = resolveStatementCurrency(merchantId);
        if ("USDT".equalsIgnoreCase(stmt)) {
            return true;
        }
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantId.trim());
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(merchantId.trim());
        }
        return ou.flatMap(o -> merchantProfileRepository.findByOrgUnitId(o.getId()))
                .map(mp -> {
                    String bc = mp.getBaseCurrency();
                    if (bc == null || bc.isBlank()) {
                        return false;
                    }
                    return bc.toUpperCase(Locale.ROOT).contains("USDT");
                })
                .orElse(false);
    }

    private CommissionPolicy resolveCommissionPolicyForMerchantRow(String merchantId) {
        if (merchantId != null && !merchantId.isBlank()) {
            Optional<CommissionPolicy> direct = commissionPolicyRepository.findByScope(merchantId.trim());
            if (direct.isPresent()) {
                return direct.get();
            }
        }
        return commissionPolicyRepository.findByScope("DEFAULT").orElse(null);
    }

    private void applyExecuteRemittanceAndFinalPay(Map<String, Object> m, SettlementRun r, String merchantId, FeeListRoundingPolicy rp) {
        BigDecimal payBd = nz(r.getPayAmt());
        BigDecimal storedRemit = r.getRemittanceFeeAmt();
        if (storedRemit != null && storedRemit.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal absStored = storedRemit.abs();
            double remD = settlementMoneyDouble(absStored, rp);
            boolean usdt = merchantUsesUsdtRemittanceFee(merchantId);
            if (usdt) {
                m.put("remittanceFeeBank", null);
                m.put("remittanceFeeUsdt", -remD);
            } else {
                m.put("remittanceFeeBank", -remD);
                m.put("remittanceFeeUsdt", null);
            }
            m.put("finalPayAfterRemittance", settlementMoneyDouble(payBd.subtract(absStored), rp));
            m.put("remittanceFee", -remD);
            return;
        }
        CommissionPolicy pol = resolveCommissionPolicyForMerchantRow(merchantId);
        boolean usdt = merchantUsesUsdtRemittanceFee(merchantId);
        if (usdt && pol != null && pol.getUsdtTransferFeeUsd() != null
                && pol.getUsdtTransferFeeUsd().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fee = FeeListRoundingPolicy.round(pol.getUsdtTransferFeeUsd(), rp);
            m.put("remittanceFeeBank", null);
            m.put("remittanceFeeUsdt", -settlementMoneyDouble(fee, rp));
            m.put("finalPayAfterRemittance", settlementMoneyDouble(payBd.subtract(fee), rp));
            m.put("remittanceFee", -settlementMoneyDouble(fee, rp));
            return;
        }
        if (!usdt && pol != null && pol.getRemittanceTransferFee() != null
                && pol.getRemittanceTransferFee().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fee = FeeListRoundingPolicy.round(pol.getRemittanceTransferFee(), rp);
            m.put("remittanceFeeBank", -settlementMoneyDouble(fee, rp));
            m.put("remittanceFeeUsdt", null);
            m.put("finalPayAfterRemittance", settlementMoneyDouble(payBd.subtract(fee), rp));
            m.put("remittanceFee", -settlementMoneyDouble(fee, rp));
            return;
        }
        // 표시 정책: 송금수수료가 없으면 빈칸이 아니라 0으로 내려 화면에 0이 보이게 함(정산리포트·TAX 리포트 공통)
        m.put("remittanceFeeBank", 0);
        m.put("remittanceFeeUsdt", 0);
        m.put("finalPayAfterRemittance", settlementMoneyDouble(payBd, rp));
        m.put("remittanceFee", 0);
    }

    public Map<String, Object> reportMeta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("depositPct", DEPOSIT_PCT.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString());
        m.put("depositHoldDays", 180);
        m.put("processingPctTotal", PROC_PCT_TOTAL.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString());
        m.put("processingPctPg", PROC_PCT_PG.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString());
        m.put("processingPctFile", PROC_PCT_FILE.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString());
        m.put("processingPctOnline", PROC_PCT_ONLINE.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString());
        m.put("feePerApproveKrw", FEE_PER_APPROVE);
        m.put("feePerRefundKrw", FEE_PER_REFUND);
        m.put("feeSettlementBatchKrw", FEE_SETTLEMENT_BATCH);
        m.put("settlementDueBusinessDays", 0);
        m.put("note", "지급예정일은 해당 정산 실행의 정산일(정산주기 일자)과 동일합니다. 정산집계·실시의 비율형·건당·VAT는 수수료내역 계산과 동일 규칙입니다.");
        return m;
    }

    public static final String KIND_MERCHANT_STMT = "MERCHANT_STMT";
    /** 총본사 → 본사(REGIONAL) 지급용 집계 */
    public static final String KIND_REGIONAL_PAYOUT = "REGIONAL_PAYOUT";

    public boolean canAccessMerchantStmt(OrgUnit org) {
        return org != null && org.getOrgLevel() != null;
    }

    /** 총본사·본사(REGIONAL) 로그인만 본사 지급 리포트 */
    public boolean canAccessRegionalPayout(OrgUnit org) {
        if (org == null || org.getOrgLevel() == null) return false;
        OrgLevel l = org.getOrgLevel();
        return l == OrgLevel.HEADQUARTERS || l == OrgLevel.REGIONAL;
    }

    public Map<String, Object> reportAccessMeta(Optional<OrgUnit> userOrg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("merchantStmt", userOrg.isPresent() && canAccessMerchantStmt(userOrg.get()));
        m.put("regionalPayout", userOrg.isPresent() && canAccessRegionalPayout(userOrg.get()));
        userOrg.ifPresent(o -> m.put("orgLevel", o.getOrgLevel().name()));
        return m;
    }

    private static String normalizeReportKind(String reportKind) {
        if (reportKind == null || reportKind.isBlank()) return KIND_MERCHANT_STMT;
        return reportKind.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 정산집계: {@link #KIND_MERCHANT_STMT} 가맹 단위 / {@link #KIND_REGIONAL_PAYOUT} 본사(REGIONAL) 단위(총본사→본사 지급).
     */
    public PageResult<Map<String, Object>> aggregate(
            Optional<OrgUnit> userOrgOpt,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchMerchantId,
            String searchMasterId,
            String searchRegionalId,
            String curType,
            String reportKind,
            int page,
            int size) {
        String kind = normalizeReportKind(reportKind);
        if (KIND_REGIONAL_PAYOUT.equals(kind)) {
            if (userOrgOpt.isEmpty() || !canAccessRegionalPayout(userOrgOpt.get())) {
                return emptyPage(page, size);
            }
            return aggregateRegionalPayout(userOrgOpt.get(), searchFromDate, searchToDate, searchRegionalId, curType, page, size);
        }
        if (userOrgOpt.isEmpty() || !canAccessMerchantStmt(userOrgOpt.get())) {
            return emptyPage(page, size);
        }
        return aggregateMerchant(userOrgOpt.get(), searchFromDate, searchToDate, searchMerchantId, searchMasterId, curType, page, size);
    }

    /**
     * 가맹점 정산 리포트 집계: 정산 실행 단위(정산대상기간·실행과 동일).
     */
    private PageResult<Map<String, Object>> aggregateMerchant(
            OrgUnit userOrg,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchMerchantId,
            String searchMasterId,
            String curType,
            int page,
            int size) {
        Set<String> allowed = resolveAllowedMerchantCodes(userOrg, searchMerchantId, searchMasterId);
        if (allowed.isEmpty()) {
            return emptyPage(page, size);
        }
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        String ct = curType != null ? curType.trim().toUpperCase(Locale.ROOT) : "";

        List<SettlementRun> runs = settlementCalcService.listRuns(fromDate, toDate);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SettlementRun r : runs) {
            if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
                continue;
            }
            if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
                continue;
            }
            String mid = r.getMerchantId();
            if (mid == null || !allowed.contains(mid)) {
                continue;
            }
            if (!ct.isEmpty() && !ct.equals(displayCurrencyForMerchant(mid))) {
                continue;
            }
            OrgUnit ou = orgUnitRepository.findByCode(mid).orElse(null);
            rows.add(baseAggregateRowFromRun(r, mid, ou));
        }
        rows.sort(Comparator
                .<Map<String, Object>, String>comparing(x -> String.valueOf(x.getOrDefault("calcDt", "")))
                .reversed()
                .thenComparing(x -> String.valueOf(x.getOrDefault("compId", ""))));
        return pageOf(rows, page, size);
    }

    /**
     * 본사 지급 리포트 집계: 정산 실행 단위 행에 본사(REGIONAL) 열을 붙임.
     */
    private PageResult<Map<String, Object>> aggregateRegionalPayout(
            OrgUnit userOrg,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchRegionalId,
            String curType,
            int page,
            int size) {
        List<OrgUnit> all = orgUnitRepository.findAll();
        Map<Long, OrgUnit> idToOu = all.stream().collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));
        Set<String> allowedRegional = resolveAllowedRegionalCodes(userOrg, all);
        if (searchRegionalId != null && !searchRegionalId.isBlank()) {
            String rid = searchRegionalId.trim();
            if (!allowedRegional.contains(rid)) {
                return emptyPage(page, size);
            }
            allowedRegional = Set.of(rid);
        }
        if (allowedRegional.isEmpty()) {
            return emptyPage(page, size);
        }
        Set<String> allowedMerchants = resolveAllowedMerchantCodes(userOrg, null, null, all);
        if (allowedMerchants.isEmpty()) {
            return emptyPage(page, size);
        }

        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        String ct = curType != null ? curType.trim().toUpperCase(Locale.ROOT) : "";

        List<SettlementRun> runs = settlementCalcService.listRuns(fromDate, toDate);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SettlementRun r : runs) {
            if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
                continue;
            }
            if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
                continue;
            }
            String mid = r.getMerchantId();
            if (mid == null || !allowedMerchants.contains(mid)) {
                continue;
            }
            OrgUnit merchantOu = orgUnitRepository.findByCode(mid).orElse(null);
            if (merchantOu == null || merchantOu.getOrgLevel() != OrgLevel.MERCHANT) {
                continue;
            }
            OrgUnit regional = findRegionalAncestor(merchantOu, idToOu);
            if (regional == null || regional.getCode() == null || !allowedRegional.contains(regional.getCode())) {
                continue;
            }
            if (!ct.isEmpty() && !ct.equals(displayCurrencyForMerchant(mid))) {
                continue;
            }
            OrgUnit regOu = orgUnitRepository.findByCode(regional.getCode()).orElse(regional);
            Map<String, Object> m = baseAggregateRowFromRun(r, mid, merchantOu);
            m.put("regionalCompId", regional.getCode());
            m.put("regionalNm", regOu.getName() != null ? regOu.getName() : regional.getCode());
            m.put("merchantCnt", 1);
            rows.add(m);
        }
        rows.sort(Comparator
                .<Map<String, Object>, String>comparing(x -> String.valueOf(x.getOrDefault("calcDt", "")))
                .reversed()
                .thenComparing(x -> String.valueOf(x.getOrDefault("regionalCompId", "")))
                .thenComparing(x -> String.valueOf(x.getOrDefault("compId", ""))));
        return pageOf(rows, page, size);
    }

    /**
     * 정산실시: 배치 정산 실행 결과 — 가맹 단위 또는 본사(REGIONAL) 합산.
     */
    public PageResult<Map<String, Object>> executeReport(
            Optional<OrgUnit> userOrgOpt,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchMerchantId,
            String searchMasterId,
            String searchRegionalId,
            String reportKind,
            int page,
            int size) {
        String kind = normalizeReportKind(reportKind);
        if (KIND_REGIONAL_PAYOUT.equals(kind)) {
            if (userOrgOpt.isEmpty() || !canAccessRegionalPayout(userOrgOpt.get())) {
                return emptyPage(page, size);
            }
            return executeReportRegional(userOrgOpt.get(), searchFromDate, searchToDate, searchRegionalId, page, size);
        }
        if (userOrgOpt.isEmpty() || !canAccessMerchantStmt(userOrgOpt.get())) {
            return emptyPage(page, size);
        }
        return executeReportMerchant(userOrgOpt.get(), searchFromDate, searchToDate, searchMerchantId, searchMasterId, page, size);
    }

    private PageResult<Map<String, Object>> executeReportMerchant(
            OrgUnit userOrg,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchMerchantId,
            String searchMasterId,
            int page,
            int size) {
        Set<String> allowed = resolveAllowedMerchantCodes(userOrg, searchMerchantId, searchMasterId);
        if (allowed.isEmpty()) {
            return emptyPage(page, size);
        }
        List<SettlementRun> runs = settlementCalcService.listRuns(searchFromDate, searchToDate);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SettlementRun r : runs) {
            if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
                continue;
            }
            if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
                continue;
            }
            String mid = r.getMerchantId();
            if (mid == null || !allowed.contains(mid)) continue;
            OrgUnit ou = orgUnitRepository.findByCode(mid).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            LocalDate calcDt = r.getCalcDt();
            String cycleFb = settlementRunTargetPeriodLabelService.settlementCycleFallbackForMerchant(mid);
            m.put("calcDt", calcDt != null ? calcDt.toString() : "");
            if (r.getId() != null) {
                m.put("settlementRunId", r.getId());
            }
            m.put("compId", mid);
            m.put("compNm", ou != null ? ou.getName() : mid);
            m.put("curType", displayCurrencyForMerchant(mid));
            FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
            BigDecimal ap = nz(r.getApproveAmt());
            BigDecimal ca = nz(r.getCancelAmt());
            BigDecimal net = ap.subtract(ca);
            m.put("targetPeriodText", settlementRunTargetPeriodLabelService.buildSettlementTargetPeriodLabel(r, cycleFb));
            m.put("approveAmt", settlementMoneyDouble(ap, rp));
            m.put("cancelAmt", settlementMoneyDouble(ca, rp));
            m.put("netPay", settlementMoneyDouble(net, rp));
            MdrPerTxnSplit split = computeMdrAndPerTxnFeeForRun(r, mid);
            m.put("mdrFeeAmt", settlementMoneyDouble(BigDecimal.valueOf(split.mdrAmt()), rp));
            m.put("perTxnFeeAmt", settlementMoneyDouble(BigDecimal.valueOf(split.perTxnAmt()), rp));
            m.put("payAmount", settlementMoneyDouble(r.getPayAmt(), rp));
            m.put("totalFee", settlementMoneyDouble(r.getTotalFee(), rp));
            m.put("rollingReserveAmt", settlementMoneyDouble(r.getRollingReserveAmt(), rp));
            m.put("settlementBatchFee", r.getSettlementBatchFeeAmt() != null
                    ? settlementMoneyDouble(r.getSettlementBatchFeeAmt(), rp) : null);
            m.put("feeVat", feeVatForRun(r, mid));
            m.put("calcCycle", displayCalcCycleForExecuteRow(r, mid));
            m.putAll(remittanceFieldsForRun(r));
            m.put("txnCnt", r.getIncludedTxnCnt());
            m.put("status", r.getStatus());
            m.put("settlementDueDt", calcDt != null ? calcDt.toString() : "");
            m.put("settledYn", "CALCULATED".equalsIgnoreCase(String.valueOf(r.getStatus())) ? "Y" : "N");
            boolean manualRecv = receivableRecoveryModeService.isManualForMerchantCode(mid);
            m.put("receivableRecoveryMode", manualRecv ? "MANUAL" : "AUTO");
            m.put("receivableRecoveryModeKr", manualRecv ? "수동(환수처리 후 차감)" : "자동(차기정산 FIFO)");
            m.put("receivableAppliedAmt", settlementMoneyDouble(
                    r.getReceivableAppliedAmt() != null ? r.getReceivableAppliedAmt() : BigDecimal.ZERO, rp));
            rows.add(m);
        }
        rows.sort(Comparator
                .<Map<String, Object>, String>comparing(x -> String.valueOf(x.getOrDefault("calcDt", "")))
                .reversed()
                .thenComparing(x -> String.valueOf(x.getOrDefault("compId", ""))));
        return pageOf(rows, page, size);
    }

    private PageResult<Map<String, Object>> executeReportRegional(
            OrgUnit userOrg,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchRegionalId,
            int page,
            int size) {
        List<OrgUnit> all = orgUnitRepository.findAll();
        Map<Long, OrgUnit> idToOu = all.stream().collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));
        Set<String> allowedRegional = resolveAllowedRegionalCodes(userOrg, all);
        if (searchRegionalId != null && !searchRegionalId.isBlank()) {
            String rid = searchRegionalId.trim();
            if (!allowedRegional.contains(rid)) {
                return emptyPage(page, size);
            }
            allowedRegional = Set.of(rid);
        }
        if (allowedRegional.isEmpty()) {
            return emptyPage(page, size);
        }
        Set<String> allowedMerchants = resolveAllowedMerchantCodes(userOrg, null, null, all);
        if (allowedMerchants.isEmpty()) {
            return emptyPage(page, size);
        }

        List<SettlementRun> runs = settlementCalcService.listRuns(searchFromDate, searchToDate);
        Map<String, RegionalExeBucket> buckets = new LinkedHashMap<>();
        for (SettlementRun r : runs) {
            if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
                continue;
            }
            if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
                continue;
            }
            String mid = r.getMerchantId();
            if (mid == null || !allowedMerchants.contains(mid)) continue;
            OrgUnit merchantOu = orgUnitRepository.findByCode(mid).orElse(null);
            if (merchantOu == null || merchantOu.getOrgLevel() != OrgLevel.MERCHANT) continue;
            OrgUnit regional = findRegionalAncestor(merchantOu, idToOu);
            if (regional == null || regional.getCode() == null || !allowedRegional.contains(regional.getCode())) {
                continue;
            }
            LocalDate calcDt = r.getCalcDt();
            if (calcDt == null) continue;
            String key = calcDt + "|" + regional.getCode();
            RegionalExeBucket b = buckets.computeIfAbsent(key, k -> new RegionalExeBucket(calcDt, regional.getCode()));
            b.add(r, resolveStatementCurrency(mid), settlementLedgerRoundPolicy(), this);
        }

        List<Map<String, Object>> rows = buckets.values().stream()
                .map(this::toRegionalExecuteRow)
                .sorted(Comparator
                        .<Map<String, Object>, String>comparing(x -> String.valueOf(x.getOrDefault("calcDt", "")))
                        .reversed()
                        .thenComparing(x -> String.valueOf(x.getOrDefault("regionalCompId", ""))))
                .collect(Collectors.toList());
        return pageOf(rows, page, size);
    }

    /**
     * 정산집계표: 기간 합계 1행 + meta.
     */
    public PageResult<Map<String, Object>> summary(
            Optional<OrgUnit> userOrgOpt,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchMerchantId,
            String searchMasterId,
            String searchRegionalId,
            String curType,
            String reportKind) {
        PageResult<Map<String, Object>> agg = aggregate(userOrgOpt, searchFromDate, searchToDate,
                searchMerchantId, searchMasterId, searchRegionalId, curType, reportKind, 1, Integer.MAX_VALUE);
        List<Map<String, Object>> list = agg.getList() != null ? agg.getList() : List.of();
        long sumGross = 0, sumRefund = 0, sumNet = 0, sumRolling = 0, sumMdr = 0, sumPerTxn = 0;
        long sumBatchFee = 0, sumFeeVat = 0, sumRemit = 0, sumPay = 0;
        long sumTxnCnt = 0;
        for (Map<String, Object> row : list) {
            sumGross += asLong(row.get("grossPay"));
            sumRefund += asLong(row.get("refundAmt"));
            sumNet += asLong(row.get("netPay"));
            sumRolling += asLong(row.get("rollingReserveAmt"));
            sumMdr += asLong(row.get("mdrFeeAmt"));
            sumPerTxn += asLong(row.get("perTxnFeeAmt"));
            sumBatchFee += asLong(row.get("settlementBatchFee"));
            sumFeeVat += asLong(row.get("feeVat"));
            sumRemit += asLong(row.get("remittanceFee"));
            sumPay += asLong(row.get("payAmount"));
            sumTxnCnt += asLong(row.get("txnCnt"));
        }
        LocalDate fromD = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toD = searchToDate != null ? searchToDate : LocalDate.now();
        Map<String, Object> one = new LinkedHashMap<>();
        one.put("periodFrom", fromD.toString());
        one.put("periodTo", toD.toString());
        String ctDisp = curType != null && !curType.isBlank() ? curType.trim().toUpperCase(Locale.ROOT) : "전체";
        one.put("curType", ctDisp);
        one.put("grossPay", sumGross);
        one.put("refundAmt", sumRefund);
        one.put("netPay", sumNet);
        one.put("rollingReserveAmt", sumRolling);
        one.put("mdrFeeAmt", sumMdr);
        one.put("perTxnFeeAmt", sumPerTxn);
        one.put("settlementBatchFee", sumBatchFee);
        one.put("feeVat", sumFeeVat);
        one.put("remittanceFee", sumRemit);
        one.put("payAmount", sumPay);
        one.put("settlementAmt", sumPay);
        one.put("txnCnt", sumTxnCnt);
        one.put("refundCnt", 0L);
        one.put("rowCount", list.size());

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(List.of(one));
        pr.setPage(1);
        pr.setSize(1);
        pr.setTotalElements(1);
        pr.setTotalPages(1);
        pr.setMeta(reportMeta());
        return pr;
    }

    // --- internal ---

    private static final class RegionalExeBucket {
        final LocalDate calcDt;
        final String regionalCode;
        BigDecimal approveAmtSum = BigDecimal.ZERO;
        BigDecimal cancelAmtSum = BigDecimal.ZERO;
        BigDecimal payAmountSum = BigDecimal.ZERO;
        BigDecimal totalFeeSum = BigDecimal.ZERO;
        BigDecimal rollingSum = BigDecimal.ZERO;
        BigDecimal mdrFeeSum = BigDecimal.ZERO;
        BigDecimal perTxnFeeSum = BigDecimal.ZERO;
        BigDecimal feeVatSum = BigDecimal.ZERO;
        BigDecimal settlementBatchFeeSum = BigDecimal.ZERO;
        BigDecimal remittanceBankSum = BigDecimal.ZERO;
        BigDecimal remittanceUsdtSum = BigDecimal.ZERO;
        BigDecimal finalPayAfterRemitSum = BigDecimal.ZERO;
        int runCount;
        boolean allCalculated = true;
        BigDecimal receivableAppliedSum = BigDecimal.ZERO;
        private final TreeSet<String> curTypes = new TreeSet<>();

        RegionalExeBucket(LocalDate calcDt, String regionalCode) {
            this.calcDt = calcDt;
            this.regionalCode = regionalCode;
        }

        void add(SettlementRun r, String stmtCurAlpha, FeeListRoundingPolicy rp,
                 SettlementReportService outer) {
            runCount++;
            if (stmtCurAlpha != null && !stmtCurAlpha.isBlank()) {
                curTypes.add(stmtCurAlpha.trim().toUpperCase(Locale.ROOT));
            }
            approveAmtSum = approveAmtSum.add(FeeListRoundingPolicy.round(nz(r.getApproveAmt()), rp));
            cancelAmtSum = cancelAmtSum.add(FeeListRoundingPolicy.round(nz(r.getCancelAmt()), rp));
            payAmountSum = payAmountSum.add(FeeListRoundingPolicy.round(r.getPayAmt() != null ? r.getPayAmt() : BigDecimal.ZERO, rp));
            totalFeeSum = totalFeeSum.add(FeeListRoundingPolicy.round(r.getTotalFee() != null ? r.getTotalFee() : BigDecimal.ZERO, rp));
            rollingSum = rollingSum.add(FeeListRoundingPolicy.round(r.getRollingReserveAmt() != null ? r.getRollingReserveAmt() : BigDecimal.ZERO, rp));
            receivableAppliedSum = receivableAppliedSum.add(FeeListRoundingPolicy.round(
                    r.getReceivableAppliedAmt() != null ? r.getReceivableAppliedAmt() : BigDecimal.ZERO, rp));
            String mid = r.getMerchantId();
            if (mid != null && !mid.isBlank()) {
                MdrPerTxnSplit sp = outer.computeMdrAndPerTxnFeeForRun(r, mid);
                mdrFeeSum = mdrFeeSum.add(FeeListRoundingPolicy.round(BigDecimal.valueOf(sp.mdrAmt()), rp));
                perTxnFeeSum = perTxnFeeSum.add(FeeListRoundingPolicy.round(BigDecimal.valueOf(sp.perTxnAmt()), rp));
                feeVatSum = feeVatSum.add(FeeListRoundingPolicy.round(
                        BigDecimal.valueOf(outer.feeVatForRun(r, mid)), rp));
            }
            if (r.getSettlementBatchFeeAmt() != null) {
                settlementBatchFeeSum = settlementBatchFeeSum.add(
                        FeeListRoundingPolicy.round(r.getSettlementBatchFeeAmt(), rp));
            }
            Map<String, Object> rem = outer.remittanceFieldsForRun(r);
            Object b = rem.get("remittanceFeeBank");
            Object u = rem.get("remittanceFeeUsdt");
            Object fp = rem.get("finalPayAfterRemittance");
            if (b instanceof Number bn) {
                remittanceBankSum = remittanceBankSum.add(BigDecimal.valueOf(bn.doubleValue()));
            }
            if (u instanceof Number un) {
                remittanceUsdtSum = remittanceUsdtSum.add(BigDecimal.valueOf(un.doubleValue()));
            }
            if (fp instanceof Number fn) {
                finalPayAfterRemitSum = finalPayAfterRemitSum.add(BigDecimal.valueOf(fn.doubleValue()));
            }
            if (!"CALCULATED".equalsIgnoreCase(String.valueOf(r.getStatus()))) {
                allCalculated = false;
            }
        }
    }

    private Map<String, Object> toRegionalExecuteRow(RegionalExeBucket b) {
        FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
        OrgUnit reg = orgUnitRepository.findByCode(b.regionalCode).orElse(null);
        BigDecimal netBd = b.approveAmtSum.subtract(b.cancelAmtSum).max(BigDecimal.ZERO);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("calcDt", b.calcDt.toString());
        m.put("regionalCompId", b.regionalCode);
        m.put("regionalNm", reg != null ? reg.getName() : b.regionalCode);
        m.put("compId", b.regionalCode);
        m.put("compNm", reg != null ? reg.getName() : b.regionalCode);
        m.put("batchRunCnt", b.runCount);
        if (b.curTypes.isEmpty()) {
            m.put("curType", "");
        } else if (b.curTypes.size() == 1) {
            m.put("curType", b.curTypes.first());
        } else {
            m.put("curType", String.join("/", b.curTypes));
        }
        m.put("approveAmt", settlementMoneyDouble(b.approveAmtSum, rp));
        m.put("cancelAmt", settlementMoneyDouble(b.cancelAmtSum, rp));
        m.put("netPay", settlementMoneyDouble(netBd, rp));
        m.put("mdrFeeAmt", settlementMoneyDouble(b.mdrFeeSum, rp));
        m.put("perTxnFeeAmt", settlementMoneyDouble(b.perTxnFeeSum, rp));
        m.put("feeVat", settlementMoneyDouble(b.feeVatSum, rp));
        m.put("settlementBatchFee", b.settlementBatchFeeSum.signum() != 0
                ? settlementMoneyDouble(b.settlementBatchFeeSum, rp) : null);
        m.put("payAmount", settlementMoneyDouble(b.payAmountSum, rp));
        m.put("totalFee", settlementMoneyDouble(b.totalFeeSum, rp));
        m.put("rollingReserveAmt", settlementMoneyDouble(b.rollingSum, rp));
        m.put("calcCycle", "-");
        boolean bankRemit = b.remittanceBankSum.signum() != 0;
        boolean usdtRemit = b.remittanceUsdtSum.signum() != 0;
        m.put("remittanceFeeBank", bankRemit ? settlementMoneyDouble(b.remittanceBankSum, rp) : null);
        m.put("remittanceFeeUsdt", usdtRemit ? settlementMoneyDouble(b.remittanceUsdtSum, rp) : null);
        m.put("finalPayAfterRemittance", settlementMoneyDouble(b.finalPayAfterRemitSum, rp));
        m.put("remittanceFee", bankRemit || usdtRemit
                ? settlementMoneyDouble(b.remittanceBankSum.add(b.remittanceUsdtSum), rp) : null);
        m.put("status", b.allCalculated ? "CALCULATED" : "PENDING");
        m.put("settlementDueDt", b.calcDt.toString());
        m.put("settledYn", b.allCalculated ? "Y" : "N");
        m.put("receivableRecoveryMode", "");
        m.put("receivableRecoveryModeKr", "본사합산(가맹 혼합)");
        m.put("receivableAppliedAmt", settlementMoneyDouble(b.receivableAppliedSum, rp));
        return m;
    }

    private OrgUnit findRegionalAncestor(OrgUnit start, Map<Long, OrgUnit> idToOu) {
        OrgUnit cur = start;
        for (int i = 0; i < 48 && cur != null; i++) {
            if (cur.getOrgLevel() == OrgLevel.REGIONAL) {
                return cur;
            }
            Long pid = cur.getParentId();
            cur = pid == null ? null : idToOu.get(pid);
        }
        return null;
    }

    private Set<String> resolveAllowedRegionalCodes(OrgUnit userOrg, List<OrgUnit> all) {
        if (userOrg == null) {
            return Set.of();
        }
        if (userOrg.getOrgLevel() == OrgLevel.REGIONAL) {
            return Set.of(userOrg.getCode());
        }
        if (userOrg.getOrgLevel() == OrgLevel.HEADQUARTERS) {
            Set<Long> subtree = new HashSet<>();
            subtree.add(userOrg.getId());
            subtree.addAll(collectDescendantOrgIds(userOrg.getId(), all));
            return all.stream()
                    .filter(o -> o.getOrgLevel() == OrgLevel.REGIONAL)
                    .filter(o -> subtree.contains(o.getId()))
                    .map(OrgUnit::getCode)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static long asLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }

    /** 주말 제외 영업일 가산 (공휴일 미반영) */
    public static LocalDate addBusinessDays(LocalDate start, int businessDays) {
        LocalDate d = start;
        int added = 0;
        while (added < businessDays) {
            d = d.plusDays(1);
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return d;
    }

    private Set<String> resolveAllowedMerchantCodes(OrgUnit userOrg, String searchMerchantId, String searchMasterId) {
        return resolveAllowedMerchantCodes(userOrg, searchMerchantId, searchMasterId, orgUnitRepository.findAll());
    }

    private Set<String> resolveAllowedMerchantCodes(OrgUnit userOrg, String searchMerchantId, String searchMasterId, List<OrgUnit> all) {
        if (userOrg == null) {
            return Set.of();
        }
        if (userOrg.getOrgLevel() == OrgLevel.MERCHANT) {
            return narrowMaster(Set.of(userOrg.getCode()), searchMerchantId, searchMasterId, all);
        }
        Set<Long> subtree = new HashSet<>();
        subtree.add(userOrg.getId());
        subtree.addAll(collectDescendantOrgIds(userOrg.getId(), all));
        Set<String> merchants = all.stream()
                .filter(o -> o.getOrgLevel() == OrgLevel.MERCHANT)
                .filter(o -> subtree.contains(o.getId()))
                .map(OrgUnit::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return narrowMaster(merchants, searchMerchantId, searchMasterId, all);
    }

    private Set<String> narrowMaster(Set<String> allowed, String searchMerchantId, String searchMasterId, List<OrgUnit> all) {
        Set<String> out = new LinkedHashSet<>(allowed);
        if (searchMerchantId != null && !searchMerchantId.isBlank()) {
            String one = searchMerchantId.trim();
            out.retainAll(Set.of(one));
        }
        if (searchMasterId != null && !searchMasterId.isBlank()) {
            Optional<OrgUnit> masterOpt = orgUnitRepository.findByCode(searchMasterId.trim());
            if (masterOpt.isEmpty()) {
                return Set.of();
            }
            OrgUnit master = masterOpt.get();
            Set<Long> ms = new HashSet<>();
            ms.add(master.getId());
            ms.addAll(collectDescendantOrgIds(master.getId(), all));
            Set<String> under = all.stream()
                    .filter(o -> o.getOrgLevel() == OrgLevel.MERCHANT)
                    .filter(o -> ms.contains(o.getId()))
                    .map(OrgUnit::getCode)
                    .collect(Collectors.toSet());
            out.retainAll(under);
        }
        return out;
    }

    private static List<Long> collectDescendantOrgIds(Long rootId, List<OrgUnit> all) {
        Map<Long, List<OrgUnit>> byParent = all.stream()
                .filter(o -> o.getParentId() != null)
                .collect(Collectors.groupingBy(OrgUnit::getParentId));
        List<Long> out = new ArrayList<>();
        collectDescRec(rootId, byParent, out);
        return out;
    }

    private static void collectDescRec(Long id, Map<Long, List<OrgUnit>> byParent, List<Long> out) {
        for (OrgUnit ch : byParent.getOrDefault(id, List.of())) {
            out.add(ch.getId());
            collectDescRec(ch.getId(), byParent, out);
        }
    }

    private PageResult<Map<String, Object>> pageOf(List<Map<String, Object>> allRows, int page, int size) {
        int s = Math.max(1, size);
        int p = Math.max(1, page);
        int from = Math.max(0, (p - 1) * s);
        int to = Math.min(allRows.size(), from + s);
        List<Map<String, Object>> rows = from < allRows.size() ? allRows.subList(from, to) : new ArrayList<>();
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(p);
        pr.setSize(s);
        pr.setTotalElements(allRows.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) allRows.size() / s)));
        pr.setMeta(reportMeta());
        return pr;
    }

    private PageResult<Map<String, Object>> emptyPage(int page, int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(Math.max(1, page));
        pr.setSize(Math.max(1, size));
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        pr.setMeta(reportMeta());
        return pr;
    }
}
