package com.pg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 전 PG 요청(08) 장기 대기 → 실패(99)·처리사유·가맹 통보.
 * OMSPAY 전용 스케줄러({@link OmspayPendingExpireScheduler})는 기본 비활성 — 중복 방지.
 */
@Service
public class PendingTxnExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(PendingTxnExpireScheduler.class);

    private final PendingTxnExpireService expireService;

    @Value("${app.pendingExpire.enabled:true}")
    private boolean enabled;

    public PendingTxnExpireScheduler(PendingTxnExpireService expireService) {
        this.expireService = expireService;
    }

    /** 기본 5분마다 — staleMinutes(기본 30) 경과 「요청」 건 처리 */
    @Scheduled(cron = "${app.pendingExpire.cron:0 */5 * * * *}", zone = "Asia/Seoul")
    public void expireStalePending() {
        if (!enabled) {
            return;
        }
        try {
            Map<String, Object> result = expireService.expireStaleBatch();
            int queried = result.get("queried") instanceof Number n ? n.intValue() : 0;
            if (queried > 0) {
                log.info("Pending expire scheduler queried={} paid={} failed={} errors={}",
                        queried, result.get("paid"), result.get("failed"), result.get("errors"));
            }
        } catch (Exception e) {
            log.warn("Pending expire scheduler failed: {}", e.getMessage());
        }
    }
}
