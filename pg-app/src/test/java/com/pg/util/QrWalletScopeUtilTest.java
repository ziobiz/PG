package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrWalletScopeUtilTest {

    @Test
    void blankMeansNoQr() {
        assertEquals("", QrWalletScopeUtil.normalize(null));
        assertEquals("", QrWalletScopeUtil.normalize(""));
        assertEquals("", QrWalletScopeUtil.normalize("NONE"));
        assertFalse(QrWalletScopeUtil.matches("", "PAYPAY"));
        assertEquals("-", QrWalletScopeUtil.displayJoined(List.of("", "NONE")));
    }

    @Test
    void payPayIsStoredAndMatched() {
        assertEquals("PAYPAY", QrWalletScopeUtil.normalize(" paypay "));
        assertTrue(QrWalletScopeUtil.matches("PAYPAY", "PAYPAY"));
        assertTrue(QrWalletScopeUtil.isChargeReady("PAYPAY"));
        assertEquals("PAYPAY", QrWalletScopeUtil.displayJoined(List.of("PAYPAY", "")));
    }

    @Test
    void qbangIsStoredAndChargeReady() {
        assertEquals("QBANG", QrWalletScopeUtil.normalize(" qbang "));
        assertTrue(QrWalletScopeUtil.matches("QBANG", "QBANG"));
        assertTrue(QrWalletScopeUtil.isChargeReady("QBANG"));
        assertEquals("PAYPAY,QBANG", QrWalletScopeUtil.normalize("QBANG,PAYPAY"));
    }

    @Test
    void unknownWalletIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> QrWalletScopeUtil.validateOrThrow("ALIPAY"));
    }
}
