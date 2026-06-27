package com.pg.util;

import com.pg.api.dto.TxnDualLineSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewDisplayTimezoneResolverTest {

    @AfterEach
    void tearDown() {
        ViewDisplayTimezoneResolver.clearRequestOverride();
    }

    @Test
    void trnDateShiftsWithViewOverride() {
        LocalDateTime naive = LocalDateTime.of(2026, 6, 25, 23, 30, 0);
        ZoneId standard = ZoneId.of("Asia/Bangkok");
        LocalDate th = ViewDisplayTimezoneResolver.trnDateInZone(
                naive, standard, Optional.of(ZoneId.of("Asia/Bangkok")));
        assertEquals(2026, th.getYear());
        assertEquals(6, th.getMonthValue());
        assertEquals(25, th.getDayOfMonth());
        LocalDate jp = ViewDisplayTimezoneResolver.trnDateInZone(
                naive, standard, Optional.of(ZoneId.of("Asia/Tokyo")));
        assertEquals(26, jp.getDayOfMonth());
    }

    @Test
    void effectiveDualSpecReplacesSecondLineOnly() {
        TxnDualLineSpec base = new TxnDualLineSpec("JP", ZoneId.of("Asia/Tokyo"),
                "TH", ZoneId.of("Asia/Bangkok"));
        Optional<TxnDualLineSpec> eff = ViewDisplayTimezoneResolver.effectiveDualSpec(
                base, ZoneId.of("Asia/Bangkok"), ZoneId.of("Asia/Tokyo"),
                Optional.of(ZoneId.of("Asia/Seoul")));
        assertTrue(eff.isPresent());
        assertEquals("JP", eff.get().tag1());
        assertEquals("KR", eff.get().tag2());
        assertEquals(ZoneId.of("Asia/Seoul"), eff.get().displayZone2());
    }
}
