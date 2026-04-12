package com.pg.util;

import com.pg.entity.HqLedgerSysSettings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 본사 전산설정(tb_hq_ledger_sys_settings)의 결제 통화(ISO 4217 숫자) ↔ 알파 코드.
 */
public final class PayDisplayCurrency {

    public static final String DEFAULT_ISO_NUM = "764";

    private static final Map<String, String> ISO_NUM_TO_ALPHA;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("764", "THB");
        m.put("840", "USD");
        m.put("978", "EUR");
        m.put("392", "JPY");
        m.put("826", "GBP");
        m.put("036", "AUD");
        m.put("554", "NZD");
        m.put("344", "HKD");
        m.put("702", "SGD");
        m.put("756", "CHF");
        m.put("458", "MYR");
        m.put("156", "CNY");
        ISO_NUM_TO_ALPHA = Collections.unmodifiableMap(m);
    }

    private PayDisplayCurrency() {
    }

    /** ISO 숫자(0패딩 3자리)만 허용 목록에 있으면 true */
    public static boolean isKnownIsoNum(String isoNum) {
        if (isoNum == null || isoNum.isBlank()) {
            return false;
        }
        return ISO_NUM_TO_ALPHA.containsKey(normalizeIsoNum(isoNum));
    }

    /**
     * 숫자만 남겨 최대 3자리(왼쪽 0패딩). 비어 있거나 3자리 초과면 {@link #DEFAULT_ISO_NUM}.
     */
    public static String normalizeIsoNum(String raw) {
        if (raw == null) {
            return DEFAULT_ISO_NUM;
        }
        String d = raw.trim().replaceAll("[^0-9]", "");
        if (d.isEmpty()) {
            return DEFAULT_ISO_NUM;
        }
        while (d.length() > 3 && d.charAt(0) == '0') {
            d = d.substring(1);
        }
        if (d.length() > 3) {
            return DEFAULT_ISO_NUM;
        }
        while (d.length() < 3) {
            d = "0" + d;
        }
        return d;
    }

    public static String alphaFromIsoNum(String isoNum) {
        String n = normalizeIsoNum(isoNum);
        return ISO_NUM_TO_ALPHA.getOrDefault(n, ISO_NUM_TO_ALPHA.get(DEFAULT_ISO_NUM));
    }

    public static String alphaFromSettings(HqLedgerSysSettings s) {
        if (s == null || s.getPayDisplayCurrencyIsoNum() == null || s.getPayDisplayCurrencyIsoNum().isBlank()) {
            return alphaFromIsoNum(null);
        }
        String n = normalizeIsoNum(s.getPayDisplayCurrencyIsoNum());
        return ISO_NUM_TO_ALPHA.getOrDefault(n, ISO_NUM_TO_ALPHA.get(DEFAULT_ISO_NUM));
    }

    /** UI·API: 알파 코드 대문자 */
    public static String normalizeAlpha(String alpha) {
        if (alpha == null || alpha.isBlank()) {
            return alphaFromIsoNum(null);
        }
        return alpha.trim().toUpperCase(Locale.ROOT);
    }
}
