package com.pg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ElementPay 요청(08) 대기 건을 주기적으로 getStatus 조회해 성공/거절을 반영합니다.
 * NOTI→ICOPAY 웹훅이 끊겨도 관리자 결제내역·가맹 통보가 복구되도록 합니다.
 */
@Service
public class ElementPayPendingReconcileScheduler {

    private static final Logger log = LoggerFactory.getLogger(ElementPayPendingReconcileScheduler.class);

    private final ElementPayPendingReconcileService reconcileService;

    /** EP abuse 탐지 방지 — 자동 getStatus 배치 기본 OFF. Status API·본사 수동만 사용. */
    @Value("${app.elementpay.pendingReconcile.enabled:false}")
    private boolean enabled;

    /** EP abuse 탐지 완화 — 기동 시 대량 getStatus 금지(기본 off). */
    @Value("${app.elementpay.pendingReconcile.runOnStartup:false}")
    private boolean runOnStartup;

    public ElementPayPendingReconcileScheduler(ElementPayPendingReconcileService reconcileService) {
        this.reconcileService = reconcileService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        if (!enabled || !runOnStartup) {
            return;
        }
        try {
            Map<String, Object> batch = reconcileService.reconcileBatch();
            log.info("ElementPay reconcile onStartup batch queried={} updated={} failed={}",
                    batch.get("queried"), batch.get("updated"), batch.get("failed"));
        } catch (Exception e) {
            log.warn("ElementPay reconcile onStartup failed: {}", e.getMessage());
        }
    }

    /** 기본 30분 간격·소량 — 연속 payment_id getStatus 패턴 방지 */
    @Scheduled(cron = "${app.elementpay.pendingReconcile.cron:0 */30 * * * *}", zone = "Asia/Seoul")
    public void reconcileStalePending() {
        if (!enabled) {
            return;
        }
        try {
            Map<String, Object> result = reconcileService.reconcileBatch();
            int queried = result.get("queried") instanceof Number n ? n.intValue() : 0;
            if (queried > 0) {
                log.info("ElementPay pending reconcile scheduler queried={} updated={} failed={}",
                        queried, result.get("updated"), result.get("failed"));
            }
        } catch (Exception e) {
            log.warn("ElementPay pending reconcile scheduler failed: {}", e.getMessage());
        }
    }
}
