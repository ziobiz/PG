package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RouteNoDisplayUtilTest {

    @Test
    void absentValues_displayAsDash() {
        assertEquals("-", RouteNoDisplayUtil.formatForDisplay((String) null));
        assertEquals("-", RouteNoDisplayUtil.formatForDisplay(""));
        assertEquals("-", RouteNoDisplayUtil.formatForDisplay("0"));
        assertEquals("-", RouteNoDisplayUtil.formatForDisplay(0));
    }

    @Test
    void presentValues_displayAsIs() {
        assertEquals("4", RouteNoDisplayUtil.formatForDisplay("4"));
        assertEquals("12", RouteNoDisplayUtil.formatForDisplay(12));
    }

    @Test
    void normalizeForStorage_skipsZero() {
        assertNull(RouteNoDisplayUtil.normalizeForStorage(0));
        assertNull(RouteNoDisplayUtil.normalizeForStorage("0"));
        assertEquals("4", RouteNoDisplayUtil.normalizeForStorage(4));
    }
}
