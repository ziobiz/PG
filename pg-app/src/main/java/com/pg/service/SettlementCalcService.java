package com.pg.service;

import com.pg.entity.ChargebackFeePolicy;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.RollingReserve;
import com.pg.entity.SettlementRun;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.RollingReserveRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementSetting;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.service.settlement.SettlementCycleTiming;
import com.pg.service.settlement.SettlementPeriodResolver;
import com.pg.util.BusinessDayCalendar;
import com.pg.util.ChargebackTierResolver;
import com.pg.util.CommissionExtraFeeUtil;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.MerchantFeeVatUtil;
import com.pg.util.PayDisplayCurrency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 정산 수학 로직: 결제 데이터 → 수수료 차감(건당·정산·차지백·실패·취소·무효·수동무효·환불·이용·결제·USDT·FX %·3DS 건당 고정·롤링) → 롤링(담보금 N% N일 보류) → 지급액
 * <p><b>정산로직(수동 「정산실행」)</b>: {@link #execute(LocalDate, LocalDate, String, boolean)} {@code manualExecuteRules=true} 일 때
 * 정산구분 {@code AUTO} 가맹은 제외하고, {@code D*}·{@code W+N}·{@code WK*} 는 {@link SettlementPeriodResolver#resolveAutoPeriodWindow} 로
 * 정산일(기간 종료일)이 주기상 실행일일 때만 집계합니다. 마감시각·정산제외 영업일·{@code D0} 시간대는 자동 배치와 동일합니다.</p>
 */
@Service
public class SettlementCalcService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final List<String> CHARGEBACK_STATUSES = List.of("30", "31");
    private static final Pattern D_CYCLE = Pattern.compile("^D\\d{1,2}$", Pattern.CASE_INSENSITIVE);

    private final PgTrnsctnRepository trnsctnRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final RollingReserveRepository rollingReserveRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final SettlementArrearsService settlementArrearsService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    public SettlementCalcService(PgTrnsctnRepository trnsctnRepository,
                                 CommissionPolicyRepository commissionPolicyRepository,
                                 ChargebackFeePolicyRepository chargebackFeePolicyRepository,
                                 SettlementRunRepository settlementRunRepository,
                                 RollingReserveRepository rollingReserveRepository,
                                 SettlementSettingRepository settlementSettingRepository,
                                 OrgUnitRepository orgUnitRepository,
                                 OrgServiceUseService orgServiceUseService,
                                 SettlementArrearsService settlementArrearsService,
                                 HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.trnsctnRepository = trnsctnRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.rollingReserveRepository = rollingReserveRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.settlementArrearsService = settlementArrearsService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    public List<SettlementRun> listRuns(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) fromDate = LocalDate.now().minusYears(1);
        if (toDate == null) toDate = LocalDate.now();
        return settlementRunRepository.findByCalcDtBetweenOrderByMerchantId(fromDate, toDate);
    }

    public CommissionPolicy getPolicy(String merchantId) {
        if (merchantId != null && !merchantId.isEmpty()) {
            return commissionPolicyRepository.findByScope(merchantId)
                    .orElseGet(() -> commissionPolicyRepository.findByScope("DEFAULT").orElse(null));
        }
        return commissionPolicyRepository.findByScope("DEFAULT").orElse(null);
    }

    /**
     * 정산 실행: 기간 내 거래 합산 → 수수료 차감 → 롤링 보류 → 지급액 계산 및 저장
     * (자동 배치·레거시 호출용 — 수동 화면 버튼은 {@link #execute(LocalDate, LocalDate, String, boolean)} {@code true} 사용)
     */
    @Transactional
    public List<SettlementRun> execute(LocalDate fromDate, LocalDate toDate, String merchantId) {
        return execute(fromDate, toDate, merchantId, false);
    }

    /**
     * 정산 실행.
     *
     * @param manualExecuteRules {@code true}: 정산관리 「정산실행」 수동 버튼 — 정산로직(자동 가맹 제외·주기·마감·영업일) 적용
     */
    @Transactional
    public List<SettlementRun> execute(LocalDate fromDate, LocalDate toDate, String merchantId, boolean manualExecuteRules) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);
        List<PgTrnsctn> list = trnsctnRepository.findForSettlement(merchantId, from, to);
        List<SettlementRun> results = new ArrayList<>();
        LocalDate calcDt = toDate;
        if (!manualExecuteRules) {
            List<String> merchantIds = list.stream().map(PgTrnsctn::getMerchantId).distinct().collect(Collectors.toList());
            for (String mid : merchantIds) {
                List<PgTrnsctn> txList = list.stream().filter(t -> mid.equals(t.getMerchantId())).collect(Collectors.toList());
                SettlementRun run = calcOne(mid, calcDt, txList, Optional.empty(), false, true);
                if (run != null) {
                    run.setPeriodFrom(fromDate);
                    run.setPeriodTo(toDate);
                    run.setPeriodEndAt(null);
                    settlementRunRepository.save(run);
                    settlementArrearsService.applyArrearsToSettledRun(run);
                    results.add(run);
                }
            }
            appendReleaseOnlyMerchants(calcDt, merchantId, results, false);
            return results;
        }

        LocalTime nowSeoul = LocalTime.now(SEOUL);
        Map<String, List<PgTrnsctn>> byMid = list.stream()
                .filter(t -> t.getMerchantId() != null && !t.getMerchantId().isBlank())
                .collect(Collectors.groupingBy(t -> t.getMerchantId().trim()));

        for (ManualExecuteRow row : buildManualExecuteRows(fromDate, toDate, merchantId, calcDt, nowSeoul, byMid)) {
            SettlementRun run = calcOne(row.mid(), calcDt, row.txList(), Optional.empty(), false, true);
            if (run != null) {
                run.setPeriodFrom(row.periodFrom());
                run.setPeriodTo(row.periodTo());
                run.setPeriodEndAt(null);
                settlementRunRepository.save(run);
                settlementArrearsService.applyArrearsToSettledRun(run);
                results.add(run);
            }
        }
        appendReleaseOnlyMerchants(calcDt, merchantId, results, true);
        return results;
    }

    private record ManualExecuteRow(String mid, List<PgTrnsctn> txList, LocalDate periodFrom, LocalDate periodTo) {}

    /**
     * 정산로직: 수동 실행 대상 가맹만 행으로 구성 (AUTO 제외, 달력 주기 미도래 제외, 마감·영업일·D0 시간대).
     */
    private List<ManualExecuteRow> buildManualExecuteRows(LocalDate fromDate,
                                                          LocalDate toDate,
                                                          String merchantIdFilter,
                                                          LocalDate calcDt,
                                                          LocalTime nowSeoul,
                                                          Map<String, List<PgTrnsctn>> txsInSearchRangeByMid) {
        List<ManualExecuteRow> rows = new ArrayList<>();
        List<OrgUnit> merchants = orgUnitRepository.findAll().stream()
                .filter(ou -> ou.getOrgLevel() == OrgLevel.MERCHANT)
                .filter(ou -> merchantIdFilter == null || merchantIdFilter.isBlank()
                        || merchantIdFilter.trim().equalsIgnoreCase(String.valueOf(ou.getCode()).trim()))
                .toList();
        for (OrgUnit ou : merchants) {
            String mid = ou.getCode();
            if (mid == null || mid.isBlank()) {
                continue;
            }
            Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.getId());
            if (ssOpt.isEmpty()) {
                continue;
            }
            SettlementSetting ss = ssOpt.get();
            if ("AUTO".equalsIgnoreCase(String.valueOf(ss.getCalcProcType()).trim())) {
                continue;
            }
            String cycleRaw = ss.getCalcCycle();
            if (cycleRaw == null || cycleRaw.isBlank() || "NONE".equalsIgnoreCase(cycleRaw.trim())) {
                continue;
            }
            if ("Y".equalsIgnoreCase(String.valueOf(ss.getCalcExcludeYn()).trim())
                    && !BusinessDayCalendar.isBusinessDay(calcDt, Collections.emptySet())) {
                continue;
            }
            LocalTime close = ss.getCalcCloseTime();
            if (close != null && nowSeoul.isBefore(close)) {
                continue;
            }
            String c0 = SettlementPeriodResolver.normalizeCalcCycle(cycleRaw);
            if ("D0".equals(c0) && !SettlementCycleTiming.isD0AutoBatchAllowedNow(nowSeoul)) {
                continue;
            }
            SettlementPeriodResolver.PeriodWindow w = SettlementPeriodResolver.resolveAutoPeriodWindow(cycleRaw, calcDt);
            if (w != null) {
                LocalDateTime qFrom = w.fromDate().atStartOfDay();
                LocalDateTime qTo = w.toDate().atTime(LocalTime.MAX);
                List<PgTrnsctn> txList = trnsctnRepository.findForSettlement(mid, qFrom, qTo);
                rows.add(new ManualExecuteRow(mid, txList, w.fromDate(), w.toDate()));
                continue;
            }
            if (isCalendarCycleExclusiveToResolver(c0)) {
                /* W7 등: 오늘(정산일)이 해당 주기의 실행일이 아니면 수동 실행에서 제외 */
                continue;
            }
            if (SettlementCycleTiming.isRealtimeCode(c0)
                    || SettlementCycleTiming.isSubDailyScheduleCode(c0)
                    || SettlementCycleTiming.isRollingIntradayGridCode(c0)) {
                List<PgTrnsctn> txs = txsInSearchRangeByMid.getOrDefault(mid.trim(), List.of());
                if (txs.isEmpty()) {
                    continue;
                }
                rows.add(new ManualExecuteRow(mid, txs, fromDate, toDate));
                continue;
            }
            List<PgTrnsctn> txs = txsInSearchRangeByMid.getOrDefault(mid.trim(), List.of());
            if (!txs.isEmpty()) {
                rows.add(new ManualExecuteRow(mid, txs, fromDate, toDate));
            }
        }
        return rows;
    }

    /** {@link SettlementPeriodResolver#resolveAutoPeriodWindow} 로만 스케줄되는 주기(미도래 시 null). RT·격자 등은 false. */
    private static boolean isCalendarCycleExclusiveToResolver(String normalizedCycle) {
        if (normalizedCycle == null || normalizedCycle.isBlank()) {
            return false;
        }
        String c = normalizedCycle.trim().toUpperCase(Locale.ROOT);
        if ("RT".equals(c) || "T0".equals(c)) {
            return false;
        }
        if (SettlementCycleTiming.isSubDailyScheduleCode(c) || SettlementCycleTiming.isRollingIntradayGridCode(c)) {
            return false;
        }
        if (c.matches("W\\d+")) {
            return true;
        }
        if (c.startsWith("WK")) {
            return true;
        }
        return D_CYCLE.matcher(c).matches();
    }

    /**
     * 기간 내 거래는 없으나, 종료일 기준 해지 대상 담보가 있는 가맹은 지급액에 반환만 하는 정산 행 생성.
     */
    private void appendReleaseOnlyMerchants(LocalDate calcDt, String merchantIdFilter, List<SettlementRun> results) {
        appendReleaseOnlyMerchants(calcDt, merchantIdFilter, results, false);
    }

    private void appendReleaseOnlyMerchants(LocalDate calcDt, String merchantIdFilter, List<SettlementRun> results,
                                            boolean skipAutoProcMerchants) {
        Set<String> done = results.stream().map(SettlementRun::getMerchantId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<RollingReserve> due = rollingReserveRepository.findByStatusAndReleaseDateLessThanEqual("HOLD", calcDt);
        Set<String> candidates = due.stream().map(RollingReserve::getMerchantId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (merchantIdFilter != null && !merchantIdFilter.isBlank()) {
            candidates.removeIf(m -> !merchantIdFilter.trim().equals(m));
        }
        candidates.removeAll(done);
        for (String mid : candidates) {
            if (skipAutoProcMerchants && isMerchantAutoCalcProc(mid)) {
                continue;
            }
            SettlementRun run = calcOne(mid, calcDt, Collections.emptyList(), Optional.empty(), false, true);
            if (run != null) {
                run.setPeriodFrom(calcDt);
                run.setPeriodTo(calcDt);
                run.setPeriodEndAt(null);
                settlementRunRepository.save(run);
                settlementArrearsService.applyArrearsToSettledRun(run);
                results.add(run);
            }
        }
    }

    private boolean isMerchantAutoCalcProc(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        String m = merchantId.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(m);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(m);
        }
        return ou.flatMap(o -> settlementSettingRepository.findByOrgUnitId(o.getId()))
                .map(ss -> "AUTO".equalsIgnoreCase(String.valueOf(ss.getCalcProcType()).trim()))
                .orElse(false);
    }

    /**
     * 실시간 자동정산: RT는 승인 건마다 정산 실행 1건(건당), T0는 당일 00:00~현재 전체 재집계(1행).
     *
     * @param paidTxn 승인 직후 노티에서 넘기는 거래. RT일 때 필수(없으면 스킵). T0는 무시 가능.
     */
    @Transactional
    public List<SettlementRun> triggerRealtimeAutoSettlementIfDue(String merchantId, PgTrnsctn paidTxn) {
        if (merchantId == null || merchantId.isBlank()) {
            return List.of();
        }
        String midRaw = merchantId.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(midRaw);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(midRaw);
        }
        if (ou.isEmpty()) {
            return List.of();
        }
        String mid = ou.get().getCode() != null && !ou.get().getCode().isBlank()
                ? ou.get().getCode().trim()
                : midRaw;
        Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.get().getId());
        if (ssOpt.isEmpty()) {
            return List.of();
        }
        SettlementSetting ss = ssOpt.get();
        if (!"AUTO".equalsIgnoreCase(String.valueOf(ss.getCalcProcType()).trim())) {
            return List.of();
        }
        String c0 = SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle());
        if (!SettlementCycleTiming.isRealtimeCode(c0)) {
            return List.of();
        }
        if (SettlementCycleTiming.isRtPerTransactionCode(c0)) {
            if (paidTxn == null) {
                return List.of();
            }
            return appendRtPerTransactionSettlement(mid, paidTxn);
        }
        if (SettlementCycleTiming.isT0RollingIntradayCode(c0)) {
            return recalcTodayIntradayAuto(mid);
        }
        return List.of();
    }

    /**
     * 거래 객체 없이 호출. RT 주기 가맹은 스킵되므로, RT는 {@link #triggerRealtimeAutoSettlementIfDue(String, PgTrnsctn)} 로 호출합니다.
     */
    @Transactional
    public List<SettlementRun> triggerRealtimeAutoSettlementIfDue(String merchantId) {
        return triggerRealtimeAutoSettlementIfDue(merchantId, null);
    }

    /**
     * N분·N시간 마감: M/H는 격자마다 직전 구간 합산 1건, TM/TH는 격자 시각마다 T0와 같이 당일 0시~현재 전체 재집계 1건.
     */
    @Transactional
    public List<SettlementRun> triggerSubDailyAutoSettlement(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return List.of();
        }
        String midRaw = merchantId.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(midRaw);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(midRaw);
        }
        if (ou.isEmpty()) {
            return List.of();
        }
        String mid = ou.get().getCode() != null && !ou.get().getCode().isBlank()
                ? ou.get().getCode().trim()
                : midRaw;
        Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.get().getId());
        if (ssOpt.isEmpty()) {
            return List.of();
        }
        SettlementSetting ss = ssOpt.get();
        if (!"AUTO".equalsIgnoreCase(String.valueOf(ss.getCalcProcType()).trim())) {
            return List.of();
        }
        String c0 = SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle());
        if (!SettlementCycleTiming.isSubDailyScheduleCode(c0)) {
            return List.of();
        }
        if (SettlementCycleTiming.isRollingIntradayGridCode(c0)) {
            return recalcTodayIntradayAuto(mid);
        }
        return appendSubDailyGridAggregateSettlement(mid, c0);
    }

    /**
     * 직전 격자 구간 [start,end) 내 거래 합산 1건. RT로 이미 settled Y 인 승인은 제외. 동일 슬롯 중복 실행 방지.
     */
    private List<SettlementRun> appendSubDailyGridAggregateSettlement(String merchantId, String cycleNorm) {
        String mid = merchantId.trim();
        LocalDateTime nowMin = LocalDateTime.now(SEOUL).withSecond(0).withNano(0);
        String plainGrid = SettlementCycleTiming.toPlainGridClosingCode(cycleNorm);
        SettlementCycleTiming.SubDailyClosedSlot slot = SettlementCycleTiming.closedSubDailySlot(nowMin, plainGrid);
        if (slot == null) {
            return List.of();
        }
        LocalDate calcDt = slot.endExclusive().toLocalDate();
        if (settlementRunRepository.existsByMerchantIdAndCalcDtAndPeriodEndAt(mid, calcDt, slot.endExclusive())) {
            return List.of();
        }
        LocalDateTime qFrom = slot.startInclusive();
        LocalDateTime qTo = slot.endExclusive().minusNanos(1);
        List<PgTrnsctn> list = trnsctnRepository.findForSettlement(mid, qFrom, qTo).stream()
                .filter(this::includeTxnInSubDailyAggregate)
                .collect(Collectors.toList());
        boolean releaseRolling = settlementRunRepository.findByCalcDtAndMerchantId(calcDt, mid).isEmpty();
        if (list.isEmpty()) {
            if (!releaseRolling) {
                return List.of();
            }
            List<RollingReserve> maturing = rollingReserveRepository.findByMerchantIdAndStatusAndReleaseDateLessThanEqual(
                    mid, "HOLD", calcDt);
            if (maturing.isEmpty()) {
                return List.of();
            }
        }
        boolean chargeMonthly = shouldChargeMonthlyUsageOnIntradayRun(mid, calcDt);
        List<SettlementRun> results = new ArrayList<>();
        SettlementRun run = calcOne(mid, calcDt, list, Optional.of(chargeMonthly), false, releaseRolling);
        if (run != null) {
            run.setPeriodFrom(slot.startInclusive().toLocalDate());
            run.setPeriodTo(slot.endExclusive().toLocalDate());
            run.setPeriodEndAt(slot.endExclusive());
            settlementRunRepository.save(run);
            settlementArrearsService.applyArrearsToSettledRun(run);
            results.add(run);
        }
        appendReleaseOnlyMerchants(calcDt, mid, results);
        return results;
    }

    /** M/H 격자 합산: 승인은 RT 등으로 이미 정산 반영된 건 제외, 그 외 상태는 구간 내 그대로 포함 */
    private boolean includeTxnInSubDailyAggregate(PgTrnsctn t) {
        if (t == null) {
            return false;
        }
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        if ("10".equals(st)) {
            String sy = t.getSettledYn();
            return sy == null || sy.isBlank() || !"Y".equalsIgnoreCase(sy.trim());
        }
        return true;
    }

    /** 월간 이용료 등: 당월 이전일까지 정산 실행이 없고 당일 첫 실행일 때만 부과 */
    private boolean shouldChargeMonthlyUsageOnIntradayRun(String mid, LocalDate today) {
        boolean hadSameDayRun = !settlementRunRepository.findByCalcDtAndMerchantId(today, mid).isEmpty();
        YearMonth ym = YearMonth.from(today);
        LocalDate monthStart = ym.atDay(1);
        LocalDate priorEnd = today.minusDays(1);
        long priorRunsInMonthBeforeToday = 0;
        if (!priorEnd.isBefore(monthStart)) {
            priorRunsInMonthBeforeToday = settlementRunRepository.countByMerchantIdAndCalcDtBetween(mid, monthStart, priorEnd);
        }
        CommissionPolicy pol = getPolicy(mid);
        BigDecimal usageRate = pol != null && pol.getUsageRate() != null ? pol.getUsageRate() : BigDecimal.ZERO;
        return usageRate.compareTo(BigDecimal.ZERO) > 0 && priorRunsInMonthBeforeToday == 0 && !hadSameDayRun;
    }

    /**
     * RT: 승인 1건만 담아 정산 실행 행을 추가(당일 다건). 이미 정산 반영된 건은 스킵.
     */
    private List<SettlementRun> appendRtPerTransactionSettlement(String mid, PgTrnsctn txn) {
        if (txn == null || txn.getMerchantId() == null
                || !mid.equalsIgnoreCase(txn.getMerchantId().trim())) {
            return List.of();
        }
        String st = txn.getStatus() != null ? txn.getStatus().trim() : "";
        if (!"10".equals(st)) {
            return List.of();
        }
        if ("Y".equalsIgnoreCase(String.valueOf(txn.getSettledYn()).trim())) {
            return List.of();
        }
        LocalDate today = LocalDate.now(SEOUL);
        boolean chargeMonthlyUsage = shouldChargeMonthlyUsageOnIntradayRun(mid, today);
        List<SettlementRun> results = new ArrayList<>();
        SettlementRun run = calcOne(mid, today, List.of(txn), Optional.of(chargeMonthlyUsage), true, true);
        if (run != null) {
            run.setPeriodFrom(today);
            run.setPeriodTo(today);
            LocalDateTime endAt = txn.getCreatedAt() != null ? txn.getCreatedAt() : LocalDateTime.now(SEOUL);
            run.setPeriodEndAt(endAt);
            settlementRunRepository.save(run);
            settlementArrearsService.applyArrearsToSettledRun(run);
            results.add(run);
        }
        appendReleaseOnlyMerchants(today, mid, results);
        return results;
    }

    private List<SettlementRun> recalcTodayIntradayAuto(String merchantId) {
        String mid = merchantId.trim();
        LocalDate today = LocalDate.now(SEOUL);
        boolean chargeMonthlyUsage = shouldChargeMonthlyUsageOnIntradayRun(mid, today);
        settlementRunRepository.deleteByMerchantIdAndCalcDt(mid, today);
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = LocalDateTime.now(SEOUL);
        List<PgTrnsctn> list = trnsctnRepository.findForSettlement(mid, from, to);
        List<SettlementRun> results = new ArrayList<>();
        SettlementRun run = calcOne(mid, today, list, Optional.of(chargeMonthlyUsage), false, true);
        if (run != null) {
            run.setPeriodFrom(today);
            run.setPeriodTo(today);
            run.setPeriodEndAt(LocalDateTime.now(SEOUL));
            settlementRunRepository.save(run);
            settlementArrearsService.applyArrearsToSettledRun(run);
            results.add(run);
        }
        appendReleaseOnlyMerchants(today, mid, results);
        return results;
    }

    /**
     * @param markEveryIncludedTxnSettled true면 배치에 포함된 모든 거래에 settled Y(건당 마감·중복 방지).
     * @param releaseRollingReservesDue     true일 때만 calcDt 기준 만기 담보 해지를 이번 실행에 반영(건당 연속 호출 시 1회만).
     */
    private SettlementRun calcOne(String merchantId, LocalDate calcDt, List<PgTrnsctn> txList,
                                   Optional<Boolean> chargeMonthlyUsageOverride,
                                   boolean markEveryIncludedTxnSettled,
                                   boolean releaseRollingReservesDue) {
        if (merchantId != null && !orgServiceUseService.isOrgServiceActiveByCompCode(merchantId)) {
            return null;
        }
        if (merchantId != null && !merchantId.isBlank()) {
            Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantId.trim());
            if (ou.isPresent()) {
                Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.get().getId());
                if (ssOpt.isPresent() && "NONE".equalsIgnoreCase(ssOpt.get().getCalcCycle())) {
                    return null;
                }
            }
        }
        CommissionPolicy policy = getPolicy(merchantId);
        if (policy == null) {
            policy = new CommissionPolicy();
            policy.setScope("DEFAULT");
            policy.setPayRate(new BigDecimal("2.5"));
            policy.setRollingPct(BigDecimal.ZERO);
            policy.setRollingDays(0);
        }
        BigDecimal approveAmt = BigDecimal.ZERO;
        BigDecimal cancelAmt = BigDecimal.ZERO;
        BigDecimal voidAmt = BigDecimal.ZERO;
        BigDecimal manualVoidAmt = BigDecimal.ZERO;
        BigDecimal refundAmt = BigDecimal.ZERO;
        int payCnt = 0;
        int cancelCnt = 0;
        int voidCnt = 0;
        int manualVoidCnt = 0;
        int refundCnt = 0;
        int failCnt = 0;
        int txCount = txList.size();
        for (PgTrnsctn t : txList) {
            BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            if ("10".equals(st)) {
                approveAmt = approveAmt.add(amt);
                payCnt++;
            } else if ("20".equals(st)) {
                cancelAmt = cancelAmt.add(amt);
                cancelCnt++;
            } else if ("21".equals(st)) {
                voidAmt = voidAmt.add(amt);
                voidCnt++;
            } else if ("22".equals(st)) {
                manualVoidAmt = manualVoidAmt.add(amt);
                manualVoidCnt++;
            } else if ("30".equals(st) || "31".equals(st)) {
                refundAmt = refundAmt.add(amt);
                refundCnt++;
            } else if ("F0".equals(st) || "99".equals(st)) {
                failCnt++;
            }
        }
        /* 해지일이 도래한 담보금(롤링) → 이번 정산 지급액에 합산 후 RELEASED 처리 */
        BigDecimal releasedFromReserve = BigDecimal.ZERO;
        LocalDateTime releaseStamp = LocalDateTime.now();
        if (releaseRollingReservesDue) {
            List<RollingReserve> maturing = rollingReserveRepository.findByMerchantIdAndStatusAndReleaseDateLessThanEqual(
                    merchantId, "HOLD", calcDt);
            if (!maturing.isEmpty()) {
                for (RollingReserve rr : maturing) {
                    if (rr.getReserveAmt() != null) {
                        releasedFromReserve = releasedFromReserve.add(rr.getReserveAmt());
                    }
                    rr.setStatus("RELEASED");
                    rr.setReleasedAt(releaseStamp);
                }
                rollingReserveRepository.saveAll(maturing);
            }
        }

        if (txList.isEmpty() && releasedFromReserve.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        var ledgerSys = hqLedgerSysSettingsService.getOrCreate();
        FeeListRoundingPolicy lr = FeeCurrencyRoundResolver.from(ledgerSys)
                .forCurrency(PayDisplayCurrency.alphaFromSettings(ledgerSys));
        releasedFromReserve = FeeListRoundingPolicy.round(releasedFromReserve, lr);

        BigDecimal approveRounded = FeeListRoundingPolicy.round(approveAmt, lr);
        BigDecimal cancelRounded = FeeListRoundingPolicy.round(cancelAmt, lr);
        BigDecimal voidRounded = FeeListRoundingPolicy.round(voidAmt, lr);
        BigDecimal manualVoidRounded = FeeListRoundingPolicy.round(manualVoidAmt, lr);
        BigDecimal netSales = approveRounded.subtract(cancelRounded).subtract(voidRounded).subtract(manualVoidRounded);

        BigDecimal perTxFee = policy.getPerTxFee() != null ? policy.getPerTxFee() : BigDecimal.ZERO;
        BigDecimal cancelRate = policy.getCancelRate() != null ? policy.getCancelRate() : BigDecimal.ZERO;
        BigDecimal voidFeePerTx = policy.getVoidFeePerTx() != null ? policy.getVoidFeePerTx() : BigDecimal.ZERO;
        BigDecimal manualVoidFeePerTx = policy.getManualVoidFeePerTx() != null ? policy.getManualVoidFeePerTx() : BigDecimal.ZERO;
        BigDecimal usageRate = policy.getUsageRate() != null ? policy.getUsageRate() : BigDecimal.ZERO;
        BigDecimal payRate = policy.getPayRate() != null ? policy.getPayRate() : BigDecimal.ZERO;
        BigDecimal refundRate = policy.getRefundRate() != null ? policy.getRefundRate() : BigDecimal.ZERO;
        BigDecimal failFee = policy.getFailFee() != null ? policy.getFailFee() : BigDecimal.ZERO;
        BigDecimal[] rollingPctRef = new BigDecimal[]{ policy.getRollingPct() != null ? policy.getRollingPct() : BigDecimal.ZERO };
        int[] rollingDaysRef = new int[]{ policy.getRollingDays() != null ? policy.getRollingDays() : 0 };
        orgUnitRepository.findByCode(merchantId).ifPresent(ou ->
                settlementSettingRepository.findByOrgUnitId(ou.getId()).ifPresent(ss -> {
                    if ("N".equalsIgnoreCase(ss.getHoldRateFollowHq() != null ? ss.getHoldRateFollowHq().trim() : "")) {
                        if (ss.getHoldRate() != null && ss.getHoldRate().compareTo(BigDecimal.ZERO) > 0) rollingPctRef[0] = ss.getHoldRate();
                        if (ss.getHoldDays() != null && ss.getHoldDays() > 0) rollingDaysRef[0] = ss.getHoldDays();
                    }
                }));
        BigDecimal rollingPct = rollingPctRef[0];
        int rollingDays = rollingDaysRef[0];

        BigDecimal feePerTx = FeeListRoundingPolicy.round(perTxFee.multiply(BigDecimal.valueOf(txCount)), lr);
        BigDecimal feePayRate = lrPctOf(approveAmt, payRate, lr);
        /* 취소·무효·수동무효·환불: 건당 고정액 × 해당 건수 */
        BigDecimal feeCancelRate = FeeListRoundingPolicy.round(cancelRate.multiply(BigDecimal.valueOf(cancelCnt)), lr);
        BigDecimal feeVoidPerTx = FeeListRoundingPolicy.round(voidFeePerTx.multiply(BigDecimal.valueOf(voidCnt)), lr);
        BigDecimal feeManualVoidPerTx = FeeListRoundingPolicy.round(manualVoidFeePerTx.multiply(BigDecimal.valueOf(manualVoidCnt)), lr);
        BigDecimal feeRefundRate = FeeListRoundingPolicy.round(refundRate.multiply(BigDecimal.valueOf(refundCnt)), lr);
        YearMonth ym = YearMonth.from(calcDt);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        long runsAlreadyThisMonth = settlementRunRepository.countByMerchantIdAndCalcDtBetween(merchantId, monthStart, monthEnd);
        boolean chargeMonthlyUsage;
        if (chargeMonthlyUsageOverride != null && chargeMonthlyUsageOverride.isPresent()) {
            chargeMonthlyUsage = chargeMonthlyUsageOverride.get();
        } else {
            chargeMonthlyUsage = runsAlreadyThisMonth == 0;
        }
        chargeMonthlyUsage = chargeMonthlyUsage && usageRate.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal feeUsage = chargeMonthlyUsage ? FeeListRoundingPolicy.round(usageRate, lr) : BigDecimal.ZERO;
        BigDecimal feeFailTotal = FeeListRoundingPolicy.round(failFee.multiply(BigDecimal.valueOf(failCnt)), lr);

        BigDecimal feeSettlementPerTxBd = policy.getFeeSettlementPerTx() != null ? policy.getFeeSettlementPerTx() : BigDecimal.ZERO;
        BigDecimal feeSettlementTotal = FeeListRoundingPolicy.round(feeSettlementPerTxBd.multiply(BigDecimal.valueOf(txCount)), lr);

        long chargebackBatchCnt = txList.stream()
                .map(PgTrnsctn::getStatus)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(st -> "30".equals(st) || "31".equals(st))
                .count();
        BigDecimal feeChargebackTotal = BigDecimal.ZERO;
        if (chargebackBatchCnt > 0) {
            BigDecimal perCase;
            Long cbPolId = policy.getChargebackPolicyId();
            if (cbPolId != null) {
                Optional<ChargebackFeePolicy> cbOpt = chargebackFeePolicyRepository.findByIdWithTiers(cbPolId);
                if (cbOpt.isPresent() && cbOpt.get().getTiers() != null && !cbOpt.get().getTiers().isEmpty()) {
                    LocalDateTime monthStartDt = ym.atDay(1).atStartOfDay();
                    LocalDateTime nextMonthStartDt = ym.plusMonths(1).atDay(1).atStartOfDay();
                    long monthCbCount = trnsctnRepository.countByMerchantIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            merchantId, CHARGEBACK_STATUSES, monthStartDt, nextMonthStartDt);
                    int mc = (int) Math.min(monthCbCount, Integer.MAX_VALUE);
                    perCase = ChargebackTierResolver.feePerCaseForMonthlyCount(mc, cbOpt.get().getTiers());
                } else {
                    perCase = policy.getChargebackFeePerTx() != null ? policy.getChargebackFeePerTx() : BigDecimal.ZERO;
                }
            } else {
                perCase = policy.getChargebackFeePerTx() != null ? policy.getChargebackFeePerTx() : BigDecimal.ZERO;
            }
            feeChargebackTotal = FeeListRoundingPolicy.round(perCase.multiply(BigDecimal.valueOf(chargebackBatchCnt)), lr);
        }

        BigDecimal feeUsdtBd = policy.getFeeUsdt() != null ? policy.getFeeUsdt() : BigDecimal.ZERO;
        BigDecimal feeFxBd = policy.getFeeFx() != null ? policy.getFeeFx() : BigDecimal.ZERO;
        BigDecimal fee3dsFixedPerTx = policy.getFee3dsRate() != null ? policy.getFee3dsRate() : BigDecimal.ZERO;
        BigDecimal extraRateOnApprove = feeUsdtBd.add(feeFxBd);
        BigDecimal feeUsdtFxPctSum = BigDecimal.ZERO;
        BigDecimal fee3dsFixedSum = BigDecimal.ZERO;
        BigDecimal feeExtraPctSum = BigDecimal.ZERO;
        for (PgTrnsctn t : txList) {
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            if (!"10".equals(st)) continue;
            BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
            if (extraRateOnApprove.signum() > 0 && amt.signum() > 0) {
                feeUsdtFxPctSum = feeUsdtFxPctSum.add(lrPctOf(amt, extraRateOnApprove, lr));
            }
            if (fee3dsFixedPerTx.signum() > 0 && amt.signum() > 0) {
                fee3dsFixedSum = fee3dsFixedSum.add(FeeListRoundingPolicy.round(fee3dsFixedPerTx, lr));
            }
            feeExtraPctSum = feeExtraPctSum.add(CommissionExtraFeeUtil.sumPctOnApprovedAmount(policy, amt, lr));
        }
        fee3dsFixedSum = FeeListRoundingPolicy.round(fee3dsFixedSum, lr);

        BigDecimal feeExtraFix = CommissionExtraFeeUtil.sumFixedForSettlementRounded(policy, lr);

        BigDecimal totalFee = FeeListRoundingPolicy.round(
                feePerTx.add(feePayRate).add(feeCancelRate).add(feeVoidPerTx).add(feeManualVoidPerTx).add(feeRefundRate).add(feeUsage)
                        .add(feeFailTotal).add(feeSettlementTotal).add(feeChargebackTotal).add(feeUsdtFxPctSum).add(fee3dsFixedSum)
                        .add(feeExtraPctSum).add(feeExtraFix),
                lr);

        SettlementSetting feeVatSs = null;
        if (merchantId != null && !merchantId.isBlank()) {
            feeVatSs = orgUnitRepository.findByCode(merchantId.trim())
                    .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                    .orElse(null);
        }
        BigDecimal feeVatAmt = MerchantFeeVatUtil.vatOnFeeAmount(totalFee, feeVatSs, lr.decimalPlaces());

        BigDecimal rollingReserveAmt = BigDecimal.ZERO;
        if (rollingDays > 0 && rollingPct.compareTo(BigDecimal.ZERO) > 0) {
            for (PgTrnsctn t : txList) {
                String stRolling = t.getStatus() != null ? t.getStatus().trim() : "";
                if (!"10".equals(stRolling)) {
                    continue;
                }
                BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
                BigDecimal reserve = lrPctOf(amt, rollingPct, lr);
                if (reserve.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                String tid = t.getTrnId();
                if (tid != null && !tid.isBlank()) {
                    String tidTrim = tid.trim();
                    if (rollingReserveRepository.existsByTrnIdAndStatus(tidTrim, "HOLD")) {
                        continue;
                    }
                }
                rollingReserveAmt = rollingReserveAmt.add(reserve);
                RollingReserve rr = new RollingReserve();
                String tidSave = tid != null ? tid.trim() : null;
                if (tidSave != null && tidSave.length() > 20) {
                    tidSave = tidSave.substring(0, 20);
                }
                rr.setTrnId(tidSave);
                rr.setMerchantId(merchantId);
                rr.setReserveAmt(reserve);
                rr.setRollingPct(rollingPct);
                rr.setHoldStartDate(calcDt);
                rr.setHoldBusinessDays(rollingDays);
                rr.setReleaseDate(BusinessDayCalendar.addBusinessDays(calcDt, rollingDays, Collections.emptySet()));
                rr.setStatus("HOLD");
                rollingReserveRepository.save(rr);
            }
        }
        rollingReserveAmt = FeeListRoundingPolicy.round(rollingReserveAmt, lr);

        BigDecimal payAmt = FeeListRoundingPolicy.round(
                netSales.subtract(totalFee).subtract(feeVatAmt).subtract(rollingReserveAmt).add(releasedFromReserve), lr);
        if (payAmt.compareTo(BigDecimal.ZERO) < 0) {
            payAmt = FeeListRoundingPolicy.round(BigDecimal.ZERO, lr);
        }

        /* 건당 마감: 포함된 모든 거래 settled Y. 당일 합산(T0) 등: 승인(10)만 Y — 정산 후 환불 시 환수금 자동 등록 기준 */
        if (markEveryIncludedTxnSettled) {
            for (PgTrnsctn t : txList) {
                t.setSettledYn("Y");
            }
            if (!txList.isEmpty()) {
                trnsctnRepository.saveAll(txList);
            }
        } else {
            List<PgTrnsctn> approvedInBatch = txList.stream()
                    .filter(t -> t.getStatus() != null && "10".equals(t.getStatus().trim()))
                    .collect(Collectors.toList());
            for (PgTrnsctn t : approvedInBatch) {
                t.setSettledYn("Y");
            }
            if (!approvedInBatch.isEmpty()) {
                trnsctnRepository.saveAll(approvedInBatch);
            }
        }

        SettlementRun run = new SettlementRun();
        run.setCalcDt(calcDt);
        run.setMerchantId(merchantId);
        run.setApproveAmt(approveRounded);
        run.setCancelAmt(cancelRounded);
        run.setTotalFee(totalFee);
        run.setRollingReserveAmt(rollingReserveAmt);
        run.setPayAmt(payAmt);
        run.setStatus("CALCULATED");
        applyPayoutHoldStagingIfDue(run, merchantId);
        return run;
    }

    /** HQ 수수료·정산(기준통화) 스케일로 {@code base × pct ÷ 100} 반올림 */
    private static BigDecimal lrPctOf(BigDecimal base, BigDecimal pctHundred, FeeListRoundingPolicy lr) {
        if (base == null || pctHundred == null || lr == null || pctHundred.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return FeeListRoundingPolicy.round(
                base.multiply(pctHundred).divide(BigDecimal.valueOf(100), 16, RoundingMode.HALF_UP), lr);
    }

    /**
     * 정산방법에서 지급보류=Y 인 가맹점이면 실행 행을 가맹점정산내역이 아닌 정산보류내역에만 두도록 표시합니다.
     */
    private void applyPayoutHoldStagingIfDue(SettlementRun run, String merchantId) {
        run.setPayoutHoldYn("N");
        run.setPayoutHoldRemark(null);
        if (merchantId == null || merchantId.isBlank() || run == null) {
            return;
        }
        orgUnitRepository.findByCode(merchantId.trim())
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                .ifPresent(ss -> {
                    if ("Y".equalsIgnoreCase(String.valueOf(ss.getPayHoldYn()).trim())) {
                        run.setPayoutHoldYn("Y");
                        run.setPayoutHoldRemark(
                                "지급보류(Y) 가맹점: 정산은 실행되었으나 가맹점정산내역·유통망정산 집계에는 표시되지 않고 정산보류내역에만 적치됩니다. "
                                        + "[선택 해제] 시 이 건만 가맹점정산내역으로 이동합니다. 가맹점 설정의 지급보류는 그대로입니다.");
                    }
                });
    }
}
