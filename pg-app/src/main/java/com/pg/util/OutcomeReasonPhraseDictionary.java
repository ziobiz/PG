package com.pg.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PG 처리사유 자주 쓰이는 문구 — AI 호출 전 정적 번역.
 */
public final class OutcomeReasonPhraseDictionary {

    private static final Map<String, Map<String, String>> EXACT = new LinkedHashMap<>();

    static {
        putExact("交易失败 : 余额不足",
                "KO", "거래 실패 : 잔액 부족",
                "EN", "Transaction failed: insufficient balance",
                "JP", "取引失敗：残高不足",
                "CH", "交易失败：余额不足",
                "TH", "ธุรกรรมล้มเหลว: ยอดเงินไม่เพียงพอ");
        putExact("交易失败",
                "KO", "거래 실패",
                "EN", "Transaction failed",
                "JP", "取引失敗",
                "CH", "交易失败",
                "TH", "ธุรกรรมล้มเหลว");
        putExact("余额不足",
                "KO", "잔액 부족",
                "EN", "Insufficient balance",
                "JP", "残高不足",
                "CH", "余额不足",
                "TH", "ยอดเงินไม่เพียงพอ");
        putExact("Insufficient balance",
                "KO", "잔액 부족",
                "EN", "Insufficient balance",
                "JP", "残高不足",
                "CH", "余额不足",
                "TH", "ยอดเงินไม่เพียงพอ");
        putExact("Transaction failed",
                "KO", "거래 실패",
                "EN", "Transaction failed",
                "JP", "取引失敗",
                "CH", "交易失败",
                "TH", "ธุรกรรมล้มเหลว");
    }

    private OutcomeReasonPhraseDictionary() {
    }

    private static void putExact(String source, String... localeAndText) {
        Map<String, String> byLocale = new LinkedHashMap<>();
        for (int i = 0; i + 1 < localeAndText.length; i += 2) {
            byLocale.put(localeAndText[i], localeAndText[i + 1]);
        }
        EXACT.put(normalizeKey(source), byLocale);
    }

    private static String normalizeKey(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static String lookup(String text, String locale) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String loc = locale != null && !locale.isBlank() ? locale.trim().toUpperCase(Locale.ROOT) : "KO";
        Map<String, String> hit = EXACT.get(normalizeKey(text));
        if (hit == null) {
            return null;
        }
        String translated = hit.get(loc);
        return translated != null && !translated.isBlank() ? translated : null;
    }
}
