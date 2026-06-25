package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifyUrlDisplayMaskTest {

    @Test
    void masksHttpsUrlWithSchemePrefix() {
        String masked = NotifyUrlDisplayMask.mask("https://notify.example.com/callback/secret");
        assertEquals("https://••••••••••••", masked);
    }

    @Test
    void emptyReturnsEmpty() {
        assertEquals("", NotifyUrlDisplayMask.mask(""));
        assertEquals("", NotifyUrlDisplayMask.mask(null));
    }

    @Test
    void shortUrlUsesBulletsOnly() {
        String masked = NotifyUrlDisplayMask.mask("http://a.co");
        assertTrue(masked.contains("••••"));
    }
}
