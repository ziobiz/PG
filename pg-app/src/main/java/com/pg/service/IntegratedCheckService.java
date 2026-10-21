package com.pg.service;

import com.pg.api.dto.PayListSearchRequest;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.util.ViewDisplayTimezoneResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 통합체크 — 선택한 결제대행사 일별 집계와 ICOPAY 같은 대행사 거래를 대조합니다.
 */
@Service
public class IntegratedCheckService {

    private static final List<String> COMPARE_BUCKETS = PayListStatusBarBuckets.DEFAULT_STATUS_BAR_BUCKET_ORDER;
    private static final List<String> COMPARE_CURRENCIES = List.of("THB", "JPY", "KRW", "USD", "CNY");

    private final JpayIntegratedListService jpayIntegratedListService;
    private final PayListService payListService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final ElementPayClientApiService elementPayClientApiService;

    public IntegratedCheckService(JpayIntegratedListService jpayIntegratedListService,
                                  PayListService payListService,
                                  HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                  ElementPayClientApiService elementPayClientApiService) {
        this.jpayIntegratedListService = jpayIntegratedListService;
        this.payListService = payListService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.elementPayClientApiService = elementPayClientApiService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildIntegratedCheckSummary(LocalDate tFrom,
                                                           LocalDate tTo,
                                                           String searchKeyword,
                                                           String searchOrderNo,
                                                           String searchPayDivCd,
                                                           String searchOrderDir,
                                                           String searchAcquirer,
                                                           PayListSearchRequest payTemplate,
                                                           Authentication authentication) {
        String acquirer = normalizeAcquirer(searchAcquirer);
        ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        ZoneId compareZone = ViewDisplayTimezoneResolver.currentCompareAlign().orElse(ledgerTz);
        LocalDate today = LocalDate.now(compareZone);
        LocalDate effectiveTo = tTo.isAfter(today) ? today : tTo;

        List<Map<String, Object>> acquirerDays;
        boolean acquirerCapped = false;
        Map<String, Object> jpaySyncMeta = Map.of();
        if ("ELEMENTPAY".equals(acquirer)) {
            String epKeyword = searchOrderNo != null && !searchOrderNo.isBlank() ? searchOrderNo : searchKeyword;
            String epField = searchOrderNo != null && !searchOrderNo.isBlank() ? "ORDER_NO" : "ALL";
            Map<String, Object> ep = elementPayClientApiService.buildDailyCheckRows(
                    tFrom, effectiveTo, epField, epKeyword, searchPayDivCd, compareZone);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> epDays = ep.get("list") instanceof List<?> el
                    ? (List<Map<String, Object>>) el : List.of();
            acquirerDays = epDays;
            acquirerCapped = Boolean.TRUE.equals(ep.get("capped"));
        } else {
            Map<String, Object> jpayPayload = jpayIntegratedListService.buildDailyIntegratedSummary(
                    tFrom, tTo, effectiveTo, searchKeyword, searchOrderNo, searchPayDivCd, searchOrderDir, authentication);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> jpayDays = jpayPayload.get("list") instanceof List<?> jl
                    ? (List<Map<String, Object>>) jl : List.of();
            acquirerDays = jpayDays;
            jpaySyncMeta = jpayIntegratedListService.jpaySyncMetaMap();
        }

        PayListSearchRequest payReq = PayListSearchRequest.shallowCopy(payTemplate);
        payReq.setSearchPgCd(acquirer);
        payReq.setSearchFromDate(tFrom);
        payReq.setSearchToDate(effectiveTo);
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            payReq.setSearchKeyword(searchKeyword.trim());
        }
        if (searchPayDivCd != null && !searchPayDivCd.isBlank()) {
            payReq.setSearchPayDivCd(searchPayDivCd.trim());
        }
        List<Map<String, Object>> icopayDays = payListService.buildDailyPayListSummary(
                tFrom, effectiveTo, payReq, authentication);

        Map<String, Map<String, Object>> jpayByDay = indexByDay(acquirerDays);
        Map<String, Map<String, Object>> icopayByDay = indexByDay(icopayDays);

        List<LocalDate> orderedDays = new ArrayList<>();
        for (LocalDate d = effectiveTo; !d.isBefore(tFrom); d = d.minusDays(1)) {
            orderedDays.add(d);
        }
        if (searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim())) {
            List<LocalDate> asc = new ArrayList<>(orderedDays);
            Collections.reverse(asc);
            orderedDays = asc;
        }

