package com.pg.service.settlement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pg.service.HqLedgerSysSettingsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * {@code app.settlement.autoRunEnabled=true} 일 때만 Bean 이 등록된다.
 * 실제 배치 본문은 본사 DB {@code settlement_auto_batch_mode}(ACTIVE / INACTIVE / AUTO) 와 AND —
 * AUTO 는 이번 tick 에 대상 AUTO 가맹이 있을 때만({@link SettlementAutoRunService#peekAnyDueAutoWorkThisTick()}).
 * RT 건별 정산은 이 스케줄과 무관.
 * 크론은 {@code app.settlement.autoRunCron} (기본 매분). 가맹·총판별 정산 시각 기준 Zone 은
 * {@link SettlementAutoRunService} 에서 적용(영업일 프로필과 별도).
 */
@Component
@ConditionalOnProperty(prefix = "app.settlement", name = "autoRunEnabled", havingValue = "true")
public class SettlementScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(SettlementScheduledJob.class);

    private final SettlementAutoRunService settlementAutoRunService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    public SettlementScheduledJob(SettlementAutoRunService settlementAutoRunService,
                                  HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.settlementAutoRunService = settlementAutoRunService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    /** zone=UTC: 매분 tick 은 전역 분 경계만 맞추고, 당일·시각 판단은 가맹 소속 총판의 정산 크론 Zone 에 따름 */
    @Scheduled(cron = "${app.settlement.autoRunCron:0 * * * * *}", zone = "UTC")
    public void runScheduledSettlements() {
        try {
            boolean peekDue = settlementAutoRunService.peekAnyDueAutoWorkThisTick();
            if (!hqLedgerSysSettingsService.isSettlementAutoBatchDbTickAllowed(peekDue)) {
                log.trace("Settlement scheduled tick skipped: settlement_auto_batch_mode disallows this tick (peekDue={})", peekDue);
                return;
            }
            var runs = settlementAutoRunService.runDueSettlements(null, null, true);
            if (runs.isEmpty()) {
                log.trace("Settlement scheduled tick: no runs this tick");
            }
        } catch (Exception e) {
            log.warn("Settlement scheduled tick failed: {}", e.getMessage(), e);
        }
    }
}
