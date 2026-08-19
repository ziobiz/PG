package com.pg.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JapanZipLookupServiceTest {

    @Test
    void digitsOnlyStripsHyphen() {
        assertEquals("1000001", JapanZipLookupService.digitsOnly("100-0001"));
        assertEquals("1000001", JapanZipLookupService.digitsOnly("100 0001"));
    }

    @Test
    void validJpZipIsSevenDigits() {
        assertTrue(JapanZipLookupService.isValidJpZip("1000001"));
        assertFalse(JapanZipLookupService.isValidJpZip("100001"));
        assertFalse(JapanZipLookupService.isValidJpZip(""));
    }

    @Test
    void formatAddsHyphen() {
        assertEquals("100-0001", JapanZipLookupService.formatJpZip("1000001"));
        assertEquals("100-0001", JapanZipLookupService.formatJpZip("100-0001"));
    }
}
