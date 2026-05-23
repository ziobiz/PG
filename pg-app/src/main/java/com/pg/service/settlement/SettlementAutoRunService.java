package com.pg.service.settlement;

import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementSetting;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.HqSettlementCycleAdminService;
import com.pg.service.MasterDistSettlementCronZoneService;
import com.pg.service.SettlementCalcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.pg.util.BusinessDayCalendar;

/**
 * 정산주기·정산구분(AUTO)에 맞춰 배치 정산을 수행한다.
 * <ul>
 *   <li>M·H·TM·TH: {@link SettlementCycleTiming#shouldRunSubDailyNow} 격자 정각에만 {@link SettlementCalcService#triggerSubDailyAutoSettlement}.</li>
 *   <li>T0: 매 tick(통상 매분) {@link SettlementCalcService#triggerRealtimeAutoSettlementIfDue(String)} 당일 누적 재집계.</li>
 *   <li>RT: 스케줄 미실행 — 승인 노티에서만 건별 정산.</li>
 *   <li>D+·W·WK 등: 마감·영업일·당일 1회 등 기존 {@link SettlementCalcService#execute}.</li>
 *   <li>M/H 직전구간 격자: 정각 tick 을 서버 장애로 놓친 경우, JVM 설정 {@code app.settlement.autoRunSubDailyMissedGraceMinutes}(기본 30분) 이내
 *       누락 슬롯을 매 tick 보강({@link SettlementCalcService#catchUpMissedPlainSubDailyGridSlots}).</li>
 * </ul>
 * 수동 기간 실행은 {@link SettlementCalcService#execute}를 직접 쓰면 된다.
 * 본사 「정산실행」화면에서 기간 미지정 호출은 {@link SettlementCalcService} 경유로 동일 규칙의 즉시 보강에 사용할 수 있다.
 */
@Service
public class SettlementAutoRunService {

    private static final Logger log = LoggerFactory.getLogger(SettlementAutoRunService.class);

    /** 0이면 격자 누락 보강 비활성. 상한은 과도한 역산 방지. */
    private static final int MISSED_SUB_DAILY_GRACE_CAP = 1440;

    private final SettlementCalcService settlementCalcService;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final HqSettlementCycleAdminService hqSettlementCycleAdminService;
    private final MasterDistSettlementCronZoneService masterDistSettlementCronZoneService;
    private final SettlementBusinessHolidayService settlementBusinessHolidayService;
    private final SettlementRunRepository settlementRunRepository;
    private final int autoRunSubDailyMissedGraceMinutes;

    public SettlementAutoRunService(SettlementCalcService settlementCalcService,
                                    OrgUnitRepository orgUnitRepository,
                                    SettlementSettingRepository settlementSettingRepository,
                                    HqSettlementCycleAdminService hqSettlementCycleAdminService,
                                    MasterDistSettlementCronZoneService masterDistSettlementCronZoneService,
                                    SettlementBusinessHolidayService settlementBusinessHolidayService,
                                    SettlementRunRepository settlementRunRepository,
                                    @Value("${app.settlement.autoRunSubDailyMissedGraceMinutes:30}") int autoRunSubDailyMissedGraceMinutes) {
        this.settlementCalcService = settlementCalcService;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqSettlementCycleAdminService = hqSettlementCycleAdminService;
        this.masterDistSettlementCronZoneService = masterDistSettlementCronZoneService;
        this.settlementBusinessHolidayService = settlementBusinessHolidayService;
        this.settlementRunRepository = settlementRunRepository;
        int g = autoRunSubDailyMissedGraceMinutes;
        if (g < 0) {
            g = 0;
        } else if (g > MISSED_SUB_DAILY_GRACE_CAP) {
            g = MISSED_SUB_DAILY_GRACE_CAP;
        }
        this.autoRunSubDailyMissedGraceMinutes = g;
    }

