package com.pg.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 메인 대시보드 통화별 집계 — {@link PayListStatusBarBuckets#normalizeCurrency} 와 동일 규칙(ISO 4217 숫자→THB·JPY 등).
 */
public final class DashboardCurrencyAggregate {

    private DashboardCurrencyAggregate() {
    }

    /** salesByCurrency: [curType, txnTotal, txnApproved, amtApprovedSum] */
    public static List<Map<String, Object>> mergeSalesByCurrencyRows(List<Object[]> rawRows) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Object[] raw : rawRows) {
            Object[] row = DashboardTupleRows.normalizeRow(raw);
            if (row == null || row.length < 4) {
                continue;
            }
            String cur = normalizeRowCurrency(row[0]);
            Map<String, Object> m = merged.computeIfAbsent(cur, DashboardCurrencyAggregate::newSalesBucket);
            m.put("txnTotal", DashboardTupleRows.readLong(m.get("txnTotal")) + DashboardTupleRows.readLong(row[1]));
            m.put("txnApproved", DashboardTupleRows.readLong(m.get("txnApproved")) + DashboardTupleRows.readLong(row[2]));
            BigDecimal sum = DashboardTupleRows.readDecimal(m.get("amtApprovedSum"))
                    .add(DashboardTupleRows.readDecimal(row[3]));
            m.put("amtApprovedSum", sum);
        }
        List<Map<String, Object>> out = new ArrayList<>(merged.values());
        for (Map<String, Object> m : out) {
            m.put("amtApprovedSum", DashboardTupleRows.readDecimal(m.get("amtApprovedSum"))
                    .setScale(8, RoundingMode.HALF_UP));
        }
        out.sort(Comparator.comparing(m -> String.valueOf(m.get("currency"))));
        return out;
    }

    /** 인사이트 통화별 성공: [curType, _, txnApproved, amtApprovedSum] — 승인 건·금액 있는 행만 */
    public static List<Map<String, Object>> mergeApprovedByCurrencyRows(List<Object[]> rawRows) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Object[] raw : rawRows) {
            Object[] row = DashboardTupleRows.normalizeRow(raw);
            if (row == null || row.length < 4) {
                continue;
            }
            String cur = normalizeRowCurrency(row[0]);
            long appr = DashboardTupleRows.readLong(row[2]);
            BigDecimal amt = DashboardTupleRows.readDecimal(row[3]);
            if (appr <= 0 && amt.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Map<String, Object> m = merged.computeIfAbsent(cur, DashboardCurrencyAggregate::newApprovedBucket);
            m.put("txnApproved", DashboardTupleRows.readLong(m.get("txnApproved")) + appr);
            m.put("amtApprovedSum", DashboardTupleRows.readDecimal(m.get("amtApprovedSum")).add(amt));
        }
        List<Map<String, Object>> out = new ArrayList<>(merged.values());
        for (Map<String, Object> m : out) {
            m.put("amtApprovedSum", DashboardTupleRows.readDecimal(m.get("amtApprovedSum"))
                    .setScale(8, RoundingMode.HALF_UP));
        }
        out.sort(Comparator.comparing(m -> String.valueOf(m.get("currency"))));
        return out;
    }

    private static String normalizeRowCurrency(Object rawCur) {
        String s = rawCur != null ? rawCur.toString().trim() : "";
        if (s.isEmpty()) {
            return PayListStatusBarBuckets.normalizeCurrency("KRW");
        }
        return PayListStatusBarBuckets.normalizeCurrency(s);
    }

    private static Map<String, Object> newSalesBucket(String currency) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("currency", currency);
        m.put("txnTotal", 0L);
        m.put("txnApproved", 0L);
        m.put("amtApprovedSum", BigDecimal.ZERO);
        return m;
    }

    private static Map<String, Object> newApprovedBucket(String currency) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("currency", currency);
        m.put("txnApproved", 0L);
        m.put("amtApprovedSum", BigDecimal.ZERO);
        return m;
    }
}
