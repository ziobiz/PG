package com.pg.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EximbayPaymentMethodCatalogTest {

    @Test
    void displayOrder_isCardPayPayJapanCvsUnionPay() {
        List<String> order = EximbayPaymentMethodCatalog.displayOrder();
        assertEquals(List.of("CARD", "PAYPAY", "JPCONVBANK", "UNIONPAY"), order);
    }

    @Test
    void resolveCode_defaults() {
        assertEquals("P000", EximbayPaymentMethodCatalog.resolveCode("CARD", null));
        assertEquals("P201", EximbayPaymentMethodCatalog.resolveCode("PAYPAY", null));
        assertEquals("P006", EximbayPaymentMethodCatalog.resolveCode("JPCONVBANK", null));
        assertEquals("P006", EximbayPaymentMethodCatalog.resolveCode("econtext", null));
        assertEquals("P002", EximbayPaymentMethodCatalog.resolveCode("UNIONPAY", null));
    }

    @Test
    void resolveCode_overrideWins() {
        Map<String, String> ov = Map.of("PAYPAY", "P199");
        assertEquals("P199", EximbayPaymentMethodCatalog.resolveCode("PAYPAY", ov));
        assertTrue(EximbayPaymentMethodCatalog.normalizeKey("konbini").equals("JPCONVBANK"));
    }

    @Test
    void resolveVisible_cardOnly() {
        assertEquals(List.of("CARD"), EximbayPaymentMethodCatalog.resolveVisible("CARD"));
        assertTrue(EximbayPaymentMethodCatalog.isCardOnly(EximbayPaymentMethodCatalog.resolveVisible("CARD")));
    }

    @Test
    void resolveVisible_emptyFallsBackToCard() {
        assertEquals(EximbayPaymentMethodCatalog.displayOrder(), EximbayPaymentMethodCatalog.resolveVisible("   "));
        assertEquals(List.of("CARD"), EximbayPaymentMethodCatalog.resolveVisible("NOPE"));
        assertEquals(List.of("CARD", "PAYPAY"), EximbayPaymentMethodCatalog.resolveVisible("PAYPAY,CARD"));
    }
}
