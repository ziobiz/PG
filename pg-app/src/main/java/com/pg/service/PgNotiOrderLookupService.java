package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * NOTI 전 PG 공통 — 주문번호·PG 거래번호로 Comp-Id 조회.
 * OMSPAY·QXIPAY·ElementPay·ILK·ox·QBANG·ChillPay·JPAY·Eximbay·CRYPTO 등 van 무관.
 */
@Service
public class PgNotiOrderLookupService {

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OmspaySaleRecordService omspaySaleRecordService;

    public PgNotiOrderLookupService(PgTrnsctnRepository pgTrnsctnRepository,
                                    OrgUnitRepository orgUnitRepository,
                                    OmspaySaleRecordService omspaySaleRecordService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.omspaySaleRecordService = omspaySaleRecordService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> lookup(String orderHint) {
        Map<String, Object> out = new LinkedHashMap<>();
        String q = orderHint != null ? orderHint.trim() : "";
        if (q.isBlank()) {
            out.put("success", false);
            out.put("errorCode", "INVALID_ORDER");
            out.put("message", "order가 필요합니다.");
            return out;
        }

        Optional<PgTrnsctn> matched = findTxn(q);
        if (matched.isEmpty()) {
            out.put("success", false);
            out.put("errorCode", "NOT_FOUND");
            out.put("message", "주문을 찾을 수 없습니다.");
            out.put("order", q);
            return out;
        }
        PgTrnsctn t = matched.get();
        String compId = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        String orderNo = t.getOrderNo() != null ? t.getOrderNo().trim() : "";
        String pgRef = firstNonBlank(t.getChillTransactionId(), firstNonBlank(t.getPayNo(), t.getApprovalNo()));
        String merchantName = "";
        if (!compId.isBlank()) {
            merchantName = orgUnitRepository.findByCode(compId)
                    .or(() -> orgUnitRepository.findByCodeIgnoreCase(compId))
                    .map(OrgUnit::getName)
                    .orElse("");
        }
        out.put("success", true);
        out.put("compId", compId);
        out.put("CompId", compId);
        out.put("merchantId", compId);
        out.put("merchantName", merchantName != null ? merchantName : "");
        out.put("orderNo", orderNo);
        out.put("pgRef", pgRef);
        out.put("omsSo", pgRef);
        out.put("order", q);
        out.put("trnId", t.getTrnId());
        out.put("status", t.getStatus());
        out.put("van", t.getVan());
        return out;
    }

    private Optional<PgTrnsctn> findTxn(String q) {
        /* Beam charge id(ch_…) 단독 조회 금지 — NOT_FOUND·오매칭 방지 (V5.92). SO-/가맹 orderNo 우선. */
        if (looksLikeBeamChargeId(q)) {
            return Optional.empty();
        }
        /* 1) OMSPAY SO-/note 전용 역추적(기존 로직 재사용) */
        String so = q.toUpperCase().startsWith("SO-") ? q : "";
        Optional<PgTrnsctn> oms = omspaySaleRecordService.resolveTxnForWebhook(so, q, q, q);
        if (oms.isPresent()) {
            return oms;
        }
        if (so.isBlank()) {
            oms = omspaySaleRecordService.resolveTxnForWebhook(q, q, q, q);
            if (oms.isPresent()) {
                return oms;
            }
        }
        /* 2) 전 PG 공통 키 */
        Optional<PgTrnsctn> byOrder = pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(q);
        if (byOrder.isPresent()) {
            return byOrder;
        }
        Optional<PgTrnsctn> byPay = pgTrnsctnRepository.findFirstByPayNoOrderByCreatedAtDesc(q);
        if (byPay.isPresent()) {
            return byPay;
        }
        Optional<PgTrnsctn> byChill = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(q);
        if (byChill.isPresent()) {
            return byChill;
        }
        return pgTrnsctnRepository.findFirstByApprovalNoOrderByCreatedAtDesc(q);
    }

    /** Waylar/Beam {@code ch_…} charge id — Comp-Id order-lookup 키로 쓰지 않음. */
    private static boolean looksLikeBeamChargeId(String q) {
        if (q == null || q.length() < 3) {
            return false;
        }
        return q.regionMatches(true, 0, "ch_", 0, 3);
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
}
