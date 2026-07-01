package com.pg.service;

import com.pg.config.GeoIpProperties;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgTrnsctnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 과거 거래 — payer_client_ip 기준 payer_location_label 일괄 보정(새벽·소량 배치). */
@Service
public class PayerLocationBackfillScheduler {

    private static final Logger log = LoggerFactory.getLogger(PayerLocationBackfillScheduler.class);

    private final GeoIpProperties geoIpProperties;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PayerLocationEnrichmentService enrichmentService;

    public PayerLocationBackfillScheduler(GeoIpProperties geoIpProperties,
                                          PgTrnsctnRepository pgTrnsctnRepository,
                                          PayerLocationEnrichmentService enrichmentService) {
        this.geoIpProperties = geoIpProperties;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.enrichmentService = enrichmentService;
    }

    @Scheduled(cron = "${app.geoip.backfill.cron:0 15 3 * * *}", zone = "Asia/Seoul")
    public void scheduledBackfill() {
        if (!geoIpProperties.getBackfill().isEnabled()) {
            return;
        }
        try {
            int n = 0;
            int batch = geoIpProperties.getBackfill().getBatchSize();
            for (int round = 0; round < 10; round++) {
                int u = runBackfillBatch(batch);
                n += u;
                if (u < Math.max(1, Math.min(batch, 1000))) {
                    break;
                }
            }
            if (n > 0) {
                log.info("payer location IP backfill updated {} rows", n);
            }
        } catch (Exception e) {
            log.warn("payer location IP backfill failed: {}", e.getMessage());
        }
    }

    @Transactional
    public int runBackfillBatch(int batchSize) {
        int limit = Math.max(1, Math.min(batchSize, 1000));
        List<PgTrnsctn> rows = pgTrnsctnRepository.findPayerLocationBackfillCandidates(PageRequest.of(0, limit));
        if (rows.isEmpty()) {
            return 0;
        }
        int updated = 0;
        for (PgTrnsctn t : rows) {
            String before = t.getPayerLocationLabel();
            if (before != null && !before.isBlank()
                    && !com.pg.urlpay.PayerLocationLabelFormatter.isCompleteOverviewLabel(before)) {
                t.setPayerLocationLabel(null);
            }
            enrichmentService.enrichFromTxnContext(t);
            if (t.getPayerLocationLabel() != null && !t.getPayerLocationLabel().isBlank()
                    && (before == null || before.isBlank() || !before.equals(t.getPayerLocationLabel()))) {
                updated++;
            }
        }
        pgTrnsctnRepository.saveAll(rows);
        return updated;
    }
}
