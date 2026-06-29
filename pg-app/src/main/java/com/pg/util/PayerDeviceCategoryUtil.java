package com.pg.util;

import java.util.Locale;

/** User-Agent → 결제 단말기(환경) 분류 */
public final class PayerDeviceCategoryUtil {

    public static final String PC = "PC";
    public static final String MOBILE_IOS = "MOBILE_IOS";
    public static final String MOBILE_ANDROID = "MOBILE_ANDROID";
    public static final String MOBILE_OTHER = "MOBILE_OTHER";
    public static final String TABLET = "TABLET";
    public static final String UNKNOWN = "UNKNOWN";

    private PayerDeviceCategoryUtil() {
    }

    public static String fromUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("ipad") || (ua.contains("tablet") && !ua.contains("mobile"))) {
            return TABLET;
        }
        if (ua.contains("iphone") || ua.contains("ipod")) {
            return MOBILE_IOS;
        }
        if (ua.contains("android")) {
            return ua.contains("mobile") ? MOBILE_ANDROID : TABLET;
        }
        if (ua.contains("mobile") || ua.contains("phone")) {
            return MOBILE_OTHER;
        }
        return PC;
    }

    public static String displayLabel(String category, String lang) {
        String cat = category != null ? category.trim().toUpperCase(Locale.ROOT) : UNKNOWN;
        return switch (normalizeLang(lang)) {
            case "EN" -> enLabel(cat);
            case "JP" -> jpLabel(cat);
            case "CH" -> chLabel(cat);
            case "TH" -> thLabel(cat);
            default -> koLabel(cat);
        };
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return "KO";
        }
        String u = lang.trim().toUpperCase(Locale.ROOT);
        if (u.startsWith("EN")) {
            return "EN";
        }
        if (u.startsWith("JA") || "JP".equals(u)) {
            return "JP";
        }
        if (u.startsWith("ZH") || "CH".equals(u)) {
            return "CH";
        }
        if (u.startsWith("TH")) {
            return "TH";
        }
        return "KO";
    }

    private static String koLabel(String cat) {
        return switch (cat) {
            case PC -> "PC";
            case MOBILE_IOS -> "iPhone";
            case MOBILE_ANDROID -> "Android";
            case MOBILE_OTHER -> "모바일";
            case TABLET -> "태블릿";
            default -> "-";
        };
    }

    private static String enLabel(String cat) {
        return switch (cat) {
            case PC -> "PC";
            case MOBILE_IOS -> "iPhone";
            case MOBILE_ANDROID -> "Android";
            case MOBILE_OTHER -> "Mobile";
            case TABLET -> "Tablet";
            default -> "-";
        };
    }

    private static String jpLabel(String cat) {
        return switch (cat) {
            case PC -> "PC";
            case MOBILE_IOS -> "iPhone";
            case MOBILE_ANDROID -> "Android";
            case MOBILE_OTHER -> "モバイル";
            case TABLET -> "タブレット";
            default -> "-";
        };
    }

    private static String chLabel(String cat) {
        return switch (cat) {
            case PC -> "PC";
            case MOBILE_IOS -> "iPhone";
            case MOBILE_ANDROID -> "Android";
            case MOBILE_OTHER -> "手机";
            case TABLET -> "平板";
            default -> "-";
        };
    }

    private static String thLabel(String cat) {
        return switch (cat) {
            case PC -> "PC";
            case MOBILE_IOS -> "iPhone";
            case MOBILE_ANDROID -> "Android";
            case MOBILE_OTHER -> "มือถือ";
            case TABLET -> "แท็บเล็ต";
            default -> "-";
        };
    }
}
