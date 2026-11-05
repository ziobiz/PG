package com.pg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** QBANG QR 서버 PL 폴링 — 기본 1초 (큐뱅 권장). */
@Component
public class QbangPendingPollScheduler {

    private static final Logger log = LoggerFactory.getLogger(QbangPendingPollScheduler.class);

    private final QbangPendingPollService pollService;

    public QbangPendingPollScheduler(QbangPendingPollService pollService) {
        this.pollService = pollService;
    }

    @Scheduled(fixedDelayString = "${app.qbang.pendingPoll.fixedDelayMs:1000}",
            initialDelayString = "${app.qbang.pendingPoll.initialDelayMs:5000}")
    public void tick() {
        try {
            pollService.pollRecentBatch();
        } catch (Exception e) {
            log.warn("QBANG pending poll tick: {}", e.getMessage());
        }
    }
}
