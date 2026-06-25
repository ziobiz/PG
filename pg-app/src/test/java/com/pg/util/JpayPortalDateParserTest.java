package com.pg.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayPortalDateParserTest {

    @Test
    void parseIsoDateTime() {
        Optional<LocalDate> d = JpayPortalDateParser.parseDate("2026-06-22 15:42:19");
        assertTrue(d.isPresent());
        assertEquals(LocalDate.of(2026, 6, 22), d.get());
    }

    @Test
    void parseExcelSerial() {
        Optional<LocalDate> d = JpayPortalDateParser.parseDate("45529");
        assertTrue(d.isPresent());
    }

    @Test
    void applyToRow() {
        Map<String, Object> row = new HashMap<>();
        JpayPortalDateParser.applyDateTimeFields("2026-06-22 15:42:19", row);
        assertEquals("2026-06-22", row.get("trnDate"));
        assertEquals("15:42:19", row.get("trnTime"));
    }
}
