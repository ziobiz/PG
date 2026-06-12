package com.pg.util;

import com.pg.entity.PgTrnsctn;

/**
 * JPAY {@code transaction_id} → 결제내역 승인번호({@code chill_transaction_id}, {@code approval_no}).
 */
public final class JpayTransactionIdApplier {

    private JpayTransactionIdApplier() {
    }

    public static void apply(PgTrnsctn t, String transactionId) {
        if (t == null || transactionId == null || transactionId.isBlank()) {
            return;
        }
        String tid = transactionId.trim();
        if (tid.length() > 64) {
            tid = tid.substring(0, 64);
        }
        t.setChillTransactionId(tid);
        t.setApprovalNo(tid.length() > 20 ? tid.substring(0, 20) : tid);
    }
}