    /**
     * @param todayOverride      모든 가맹에 동일 정산일을 강제할 때만 지정(일반 배치·API 자동는 null).
     *                           null이면 가맹 소속 총판의 {@code settlement_cron_zone_id} 기준 당일·현재 시각.
     * @param merchantIdFilter   가맹점 코드(선택), null/blank 이면 전체
     * @param requireAutoProcType true면 정산구분이 AUTO인 가맹만
     * @return 이번 호출에서 생성·갱신된 정산 실행 행
     */
    public List<SettlementRun> runDueSettlements(LocalDate todayOverride, String merchantIdFilter, boolean requireAutoProcType) {
        Set<String> activeCycles = hqSettlementCycleAdminService.activeNormalizedCycleCodes();

        List<SettlementRun> allRuns = new ArrayList<>();
        List<OrgUnit> merchants = orgUnitRepository.findAll().stream()
                .filter(ou -> ou.getOrgLevel() == OrgLevel.MERCHANT)
                .filter(ou -> !StringUtils.hasText(merchantIdFilter)
                        || merchantIdFilter.trim().equalsIgnoreCase(ou.getCode()))
                .toList();

        for (OrgUnit ou : merchants) {
            String mid = ou.getCode();
            if (!StringUtils.hasText(mid)) {
                continue;
            }
            try {
                ZoneId cronZone = masterDistSettlementCronZoneService.resolveSettlementCronZoneForOrgUnitId(ou.getId());
                LocalDate merchantToday = todayOverride != null ? todayOverride : LocalDate.now(cronZone);
                LocalTime merchantNow = LocalTime.now(cronZone);
                Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.getId());
                if (ssOpt.isEmpty()) {
                    continue;
                }
                SettlementSetting ss = ssOpt.get();
                String cycle = ss.getCalcCycle();
                if (!StringUtils.hasText(cycle) || "NONE".equalsIgnoreCase(cycle.trim())) {
                    continue;
                }
                if (requireAutoProcType) {
                    String proc = ss.getCalcProcType() != null ? ss.getCalcProcType().trim() : "MANUAL";
                    if (!"AUTO".equalsIgnoreCase(proc)) {
                        continue;
                    }
                }
                String c0 = SettlementPeriodResolver.normalizeCalcCycle(cycle);
                if (!activeCycles.contains(c0)) {
                    continue;
                }
                var merchantHolidays = settlementBusinessHolidayService.resolveNonBusinessDatesForMerchantOrgUnitId(ou.getId());
                if ("Y".equalsIgnoreCase(ss.getCalcExcludeYn() != null ? ss.getCalcExcludeYn().trim() : "")
                        && !BusinessDayCalendar.isBusinessDay(merchantToday, merchantHolidays)) {
                    continue;
                }
                if (SettlementCycleTiming.isSubDailyScheduleCode(c0)) {
                    if (autoRunSubDailyMissedGraceMinutes > 0
                            && SettlementCycleTiming.isPlainSubDailyGridClosingCode(c0)) {
                        allRuns.addAll(settlementCalcService.catchUpMissedPlainSubDailyGridSlots(
                                mid, c0, autoRunSubDailyMissedGraceMinutes));
                    }
                    if (SettlementCycleTiming.shouldRunSubDailyNow(merchantNow, c0)) {
                        allRuns.addAll(settlementCalcService.triggerSubDailyAutoSettlement(mid));
                    }
                    continue;
                }
                /* RT: 승인 노티 건별만 — 스케줄에서는 건너뜀 */
                if (SettlementCycleTiming.isRtPerTransactionCode(c0)) {
                    continue;
                }
                /* T0: 당일 0시~현재 누적 1행 — 노티 외에도 매 tick(통상 매분) 재집계해 롤링·합산 유지 */
                if (SettlementCycleTiming.isT0RollingIntradayCode(c0)) {
                    allRuns.addAll(settlementCalcService.triggerRealtimeAutoSettlementIfDue(mid));
                    continue;
                }
                LocalTime close = ss.getCalcCloseTime();
                if (close != null && merchantNow.isBefore(close)) {
                    continue;
                }
                /* D1+·주간 등: 화면·저장 규칙과 동일하게 정산개시시간 이전에는 달력 자동정산 미실행(RT·격자·D0는 미사용) */
                if (SettlementCycleTiming.isCalcStartTimeApplicableForAuto(c0)) {
                    LocalTime start = ss.getCalcStartTime();
                    if (start != null && merchantNow.isBefore(start)) {
                        continue;
                    }
                }
                SettlementPeriodResolver.PeriodWindow w = SettlementPeriodResolver.resolveAutoPeriodWindow(
                        cycle, merchantToday, merchantHolidays);
                if (w == null) {
                    continue;
                }
                if ("D0".equals(c0) && !SettlementCycleTiming.isD0AutoBatchAllowedNow(merchantNow)) {
                    continue;
                }
                /* execute() 저장 calc_dt = period toDate( W7 → 주간 일요일). merchantToday(배치 도래일)와 다르면 매 tick 중복 실행됨 */
                LocalDate runCalcDt = w.toDate();
                if (settlementRunRepository.existsByMerchantNormAndCalendarSlot(
                        mid, w.fromDate(), w.toDate(), runCalcDt)) {
                    continue;
                }
                List<SettlementRun> runs = settlementCalcService.execute(w.fromDate(), w.toDate(), mid);
                if (!runs.isEmpty()) {
                    allRuns.addAll(runs);
                }
            } catch (Exception ex) {
                log.warn("Settlement auto-run merchant failed mid={}: {}", mid, ex.getMessage(), ex);
            }
        }
        if (!allRuns.isEmpty()) {
            log.info("Settlement auto-run: {} row(s)", allRuns.size());
        }
        return allRuns;
    }

    /**
     * {@link #runDueSettlements(LocalDate, String, boolean)} 와 동일한 조건으로, 이번 호출 시점에
     * 스케줄 tick 본문이 <strong>한 건이라도</strong> 정산 호출을 시도할 AUTO 가맹이 있는지(부작용 없음).
     * RT 는 스케줄 미대상. T0 는 조건만 맞으면 true(매분 재집계 가능 구간).
     */
    public boolean peekAnyDueAutoWorkThisTick() {
        Set<String> activeCycles = hqSettlementCycleAdminService.activeNormalizedCycleCodes();
        for (OrgUnit ou : orgUnitRepository.findAll()) {
            if (ou.getOrgLevel() != OrgLevel.MERCHANT) {
                continue;
            }
            String mid = ou.getCode();
            if (!StringUtils.hasText(mid)) {
                continue;
            }
            ZoneId cronZone = masterDistSettlementCronZoneService.resolveSettlementCronZoneForOrgUnitId(ou.getId());
            LocalDate merchantToday = LocalDate.now(cronZone);
            LocalTime merchantNow = LocalTime.now(cronZone);
            Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.getId());
            if (ssOpt.isEmpty()) {
                continue;
            }
            SettlementSetting ss = ssOpt.get();
            String cycle = ss.getCalcCycle();
            if (!StringUtils.hasText(cycle) || "NONE".equalsIgnoreCase(cycle.trim())) {
                continue;
            }
            String proc = ss.getCalcProcType() != null ? ss.getCalcProcType().trim() : "MANUAL";
            if (!"AUTO".equalsIgnoreCase(proc)) {
                continue;
            }
            String c0 = SettlementPeriodResolver.normalizeCalcCycle(cycle);
            if (!activeCycles.contains(c0)) {
                continue;
            }
            var merchantHolidaysPeek = settlementBusinessHolidayService.resolveNonBusinessDatesForMerchantOrgUnitId(ou.getId());
            if ("Y".equalsIgnoreCase(ss.getCalcExcludeYn() != null ? ss.getCalcExcludeYn().trim() : "")
                    && !BusinessDayCalendar.isBusinessDay(merchantToday, merchantHolidaysPeek)) {
                continue;
            }
            if (SettlementCycleTiming.isSubDailyScheduleCode(c0)) {
                if (SettlementCycleTiming.shouldRunSubDailyNow(merchantNow, c0)) {
                    return true;
                }
                if (autoRunSubDailyMissedGraceMinutes > 0
                        && SettlementCycleTiming.isPlainSubDailyGridClosingCode(c0)
                        && merchantHasMissedPlainSubDailyCatchUp(mid, c0, cronZone)) {
                    return true;
                }
                continue;
            }
            if (SettlementCycleTiming.isRtPerTransactionCode(c0)) {
                continue;
            }
            if (SettlementCycleTiming.isT0RollingIntradayCode(c0)) {
                return true;
            }
            LocalTime close = ss.getCalcCloseTime();
            if (close != null && merchantNow.isBefore(close)) {
                continue;
            }
            if (SettlementCycleTiming.isCalcStartTimeApplicableForAuto(c0)) {
                LocalTime start = ss.getCalcStartTime();
                if (start != null && merchantNow.isBefore(start)) {
                    continue;
                }
            }
            SettlementPeriodResolver.PeriodWindow w = SettlementPeriodResolver.resolveAutoPeriodWindow(
                    cycle, merchantToday, merchantHolidaysPeek);
            if (w == null) {
                continue;
            }
            if ("D0".equals(c0) && !SettlementCycleTiming.isD0AutoBatchAllowedNow(merchantNow)) {
                continue;
            }
            LocalDate runCalcDtPeek = w.toDate();
            if (!settlementRunRepository.existsByMerchantNormAndCalendarSlot(
                    mid, w.fromDate(), w.toDate(), runCalcDtPeek)) {
                return true;
            }
        }
        return false;
    }

    /**
     * M/H 격자: grace 이내에 periodEndAt 이 비어 있는(미실행) 마감 시각이 하나라도 있으면 true.
     */
    private boolean merchantHasMissedPlainSubDailyCatchUp(String mid, String c0, ZoneId cronZone) {
        LocalDateTime nowMin = LocalDateTime.now(cronZone).withSecond(0).withNano(0);
        String plainGrid = SettlementCycleTiming.toPlainGridClosingCode(c0);
        for (LocalDateTime endExclusive : SettlementCycleTiming.listMissedPlainSubDailyGridEndsInGrace(
                nowMin, plainGrid, autoRunSubDailyMissedGraceMinutes)) {
            LocalDateTime startInclusive = SettlementCycleTiming.subDailySlotStartInclusiveFromEndExclusive(endExclusive, c0);
            if (startInclusive == null) {
                continue;
            }
            LocalDate gridAnchorCalcDt = startInclusive.toLocalDate();
            if (!settlementRunRepository.existsByMerchantIdAndCalcDtAndPeriodEndAt(mid, gridAnchorCalcDt, endExclusive)) {
                return true;
            }
        }
        return false;
    }
}
