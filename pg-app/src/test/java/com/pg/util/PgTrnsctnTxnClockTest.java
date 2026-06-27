package com.pg.util;

import com.pg.entity.PgTrnsctn;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PgTrnsctnTxnClockTest {

    @Test
    void prefersPaidAtOverCreatedAt() {
        PgTrnsctn t = new PgTrnsctn();
        t.setPaidAt(LocalDateTime.of(2026, 6, 19, 15, 20, 0));
        t.setCreatedAt(LocalDateTime.of(2026, 6, 20, 2, 0, 0));
        assertEquals(LocalDate.of(2026, 6, 19), PgTrnsctnTxnClock.effectiveTxnDate(t));
        assertEquals(t.getPaidAt(), PgTrnsctnTxnClock.effectiveTxnDateTime(t));
    }

    @Test
    void fallsBackToCreatedAtWhenPaidAtNull() {
        PgTrnsctn t = new PgTrnsctn();
        t.setCreatedAt(LocalDateTime.of(2026, 6, 20, 2, 0, 0));
        assertEquals(LocalDate.of(2026, 6, 20), PgTrnsctnTxnClock.effectiveTxnDate(t));
    }

    @Test
    void nullTxnReturnsNull() {
        assertNull(PgTrnsctnTxnClock.effectiveTxnDateTime(null));
        assertNull(PgTrnsctnTxnClock.effectiveTxnDate(null));
    }
}
