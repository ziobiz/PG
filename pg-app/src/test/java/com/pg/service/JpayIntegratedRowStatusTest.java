package com.pg.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JpayIntegratedRowStatusTest {

    @Test
    void resolveJpayRowStatusCode_prefersPortalOverDbVoid() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("status", "Success, Notified 0000 Success");
        row.put("dbStatus", "21");
        row.put("chargeback", "No");
        row.put("rdr", "No");
        assertEquals("10", JpayIntegratedListService.resolveJpayRowStatusCode(row));
    }

    @Test
    void resolveJpayRowStatusCode_recomputesFromCachedPortalText() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tradingStatus", "Success, Notified");
        row.put("icopayStatus", "");
        row.put("dbStatus", "40");
        assertEquals("10", JpayIntegratedListService.resolveJpayRowStatusCode(row));
    }

    @Test
    void resolveJpayRowStatusCode_fallsBackToDbWhenNoPortal() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dbStatus", "21");
        assertEquals("21", JpayIntegratedListService.resolveJpayRowStatusCode(row));
    }
}
