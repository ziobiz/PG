package com.pg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OMSPAY 요청(08) 장기 대기 → OMS 조회 후 미완료면 실패(99)·가맹 통보.
 */
@Service
public class OmspayPendingExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(OmspayPendingExpireScheduler.class);

    private final OmspayPendingExpireService expireService;

    @Value("${app.omspay.pendingExpire.enabled:true}")
    private boolean enabled;

    public OmspayPendingExpireScheduler(OmspayPendingExpireService expireService) {
        this.expireService = expireService;
    }

    /** 기본 5분마다 — staleMinutes(기본 30) 경과 요청 건 처리 */
    @Scheduled(cron = "${app.omspay.pendingExpire.cron:0 */5 * * * *}", zone = "Asia/Seoul")
    public void expireStalePending() {
        if (!enabled) {
            return;
        }
        try {
            Map<String, Object> result = expireService.expireStaleBatch();
            int queried = result.get("queried") instanceof Number n ? n.intValue() : 0;
            if (queried > 0) {
                log.info("OMSPAY pending expire scheduler queried={} paid={} failed={} errors={}",
                        queried, result.get("paid"), result.get("failed"), result.get("errors"));
            }
        } catch (Exception e) {
            log.warn("OMSPAY pending expire scheduler failed: {}", e.getMessage());
        }
    }
}
