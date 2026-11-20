package com.pg.service;

import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.TxnOutcomeReasonApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 전 PG 공통 — 요청(08)이 {@code staleMinutes} 경과해도 승인·웹훅이 없으면 실패(99)로 전환하고 처리사유를 남긴다.
 * OMSPAY는 기존처럼 OMS 상태 조회 후 미완료일 때만 실패. PAYSO·KSNET·기타 URL/인라인 대기는 로컬 만료.
 */
@Service
public class PendingTxnExpireService {

    private static final Logger log = LoggerFactory.getLogger(PendingTxnExpireService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    public static final String REASON_CODE = "PENDING_PAYMENT_EXPIRED";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OmspayPendingExpireService omspayPendingExpireService;
    private final PaysoSaleRecordService paysoSaleRecordService;
    private final KsnetSaleRecordService ksnetSaleRecordService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;

    /** 기본 30분 — OMSPAY 기존 정책과 동일. */
    @Value("${app.pendingExpire.staleMinutes:${app.omspay.pendingExpire.staleMinutes:30}}")
    private int staleMinutes;

    @Value("${app.pendingExpire.maxAgeDays:${app.omspay.pendingExpire.maxAgeDays:14}}")
    private int maxAgeDays;

    @Value("${app.pendingExpire.batchSize:${app.omspay.pendingExpire.batchSize:120}}")
    private int batchSize;

    public PendingTxnExpireService(PgTrnsctnRepository pgTrnsctnRepository,
                                   OmspayPendingExpireService omspayPendingExpireService,
                                   PaysoSaleRecordService paysoSaleRecordService,
                                   KsnetSaleRecordService ksnetSaleRecordService,
                                   MerchantOutboundNotifyService merchantOutboundNotifyService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.omspayPendingExpireService = omspayPendingExpireService;
        this.paysoSaleRecordService = paysoSaleRecordService;
        this.ksnetSaleRecordService = ksnetSaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
    }

    @Transactional
    public Map<String, Object> expireStaleBatch() {
        Map<String, Object> out = new LinkedHashMap<>();
        int minutes = Math.max(5, staleMinutes);
        int days = Math.max(1, maxAgeDays);
        int limit = Math.min(300, Math.max(1, batchSize));

        LocalDateTime now = LocalDateTime.now(SEOUL);
        LocalDateTime staleBefore = now.minusMinutes(minutes);
        LocalDateTime notOlderThan = now.minusDays(days);

        List<PgTrnsctn> batch = pgTrnsctnRepository.findStalePendingForExpire(
                staleBefore, notOlderThan, PageRequest.of(0, limit));
        int paid = 0;
        int failed = 0;
        int skipped = 0;
        int errors = 0;
        for (PgTrnsctn t : batch) {
            try {
                String result = processOne(t, minutes);
                switch (result) {
                    case "PAID" -> paid++;
                    case "FAILED" -> failed++;
                    default -> skipped++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("Pending expire skip trnId={} van={} orderNo={}: {}",
                        t.getTrnId(), t.getVan(), t.getOrderNo(), e.getMessage());
            }
        }
        out.put("queried", batch.size());
        out.put("paid", paid);
        out.put("failed", failed);
        out.put("skipped", skipped);
        out.put("errors", errors);
        out.put("staleMinutes", minutes);
        if (!batch.isEmpty()) {
            log.info("Pending expire (all PG): queried={} paid={} failed={} skipped={} errors={} staleMin={}",
                    batch.size(), paid, failed, skipped, errors, minutes);
        }
        return out;
    }

    private String processOne(PgTrnsctn t, int minutes) {
        if (t == null || !"08".equals(trim(t.getStatus()))) {
            return "SKIP";
        }
        String van = t.getVan() != null ? t.getVan().trim() : "";
        String mid = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        String orderNo = t.getOrderNo() != null ? t.getOrderNo().trim() : "";
        if (mid.isBlank() || orderNo.isBlank()) {
            return "SKIP";
        }

        if (PgVendor.isOmspayFamily(van)) {
            return omspayPendingExpireService.expireOne(t);
        }
        if (PgVendor.isPaysoFamily(van)) {
            Optional<PgTrnsctn> failed = paysoSaleRecordService.applyFailedOutcome(
                    orderNo, mid, null, "PAYSO_PENDING_EXPIRED", expireReasonMessage(minutes));
            return notifyIfFailed(failed, "PAYSO_PENDING_EXPIRED");
        }
        if (PgVendor.isKsnetFamily(van)) {
            Optional<PgTrnsctn> failed = ksnetSaleRecordService.applyPendingExpired(mid, orderNo, minutes);
            return notifyIfFailed(failed, "KSNET_PENDING_EXPIRED");
        }
        return expireGeneric(t, minutes);
    }

    private String expireGeneric(PgTrnsctn t, int minutes) {
        String prev = trim(t.getStatus());
        if (!"08".equals(prev)) {
            return "SKIP";
        }
        t.setStatus("99");
        String msg = expireReasonMessage(minutes);
        t.setChillPaymentStatus(msg.length() > 50 ? msg.substring(0, 50) : msg);
        TxnOutcomeReasonApplier.apply(t, prev, "99", msg, REASON_CODE, TxnOutcomeReasonApplier.SOURCE_ICOPAY);
        pgTrnsctnRepository.save(t);
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, REASON_CODE);
        } catch (Exception e) {
            log.warn("Pending expire notify: {}", e.getMessage());
        }
        return "FAILED";
    }

    private String notifyIfFailed(Optional<PgTrnsctn> failed, String notifyTag) {
        if (failed.isEmpty()) {
            return "SKIP";
        }
        PgTrnsctn txn = failed.get();
        if (!"99".equals(trim(txn.getStatus()))) {
            return "SKIP";
        }
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(txn, null, notifyTag);
        } catch (Exception e) {
            log.warn("Pending expire notify {}: {}", notifyTag, e.getMessage());
        }
        return "FAILED";
    }

    public static String expireReasonMessage(int staleMinutes) {
        int m = Math.max(5, staleMinutes);
        return "결제 시간 초과(미완료) — " + m + "분 내 승인·완료 통보가 없어 실패 처리되었습니다. "
                + "/ Payment timed out — no approval within " + m + " minutes. "
                + "/ 決済時間超過(未完了) — " + m + "分以内に承認がなく失敗処理しました。 "
                + "/ 支付超时(未完成) — " + m + " 分钟内未获批准，已作失败处理。 "
                + "/ หมดเวลาชำระ (ไม่สำเร็จ) — ไม่มีการอนุมัติภายใน " + m + " นาที จึงถือว่าล้มเหลว";
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }
}
