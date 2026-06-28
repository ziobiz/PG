package com.pg.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CardRiskTrackPeriodTest {

    @Test
    void noneModeHasNoWindow() {
        assertFalse(CardRiskTrackPeriod.hasWindow(CardRiskTrackPeriod.MODE_NONE, 1));
        assertNull(CardRiskTrackPeriod.windowStart(CardRiskTrackPeriod.MODE_NONE, 1, LocalDateTime.of(2026, 6, 28, 12, 0)));
        assertEquals("미사용", CardRiskTrackPeriod.formatDisplay(CardRiskTrackPeriod.MODE_NONE, 0));
    }

    @Test
    void dayWindowStart() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 28, 12, 0);
        LocalDateTime start = CardRiskTrackPeriod.windowStart(CardRiskTrackPeriod.MODE_DAY, 1, now);
        assertEquals(LocalDateTime.of(2026, 6, 27, 12, 0), start);
        assertEquals("1일", CardRiskTrackPeriod.formatDisplay(CardRiskTrackPeriod.MODE_DAY, 1));
    }

    @Test
    void monthAndYearWindowStart() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 28, 12, 0);
        assertEquals(LocalDateTime.of(2026, 5, 28, 12, 0),
                CardRiskTrackPeriod.windowStart(CardRiskTrackPeriod.MODE_MONTH, 1, now));
        assertEquals(LocalDateTime.of(2025, 6, 28, 12, 0),
                CardRiskTrackPeriod.windowStart(CardRiskTrackPeriod.MODE_YEAR, 1, now));
    }

    @Test
    void normalizeMode() {
        assertEquals(CardRiskTrackPeriod.MODE_DAY, CardRiskTrackPeriod.normalizeMode("day"));
        assertEquals(CardRiskTrackPeriod.MODE_NONE, CardRiskTrackPeriod.normalizeMode(null));
        assertEquals(CardRiskTrackPeriod.MODE_NONE, CardRiskTrackPeriod.normalizeMode("INVALID"));
    }
}
