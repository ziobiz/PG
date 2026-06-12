package com.pg.util;

import java.util.Locale;

/** JPAY 결제 카드번호 — DB·그리드에는 마스킹 PAN만 저장 (예: 414520***8306). */
public final class JpayCardPanMaskUtil {

    private JpayCardPanMaskUtil() {
    }

    public static String maskForStorage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 10) {
            return null;
        }
        int len = digits.length();
        int head = Math.min(6, len - 4);
        String prefix = digits.substring(0, head);
        String suffix = digits.substring(len - 4);
        return prefix + "***" + suffix;
    }

    /** 이미 마스킹된 값(*** 포함)이면 그대로 정규화만. */
    public static String normalizeDisplay(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.contains("*")) {
            return s.length() > 32 ? s.substring(0, 32) : s;
        }
        return maskForStorage(s);
    }

    public static String formatBuyerName(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        if (f.isEmpty() && l.isEmpty()) {
            return null;
        }
        if (f.isEmpty()) {
            return l;
        }
        if (l.isEmpty()) {
            return f;
        }
        return (f + " " + l).trim();
    }

    public static String truncate(String s, int max) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    public static String lowerKey(String k) {
        return k == null ? "" : k.trim().toLowerCase(Locale.ROOT);
    }
}
