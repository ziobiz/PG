package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayPendingAutoCancelScheduleTest {

    @Test
    void clampMinutes_acceptsAllowedValues() {
        assertEquals(0, JpayPendingAutoCancelSchedule.clampMinutes(0));
        assertEquals(30, JpayPendingAutoCancelSchedule.clampMinutes(30));
        assertEquals(720, JpayPendingAutoCancelSchedule.clampMinutes(720));
    }

    @Test
    void clampMinutes_rejectsUnknownToOff() {
        assertEquals(0, JpayPendingAutoCancelSchedule.clampMinutes(15));
        assertEquals(0, JpayPendingAutoCancelSchedule.clampMinutes(null));
    }

    @Test
    void isEnabled() {
        assertFalse(JpayPendingAutoCancelSchedule.isEnabled(0));
        assertTrue(JpayPendingAutoCancelSchedule.isEnabled(30));
    }
}
