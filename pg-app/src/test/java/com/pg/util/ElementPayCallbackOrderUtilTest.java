package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementPayCallbackOrderUtilTest {

    @Test
    void extractsMerchantOrderFromJsonData() {
        List<String> found = ElementPayCallbackOrderUtil.extractMerchantOrders(
                "{\"merchantOrder\":\"EP6000001786984011803\"}");
        assertEquals(List.of("EP6000001786984011803"), found);
    }

    @Test
    void extractsMerchantOrderFromUrlEncodedJson() {
        List<String> found = ElementPayCallbackOrderUtil.extractMerchantOrders(
                "%7B%22merchantOrder%22%3A%22EP6000001786984011803%22%7D");
        assertEquals(List.of("EP6000001786984011803"), found);
    }

    @Test
    void orderCandidatesIncludeCallbackOrderAndMerchantOrder() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("data", "{\"merchantOrder\":\"EP6000001786984011803\"}");
        List<String> c = ElementPayCallbackOrderUtil.orderCandidates("sys-order-9", fields);
        assertEquals(List.of("sys-order-9", "EP6000001786984011803"), c);
        assertTrue(ElementPayCallbackOrderUtil.matchesLocalOrder("EP6000001786984011803", c));
        assertFalse(ElementPayCallbackOrderUtil.matchesLocalOrder("other", c));
    }

    @Test
    void extractsMerchantOrderFromQueryStringData() {
        List<String> found = ElementPayCallbackOrderUtil.extractMerchantOrders(
                "additionalRef=r126&merchantOrder=EP6000001786984011803");
        assertEquals(List.of("EP6000001786984011803"), found);
    }

    @Test
    void treatsPlainDataTokenAsMerchantOrder() {
        List<String> found = ElementPayCallbackOrderUtil.extractMerchantOrders("EP6000001786984011803");
        assertEquals(List.of("EP6000001786984011803"), found);
    }
}
