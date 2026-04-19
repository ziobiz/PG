package com.pg.service;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.RollingReserve;
import com.pg.entity.SettlementRun;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.RollingReserveRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementSetting;
import com.pg.entity.ChargebackFeeTier;
import com.pg.service.settlement.FeeListTxnBreakdownCalculator;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.service.settlement.SettlementCycleTiming;
import com.pg.service.settlement.SettlementPeriodResolver;
import com.pg.util.BusinessDayCalendar;
import com.pg.util.CommissionExtraFeeUtil;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.MerchantFeeVatUtil;
import com.pg.util.PayDisplayCurrency;
import com.pg.util.VoidRefundSettlementModeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 정산 수학 로직: 결제 데이터 → 거래 수수료 차감(수수료내역과 동일한 건별 {@link FeeListTxnBreakdownCalculator} 합산 + 월간이용료·기타FIX)
 * → 정산수수료·송금수수료는 실행당 1회 별도 공제 → 롤링(담보금) → 지급액
 * <p><b>정산로직(수동 「정산실행」)</b>: {@link #execute(LocalDate, LocalDate, String, boolean)} {@code manualExecuteRules=true} 일 때
 * 정산구분 {@code AUTO}·{@code MANUAL} 가맹 모두 동일 규칙으로 집계합니다. {@code D*}·{@code W+N}·{@code WK*} 는
 * {@link SettlementPeriodResolver#resolveAutoPeriodWindow} 로 정산일(기간 종료일)이 주기상 실행일일 때만 집계합니다.
 * 마감시각·정산제외 영업일·{@code D0} 시간대·격자 슬롯은 자동 배치와 동일합니다.</p>
 */
@Service
public class SettlementCalcService {

    private static final Logger log = LoggerFactory.getLogger(SettlementCalcService.class);

    private static final Pattern D_CYCLE = Pattern.compile("^D\\d{1,2}$", Pattern.CASE_INSENSITIVE);

    private final PgTrnsctnRepository trnsctnRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final RollingReserveRepository rollingReserveRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final SettlementArrearsService settlementArrearsService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final SettlementCalcCycleTransitionService settlementCalcCycleTransitionService;
    private final HqSettlementCycleAdminService hqSettlementCycleAdminService;
    private final MasterDistSettlementCronZoneService masterDistSettlementCronZoneService;
    private final VoidRefundSettlementModeResolutionService voidRefundSettlementModeResolutionService;
    private final CommissionService commissionService;
    private final FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator;

    public SettlementCalcService(PgTrnsctnRepository trnsctnRepository,
                                 CommissionPolicyRepository commissionPolicyRepository,
                                 SettlementRunRepository settlementRunRepository,
                                 RollingReserveRepository rollingReserveRepository,
                                 SettlementSettingRepository settlementSettingRepository,
                                 OrgUnitRepository orgUnitRepository,
                                 OrgServiceUseService orgServiceUseService,
                                 SettlementArrearsService settlementArrearsService,
                                 HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                 SettlementCalcCycleTransitionService settlementCalcCycleTransitionService,
                                 HqSettlementCycleAdminService hqSettlementCycleAdminService,
                                 MasterDistSettlementCronZoneService masterDistSettlementCronZoneService,
                                 VoidRefundSettlementModeResolutionService voidRefundSettlementModeResolutionService,
                                 CommissionService commissionService,
                                 FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator) {
        this.trnsctnRepository = trnsctnRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.rollingReserveRepository = rollingReserveRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.settlementArrearsService = settlementArrearsService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.settlementCalcCycleTransitionService = settlementCalcCycleTransitionService;
        this.hqSettlementCycleAdminService = hqSettlementCycleAdminService;
        this.masterDistSettlementCronZoneService = masterDistSettlementCronZoneService;
        this.voidRefundSettlementModeResolutionService = voidRefundSettlementModeResolutionService;
        this.commissionService = commissionService;
        this.feeListTxnBreakdownCalculator = feeListTxnBreakdownCalculator;
    }

    public List<SettlementRun> listRuns(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) fromDate = LocalDate.now().minusYears(1);
        if (toDate == null) toDate = LocalDate.now();
        return settlementRunRepository.findByCalcDtBetweenOrderByMerchantId(fromDate, toDate);
    }

    /**
     * 가맹점정산내역·유통 배분·정산실시 리포트에 올릴 실행 행인지(주기 마감·건당 규칙).
     * {@link SettlementCycleTiming#isMerchantStatementSettlementRunVisible}
     */
    public boolean isMerchantStatementVisibleSettlementRun(SettlementRun r) {
        if (r == null) {
            return false;
        }
        String mid = r.getMerchantId();
        if (mid == null || mid.isBlank()) {
            return false;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(mid.trim());
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(mid.trim());
        }
        if (ou.isEmpty()) {
            return true;
        }
        Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.get().getId());
        String cycleRaw;
        if (r.getCalcCycleSnapshot() != null && !r.getCalcCycleSnapshot().isBlank()) {
            cycleRaw = r.getCalcCycleSnapshot().trim();
        } else {
            cycleRaw = ssOpt.map(SettlementSetting::getCalcCycle).orElse("");
        }
        return SettlementCycleTiming.isMerchantStatementSettlementRunVisible(
                r.getPeriodEndAt(), SettlementPeriodResolver.normalizeCalcCycle(cycleRaw));
    }

    public CommissionPolicy getPolicy(String merchantId) {
        if (merchantId != null && !merchantId.isEmpty()) {
            return commissionService.resolveCommissionPolicyForSettlement(merchantId.trim());
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
     * @param manualExecuteRules {@code true}: 정산관리 「정산실행」 버튼 — 정산로직(AUTO·MANUAL 공통, 주기·마감·영업일·격자) 적용
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
                SettlementRun run = calcOne(mid, calcDt, txList, Optional.empty(), false, true, Optional.empty());
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
            flushPendingCalcCycleIfNeeded(results);
            return results;
        }

        Map<String, List<PgTrnsctn>> byMid = list.stream()
                .filter(t -> t.getMerchantId() != null && !t.getMerchantId().isBlank())
                .collect(Collectors.groupingBy(t -> t.getMerchantId().trim()));

        for (ManualExecuteRow row : buildManualExecuteRows(fromDate, toDate, merchantId, calcDt, byMid)) {
            SettlementRun run = calcOne(row.mid(), calcDt, row.txList(), Optional.empty(), false, true, Optional.empty());
            if (run != null) {
                run.setPeriodFrom(row.periodFrom());
                run.setPeriodTo(row.periodTo());
                run.setPeriodEndAt(null);
                settlementRunRepository.save(run);
                settlementArrearsService.applyArrearsToSettledRun(run);
                results.add(run);
            }
        }
        appendReleaseOnlyMerchants(calcDt, merchantId, results, false);
        flushPendingCalcCycleIfNeeded(results);
        return results;
    }

    private void flushPendingCalcCycleIfNeeded(List<SettlementRun> results) {
        if (results != null && !results.isEmpty()) {
            settlementCalcCycleTransitionService.tryApplyPendingAfterRuns(results, "");
        }
    }

    private record ManualExecuteRow(String mid, List<PgTrnsctn> txList, LocalDate periodFrom, LocalDate periodTo) {}

    /**
     * 정산로직: 「정산실행」 대상 가맹 행 구성 (AUTO·MANUAL 공통).
     * <ul>
     *   <li>정산일({@code calcDt} = 요청 {@code toDate})이 가맹 정산 크론 존 기준 달력일보다 미래이면 제외.</li>
     *   <li>W·WK·D+N 등 달력형: {@link SettlementPeriodResolver#resolveAutoPeriodWindow} 가 null이면 해당일은 실행일이 아님(미도래).</li>
     *   <li>당일·달력형·개시시간 적용 주기: 마감 다음 + 정산개시시각 이전이면 제외.</li>
     *   <li>resolve/격자/실시간으로 분류되지 않은 주기 코드는 검색 구간 거래만으로 집계하지 않음.</li>
     * </ul>
     */
    /**
     * 수동 「정산실행」: 격자 주기(M5·H1·TM 등)는 당일 기준 격자 구간이 끝난 뒤에만 허용.
     * 단일 가맹 지정 시 사전 검증 — AUTO·MANUAL 동일.
     */
    public Optional<String> validateManualSettlementExecuteWindow(String merchantCompId, LocalDate runTo) {
        if (merchantCompId == null || merchantCompId.isBlank() || runTo == null) {
            return Optional.empty();
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCodeIgnoreCase(merchantCompId.trim());
        if (ouOpt.isEmpty() || ouOpt.get().getOrgLevel() != OrgLevel.MERCHANT) {
            return Optional.empty();
        }
        Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ouOpt.get().getId());
        if (ssOpt.isEmpty()) {
            return Optional.empty();
        }
        SettlementSetting ss = ssOpt.get();
        String cycleRaw = ss.getCalcCycle();
        if (cycleRaw == null || cycleRaw.isBlank() || "NONE".equalsIgnoreCase(cycleRaw.trim())) {
            return Optional.empty();
        }
        String c0 = SettlementPeriodResolver.normalizeCalcCycle(cycleRaw);
        if (!hqSettlementCycleAdminService.isActiveSettlementCycle(cycleRaw)) {
            return Optional.of("본사 정산주기관리(DB)에서 사용이 아닌(N) 정산주기입니다: " + c0);
        }
        ZoneId z = masterDistSettlementCronZoneService.resolveSettlementCronZoneForOrgUnitId(ouOpt.get().getId());
        LocalDate todayZ = LocalDate.now(z);
        LocalTime nowZ = LocalTime.now(z);
        if (runTo.isAfter(todayZ)) {
            return Optional.of("정산대상 종료일이 가맹 정산 기준일보다 미래일 수 없습니다. (정산주기 도래 전 실행 방지)");
        }
        if (runTo.equals(todayZ)) {
            LocalTime close = ss.getCalcCloseTime();
            if (close != null && nowZ.isBefore(close)) {
                return Optional.of("정산마감시각 이전에는 실행할 수 없습니다.");
            }
        }
        if (SettlementCycleTiming.isCalcStartTimeApplicableForAuto(c0)) {
            LocalTime start = ss.getCalcStartTime();
            if (runTo.equals(todayZ) && start != null && nowZ.isBefore(start)) {
                return Optional.of("정산개시시각 이전에는 실행할 수 없습니다.");
            }
        }
        if ("D0".equals(c0) && runTo.equals(todayZ) && !SettlementCycleTiming.isD0AutoBatchAllowedNow(nowZ)) {
            return Optional.of("D0 정산은 당일 허용 시각(0:00~23:50) 내에서만 실행할 수 있습니다.");
        }
        if (isCalendarCycleExclusiveToResolver(c0)) {
            if (SettlementPeriodResolver.resolveAutoPeriodWindow(cycleRaw, runTo) == null) {
                return Optional.of("선택한 정산대상 종료일은 해당 가맹 정산주기(" + c0 + ")의 실행일이 아닙니다.");
            }
        }
        if (!SettlementCycleTiming.isManualIntradayGridSlotElapsed(nowZ, runTo, todayZ, c0)) {
            return Optional.of("정산주기(" + c0 + ")는 수동 실행 시 격자 구간이 끝난 뒤에만 가능합니다. "
                    + "(예: H1·H2 등은 해당 시간 블록이 끝난 정각 이후, M5 등은 N분 경계 이후)");
        }
        return Optional.empty();
    }

    private List<ManualExecuteRow> buildManualExecuteRows(LocalDate fromDate,
                                                          LocalDate toDate,
                                                          String merchantIdFilter,
                                                          LocalDate calcDt,
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
            ZoneId cronZ = masterDistSettlementCronZoneService.resolveSettlementCronZoneForOrgUnitId(ou.getId());
            LocalDate merchantToday = LocalDate.now(cronZ);
            LocalTime merchantNow = LocalTime.now(cronZ);
            if (calcDt.isAfter(merchantToday)) {
                continue;
            }
            Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.getId());
            if (ssOpt.isEmpty()) {
                continue;
            }
            SettlementSetting ss = ssOpt.get();
            String cycleRaw = ss.getCalcCycle();
            if (cycleRaw == null || cycleRaw.isBlank() || "NONE".equalsIgnoreCase(cycleRaw.trim())) {
                continue;
            }
            if (!hqSettlementCycleAdminService.isActiveSettlementCycle(cycleRaw)) {
                continue;
            }
            if ("Y".equalsIgnoreCase(String.valueOf(ss.getCalcExcludeYn()).trim())
                    && !BusinessDayCalendar.isBusinessDay(calcDt, Collections.emptySet())) {
                continue;
            }
            LocalTime close = ss.getCalcCloseTime();
            if (calcDt.equals(merchantToday) && close != null && merchantNow.isBefore(close)) {
                continue;
            }
            String c0 = SettlementPeriodResolver.normalizeCalcCycle(cycleRaw);
            if (SettlementCycleTiming.isCalcStartTimeApplicableForAuto(c0)) {
                LocalTime start = ss.getCalcStartTime();
                if (calcDt.equals(merchantToday) && start != null && merchantNow.isBefore(start)) {
                    continue;
                }
            }
            if ("D0".equals(c0) && calcDt.equals(merchantToday)
                    && !SettlementCycleTiming.isD0AutoBatchAllowedNow(merchantNow)) {
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
                if ((SettlementCycleTiming.isSubDailyScheduleCode(c0) || SettlementCycleTiming.isRollingIntradayGridCode(c0))
                        && !SettlementCycleTiming.isManualIntradayGridSlotElapsed(merchantNow, calcDt, merchantToday, c0)) {
                    continue;
                }
                List<PgTrnsctn> txs = txsInSearchRangeByMid.getOrDefault(mid.trim(), List.of());
                if (txs.isEmpty()) {
                    continue;
                }
                rows.add(new ManualExecuteRow(mid, txs, fromDate, toDate));
                continue;
            }
            /* resolve·격자·실시간 분류 밖 주기: 검색 구간에 거래가 있어도 임의 집계하지 않음(미도래·오설정 방지) */
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
            String f = merchantIdFilter.trim();
            candidates.removeIf(m -> m == null || !f.equalsIgnoreCase(m.trim()));
        }
        candidates.removeAll(done);
        for (String mid : candidates) {
            if (skipAutoProcMerchants && isMerchantAutoCalcProc(mid)) {
                continue;
            }
            SettlementRun run = calcOne(mid, calcDt, Collections.emptyList(), Optional.empty(), false, true, Optional.empty());
            if (run != null) {
                run.setPeriodFrom(calcDt);
                run.setPeriodTo(calcDt);
                /*
                 * LocalTime.MAX 는 초≠0 이라 H/M 격자 마감 복원이 불가해 하루 전체로 오인됨.
                 * 격자·표시 일관: 익일 00:00 을 배타 상한으로 두면 H12 등에서 12:00~24:00 구간으로 복원 가능.
                 */
                run.setPeriodEndAt(calcDt.plusDays(1).atStartOfDay());
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
        if (!hqSettlementCycleAdminService.isActiveSettlementCycle(ss.getCalcCycle())) {
            return List.of();
        }
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
        if (!hqSettlementCycleAdminService.isActiveSettlementCycle(ss.getCalcCycle())) {
            return List.of();
        }
        if (!SettlementCycleTiming.isSubDailyScheduleCode(c0)) {
            return List.of();
        }
        List<SettlementRun> subOut;
        if (SettlementCycleTiming.isRollingIntradayGridCode(c0)) {
            subOut = recalcTodayIntradayAuto(mid);
        } else {
            subOut = appendSubDailyGridAggregateSettlement(mid, c0);
        }
        flushPendingCalcCycleIfNeeded(subOut);
        return subOut;
    }

    /**
     * M/H 직전 구간 격자: 서버 장애 등으로 정각 tick 을 놓친 마감 시각이 grace 이내에 있으면, 누락 슬롯만 시간순으로 보강한다.
     * TM/TH·T0·RT 는 대상 아님({@link SettlementCycleTiming#isPlainSubDailyGridClosingCode}).
     */
    @Transactional
    public List<SettlementRun> catchUpMissedPlainSubDailyGridSlots(String merchantId, String cycleNorm, int graceMinutes) {
        if (merchantId == null || merchantId.isBlank() || graceMinutes <= 0) {
            return List.of();
        }
        String mid = merchantId.trim();
        String c0 = SettlementPeriodResolver.normalizeCalcCycle(cycleNorm);
        if (!SettlementCycleTiming.isPlainSubDailyGridClosingCode(c0)) {
            return List.of();
        }
        ZoneId z = masterDistSettlementCronZoneService.resolveSettlementCronZoneForMerchantCode(mid);
        LocalDateTime nowMin = LocalDateTime.now(z).withSecond(0).withNano(0);
        String plainGrid = SettlementCycleTiming.toPlainGridClosingCode(c0);
        List<LocalDateTime> ends = SettlementCycleTiming.listMissedPlainSubDailyGridEndsInGrace(nowMin, plainGrid, graceMinutes);
        if (ends.isEmpty()) {
            return List.of();
        }
        List<SettlementRun> out = new ArrayList<>();
        for (LocalDateTime endExclusive : ends) {
            LocalDateTime startInclusive = SettlementCycleTiming.subDailySlotStartInclusiveFromEndExclusive(endExclusive, c0);
            if (startInclusive == null) {
                continue;
            }
            SettlementCycleTiming.SubDailyClosedSlot slot = new SettlementCycleTiming.SubDailyClosedSlot(startInclusive, endExclusive);
            out.addAll(appendPlainSubDailyClosedSlot(mid, c0, slot));
        }
        if (!out.isEmpty()) {
            log.info("Settlement sub-daily catch-up: merchant={} cycle={} slotCount={}", mid, c0, out.size());
        }
        flushPendingCalcCycleIfNeeded(out);
        return out;
    }

    /**
     * 직전 격자 구간 [start,end) 내 거래 합산 1건. RT로 이미 settled Y 인 승인은 제외. 동일 슬롯 중복 실행 방지.
     */
    private List<SettlementRun> appendSubDailyGridAggregateSettlement(String merchantId, String cycleNorm) {
        String mid = merchantId.trim();
        ZoneId z = masterDistSettlementCronZoneService.resolveSettlementCronZoneForMerchantCode(mid);
        LocalDateTime nowMin = LocalDateTime.now(z).withSecond(0).withNano(0);
        String plainGrid = SettlementCycleTiming.toPlainGridClosingCode(cycleNorm);
        SettlementCycleTiming.SubDailyClosedSlot slot = SettlementCycleTiming.closedSubDailySlot(nowMin, plainGrid);
        if (slot == null) {
            return List.of();
        }
        return appendPlainSubDailyClosedSlot(mid, cycleNorm, slot);
    }

    /**
     * M/H 격자 1슬롯 집계. 이미 동일 {@code periodEndAt} 행이 있으면 스킵.
     */
    private List<SettlementRun> appendPlainSubDailyClosedSlot(String mid, String cycleNorm,
                                                              SettlementCycleTiming.SubDailyClosedSlot slot) {
        if (slot == null) {
            return List.of();
        }
        /* 정산일(calc_dt): 격자 구간 시작일 — H12면 자정·정오 마감 두 건이 동일 달력일에 나란히 보이도록 함 */
        LocalDate gridAnchorCalcDt = slot.startInclusive().toLocalDate();
        /* 담보 해지 조회: 격자가 끝나는 달력일 기준(자정 마감 건은 익일 0시까지 구간) */
        LocalDate reserveReleaseCutoffDt = slot.endExclusive().toLocalDate();
        if (settlementRunRepository.existsByMerchantIdAndCalcDtAndPeriodEndAt(mid, gridAnchorCalcDt, slot.endExclusive())) {
            return List.of();
        }
        LocalDateTime qFrom = slot.startInclusive();
        LocalDateTime qTo = slot.endExclusive().minusNanos(1);
        List<PgTrnsctn> list = trnsctnRepository.findForSettlement(mid, qFrom, qTo).stream()
                .filter(this::includeTxnInSubDailyAggregate)
                .collect(Collectors.toList());
        boolean releaseRolling = settlementRunRepository.findByCalcDtAndMerchantId(gridAnchorCalcDt, mid).isEmpty();
        if (list.isEmpty()) {
            if (!releaseRolling) {
                return List.of();
            }
            List<RollingReserve> maturing = rollingReserveRepository.findByMerchantIdAndStatusAndReleaseDateLessThanEqual(
                    mid, "HOLD", reserveReleaseCutoffDt);
            if (maturing.isEmpty()) {
                return List.of();
            }
        }
        boolean chargeMonthly = shouldChargeMonthlyUsageOnIntradayRun(mid, gridAnchorCalcDt);
        List<SettlementRun> results = new ArrayList<>();
        SettlementRun run = calcOne(mid, gridAnchorCalcDt, list, Optional.of(chargeMonthly), false, releaseRolling,
                Optional.of(reserveReleaseCutoffDt));
        if (run != null) {
            run.setPeriodFrom(slot.startInclusive().toLocalDate());
            run.setPeriodTo(slot.endExclusive().toLocalDate());
            run.setPeriodEndAt(slot.endExclusive());
            settlementRunRepository.save(run);
            settlementArrearsService.applyArrearsToSettledRun(run);
            results.add(run);
        }
        appendReleaseOnlyMerchants(gridAnchorCalcDt, mid, results);
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
        ZoneId z = masterDistSettlementCronZoneService.resolveSettlementCronZoneForMerchantCode(mid);
        LocalDate today = LocalDate.now(z);
        boolean chargeMonthlyUsage = shouldChargeMonthlyUsageOnIntradayRun(mid, today);
        List<SettlementRun> results = new ArrayList<>();
        SettlementRun run = calcOne(mid, today, List.of(txn), Optional.of(chargeMonthlyUsage), true, true, Optional.empty());
        if (run != null) {
            run.setPeriodFrom(today);
            run.setPeriodTo(today);
            LocalDateTime endAt = txn.getCreatedAt() != null ? txn.getCreatedAt() : LocalDateTime.now(z);
            run.setPeriodEndAt(endAt);
            settlementRunRepository.save(run);
            settlementArrearsService.applyArrearsToSettledRun(run);
            results.add(run);
        }
        appendReleaseOnlyMerchants(today, mid, results);
        return results;
    }

    private List<SettlementRun> recalcTodayIntradayAuto(String merchantId) {
        String midRaw = merchantId.trim();
        Optional<OrgUnit> ouRecalc = orgUnitRepository.findByCode(midRaw);
        if (ouRecalc.isEmpty()) {
            ouRecalc = orgUnitRepository.findByCodeIgnoreCase(midRaw);
        }
        if (ouRecalc.isEmpty()) {
            return List.of();
        }
        String mid = ouRecalc.get().getCode() != null && !ouRecalc.get().getCode().isBlank()
                ? ouRecalc.get().getCode().trim()
                : midRaw;
        Optional<SettlementSetting> ssRecalc = settlementSettingRepository.findByOrgUnitId(ouRecalc.get().getId());
        if (ssRecalc.isEmpty()) {
            return List.of();
        }
        String cRecalc = SettlementPeriodResolver.normalizeCalcCycle(ssRecalc.get().getCalcCycle());
        if (!SettlementCycleTiming.isT0RollingIntradayCode(cRecalc)
                && !SettlementCycleTiming.isRollingIntradayGridCode(cRecalc)) {
            log.warn("recalcTodayIntradayAuto skipped: merchant {} cycle {} is not T0 or TM/TH rolling grid", mid, cRecalc);
            return List.of();
        }
        ZoneId z = masterDistSettlementCronZoneService.resolveSettlementCronZoneForOrgUnitId(ouRecalc.get().getId());
        LocalDate today = LocalDate.now(z);
        boolean chargeMonthlyUsage = shouldChargeMonthlyUsageOnIntradayRun(mid, today);
        settlementRunRepository.deleteByMerchantIdAndCalcDt(mid, today);
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = LocalDateTime.now(z);
        List<PgTrnsctn> list = trnsctnRepository.findForSettlement(mid, from, to);
        List<SettlementRun> results = new ArrayList<>();
        SettlementRun run = calcOne(mid, today, list, Optional.of(chargeMonthlyUsage), false, true, Optional.empty());
        if (run != null) {
            run.setPeriodFrom(today);
            run.setPeriodTo(today);
            run.setPeriodEndAt(LocalDateTime.now(z).withSecond(0).withNano(0));
            settlementRunRepository.save(run);
            settlementArrearsService.applyArrearsToSettledRun(run);
            results.add(run);
        }
        appendReleaseOnlyMerchants(today, mid, results);
        return results;
    }

    /**
     * @param markEveryIncludedTxnSettled true면 배치에 포함된 모든 거래에 settled Y(건당 마감·중복 방지).
     * @param releaseRollingReservesDue     true일 때만 만기 담보 해지를 이번 실행에 반영(건당 연속 호출 시 1회만).
     * @param rollingReserveReleaseCutoffOverride 비어 있지 않으면 담보 해지 조회 시 {@code release_date <=} 에 이 날짜를 씀
     *        (N시간 격자가 자정에 끝날 때 {@code calc_dt} 는 구간 시작일·해지 판정은 구간 종료일을 쓰기 위함).
     */
    private SettlementRun calcOne(String merchantId, LocalDate calcDt, List<PgTrnsctn> txList,
                                   Optional<Boolean> chargeMonthlyUsageOverride,
                                   boolean markEveryIncludedTxnSettled,
                                   boolean releaseRollingReservesDue,
                                   Optional<LocalDate> rollingReserveReleaseCutoffOverride) {
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
        var ledgerSys = hqLedgerSysSettingsService.getOrCreate();
        FeeListRoundingPolicy lr = FeeCurrencyRoundResolver.from(ledgerSys)
                .forCurrency(PayDisplayCurrency.alphaFromSettings(ledgerSys));
        String voidSettleMode = voidRefundSettlementModeResolutionService.resolveVoidSettlementMode(merchantId);
        String manualVoidSettleMode = voidRefundSettlementModeResolutionService.resolveManualVoidSettlementMode(merchantId);
        String refundSettleMode = voidRefundSettlementModeResolutionService.resolveRefundSettlementMode(merchantId);
        String forceRefundSettleMode = voidRefundSettlementModeResolutionService.resolveForceRefundSettlementMode(merchantId);
        BigDecimal approveAmt = BigDecimal.ZERO;
        BigDecimal cancelAmt = BigDecimal.ZERO;
        BigDecimal voidAmt = BigDecimal.ZERO;
        BigDecimal manualVoidAmt = BigDecimal.ZERO;
        BigDecimal refundNormalAmt = BigDecimal.ZERO;
        BigDecimal forceRefundAmt = BigDecimal.ZERO;
        int txCount = txList.size();
        for (PgTrnsctn t : txList) {
            BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            if ("10".equals(st)) {
                approveAmt = approveAmt.add(amt);
            } else if ("20".equals(st)) {
                cancelAmt = cancelAmt.add(amt);
            } else if ("21".equals(st) || "40".equals(st)) {
                voidAmt = voidAmt.add(amt);
            } else if ("22".equals(st) || "41".equals(st)) {
                manualVoidAmt = manualVoidAmt.add(amt);
            } else if ("30".equals(st) || "42".equals(st)) {
                refundNormalAmt = refundNormalAmt.add(amt);
            } else if ("31".equals(st)) {
                forceRefundAmt = forceRefundAmt.add(amt);
            }
        }
        /* 해지일이 도래한 담보금(롤링) → 이번 정산 지급액에 합산 후 RELEASED 처리 */
        BigDecimal releasedFromReserve = BigDecimal.ZERO;
        LocalDateTime releaseStamp = LocalDateTime.now();
        if (releaseRollingReservesDue) {
            LocalDate rollingCutoff = rollingReserveReleaseCutoffOverride.orElse(calcDt);
            List<RollingReserve> maturing = rollingReserveRepository.findByMerchantIdAndStatusAndReleaseDateLessThanEqual(
                    merchantId, "HOLD", rollingCutoff);
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

        releasedFromReserve = FeeListRoundingPolicy.round(releasedFromReserve, lr);

        BigDecimal approveRounded = FeeListRoundingPolicy.round(approveAmt, lr);
        BigDecimal cancelRounded = FeeListRoundingPolicy.round(cancelAmt, lr);
        BigDecimal voidRounded = FeeListRoundingPolicy.round(voidAmt, lr);
        BigDecimal manualVoidRounded = FeeListRoundingPolicy.round(manualVoidAmt, lr);
        BigDecimal refundNormalRounded = FeeListRoundingPolicy.round(refundNormalAmt, lr);
        BigDecimal forceRefundRounded = FeeListRoundingPolicy.round(forceRefundAmt, lr);
        BigDecimal netSales = approveRounded.subtract(cancelRounded);
        if (VoidRefundSettlementModeUtil.subtractVoidAmountFromNet(voidSettleMode)) {
            netSales = netSales.subtract(voidRounded);
        }
        if (VoidRefundSettlementModeUtil.subtractManualVoidAmountFromNet(manualVoidSettleMode)) {
            netSales = netSales.subtract(manualVoidRounded);
        }
        if (VoidRefundSettlementModeUtil.subtractRefundAmountFromNet(refundSettleMode)) {
            netSales = netSales.subtract(refundNormalRounded);
        }
        if (VoidRefundSettlementModeUtil.subtractForceRefundAmountFromNet(forceRefundSettleMode)) {
            netSales = netSales.subtract(forceRefundRounded);
        }

        BigDecimal usageRate = policy.getUsageRate() != null ? policy.getUsageRate() : BigDecimal.ZERO;
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

        SettlementSetting feeVatSs = null;
        if (merchantId != null && !merchantId.isBlank()) {
            feeVatSs = orgUnitRepository.findByCode(merchantId.trim())
                    .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                    .orElse(null);
        }

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

        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        BigDecimal sumTxnFee = BigDecimal.ZERO;
        for (PgTrnsctn t : txList) {
            FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                    t, merchantId, policy, monthCbCountCache, tiersByPolicyId, feeVatSs, lr);
            sumTxnFee = sumTxnFee.add(FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), lr));
        }
        BigDecimal feeExtraFix = CommissionExtraFeeUtil.sumFixedForSettlementRounded(policy, lr);
        /* total_fee: 수수료내역과 동일한 건별 합산 + 월간이용료(실행당 1회) + 기타 FIX(실행당 1회). 정산수수료·송금은 별도 컬럼. */
        BigDecimal totalFeeTxnOnly = FeeListRoundingPolicy.round(sumTxnFee.add(feeUsage).add(feeExtraFix), lr);

        BigDecimal feeSettlementPerTxBd = policy.getFeeSettlementPerTx() != null ? policy.getFeeSettlementPerTx() : BigDecimal.ZERO;
        /* 정산수수료: 정산 실행 1회당 1회(거래 건수와 무관). 담보 해지 전용 실행 등 거래 0건이어도 실행이 있으면 동일. */
        BigDecimal settlementBatchFeeAmt = FeeListRoundingPolicy.round(feeSettlementPerTxBd, lr);
        /* 송금수수료는 정산 실행 지급액·부가세 계산에서 제외(정책의 remittance_transfer_fee 미사용). */
        BigDecimal remittanceFeeAmt = BigDecimal.ZERO;
        BigDecimal feeBaseForVat = totalFeeTxnOnly.add(settlementBatchFeeAmt);
        BigDecimal feeVatAmt = MerchantFeeVatUtil.vatOnFeeAmount(feeBaseForVat, feeVatSs, lr.decimalPlaces());

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
                netSales.subtract(totalFeeTxnOnly).subtract(settlementBatchFeeAmt)
                        .subtract(feeVatAmt).subtract(rollingReserveAmt).add(releasedFromReserve), lr);
        /* 음수 지급액은 0으로 올리지 않음. 절대액은 {@link SettlementArrearsService#applyArrearsToSettledRun} 에서 미수금 자동 등록 */

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
        run.setIncludedTxnCnt(txCount);
        run.setApproveAmt(approveRounded);
        run.setCancelAmt(cancelRounded);
        run.setTotalFee(totalFeeTxnOnly);
        run.setSettlementBatchFeeAmt(settlementBatchFeeAmt);
        run.setRemittanceFeeAmt(remittanceFeeAmt);
        run.setRollingReserveAmt(rollingReserveAmt);
        run.setPayAmt(payAmt);
        run.setStatus("CALCULATED");
        applyCalcCycleSnapshotToRun(run, merchantId);
        applyPayoutHoldStagingIfDue(run, merchantId);
        applySettlementPublishDefaultStaging(run);
        return run;
    }

    /**
     * 정산결과 단계: 지급보류 적치 가맹은 HOLD, 그 외는 배포 전 PENDING.
     */
    private void applySettlementPublishDefaultStaging(SettlementRun run) {
        if (run == null) {
            return;
        }
        if ("Y".equalsIgnoreCase(String.valueOf(run.getPayoutHoldYn()).trim())) {
            run.setSettlementPublishSts("HOLD");
        } else {
            run.setSettlementPublishSts("PENDING");
        }
    }

    /** 실행 저장 시점 가맹 정산주기(정규화) — 이후 가맹 설정이 바뀌어도 행 단위로 표시 유지 */
    private void applyCalcCycleSnapshotToRun(SettlementRun run, String merchantId) {
        if (run == null || merchantId == null || merchantId.isBlank()) {
            return;
        }
        String m = merchantId.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(m);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(m);
        }
        String snap = ou.flatMap(o -> settlementSettingRepository.findByOrgUnitId(o.getId()))
                .map(ss -> ss.getCalcCycle() != null && !ss.getCalcCycle().isBlank()
                        ? SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle().trim())
                        : "")
                .orElse("");
        run.setCalcCycleSnapshot(snap.isEmpty() ? null : snap);
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
