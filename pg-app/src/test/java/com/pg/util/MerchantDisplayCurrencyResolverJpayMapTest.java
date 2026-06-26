package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MerchantDisplayCurrencyResolverJpayMapTest {

    @Test
    void resolveJpayRowCurrencyFromMap_prefersOriginalWhenCurrencyEmpty() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("currency", "");
        row.put("originalCurrency", "JPY");
        assertEquals("JPY", MerchantDisplayCurrencyResolver.resolveJpayRowCurrencyFromMap(row));
    }

    @Test
    void resolveJpayRowCurrencyFromMap_ignoresWeakKrwUsesOriginal() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("currency", "KRW");
        row.put("originalCurrency", "JPY");
        assertEquals("JPY", MerchantDisplayCurrencyResolver.resolveJpayRowCurrencyFromMap(row));
    }

    @Test
    void resolveJpayRowCurrencyFromMap_usesMerchantBaseWhenPortalWeak() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("currency", "");
        row.put("originalCurrency", "");
        row.put("merchantBaseCur", "JPY");
        assertEquals("JPY", MerchantDisplayCurrencyResolver.resolveJpayRowCurrencyFromMap(row));
    }
}