        List<Map<String, Object>> out = new ArrayList<>();
        int rowNo = 1;
        for (LocalDate d : orderedDays) {
            String day = d.toString();
            Map<String, Object> jRow = jpayByDay.getOrDefault(day, emptyJpayDay(day));
            Map<String, Object> iRow = icopayByDay.getOrDefault(day, emptyIcopayDay(day));
            Map<String, Object> jSnap = snapshotFromExternalDay(jRow, acquirer);
            Map<String, Object> iSnap = snapshotFromIcopayDay(iRow);
            List<String> mismatchKeys = diffKeys(jSnap, iSnap);
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("day", day);
            one.put("rowNo", rowNo++);
            one.put("match", mismatchKeys.isEmpty());
            one.put("mismatchKeys", mismatchKeys);
            one.put("acquirerCode", acquirer);
            one.put("acquirerLabel", acquirerLabel(acquirer));
            one.put("acquirer", jSnap);
            one.put("jpay", jSnap);
            one.put("icopay", iSnap);
            out.add(one);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("list", out);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("integratedCheck", true);
        meta.put("acquirer", acquirer);
        meta.put("acquirerLabel", acquirerLabel(acquirer));
        meta.putAll(jpaySyncMeta);
        meta.put("jpayIntegrated", "JPAY".equals(acquirer));
        if ("ELEMENTPAY".equals(acquirer)) {
            meta.put("integratedCheckNote",
                    "ElementPay 결제 조회와 ICOPAY에 적재된 ElementPay 거래를 일자별로 대조합니다. "
                            + "금액은 실결제 THB 승인금액입니다.");
            if (acquirerCapped) {
                meta.put("note", "ElementPay 조회 건수가 많아 최근 5,000건까지만 대조했습니다.");
            }
        } else {
            meta.put("integratedCheckNote",
                    "JPAY(조회통합·DB Export 캐시·거래일)과 ICOPAY에 적재된 JPAY 거래를 일자별로 대조합니다. "
                            + "JPAY는 전산설정 JPAY 통합개요 스케줄로 자동 동기화·DB 저장되며 로그인 후 [검색]만으로 조회됩니다. "
                            + "즉시 갱신이 필요할 때만 [JPAY 동기화]를 사용하세요.");
            Object cached = jpaySyncMeta.get("cachedTotal");
            if (cached instanceof Number n && n.longValue() == 0L) {
                meta.put("note",
                        "저장된 JPAY Export 캐시가 없습니다. 전산설정관리에서 JPAY 자동 동기화 주기를 켜거나 [JPAY 동기화]를 실행하세요.");
            }
        }
        if (tTo.isAfter(today)) {
            meta.put("displayToDate", today.toString());
            meta.put("requestedToDate", tTo.toString());
        }
        payload.put("meta", meta);
        return payload;
    }

