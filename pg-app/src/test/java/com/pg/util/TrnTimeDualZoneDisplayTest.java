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

    @Test
    void configurable_jpLine_bangkokCronLine() {
        LocalDateTime noonBkk = LocalDateTime.of(2026, 6, 15, 12, 0, 0);
        ZoneId bkk = ZoneId.of("Asia/Bangkok");
        String s = TrnTimeDualZoneDisplay.formatConfigurableDualLineTimeOnly(noonBkk, bkk,
                "JP", ZoneId.of("Asia/Tokyo"), "TH", ZoneId.of("Asia/Bangkok"));
        assertEquals("JP 14:00:00\nTH 12:00:00", s);
    }

    @Test
    void withSpec_standardSingapore_oneHourBehindJapan() {
        LocalDateTime sgWall = LocalDateTime.of(2026, 6, 19, 21, 10, 9);
        var spec = new com.pg.api.dto.TxnDualLineSpec(
                "JP", ZoneId.of("Asia/Tokyo"),
                "SG", ZoneId.of("Asia/Singapore"));
        String s = TrnTimeDualZoneDisplay.formatWithSpecTimeOnly(sgWall, spec);
        assertEquals("JP 22:10:09\nSG 21:10:09", s);
    }

    @Test
    void withSpec_standardThailand_twoHoursBehindJapan() {
        LocalDateTime bkkWall = LocalDateTime.of(2026, 6, 19, 20, 10, 9);
        var spec = new com.pg.api.dto.TxnDualLineSpec(
                "JP", ZoneId.of("Asia/Tokyo"),
                "TH", ZoneId.of("Asia/Bangkok"));
        String s = TrnTimeDualZoneDisplay.formatWithSpecTimeOnly(bkkWall, spec);
        assertEquals("JP 22:10:09\nTH 20:10:09", s);
    }

    @Test
    void sameInstant_thVsSg_standardLineDiffersByOneHour() {
        LocalDateTime bkkWall = LocalDateTime.of(2026, 6, 19, 20, 10, 9);
        var thSpec = new com.pg.api.dto.TxnDualLineSpec(
                "JP", ZoneId.of("Asia/Tokyo"), "TH", ZoneId.of("Asia/Bangkok"));
        var sgSpec = new com.pg.api.dto.TxnDualLineSpec(
                "JP", ZoneId.of("Asia/Tokyo"), "SG", ZoneId.of("Asia/Singapore"));
        String thLine = TrnTimeDualZoneDisplay.formatWithSpecTimeOnly(bkkWall, thSpec);
        LocalDateTime sgWall = LocalDateTime.of(2026, 6, 19, 21, 10, 9);
        String sgLine = TrnTimeDualZoneDisplay.formatWithSpecTimeOnly(sgWall, sgSpec);
        assertEquals("JP 22:10:09\nTH 20:10:09", thLine);
        assertEquals("JP 22:10:09\nSG 21:10:09", sgLine);
    }
}
