package com.pg.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayReconcileStatusPolicyTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void staleUnpaidProvisional_pendingAfter30Min_notSuccessTradeState() {
        LocalDateTime created = LocalDateTime.now(SEOUL).minusMinutes(45);
        assertTrue(JpayReconcileStatusPolicy.mayApplyStaleUnpaidProvisional(
                "08", "UNPAID", created, 30, SEOUL));
        assertTrue(JpayReconcileStatusPolicy.mayApplyStaleUnpaidProvisional(
                "10", "", created, 30, SEOUL));
    }

    @Test
    void staleUnpaidProvisional_blocksRecentOrConfirmedSuccess() {
        LocalDateTime recent = LocalDateTime.now(SEOUL).minusMinutes(10);
        LocalDateTime old = LocalDateTime.now(SEOUL).minusMinutes(60);
        assertFalse(JpayReconcileStatusPolicy.mayApplyStaleUnpaidProvisional(
                "08", "UNPAID", recent, 30, SEOUL));
        assertFalse(JpayReconcileStatusPolicy.mayApplyStaleUnpaidProvisional(
                "10", "SUCCESS", old, 30, SEOUL));
    }

    @Test
    void mayApplyReconcileMapping_blocksPaid() {
        assertFalse(JpayReconcileStatusPolicy.mayApplyReconcileMapping("10"));
        assertTrue(JpayReconcileStatusPolicy.mayApplyReconcileMapping("20"));
    }
}
