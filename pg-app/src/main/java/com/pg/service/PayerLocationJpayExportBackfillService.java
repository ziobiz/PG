package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.config.GeoIpProperties;
import com.pg.entity.JpayPortalExportCache;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.JpayPortalExportCacheRepository;
import com.pg.repository.PgTrnsctnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JPAY 포털 Export 캐시 — {@code Customer IP} 파싱으로 payer_location_label 보정(비동기·동기화 경로와 분리). */
@Service
public class PayerLocationJpayExportBackfillService {

    private static final Logger log = LoggerFactory.getLogger(PayerLocationJpayExportBackfillService.class);
    private static final TypeReference<List<Map<String, Object>>> ROW_LIST = new TypeReference<>() {
    };

    private final GeoIpProperties geoIpProperties;
    private final JpayPortalExportCacheRepository exportCacheRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PayerLocationEnrichmentService enrichmentService;
    private final ObjectMapper objectMapper;

    public PayerLocationJpayExportBackfillService(GeoIpProperties geoIpProperties,
                                                  JpayPortalExportCacheRepository exportCacheRepository,
                                                  PgTrnsctnRepository pgTrnsctnRepository,
                                                  PayerLocationEnrichmentService enrichmentService,
                                                  ObjectMapper objectMapper) {
        this.geoIpProperties = geoIpProperties;
        this.exportCacheRepository = exportCacheRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.enrichmentService = enrichmentService;
        this.objectMapper = objectMapper;
    }

    @Async
    public void scheduleBackfillFromExportCacheAsync() {
        if (!geoIpProperties.getJpayExportBackfill().isEnabled()) {
            return;
        }
        try {
            int n = backfillFromExportCache();
            if (n > 0) {
                log.info("JPAY export Customer IP backfill updated {} rows", n);
            }
        } catch (Exception e) {
            log.warn("JPAY export Customer IP backfill failed: {}", e.getMessage());
        }
    }

    @Transactional
    public int backfillFromExportCache() {
        JpayPortalExportCache cache = exportCacheRepository.findById(JpayPortalExportCache.DEFAULT_KEY).orElse(null);
        if (cache == null || cache.getRowsJson() == null || cache.getRowsJson().isBlank()) {
            return 0;
        }
        List<Map<String, Object>> rows;
        try {
            rows = objectMapper.readValue(cache.getRowsJson(), ROW_LIST);
        } catch (Exception e) {
            log.debug("JPAY export cache parse failed: {}", e.getMessage());
            return 0;
        }
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        int batchCap = Math.max(1, Math.min(geoIpProperties.getJpayExportBackfill().getBatchSize(), 5000));
        int updated = 0;
        List<PgTrnsctn> dirty = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (updated >= batchCap) {
                break;
            }
            String customerIp = str(row.get("Customer IP"));
            if (customerIp.isEmpty()) {
                continue;
            }
            Optional<PgTrnsctn> txn = resolveTxn(row);
            if (txn.isEmpty()) {
                continue;
            }
            PgTrnsctn t = txn.get();
            if (com.pg.urlpay.PayerLocationLabelFormatter.isCompleteOverviewLabel(t.getPayerLocationLabel())) {
                continue;
            }
            if (enrichmentService.enrichFromJpayCustomerIpField(t, customerIp)) {
                dirty.add(t);
                updated++;
            }
        }
        if (!dirty.isEmpty()) {
            pgTrnsctnRepository.saveAll(dirty);
        }
        return updated;
    }

    private Optional<PgTrnsctn> resolveTxn(Map<String, Object> row) {
        String txnId = str(row.get("Transaction ID"));
        if (!txnId.isEmpty()) {
            Optional<PgTrnsctn> byChill = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(txnId);
            if (byChill.isPresent()) {
                return byChill;
            }
        }
        String orderNo = str(row.get("Merchant Order Number"));
        if (!orderNo.isEmpty()) {
            return pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(orderNo);
        }
        return Optional.empty();
    }

    private static String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }
}