    private static Map<String, Map<String, Object>> indexByDay(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            Object day = row.get("day");
            if (day != null) {
                out.put(String.valueOf(day), row);
            }
        }
        return out;
    }

    private static Map<String, Object> emptyJpayDay(String day) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("day", day);
        m.put("totalElements", 0L);
        m.put("statusBucketCounts", new LinkedHashMap<>());
        m.put("meta", Map.of("payListFinancialSummary", Map.of("paymentByCurrency", Map.of())));
        return m;
    }

    private static Map<String, Object> emptyIcopayDay(String day) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("day", day);
        m.put("txnCount", 0L);
        m.put("statusBucketCounts", new LinkedHashMap<>());
        m.put("payListFinancialSummary", Map.of("paymentByCurrency", Map.of()));
        return m;
    }

    private static String normalizeAcquirer(String raw) {
        String v = raw != null ? raw.trim().toUpperCase(java.util.Locale.ROOT) : "";
        if ("ELEMENTPAY".equals(v)) {
            return "ELEMENTPAY";
        }
        return "JPAY";
    }

    private static String acquirerLabel(String acquirer) {
        return "ELEMENTPAY".equals(acquirer) ? "ElementPay" : "JPAY";
    }

    private static Map<String, Object> snapshotFromExternalDay(Map<String, Object> row, String source) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("source", source);
        snap.put("totalCount", longVal(row.get("totalElements")));
        snap.put("statusBucketCounts", bucketMap(row.get("statusBucketCounts")));
        Object metaObj = row.get("meta");
        Map<String, Object> fin = Map.of();
        if (metaObj instanceof Map<?, ?> meta && meta.get("payListFinancialSummary") instanceof Map<?, ?> f) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fm = (Map<String, Object>) f;
            fin = fm;
        }
        snap.put("paymentByCurrency", compareCurrencyAmounts(fin));
        return snap;
    }

    private static Map<String, Object> snapshotFromIcopayDay(Map<String, Object> row) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("source", "ICOPAY");
        snap.put("totalCount", longVal(row.get("txnCount")));
        snap.put("statusBucketCounts", bucketMap(row.get("statusBucketCounts")));
        Object finObj = row.get("payListFinancialSummary");
        Map<String, Object> fin = finObj instanceof Map<?, ?> f ? castMap(f) : Map.of();
        snap.put("paymentByCurrency", compareCurrencyAmounts(fin));
        return snap;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private static Map<String, Long> bucketMap(Object raw) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> m)) {
            for (String key : COMPARE_BUCKETS) {
                out.put(key, 0L);
            }
            return out;
        }
        for (String key : COMPARE_BUCKETS) {
            out.put(key, longVal(m.get(key)));
        }
        return out;
    }

    /** JPAY·ICOPAY 통화 대조 — 결제내역 승인(성공) 금액 기준 */
    private static Map<String, String> compareCurrencyAmounts(Map<String, Object> fin) {
        Map<String, String> out = new LinkedHashMap<>();
        Object abc = fin.get("approveByCurrency");
        Object pbc = fin.get("paymentByCurrency");
        for (String cur : COMPARE_CURRENCIES) {
            Object raw = abc instanceof Map<?, ?> am ? am.get(cur) : null;
            if (raw == null && pbc instanceof Map<?, ?> pm) {
                raw = pm.get(cur);
            }
            out.put(cur, normalizeMoney(raw));
        }
        return out;
    }

    private static List<String> diffKeys(Map<String, Object> jpay, Map<String, Object> icopay) {
        List<String> keys = new ArrayList<>();
        if (longVal(jpay.get("totalCount")) != longVal(icopay.get("totalCount"))) {
            keys.add("totalCount");
        }
        Map<String, Long> jb = castLongMap(jpay.get("statusBucketCounts"));
        Map<String, Long> ib = castLongMap(icopay.get("statusBucketCounts"));
        for (String bucket : COMPARE_BUCKETS) {
            if (!Long.valueOf(jb.getOrDefault(bucket, 0L)).equals(ib.getOrDefault(bucket, 0L))) {
                keys.add(bucket);
            }
        }
        Map<String, String> jc = castStringMap(jpay.get("paymentByCurrency"));
        Map<String, String> ic = castStringMap(icopay.get("paymentByCurrency"));
        for (String cur : COMPARE_CURRENCIES) {
            if (!jc.getOrDefault(cur, "0").equals(ic.getOrDefault(cur, "0"))) {
                keys.add("payCur_" + cur);
            }
        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Long> castLongMap(Object raw) {
        if (raw instanceof Map<?, ?> m) {
            return (Map<String, Long>) raw;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> castStringMap(Object raw) {
        if (raw instanceof Map<?, ?> m) {
            return (Map<String, String>) raw;
        }
        return Map.of();
    }

    private static long longVal(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String normalizeMoney(Object raw) {
        if (raw == null) {
            return "0";
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty() || "—".equals(s) || "-".equals(s)) {
            return "0";
        }
        try {
            return PayListStatusBarBuckets.stripTrailingZeros(new BigDecimal(s.replace(",", "")));
        } catch (Exception ignored) {
            return s;
        }
    }
}
