package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifyToTxnStatusMergeTest {

    @Test
    void unpaidProvisionalCancel_lateFailNotify_becomesFail() {
        String merged = NotifyToTxnStatusMerge.merge(
                "20", "99", "CALLBACK", NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL);
        assertEquals("99", merged);
    }

    @Test
    void unpaidProvisionalCancel_lateSuccessNotify_becomesSuccess() {
        String merged = NotifyToTxnStatusMerge.merge(
                "20", "10", "CALLBACK", NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL);
        assertEquals("10", merged);
    }

    @Test
    void unpaidProvisionalCancel_noFurtherNotify_staysCancel() {
        assertTrue(NotifyToTxnStatusMerge.isUnpaidProvisionalCancel(
                "20", NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL));
    }

    @Test
    void regularCancel_withoutUnpaidCode_lateFailStillWinsByRank() {
        String merged = NotifyToTxnStatusMerge.merge("20", "99", "CALLBACK", null);
        assertEquals("99", merged);
    }

    @Test
    void pending_lateFail_becomesFail() {
        assertEquals("99", NotifyToTxnStatusMerge.merge("08", "99", "CALLBACK"));
    }

    @Test
    void wrongSuccess_downgradeToUnpaidCancel_viaReconcileMerge() {
        assertEquals("20", NotifyToTxnStatusMerge.merge("10", "20", "RESULT"));
    }

    @Test
    void pending_lateSuccessFromReconcile_blockedByPolicy() {
        assertFalse(JpayReconcileStatusPolicy.mayApplyReconcileMapping("10"));
        /* merge 자체는 08→10 허용(노티 경로) — reconcile은 policy로 10 반영 전 차단 */
        assertEquals("10", NotifyToTxnStatusMerge.merge("08", "10", "RESULT"));
    }

    @Test
    void failThenPaid_callbackUpgradesToSuccess() {
        assertEquals("10", NotifyToTxnStatusMerge.merge("99", "10", "CALLBACK"));
    }

    @Test
    void isUnpaidProvisionalCancel_requiresUnpaidCode() {
        assertFalse(NotifyToTxnStatusMerge.isUnpaidProvisionalCancel("20", null));
        assertFalse(NotifyToTxnStatusMerge.isUnpaidProvisionalCancel("20", "MANUAL"));
        assertTrue(NotifyToTxnStatusMerge.isUnpaidProvisionalCancel(
                "20", NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL));
    }
}
