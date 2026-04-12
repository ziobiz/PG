package com.pg.service.settlement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * {@code app.settlement.autoRunEnabled=true} 일 때만 등록된다.
 * 크론은 {@code app.settlement.autoRunCron} (기본 15분마다). 서버는 {@code Asia/Seoul} 권장.
 */
@Component
@ConditionalOnProperty(prefix = "app.settlement", name = "autoRunEnabled", havingValue = "true")
public class SettlementScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(SettlementScheduledJob.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final SettlementAutoRunService settlementAutoRunService;

    public SettlementScheduledJob(SettlementAutoRunService settlementAutoRunService) {
        this.settlementAutoRunService = settlementAutoRunService;
    }

    @Scheduled(cron = "${app.settlement.autoRunCron:0 */15 * * * *}")
    public void runScheduledSettlements() {
        try {
            LocalDate today = LocalDate.now(SEOUL);
            var runs = settlementAutoRunService.runDueSettlements(today, null, true);
            if (runs.isEmpty()) {
                log.trace("Settlement scheduled tick: no runs for {}", today);
            }
        } catch (Exception e) {
            log.warn("Settlement scheduled tick failed: {}", e.getMessage(), e);
        }
    }
}
