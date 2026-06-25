package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayCardMaskKeyUtilTest {

    @Test
    void maskKeyFromPanUsesFirst6AndLast4() {
        assertEquals("531289***8601", PayCardMaskKeyUtil.maskKeyFromPan("5312891234568601"));
    }

    @Test
    void normalizeMaskInputAcceptsSpacedInput() {
        assertEquals("531289***8601", PayCardMaskKeyUtil.normalizeMaskInput("531289 *** 8601"));
    }

    @Test
    void isValidMaskKeyRequiresPattern() {
        assertTrue(PayCardMaskKeyUtil.isValidMaskKey("531289***8601"));
        assertFalse(PayCardMaskKeyUtil.isValidMaskKey("53128***8601"));
    }

    @Test
    void hashForMaskKeyIsStable() {
        String h1 = PayCardMaskKeyUtil.hashForMaskKey("531289***8601");
        String h2 = PayCardMaskKeyUtil.hashForMaskKey("531289***8601");
        assertEquals(h1, h2);
        assertFalse(h1.isEmpty());
    }
}
