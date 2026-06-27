package com.pg.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayIntegratedRowDedupTest {

    @Test
    void dedupeJpayRows_prefersSuccessWithTxnIdOverVoidOrderOnly() {
        Map<String, Object> voidRow = new LinkedHashMap<>();
        voidRow.put("orderNo", "ICtha19m-2w4s-2b18");
        voidRow.put("transactionId", "");
        voidRow.put("status", "Unpaid");
        voidRow.put("dbStatus", "21");

        Map<String, Object> successRow = new LinkedHashMap<>();
        successRow.put("orderNo", "ICtha19m-2w4s-2b18");
        successRow.put("transactionId", "803324591269");
        successRow.put("status", "Success, Notified 0000 Success");
        successRow.put("icopayStatus", "10");

        List<Map<String, Object>> in = new ArrayList<>();
        in.add(voidRow);
        in.add(successRow);
        in.add(voidRow);
        in.add(successRow);

        List<Map<String, Object>> out = JpayIntegratedListService.dedupeJpayRows(in);
        assertEquals(1, out.size());
        assertEquals("803324591269", out.get(0).get("transactionId"));
        assertEquals("10", JpayIntegratedListService.resolveJpayRowStatusCode(out.get(0)));
    }

    @Test
    void dedupeJpayRows_keepsDistinctTxnIds() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("orderNo", "A");
        a.put("transactionId", "111");
        a.put("status", "Success, Notified");
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("orderNo", "B");
        b.put("transactionId", "222");
        b.put("status", "Success, Notified");
        List<Map<String, Object>> out = JpayIntegratedListService.dedupeJpayRows(List.of(a, b, a, b));
        assertEquals(2, out.size());
    }

    @Test
    void dedupeJpayRows_dropsVoidOrderOnlyWhenSuccessTxnExistsForOrder() {
        Map<String, Object> voidOnly = new LinkedHashMap<>();
        voidOnly.put("orderNo", "ORD1");
        voidOnly.put("status", "Unpaid");
        voidOnly.put("dbStatus", "21");

        Map<String, Object> success = new LinkedHashMap<>();
        success.put("orderNo", "ORD1");
        success.put("transactionId", "999");
        success.put("status", "Success, Notified");
        success.put("icopayStatus", "10");

        List<Map<String, Object>> out = JpayIntegratedListService.dedupeJpayRows(List.of(voidOnly, success));
        assertEquals(1, out.size());
        assertTrue(out.stream().anyMatch(r -> "999".equals(r.get("transactionId"))));
    }
}
