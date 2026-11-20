package com.pg.util;

import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgTrnsctnRepository;

import java.util.List;
import java.util.Optional;

/**
 * 동일 주문번호·승인번호에 대한 기존 {@link PgTrnsctn} 조회 — URL/API 행 우선, NOTI guest 중복 방지.
 */
public final class PgTrnsctnOrderLookup {

    private PgTrnsctnOrderLookup() {
    }

    public static Optional<PgTrnsctn> findByMerchantChillTxnOrOrder(PgTrnsctnRepository repo,
                                                                    String merchantId,
                                                                    String chillTxnId,
                                                                    String orderNo) {
        if (repo == null || merchantId == null || merchantId.isBlank()) {
            return Optional.empty();
        }
        String mid = merchantId.trim();
        if (chillTxnId != null && !chillTxnId.isBlank()) {
            String tid = chillTxnId.trim();
            Optional<PgTrnsctn> byChill = repo.findFirstByChillTransactionIdAndMerchantId(tid, mid);
            if (byChill.isPresent()) {
                return byChill;
            }
            Optional<PgTrnsctn> byChillGlobal = repo.findFirstByChillTransactionIdOrderByCreatedAtDesc(tid);
            if (byChillGlobal.isPresent()) {
                return byChillGlobal;
            }
        }
        if (orderNo != null && !orderNo.isBlank()) {
            /* order_no(가맹) 또는 pay_no(ICOPAY→PG opaque ref) */
            return findByMerchantOrderOrPayNo(repo, mid, orderNo.trim());
        }
        return Optional.empty();
    }

    public static Optional<PgTrnsctn> findPreferredByMerchantAndOrder(PgTrnsctnRepository repo,
                                                                      String merchantId,
                                                                      String orderNo) {
        if (repo == null || merchantId == null || orderNo == null
                || merchantId.isBlank() || orderNo.isBlank()) {
            return Optional.empty();
        }
        List<PgTrnsctn> rows = repo.findByMerchantIdAndOrderNoOrderByCreatedAtAsc(
                merchantId.trim(), orderNo.trim());
        return PaidApprovalEvidenceGuard.pickPreferredOrderRowFromList(rows);
    }

    /**
     * PG 콜백용: 가맹 orderNo 또는 ICOPAY→PG 불투명 ref({@code pay_no})로 조회.
     * 신규 결제는 PG가 pay_no(ref)만 돌려주므로 pay_no 우선 후 order_no fallback(레거시).
     */
    public static Optional<PgTrnsctn> findByMerchantOrderOrPayNo(PgTrnsctnRepository repo,
                                                                 String merchantId,
                                                                 String orderOrPgRef) {
        if (repo == null || orderOrPgRef == null || orderOrPgRef.isBlank()) {
            return Optional.empty();
        }
        String key = orderOrPgRef.trim();
        if (merchantId != null && !merchantId.isBlank()) {
            String mid = merchantId.trim();
            Optional<PgTrnsctn> byOrder = findPreferredByMerchantAndOrder(repo, mid, key);
            if (byOrder.isPresent()) {
                return byOrder;
            }
        }
        Optional<PgTrnsctn> byPay = repo.findFirstByPayNoOrderByCreatedAtDesc(key);
        if (byPay.isPresent()) {
            if (merchantId == null || merchantId.isBlank()) {
                return byPay;
            }
            String mid = merchantId.trim();
            String m = byPay.get().getMerchantId();
            if (m != null && mid.equalsIgnoreCase(m.trim())) {
                return byPay;
            }
        }
        if (merchantId == null || merchantId.isBlank()) {
            return repo.findFirstByOrderNoOrderByCreatedAtDesc(key);
        }
        return Optional.empty();
    }

    /** 가맹 MID 없이 PG ref/order 키로만 역추적. */
    public static Optional<PgTrnsctn> findByOrderNoOrPayNo(PgTrnsctnRepository repo, String orderOrPgRef) {
        return findByMerchantOrderOrPayNo(repo, null, orderOrPgRef);
    }

    /** 취소·환불 시 PG에 보낼 주문키 = pay_no(ICOPAY ref) 우선, 없으면 order_no(레거시). */
    public static String pgOrderKey(PgTrnsctn t) {
        if (t == null) {
            return "";
        }
        String pay = t.getPayNo();
        if (pay != null && !pay.isBlank()) {
            return pay.trim();
        }
        String on = t.getOrderNo();
        return on != null ? on.trim() : "";
    }
}
