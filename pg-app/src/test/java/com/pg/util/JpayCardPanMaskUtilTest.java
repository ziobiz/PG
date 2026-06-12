package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JpayCardPanMaskUtilTest {

    @Test
    void masksPanLikeJpayPortal() {
        assertEquals("414520***8306", JpayCardPanMaskUtil.maskForStorage("4145201238306"));
    }

    @Test
    void formatBuyerNameJoinsFirstLast() {
        assertEquals("John Doe", JpayCardPanMaskUtil.formatBuyerName("John", "Doe"));
    }

    @Test
    void rejectsShortPan() {
        assertNull(JpayCardPanMaskUtil.maskForStorage("123456789"));
    }
}
