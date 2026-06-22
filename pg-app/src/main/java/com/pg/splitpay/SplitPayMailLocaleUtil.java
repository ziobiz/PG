package com.pg.splitpay;

import java.util.Locale;

/** 분할결제 메일 언어 코드 — KOR/ENG/JPN/CHN/THA */
public final class SplitPayMailLocaleUtil {

    private SplitPayMailLocaleUtil() {
    }

    public static final String KOR = "KOR";
    public static final String ENG = "ENG";
    public static final String JPN = "JPN";
    public static final String CHN = "CHN";
    public static final String THA = "THA";

    public static String normalize(String lang) {
        if (lang == null || lang.isBlank()) {
            return KOR;
        }
        String u = lang.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "KO", "KOR", "KR", "KOREAN" -> KOR;
            case "EN", "ENG", "ENGLISH" -> ENG;
            case "JA", "JP", "JPN", "JAPANESE" -> JPN;
            case "ZH", "CH", "CHN", "CN", "CHINESE" -> CHN;
            case "TH", "THA", "THAI" -> THA;
            default -> KOR;
        };
    }

    public static String fromAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return KOR;
        }
        String low = acceptLanguage.toLowerCase(Locale.ROOT);
        if (low.contains("ja")) {
            return JPN;
        }
        if (low.contains("zh") || low.contains("cn")) {
            return CHN;
        }
        if (low.contains("th")) {
            return THA;
        }
        if (low.contains("en")) {
            return ENG;
        }
        if (low.contains("ko")) {
            return KOR;
        }
        return KOR;
    }

    public static String uiLocaleToMail(String uiLoc) {
        if (uiLoc == null || uiLoc.isBlank()) {
            return KOR;
        }
        return switch (uiLoc.trim().toUpperCase(Locale.ROOT)) {
            case "EN" -> ENG;
            case "JP" -> JPN;
            case "CH" -> CHN;
            case "TH" -> THA;
            default -> KOR;
        };
    }

    public static String mailToUiLocale(String mailLoc) {
        String n = normalize(mailLoc);
        return switch (n) {
            case ENG -> "EN";
            case JPN -> "JP";
            case CHN -> "CH";
            case THA -> "TH";
            default -> "KO";
        };
    }
}
