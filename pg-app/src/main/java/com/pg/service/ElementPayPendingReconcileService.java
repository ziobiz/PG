package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.ElementPayInlineStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ElementPay 요청(08) 대기 건 — 웹훅(NOTI→ICOPAY) 누락 시 getStatus 로 최종 상태 동기화.
 * <p>EP Cabinet Webhook 이 NOTI 로만 가고 ICOPAY ingress 가 끊기면 로컬이 요청에 고착됩니다.
 * 브라우저 폴링·가맹 Status API·본 스케줄러가 getStatus 로 복구합니다.
 */
@Service
public class ElementPayPendingReconcileService {

    private static final Logger log = LoggerFactory.getLogger(ElementPayPendingReconcileService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ElementPayPaymentService elementPayPaymentService;
    private final ElementPaySaleRecordService elementPaySaleRecordService;

    /** 생성 후 이 분 이상 지난 건만 조회(너무 이른 204 선제 실패 방지). */
    @Value("${app.elementpay.pendingReconcile.staleMin:5}")
    private int staleMin;

    /** 이 분 이상이면 getStatus 204/209 를 로컬 실패로 확정. */
    @Value("${app.elementpay.pendingReconcile.finalizeRejectMin:15}")
    private int finalizeRejectMin;

    @Value("${app.elementpay.pendingReconcile.maxAgeDays:2}")
    private int maxAgeDays;

    @Value("${app.elementpay.pendingReconcile.batchSize:5}")
    private int batchSize;

    /** getStatus 호출 사이 대기(ms). EP rate/abuse 탐지 완화. */
    @Value("${app.elementpay.pendingReconcile.delayMs:800}")
    private long delayMs;

    public ElementPayPendingReconcileService(PgTrnsctnRepository pgTrnsctnRepository,
                                             OrgUnitRepository orgUnitRepository,
                                             ElementPayPaymentService elementPayPaymentService,
                                             ElementPaySaleRecordService elementPaySaleRecordService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.elementPayPaymentService = elementPayPaymentService;
        this.elementPaySaleRecordService = elementPaySaleRecordService;
    }

    public Map<String, Object> reconcileBatch() {
        int days = Math.max(1, maxAgeDays);
        int limit = Math.min(200, Math.max(1, batchSize));
        int stale = Math.max(1, staleMin);
        LocalDateTime now = LocalDateTime.now(SEOUL);
        LocalDateTime staleBefore = now.minusMinutes(stale);
        LocalDateTime notOlderThan = now.minusDays(days);
        List<PgTrnsctn> batch = pgTrnsctnRepository.findStaleElementPayPendingForReconcile(
                staleBefore, notOlderThan, PageRequest.of(0, limit));
        return reconcileList(batch, "scheduler");
    }

    /**
     * 특정 paymentId(승인번호·chill_transaction_id) 또는 orderNo 로 강제 동기화.
     */
    public Map<String, Object> reconcileByKeys(List<String> paymentIds, List<String> orderNos,
                                               boolean forceFinalizeReject) {
        List<PgTrnsctn> found = new ArrayList<>();
        if (paymentIds != null) {
            for (String pid : paymentIds) {
                if (pid == null || pid.isBlank()) {
                    continue;
                }
                String p = pid.trim();
                elementPaySaleRecordService.findAnyByPaymentId(p).ifPresent(t -> addUnique(found, t));
            }
        }
        if (orderNos != null) {
            for (String on : orderNos) {
                if (on == null || on.isBlank()) {
                    continue;
                }
                elementPaySaleRecordService.findAnyByOrder(on.trim()).ifPresent(t -> addUnique(found, t));
            }
        }
        Map<String, Object> out = reconcileList(found, forceFinalizeReject ? "hq_force" : "hq");
        out.put("requested", (paymentIds != null ? paymentIds.size() : 0)
                + (orderNos != null ? orderNos.size() : 0));
        out.put("matched", found.size());
        return out;
    }

    public Map<String, Object> reconcileOne(PgTrnsctn t, boolean forceFinalizeReject) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (t == null || t.getTrnId() == null) {
            row.put("success", false);
            row.put("error", "txn_null");
            return row;
        }
        row.put("trnId", t.getTrnId());
        row.put("orderNo", t.getOrderNo());
        row.put("paymentId", firstNonBlank(t.getChillTransactionId(), t.getApprovalNo()));
        row.put("merchantId", t.getMerchantId());
        row.put("beforeStatus", t.getStatus());
        try {
            Optional<Long> orgId = resolveOrgUnitId(t.getMerchantId());
            if (orgId.isEmpty()) {
                row.put("success", false);
                row.put("error", "org_not_found");
                return row;
            }
            boolean finalize = forceFinalizeReject || shouldFinalizeReject(t.getCreatedAt());
            Map<String, Object> st = elementPayPaymentService.queryInlineStatus(
                    orgId.get(),
                    firstNonBlank(t.getChillTransactionId(), t.getApprovalNo()),
                    t.getOrderNo(),
                    finalize);
            row.put("epQuery", st);
            Optional<PgTrnsctn> after = pgTrnsctnRepository.findById(t.getTrnId());
            String afterStatus = after.map(PgTrnsctn::getStatus).orElse(t.getStatus());
            row.put("afterStatus", afterStatus);
            boolean changed = afterStatus != null && !afterStatus.equals(nz(t.getStatus()));
            row.put("updated", changed);
            row.put("finalizeReject", finalize);
            row.put("success", Boolean.TRUE.equals(st.get("success")));
            if (!Boolean.TRUE.equals(st.get("success"))) {
                String err = String.valueOf(st.getOrDefault("message",
                        st.getOrDefault("errorCode", "getStatus_failed")));
                row.put("error", err);
                /*
                 * EP 에서 이미 삭제·만료된 옛 대기건 — getStatus "Payment not found" 이면
                 * finalizeReject 대상은 로컬 실패(99)로 정리해 요청 고착을 해소.
                 */
                if (finalize && err.toLowerCase(java.util.Locale.ROOT).contains("not found")) {
                    Optional<PgTrnsctn> closed = elementPaySaleRecordService.applyOutcome(
                            t.getMerchantId(), t.getOrderNo(), false,
                            firstNonBlank(t.getChillTransactionId(), t.getApprovalNo()),
                            "ELEMENTPAY_STATUS_NOT_FOUND");
                    if (closed.isPresent()) {
                        row.put("updated", true);
                        row.put("afterStatus", closed.get().getStatus());
                        row.put("success", true);
                        log.info("ElementPay reconcile closed-not-found trnId={} order={} →{}",
                                t.getTrnId(), t.getOrderNo(), closed.get().getStatus());
                        return row;
                    }
                }
                log.warn("ElementPay reconcile getStatus fail trnId={} order={} paymentId={} msg={}",
                        t.getTrnId(), t.getOrderNo(), row.get("paymentId"), err);
            }
            if (changed) {
                log.info("ElementPay reconcile updated trnId={} order={} {}→{} paymentStatus={}",
                        t.getTrnId(), t.getOrderNo(), t.getStatus(), afterStatus, st.get("paymentStatus"));
            }
            return row;
        } catch (Exception e) {
            row.put("success", false);
            row.put("error", e.getMessage());
            log.warn("ElementPay reconcile fail trnId={} order={}: {}",
                    t.getTrnId(), t.getOrderNo(), e.getMessage());
            return row;
        }
    }

