package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayTrSyncScheduleTest {

    @Test
    void clampMinutes_acceptsAllowedValues() {
        assertEquals(0, JpayTrSyncSchedule.clampMinutes(0));
        assertEquals(10, JpayTrSyncSchedule.clampMinutes(10));
        assertEquals(720, JpayTrSyncSchedule.clampMinutes(720));
    }

    @Test
    void clampMinutes_rejectsUnknown() {
        assertEquals(0, JpayTrSyncSchedule.clampMinutes(15));
        assertEquals(0, JpayTrSyncSchedule.clampMinutes(null));
    }

    @Test
    void isEnabled() {
        assertFalse(JpayTrSyncSchedule.isEnabled(0));
        assertTrue(JpayTrSyncSchedule.isEnabled(360));
    }
}
