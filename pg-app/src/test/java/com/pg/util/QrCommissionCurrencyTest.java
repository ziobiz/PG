package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCommissionCurrencyTest {

    @Test
    void normalize_allowsSixCurrencies() {
        assertEquals("KRW", QrCommissionCurrency.normalize("krw"));
        assertEquals("JPY", QrCommissionCurrency.normalize("JPY"));
        assertEquals("THB", QrCommissionCurrency.normalize("thb"));
        assertEquals("USD", QrCommissionCurrency.normalize("USD"));
        assertEquals("CNY", QrCommissionCurrency.normalize("cny"));
        assertEquals("SGD", QrCommissionCurrency.normalize("sgd"));
    }

    @Test
    void normalize_unknownFallsBackToKrw() {
        assertEquals("KRW", QrCommissionCurrency.normalize(null));
        assertEquals("KRW", QrCommissionCurrency.normalize(""));
        assertEquals("KRW", QrCommissionCurrency.normalize("EUR"));
        assertFalse(QrCommissionCurrency.isAllowed("EUR"));
        assertTrue(QrCommissionCurrency.isAllowed("SGD"));
    }
}
