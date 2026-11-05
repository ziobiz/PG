package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * QBANG QR — 브라우저 PL과 별도로 서버가 최근 대기 건을 폴링해 승인 즉시 OKAY.
 * (큐뱅: 폴링 중단·OKAY 지연 시 내부 망취소 가능)
 */
@Service
public class QbangPendingPollService {

    private static final Logger log = LoggerFactory.getLogger(QbangPendingPollService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final QbangPaymentService qbangPaymentService;

    @Value("${app.qbang.pendingPoll.batchSize:40}")
    private int batchSize;

    /** 유효 최대 120초 + 여유 — 그보다 오래된 08 은 이 폴러 대상에서 제외 */
    @Value("${app.qbang.pendingPoll.maxAgeSeconds:180}")
    private int maxAgeSeconds;

    public QbangPendingPollService(PgTrnsctnRepository pgTrnsctnRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   QbangPaymentService qbangPaymentService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.qbangPaymentService = qbangPaymentService;
    }

    public Map<String, Object> pollRecentBatch() {
        Map<String, Object> out = new LinkedHashMap<>();
        int age = Math.max(QbangPaymentService.QR_TTL_MAX_SECONDS, maxAgeSeconds);
        int limit = Math.min(100, Math.max(1, batchSize));
        LocalDateTime notOlderThan = LocalDateTime.now(SEOUL).minusSeconds(age);
        List<PgTrnsctn> batch = pgTrnsctnRepository.findRecentQbangPendingForPoll(
                notOlderThan, PageRequest.of(0, limit));
        int paid = 0;
        int failed = 0;
        int pending = 0;
        int errors = 0;
        for (PgTrnsctn t : batch) {
            try {
                String mid = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
                String orderNo = t.getOrderNo() != null ? t.getOrderNo().trim() : "";
                String txn = t.getChillTransactionId() != null ? t.getChillTransactionId().trim() : "";
                if (mid.isBlank() || orderNo.isBlank() || txn.isBlank()) {
                    pending++;
                    continue;
                }
                Optional<OrgUnit> ou = orgUnitRepository.findByCode(mid)
                        .or(() -> orgUnitRepository.findByCodeIgnoreCase(mid));
                if (ou.isEmpty()) {
                    errors++;
                    continue;
                }
                Map<String, Object> r = qbangPaymentService.pollAndMaybeConfirm(ou.get().getId(), orderNo, txn);
                if (!Boolean.TRUE.equals(r.get("success"))) {
                    errors++;
                    continue;
                }
                Object data = r.get("data");
                String st = "";
                if (data instanceof Map<?, ?> m) {
                    Object ps = m.get("paymentStatus");
                    st = ps != null ? String.valueOf(ps) : "";
                }
                if ("PAID".equalsIgnoreCase(st)) {
                    paid++;
                } else if ("FAILED".equalsIgnoreCase(st)) {
                    failed++;
                } else {
                    pending++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("QBANG server poll skip trnId={}: {}", t.getTrnId(), e.getMessage());
            }
        }
        out.put("queried", batch.size());
        out.put("paid", paid);
        out.put("failed", failed);
        out.put("pending", pending);
        out.put("errors", errors);
        if (!batch.isEmpty() && (paid > 0 || failed > 0 || errors > 0)) {
            log.info("QBANG pending poll: queried={} paid={} failed={} pending={} errors={}",
                    batch.size(), paid, failed, pending, errors);
        }
        return out;
    }
}
