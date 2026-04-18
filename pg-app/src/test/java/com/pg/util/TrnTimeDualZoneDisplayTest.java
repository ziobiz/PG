package com.pg.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrnTimeDualZoneDisplayTest {

    @Test
    void bangkokNaive_jpFirst_thTwoHoursBehindJp() {
        LocalDateTime noonBangkok = LocalDateTime.of(2026, 6, 15, 12, 0, 0);
        String s = TrnTimeDualZoneDisplay.formatDualLineTimeOnly(noonBangkok, ZoneId.of("Asia/Bangkok"));
        assertEquals("JP 14:00:00\nTH 12:00:00", s);
    }

    @Test
    void tokyoNaive_wallMatchesJp_thTwoHoursBehind() {
        LocalDateTime wallTokyo = LocalDateTime.of(2026, 6, 15, 14, 30, 0);
        String s = TrnTimeDualZoneDisplay.formatDualLineTimeOnly(wallTokyo, ZoneId.of("Asia/Tokyo"));
        assertEquals("JP 14:30:00\nTH 12:30:00", s);
    }

    @Test
    void range_bangkokNaive_jpLineTwoHoursAheadOfTh() {
        ZoneId bkk = ZoneId.of("Asia/Bangkok");
        LocalDateTime a = LocalDateTime.of(2026, 6, 15, 0, 0, 0);
        LocalDateTime b = LocalDateTime.of(2026, 6, 15, 0, 5, 0);
        String s = TrnTimeDualZoneDisplay.formatDualLineDateTimeRange(a, b, bkk);
        assertTrue(s.startsWith("JP 2026-06-15 02:00:00 ~ 2026-06-15 02:05:00"));
        assertTrue(s.contains("\nTH 2026-06-15 00:00:00 ~ 2026-06-15 00:05:00"));
    }
}