    private Map<String, Object> reconcileList(List<PgTrnsctn> batch, String source) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("source", source);
        if (batch == null || batch.isEmpty()) {
            out.put("queried", 0);
            out.put("updated", 0);
            out.put("unchanged", 0);
            out.put("failed", 0);
            out.put("items", List.of());
            return out;
        }
        int updated = 0;
        int unchanged = 0;
        int failed = 0;
        List<Map<String, Object>> items = new ArrayList<>();
        boolean first = true;
        for (PgTrnsctn t : batch) {
            String st0 = t.getStatus() != null ? t.getStatus().trim() : "";
            if (!"08".equals(st0) && !ElementPayInlineStatusUtil.isLocalProvisionalFail(st0)) {
                unchanged++;
                continue;
            }
            if (!first && delayMs > 0L) {
                try {
                    Thread.sleep(Math.min(5_000L, Math.max(0L, delayMs)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            first = false;
            Map<String, Object> row = reconcileOne(t, "hq_force".equals(source));
            items.add(row);
            if (!Boolean.TRUE.equals(row.get("success"))) {
                failed++;
            } else if (Boolean.TRUE.equals(row.get("updated"))) {
                updated++;
            } else {
                unchanged++;
            }
        }
        out.put("queried", batch.size());
        out.put("updated", updated);
        out.put("unchanged", unchanged);
        out.put("failed", failed);
        out.put("items", items);
        log.info("ElementPay pending reconcile source={} queried={} updated={} unchanged={} failed={}",
                source, batch.size(), updated, unchanged, failed);
        return out;
    }

    private boolean shouldFinalizeReject(LocalDateTime createdAt) {
        if (createdAt == null) {
            return true;
        }
        int min = Math.max(1, finalizeRejectMin);
        return createdAt.isBefore(LocalDateTime.now(SEOUL).minusMinutes(min));
    }

    private Optional<Long> resolveOrgUnitId(String merchantCode) {
        if (merchantCode == null || merchantCode.isBlank()) {
            return Optional.empty();
        }
        String code = merchantCode.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(code);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(code);
        }
        return ou.map(OrgUnit::getId);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }

    private static void addUnique(List<PgTrnsctn> found, PgTrnsctn t) {
        if (t == null || t.getTrnId() == null) {
            return;
        }
        boolean exists = found.stream().anyMatch(x -> t.getTrnId().equals(x.getTrnId()));
        if (!exists) {
            found.add(t);
        }
    }

    private static String nz(String s) {
        return s != null ? s.trim() : "";
    }
}
