package com.pg.service;

import com.pg.util.JpayTrSyncSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * JPAY 통합조회 — 전산설정 주기 자동 동기화 + 매일 00:00 기본 동기화.
 * ICOPAY 접속 없이 서버가 {@link JpayIntegratedListService#startSyncJob} 을 실행합니다.
 */
@Service
public class JpayPortalExportScheduler {

    private static final Logger log = LoggerFactory.getLogger(JpayPortalExportScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final JpayIntegratedListService jpayIntegratedListService;

    private volatile LocalDateTime lastTriggeredAt = LocalDateTime.now(SEOUL);

    public JpayPortalExportScheduler(HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                     JpayIntegratedListService jpayIntegratedListService) {
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.jpayIntegratedListService = jpayIntegratedListService;
    }

    /** 매일 00:00(서울) — 스케줄과 별도 기본 동기화(어제·오늘 2일). */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void basicMidnightSync() {
        try {
            log.info("JPAY 통합조회 기본 동기화(00:00·2일) 시작");
            jpayIntegratedListService.runBasicMidnightSyncIfDue();
        } catch (Exception e) {
            log.warn("JPAY 통합조회 기본 동기화 시작 실패: {}", e.getMessage());
        }
    }

    /** 1분마다 전산설정 주기를 확인해 경과 시 증분 동기화를 시작합니다. */
    @Scheduled(fixedRate = 60_000, zone = "Asia/Seoul")
    public void tick() {
        int intervalMin = resolveIntervalMinutes();
        if (!JpayTrSyncSchedule.isEnabled(intervalMin)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(SEOUL);
        if (lastTriggeredAt != null
                && lastTriggeredAt.plusMinutes(intervalMin).isAfter(now)) {
            return;
        }
        Map<String, Object> status = jpayIntegratedListService.syncJobStatusMap();
        if ("RUNNING".equalsIgnoreCase(String.valueOf(status.getOrDefault("status", "")))) {
            return;
        }
        lastTriggeredAt = now;
        try {
            log.info("JPAY 통합조회 스케줄 동기화 시작 (주기 {}분)", intervalMin);
            jpayIntegratedListService.startSyncJob(null, null, JpaySyncTrigger.SCHEDULED);
        } catch (Exception e) {
            log.warn("JPAY 통합조회 스케줄 동기화 시작 실패: {}", e.getMessage());
        }
    }

    private int resolveIntervalMinutes() {
        try {
            var settings = hqLedgerSysSettingsService.getOrCreate();
            return JpayTrSyncSchedule.clampMinutes(settings.getJpayTrSyncScheduleMin());
        } catch (Exception e) {
            log.debug("JPAY sync schedule settings read failed: {}", e.getMessage());
            return JpayTrSyncSchedule.OFF;
        }
    }
}
