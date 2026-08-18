package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementPayInlineStatusUtilTest {

    @Test
    void bankRejectDuringPollStaysPendingAndDoesNotPersistFail() {
        ElementPayInlineStatusUtil.Mapped m = ElementPayInlineStatusUtil.fromGetStatus(204, false);
        assertEquals("PENDING", m.paymentStatus());
        assertFalse(m.paid());
        assertFalse(m.persistFail());
        assertTrue(m.provisionalReject());
    }

    @Test
    void bankRejectOnFinalizePersistsFail() {
        ElementPayInlineStatusUtil.Mapped m = ElementPayInlineStatusUtil.fromGetStatus(204, true);
        assertEquals("FAILED", m.paymentStatus());
        assertTrue(m.persistFail());
        assertFalse(m.provisionalReject());
    }

    @Test
    void paidStatusesPersistSuccess() {
        assertTrue(ElementPayInlineStatusUtil.fromGetStatus(203, false).persistPaid());
        assertTrue(ElementPayInlineStatusUtil.fromGetStatus(205, true).paid());
        assertEquals("PAID", ElementPayInlineStatusUtil.fromGetStatus(205, false).paymentStatus());
    }

    @Test
    void local99IsProvisionalAndCanBeUpgradedByPaidSync() {
        assertTrue(ElementPayInlineStatusUtil.isLocalProvisionalFail("99"));
        assertFalse(ElementPayInlineStatusUtil.skipSyncWhenPaid("99"));
        assertTrue(ElementPayInlineStatusUtil.skipSyncWhenUnpaid("99"));
        assertFalse(ElementPayInlineStatusUtil.isLocalHardFail("99"));
    }

    @Test
    void localPaidAndVoidAreTerminal() {
        assertTrue(ElementPayInlineStatusUtil.skipSyncWhenPaid("10"));
        assertTrue(ElementPayInlineStatusUtil.isLocalHardFail("20"));
        assertTrue(ElementPayInlineStatusUtil.skipSyncWhenPaid("21"));
        assertTrue(ElementPayInlineStatusUtil.skipSyncWhenPaid("42"));
    }
}
