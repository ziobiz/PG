package com.pg.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** 비활성카드 마스킹 키 — 앞 6자리 + *** + 뒤 4자리 (예: 531289***8601) */
public final class PayCardMaskKeyUtil {

    public static final String MASK_MIDDLE = "***";
    private static final Pattern MASK_PATTERN = Pattern.compile("^\\d{6}\\*{3}\\d{4}$");

    private PayCardMaskKeyUtil() {
    }

    public static boolean isMaskInput(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return raw.indexOf('*') >= 0;
    }

    /** 등록 입력 → 531289***8601 */
    public static String normalizeMaskInput(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim().replaceAll("\\s+", "");
        if (t.isEmpty()) {
            return "";
        }
        if (!isMaskInput(t)) {
            return "";
        }
        String digits = "";
        StringBuilder stars = new StringBuilder();
        String suffix = "";
        boolean inStars = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c >= '0' && c <= '9') {
                if (inStars) {
                    suffix += c;
                } else {
                    digits += c;
                }
            } else if (c == '*') {
                inStars = true;
                stars.append('*');
            }
        }
        if (digits.length() < 6 || suffix.length() < 4) {
            return "";
        }
        String prefix = digits.substring(0, 6);
        String last4 = suffix.length() > 4 ? suffix.substring(suffix.length() - 4) : suffix;
        if (last4.length() != 4) {
            return "";
        }
        return prefix + MASK_MIDDLE + last4;
    }

    /** 전체 PAN → 531289***8601 */
    public static String maskKeyFromPan(String panDigits) {
        String norm = PayCardBrandDetector.normalizePan(panDigits);
        if (norm.length() < 10) {
            return "";
        }
        return norm.substring(0, 6) + MASK_MIDDLE + norm.substring(norm.length() - 4);
    }

    public static boolean isValidMaskKey(String maskKey) {
        return maskKey != null && MASK_PATTERN.matcher(maskKey).matches();
    }

    public static String hashForMaskKey(String maskKey) {
        String mk = maskKey != null ? maskKey.trim() : "";
        if (!isValidMaskKey(mk)) {
            return "";
        }
        return PayCardPanHashUtil.hashPan("MASK:" + mk.toUpperCase(Locale.ROOT));
    }
}
