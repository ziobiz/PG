package com.pg.service;

import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.JpayPendingAutoCancelSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * JPAY 요청(08) 대기 건 중 노티가 오지 않은 미완료 결제를 Trade Query로 주기 조회해
 * UNPAID 등 터미널 상태(주로 취소 20)로 자동 반영합니다.
 * 대기 시간은 전산설정관리 {@code jpayPendingAutoCancelMin}(0=미사용)을 따릅니다.
 */
@Service
public class JpayPendingReconcileScheduler {

    private static final Logger log = LoggerFactory.getLogger(JpayPendingReconcileScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final JpayTradeApiService jpayTradeApiService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    @Value("${app.jpay.pendingReconcile.enabled:true}")
    private boolean enabled;

    @Value("${app.jpay.pendingReconcile.maxAgeDays:14}")
    private int maxAgeDays;

    @Value("${app.jpay.pendingReconcile.batchSize:50}")
    private int batchSize;

    public JpayPendingReconcileScheduler(PgTrnsctnRepository pgTrnsctnRepository,
                                         JpayTradeApiService jpayTradeApiService,
                                         HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.jpayTradeApiService = jpayTradeApiService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    @Scheduled(cron = "${app.jpay.pendingReconcile.cron:0 */15 * * * *}", zone = "Asia/Seoul")
    public void reconcileStalePending() {
        if (!enabled) {
            return;
        }
        int minutes = hqLedgerSysSettingsService.resolveJpayPendingAutoCancelMin();
        if (!JpayPendingAutoCancelSchedule.isEnabled(minutes)) {
            return;
        }
        int days = Math.max(1, maxAgeDays);
        int limit = Math.min(200, Math.max(1, batchSize));

        LocalDateTime now = LocalDateTime.now(SEOUL);
        LocalDateTime staleBefore = now.minusMinutes(minutes);
        LocalDateTime notOlderThan = now.minusDays(days);

        List<PgTrnsctn> batch = pgTrnsctnRepository.findStaleJpayPendingForReconcile(
                staleBefore, notOlderThan, PageRequest.of(0, limit));
        if (batch.isEmpty()) {
            return;
        }

        int updated = 0;
        int unchanged = 0;
        int failed = 0;
        for (PgTrnsctn t : batch) {
            try {
                Map<String, Object> result = jpayTradeApiService.queryAndApplyToTxn(t.getTrnId());
                if (Boolean.TRUE.equals(result.get("updated"))) {
                    updated++;
                } else {
                    unchanged++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("JPAY pending reconcile skip trnId={} orderNo={}: {}",
                        t.getTrnId(), t.getOrderNo(), e.getMessage());
            }
        }
        log.info("JPAY pending reconcile: queried={} updated={} unchanged={} failed={} staleMin={}",
                batch.size(), updated, unchanged, failed, minutes);
    }
}
