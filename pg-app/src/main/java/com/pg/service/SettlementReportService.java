package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementRun;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayDisplayCurrency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public SettlementReportService(OrgUnitRepository orgUnitRepository,
                                   PgTrnsctnRepository pgTrnsctnRepository,
                                   SettlementCalcService settlementCalcService,
                                   CommissionPolicyRepository commissionPolicyRepository,
                                   HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.settlementCalcService = settlementCalcService;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
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

    private static double settlementPctDisplay(BigDecimal net, BigDecimal rate, FeeListRoundingPolicy rp) {
        if (net == null || rate == null || rp == null || net.signum() <= 0) {
            return 0d;
        }
        return FeeListRoundingPolicy.round(
                net.multiply(rate).setScale(8, RoundingMode.HALF_UP), rp).doubleValue();
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
        m.put("settlementDueBusinessDays", 7);
        m.put("note", "정산일+7영업일은 주말 제외 근사치이며 공휴일은 미반영입니다. 예치·비율·건당요금은 운영 정책에 맞게 조정 가능합니다.");
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
     * 가맹점 정산 리포트: 결제일·가맹·통화 단위.
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
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX);

        String ct = curType != null ? curType.trim() : "";
        List<PgTrnsctn> raw = pgTrnsctnRepository.findForReportRange(fromDt, toDt, ct.isEmpty() ? null : ct);
        List<PgTrnsctn> filtered = raw.stream()
                .filter(t -> t.getMerchantId() != null && allowed.contains(t.getMerchantId()))
                .collect(Collectors.toList());

        Map<String, AggBucket> buckets = new LinkedHashMap<>();
        for (PgTrnsctn t : filtered) {
            LocalDate payDate = payDateOf(t);
            String mid = t.getMerchantId();
            String currency = t.getCurType() != null ? t.getCurType() : "KRW";
            String key = payDate + "|" + mid + "|" + currency;
            AggBucket b = buckets.computeIfAbsent(key, k -> new AggBucket(payDate, mid, currency));
            b.add(t);
        }

        List<Map<String, Object>> rows = buckets.values().stream()
                .map(this::toAggregateRow)
                .sorted(Comparator
                        .<Map<String, Object>, LocalDate>comparing(m -> LocalDate.parse(String.valueOf(m.get("payDate"))))
                        .reversed()
                        .thenComparing(m -> String.valueOf(m.getOrDefault("compId", ""))))
                .collect(Collectors.toList());

        return pageOf(rows, page, size);
    }

    /**
     * 본사 지급 리포트: 소속 가맹 거래를 상위 본사(REGIONAL) 단위로 합산 (총본사가 본사에 지급할 때 사용).
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
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX);
        String ct = curType != null ? curType.trim() : "";
        List<PgTrnsctn> raw = pgTrnsctnRepository.findForReportRange(fromDt, toDt, ct.isEmpty() ? null : ct);

        Map<String, AggBucketRegional> buckets = new LinkedHashMap<>();
        for (PgTrnsctn t : raw) {
            String mid = t.getMerchantId();
            if (mid == null || !allowedMerchants.contains(mid)) continue;
            OrgUnit merchantOu = orgUnitRepository.findByCode(mid).orElse(null);
            if (merchantOu == null || merchantOu.getOrgLevel() != OrgLevel.MERCHANT) continue;
            OrgUnit regional = findRegionalAncestor(merchantOu, idToOu);
            if (regional == null || regional.getCode() == null || !allowedRegional.contains(regional.getCode())) {
                continue;
            }
            LocalDate payDate = payDateOf(t);
            String currency = t.getCurType() != null ? t.getCurType() : "KRW";
            String key = payDate + "|" + regional.getCode() + "|" + currency;
            AggBucketRegional b = buckets.computeIfAbsent(key, k -> new AggBucketRegional(payDate, regional.getCode(), currency));
            b.add(t, mid);
        }

        List<Map<String, Object>> rows = buckets.values().stream()
                .map(b -> toRegionalAggregateRow(b, idToOu))
                .sorted(Comparator
                        .<Map<String, Object>, LocalDate>comparing(m -> LocalDate.parse(String.valueOf(m.get("payDate"))))
                        .reversed()
                        .thenComparing(m -> String.valueOf(m.getOrDefault("regionalCompId", ""))))
                .collect(Collectors.toList());
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
            if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
                continue;
            }
            String mid = r.getMerchantId();
            if (mid == null || !allowed.contains(mid)) continue;
            OrgUnit ou = orgUnitRepository.findByCode(mid).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            LocalDate calcDt = r.getCalcDt();
            m.put("calcDt", calcDt != null ? calcDt.toString() : "");
            m.put("compId", mid);
            m.put("compNm", ou != null ? ou.getName() : mid);
            m.put("curType", resolveStatementCurrency(mid));
            FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
            BigDecimal ap = nz(r.getApproveAmt());
            BigDecimal ca = nz(r.getCancelAmt());
            BigDecimal net = ap.subtract(ca);
            m.put("approveAmt", settlementMoneyDouble(ap, rp));
            m.put("cancelAmt", settlementMoneyDouble(ca, rp));
            m.put("netPay", settlementMoneyDouble(net, rp));
            m.put("depositAmt", settlementPctDisplay(net, DEPOSIT_PCT, rp));
            m.put("processingFeeTotal", settlementPctDisplay(net, PROC_PCT_TOTAL, rp));
            m.put("processingFeePg", settlementPctDisplay(net, PROC_PCT_PG, rp));
            m.put("processingFeeFile", settlementPctDisplay(net, PROC_PCT_FILE, rp));
            m.put("processingFeeOnline", settlementPctDisplay(net, PROC_PCT_ONLINE, rp));
            m.put("payAmount", settlementMoneyDouble(r.getPayAmt(), rp));
            m.put("totalFee", settlementMoneyDouble(r.getTotalFee(), rp));
            m.put("rollingReserveAmt", settlementMoneyDouble(r.getRollingReserveAmt(), rp));
            m.put("status", r.getStatus());
            m.put("settlementDueDt", calcDt != null ? addBusinessDays(calcDt, 7).toString() : "");
            m.put("settledYn", "CALCULATED".equalsIgnoreCase(String.valueOf(r.getStatus())) ? "Y" : "N");
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
            b.add(r, resolveStatementCurrency(mid), settlementLedgerRoundPolicy());
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
        long sumGross = 0, sumRefund = 0, sumNet = 0, sumDeposit = 0, sumProc = 0, sumTxn = 0, sumSettle = 0;
        long apCnt = 0, rfCnt = 0;
        for (Map<String, Object> row : list) {
            sumGross += asLong(row.get("grossPay"));
            sumRefund += asLong(row.get("refundAmt"));
            sumNet += asLong(row.get("netPay"));
            sumDeposit += asLong(row.get("depositAmt"));
            sumProc += asLong(row.get("processingFeeTotal"));
            sumTxn += asLong(row.get("txnFeeTotal"));
            sumSettle += asLong(row.get("settlementAmt"));
            apCnt += asLong(row.get("approveCnt"));
            rfCnt += asLong(row.get("refundCnt"));
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
        one.put("depositAmt", sumDeposit);
        one.put("processingFeeTotal", sumProc);
        one.put("txnFeeTotal", sumTxn);
        one.put("settlementAmt", sumSettle);
        one.put("approveCnt", apCnt);
        one.put("refundCnt", rfCnt);
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

    private static final class AggBucket {
        final LocalDate payDate;
        final String merchantId;
        final String curType;
        long gross;
        long refund;
        int approveCnt;
        int refundCnt;
        boolean allSettled = true;
        boolean anyTxn;

        AggBucket(LocalDate payDate, String merchantId, String curType) {
            this.payDate = payDate;
            this.merchantId = merchantId;
            this.curType = curType;
        }

        void add(PgTrnsctn t) {
            anyTxn = true;
            long amt = txAmount(t);
            String st = t.getStatus();
            if (isApprove(st)) {
                gross += amt;
                approveCnt++;
            } else if (isRefundLike(st)) {
                refund += amt;
                refundCnt++;
            }
            if (t.getSettledYn() == null || !"Y".equalsIgnoreCase(t.getSettledYn())) {
                allSettled = false;
            }
        }
    }

    private Map<String, Object> toAggregateRow(AggBucket b) {
        OrgUnit ou = orgUnitRepository.findByCode(b.merchantId).orElse(null);
        long net = b.gross - b.refund;
        BigDecimal netBd = BigDecimal.valueOf(Math.max(net, 0L));
        long deposit = pct(netBd, DEPOSIT_PCT);
        long procTotal = pct(netBd, PROC_PCT_TOTAL);
        long procPg = pct(netBd, PROC_PCT_PG);
        long procFile = pct(netBd, PROC_PCT_FILE);
        long procOn = pct(netBd, PROC_PCT_ONLINE);
        long txnFee = b.approveCnt * FEE_PER_APPROVE + b.refundCnt * FEE_PER_REFUND + FEE_SETTLEMENT_BATCH;
        long settlement = net - deposit - procTotal - txnFee;
        if (settlement < 0) settlement = 0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("payDate", b.payDate.toString());
        m.put("compId", b.merchantId);
        m.put("compNm", ou != null ? ou.getName() : b.merchantId);
        m.put("curType", b.curType);
        m.put("grossPay", b.gross);
        m.put("refundAmt", b.refund);
        m.put("netPay", net);
        m.put("approveCnt", b.approveCnt);
        m.put("refundCnt", b.refundCnt);
        m.put("depositAmt", deposit);
        m.put("depositHoldDays", 180);
        m.put("processingFeeTotal", procTotal);
        m.put("processingFeePg", procPg);
        m.put("processingFeeFile", procFile);
        m.put("processingFeeOnline", procOn);
        m.put("txnFeePerApprove", FEE_PER_APPROVE);
        m.put("txnFeePerRefund", FEE_PER_REFUND);
        m.put("txnFeeSettlementBatch", FEE_SETTLEMENT_BATCH);
        m.put("txnFeeTotal", txnFee);
        m.put("settlementAmt", settlement);
        m.put("settlementDueDt", addBusinessDays(b.payDate, 7).toString());
        m.put("settledYn", b.anyTxn && b.allSettled ? "Y" : "N");
        return m;
    }

    private static final class AggBucketRegional {
        final LocalDate payDate;
        final String regionalCode;
        final String curType;
        long gross;
        long refund;
        int approveCnt;
        int refundCnt;
        final Set<String> merchantIds = new LinkedHashSet<>();
        boolean allSettled = true;
        boolean anyTxn;

        AggBucketRegional(LocalDate payDate, String regionalCode, String curType) {
            this.payDate = payDate;
            this.regionalCode = regionalCode;
            this.curType = curType;
        }

        void add(PgTrnsctn t, String merchantId) {
            anyTxn = true;
            merchantIds.add(merchantId);
            long amt = txAmount(t);
            String st = t.getStatus();
            if (isApprove(st)) {
                gross += amt;
                approveCnt++;
            } else if (isRefundLike(st)) {
                refund += amt;
                refundCnt++;
            }
            if (t.getSettledYn() == null || !"Y".equalsIgnoreCase(t.getSettledYn())) {
                allSettled = false;
            }
        }
    }

    private Map<String, Object> toRegionalAggregateRow(AggBucketRegional b, Map<Long, OrgUnit> idToOu) {
        OrgUnit reg = orgUnitRepository.findByCode(b.regionalCode).orElse(null);
        long net = b.gross - b.refund;
        BigDecimal netBd = BigDecimal.valueOf(Math.max(net, 0L));
        long deposit = pct(netBd, DEPOSIT_PCT);
        long procTotal = pct(netBd, PROC_PCT_TOTAL);
        long procPg = pct(netBd, PROC_PCT_PG);
        long procFile = pct(netBd, PROC_PCT_FILE);
        long procOn = pct(netBd, PROC_PCT_ONLINE);
        long txnFee = b.approveCnt * FEE_PER_APPROVE + b.refundCnt * FEE_PER_REFUND + FEE_SETTLEMENT_BATCH;
        long settlement = net - deposit - procTotal - txnFee;
        if (settlement < 0) settlement = 0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("payDate", b.payDate.toString());
        m.put("regionalCompId", b.regionalCode);
        m.put("regionalNm", reg != null ? reg.getName() : b.regionalCode);
        m.put("compId", b.regionalCode);
        m.put("compNm", reg != null ? reg.getName() : b.regionalCode);
        m.put("merchantCnt", b.merchantIds.size());
        m.put("curType", b.curType);
        m.put("grossPay", b.gross);
        m.put("refundAmt", b.refund);
        m.put("netPay", net);
        m.put("approveCnt", b.approveCnt);
        m.put("refundCnt", b.refundCnt);
        m.put("depositAmt", deposit);
        m.put("depositHoldDays", 180);
        m.put("processingFeeTotal", procTotal);
        m.put("processingFeePg", procPg);
        m.put("processingFeeFile", procFile);
        m.put("processingFeeOnline", procOn);
        m.put("txnFeeTotal", txnFee);
        m.put("settlementAmt", settlement);
        m.put("settlementDueDt", addBusinessDays(b.payDate, 7).toString());
        m.put("settledYn", b.anyTxn && b.allSettled ? "Y" : "N");
        return m;
    }

    private static final class RegionalExeBucket {
        final LocalDate calcDt;
        final String regionalCode;
        BigDecimal approveAmtSum = BigDecimal.ZERO;
        BigDecimal cancelAmtSum = BigDecimal.ZERO;
        BigDecimal payAmountSum = BigDecimal.ZERO;
        BigDecimal totalFeeSum = BigDecimal.ZERO;
        BigDecimal rollingSum = BigDecimal.ZERO;
        int runCount;
        boolean allCalculated = true;
        private final TreeSet<String> curTypes = new TreeSet<>();

        RegionalExeBucket(LocalDate calcDt, String regionalCode) {
            this.calcDt = calcDt;
            this.regionalCode = regionalCode;
        }

        void add(SettlementRun r, String stmtCurAlpha, FeeListRoundingPolicy rp) {
            runCount++;
            if (stmtCurAlpha != null && !stmtCurAlpha.isBlank()) {
                curTypes.add(stmtCurAlpha.trim().toUpperCase(Locale.ROOT));
            }
            approveAmtSum = approveAmtSum.add(FeeListRoundingPolicy.round(nz(r.getApproveAmt()), rp));
            cancelAmtSum = cancelAmtSum.add(FeeListRoundingPolicy.round(nz(r.getCancelAmt()), rp));
            payAmountSum = payAmountSum.add(FeeListRoundingPolicy.round(r.getPayAmt() != null ? r.getPayAmt() : BigDecimal.ZERO, rp));
            totalFeeSum = totalFeeSum.add(FeeListRoundingPolicy.round(r.getTotalFee() != null ? r.getTotalFee() : BigDecimal.ZERO, rp));
            rollingSum = rollingSum.add(FeeListRoundingPolicy.round(r.getRollingReserveAmt() != null ? r.getRollingReserveAmt() : BigDecimal.ZERO, rp));
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
        m.put("depositAmt", settlementPctDisplay(netBd, DEPOSIT_PCT, rp));
        m.put("processingFeeTotal", settlementPctDisplay(netBd, PROC_PCT_TOTAL, rp));
        m.put("processingFeePg", settlementPctDisplay(netBd, PROC_PCT_PG, rp));
        m.put("processingFeeFile", settlementPctDisplay(netBd, PROC_PCT_FILE, rp));
        m.put("processingFeeOnline", settlementPctDisplay(netBd, PROC_PCT_ONLINE, rp));
        m.put("payAmount", settlementMoneyDouble(b.payAmountSum, rp));
        m.put("totalFee", settlementMoneyDouble(b.totalFeeSum, rp));
        m.put("rollingReserveAmt", settlementMoneyDouble(b.rollingSum, rp));
        m.put("status", b.allCalculated ? "CALCULATED" : "PENDING");
        m.put("settlementDueDt", addBusinessDays(b.calcDt, 7).toString());
        m.put("settledYn", b.allCalculated ? "Y" : "N");
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

    private static LocalDate payDateOf(PgTrnsctn t) {
        LocalDateTime ts = t.getPaidAt() != null ? t.getPaidAt() : t.getCreatedAt();
        if (ts == null) return LocalDate.now();
        return ts.toLocalDate();
    }

    private static long txAmount(PgTrnsctn t) {
        BigDecimal a = t.getAmtKrw();
        if (a == null) a = t.getIcopayAmt();
        if (a == null) a = t.getTotalAmt();
        if (a == null) return 0L;
        return a.abs().setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static boolean isApprove(String status) {
        return "10".equals(status);
    }

    private static boolean isRefundLike(String status) {
        return "20".equals(status) || "30".equals(status) || "31".equals(status);
    }

    private static long pct(BigDecimal net, BigDecimal rate) {
        if (net == null || net.signum() <= 0) return 0L;
        return net.multiply(rate).setScale(0, RoundingMode.HALF_UP).longValue();
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
