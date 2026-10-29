package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.omspay.OmspayApiClient;
import com.pg.integration.pg.omspay.OmspayCredentials;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
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
 * OMSPAY 요청(08) 대기 건: OMS 상태 조회 후 미완료면 실패(99)·가맹 통보.
 * 웹훅 없이 HPP 이탈·만료된 건이 「요청」에 고착되지 않게 합니다.
 */
@Service
public class OmspayPendingExpireService {

    private static final Logger log = LoggerFactory.getLogger(OmspayPendingExpireService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OmspayPaymentService omspayPaymentService;
    private final OmspayApiClient omspayApiClient;
    private final OmspaySaleRecordService omspaySaleRecordService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;

    @Value("${app.omspay.pendingExpire.staleMinutes:30}")
    private int staleMinutes;

    @Value("${app.omspay.pendingExpire.maxAgeDays:14}")
    private int maxAgeDays;

    @Value("${app.omspay.pendingExpire.batchSize:80}")
    private int batchSize;

    public OmspayPendingExpireService(PgTrnsctnRepository pgTrnsctnRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      OmspayPaymentService omspayPaymentService,
                                      OmspayApiClient omspayApiClient,
                                      OmspaySaleRecordService omspaySaleRecordService,
                                      MerchantOutboundNotifyService merchantOutboundNotifyService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.omspayPaymentService = omspayPaymentService;
        this.omspayApiClient = omspayApiClient;
        this.omspaySaleRecordService = omspaySaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
    }

    @Transactional
    public Map<String, Object> expireStaleBatch() {
        Map<String, Object> out = new LinkedHashMap<>();
        int minutes = Math.max(5, staleMinutes);
        int days = Math.max(1, maxAgeDays);
        int limit = Math.min(200, Math.max(1, batchSize));

        LocalDateTime now = LocalDateTime.now(SEOUL);
        LocalDateTime staleBefore = now.minusMinutes(minutes);
        LocalDateTime notOlderThan = now.minusDays(days);

        List<PgTrnsctn> batch = pgTrnsctnRepository.findStaleOmspayPendingForExpire(
                staleBefore, notOlderThan, PageRequest.of(0, limit));
        int paid = 0;
        int failed = 0;
        int skipped = 0;
        int errors = 0;
        for (PgTrnsctn t : batch) {
            try {
                String result = processOne(t);
                switch (result) {
                    case "PAID" -> paid++;
                    case "FAILED" -> failed++;
                    default -> skipped++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("OMSPAY pending expire skip trnId={} orderNo={}: {}",
                        t.getTrnId(), t.getOrderNo(), e.getMessage());
            }
        }
        out.put("queried", batch.size());
        out.put("paid", paid);
        out.put("failed", failed);
        out.put("skipped", skipped);
        out.put("errors", errors);
        out.put("staleMinutes", minutes);
        if (!batch.isEmpty()) {
            log.info("OMSPAY pending expire: queried={} paid={} failed={} skipped={} errors={} staleMin={}",
                    batch.size(), paid, failed, skipped, errors, minutes);
        }
        return out;
    }

    private String processOne(PgTrnsctn t) {
        if (t == null || !"08".equals(t.getStatus() != null ? t.getStatus().trim() : "")) {
            return "SKIP";
        }
        String mid = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        String orderNo = t.getOrderNo() != null ? t.getOrderNo().trim() : "";
        if (mid.isBlank() || orderNo.isBlank()) {
            return "SKIP";
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(mid)
                .or(() -> orgUnitRepository.findByCodeIgnoreCase(mid));
        if (ouOpt.isEmpty()) {
            return expireLocal(t, mid, orderNo, null);
        }
        Long orgUnitId = ouOpt.get().getId();
        String omsCode = firstNonBlank(t.getChillTransactionId(), t.getPayNo(), t.getApprovalNo());
        if (!omsCode.isBlank()) {
            Optional<OmspayCredentials> credOpt = omspayPaymentService.resolveCredentials(orgUnitId);
            if (credOpt.isPresent() && credOpt.get().isUsable()) {
                Map<String, Object> st = omspayApiClient.getOrderStatus(credOpt.get(), omsCode);
                if (Boolean.TRUE.equals(st.get("ok")) && isPaidFlag(st)) {
                    Optional<PgTrnsctn> paid = omspaySaleRecordService.applyPaidOutcome(
                            orderNo, mid, omsCode, null, null);
                    paid.ifPresent(txn -> {
                        try {
                            merchantOutboundNotifyService.scheduleAfterTxnCommit(txn, null, "OMSPAY_EXPIRE_RECONCILE_PAID");
                        } catch (Exception e) {
                            log.warn("OMSPAY expire paid notify: {}", e.getMessage());
                        }
                    });
                    return paid.isPresent() ? "PAID" : "SKIP";
                }
            }
        }
        return expireLocal(t, mid, orderNo, omsCode);
    }

    private String expireLocal(PgTrnsctn t, String mid, String orderNo, String omsCode) {
        Optional<PgTrnsctn> failed = omspaySaleRecordService.applyFailedOutcome(
                orderNo, mid, omsCode,
                "OMSPAY_PENDING_EXPIRED",
                "Payment expired — no completion within " + Math.max(5, staleMinutes) + " minutes");
        if (failed.isEmpty()) {
            return "SKIP";
        }
        PgTrnsctn txn = failed.get();
        if (!"99".equals(txn.getStatus() != null ? txn.getStatus().trim() : "")) {
            return "SKIP";
        }
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(txn, null, "OMSPAY_PENDING_EXPIRED");
        } catch (Exception e) {
            log.warn("OMSPAY expire fail notify: {}", e.getMessage());
        }
        return "FAILED";
    }

    private static boolean isPaidFlag(Map<String, Object> st) {
        if (Boolean.TRUE.equals(st.get("isPaid")) || Boolean.TRUE.equals(st.get("paid"))) {
            return true;
        }
        Object json = st.get("json");
        if (json instanceof com.fasterxml.jackson.databind.JsonNode n) {
            if (n.has("is_paid") && n.get("is_paid").asBoolean(false)) {
                return true;
            }
            if (n.has("paid") && n.get("paid").asBoolean(false)) {
                return true;
            }
            String status = n.has("status") ? n.get("status").asText("") : "";
            if ("paid".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)
                    || "completed".equalsIgnoreCase(status)) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }
}
