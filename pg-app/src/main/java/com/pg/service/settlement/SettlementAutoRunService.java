package com.pg.service.settlement;

import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementSetting;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.SettlementCalcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.pg.util.BusinessDayCalendar;

/**
 * 정산주기·정산구분(AUTO)에 맞춰 {@link SettlementCalcService#execute}를 호출한다.
 * 수동 기간 실행은 {@link SettlementCalcService#execute}를 직접 쓰면 된다.
 */
@Service
public class SettlementAutoRunService {

    private static final Logger log = LoggerFactory.getLogger(SettlementAutoRunService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final SettlementCalcService settlementCalcService;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;

    public SettlementAutoRunService(SettlementCalcService settlementCalcService,
                                    OrgUnitRepository orgUnitRepository,
                                    SettlementSettingRepository settlementSettingRepository) {
        this.settlementCalcService = settlementCalcService;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
    }

    /**
     * @param today              기준일(통상 서울 달력일)
     * @param merchantIdFilter   가맹점 코드(선택), null/blank 이면 전체
     * @param requireAutoProcType true면 정산구분이 AUTO인 가맹만
     * @return 이번 호출에서 생성·갱신된 정산 실행 행
     */
    public List<SettlementRun> runDueSettlements(LocalDate today, String merchantIdFilter, boolean requireAutoProcType) {
        LocalDate day = today != null ? today : LocalDate.now(SEOUL);
        LocalTime now = LocalTime.now(SEOUL);

        Set<String> alreadyDoneToday = settlementCalcService.listRuns(day, day).stream()
                .map(SettlementRun::getMerchantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

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
            if (alreadyDoneToday.contains(mid)) {
                continue;
            }
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
            LocalTime close = ss.getCalcCloseTime();
            if (close != null && now.isBefore(close)) {
                continue;
            }
            if ("Y".equalsIgnoreCase(ss.getCalcExcludeYn() != null ? ss.getCalcExcludeYn().trim() : "")
                    && !BusinessDayCalendar.isBusinessDay(day, Collections.emptySet())) {
                continue;
            }
            SettlementPeriodResolver.PeriodWindow w = SettlementPeriodResolver.resolveAutoPeriodWindow(cycle, day);
            if (w == null) {
                continue;
            }
            List<SettlementRun> runs = settlementCalcService.execute(w.fromDate(), w.toDate(), mid);
            if (!runs.isEmpty()) {
                allRuns.addAll(runs);
            }
        }
        if (!allRuns.isEmpty()) {
            log.info("Settlement auto-run: {} row(s) for date {}", allRuns.size(), day);
        }
        return allRuns;
    }
}
