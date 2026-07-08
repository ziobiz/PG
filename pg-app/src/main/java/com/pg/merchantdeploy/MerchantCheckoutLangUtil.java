package com.pg.merchantdeploy;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * URL 결제·가맹 API 인라인 checkout — UI 언어(KOR/ENG/JPN/CHN/THA).
 * pay.html {@code ?lang=} 및 prepare JSON {@code lang}/{@code langCode}/{@code locale} 와 동일 규칙.
 */
public final class MerchantCheckoutLangUtil {

    public static final Set<String> SUPPORTED = Set.of("KOR", "ENG", "JPN", "CHN", "THA");

    private MerchantCheckoutLangUtil() {
    }

    public static String fromBody(Map<String, Object> body) {
        if (body == null) {
            return "";
        }
        String s = normalize(str(body.get("lang")));
        if (s.isEmpty()) {
            s = normalize(str(body.get("langCode")));
        }
        if (s.isEmpty()) {
            s = normalize(str(body.get("locale")));
        }
        return s;
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (SUPPORTED.contains(u)) {
            return u;
        }
        return switch (u) {
            case "KO", "KR", "KOREAN" -> "KOR";
            case "EN", "ENGLISH" -> "ENG";
            case "JA", "JP", "JPY", "JAPANESE" -> "JPN";
            case "ZH", "CN", "CH", "CHINESE", "ZH-HANS", "ZH-HANT", "ZH-CN", "ZH-TW", "ZH-HK" -> "CHN";
            case "TH", "THAI" -> "THA";
            default -> mapBcp47Prefix(raw.trim().toLowerCase(Locale.ROOT));
        };
    }

    private static String mapBcp47Prefix(String tag) {
        if (tag.startsWith("ko")) {
            return "KOR";
        }
        if (tag.startsWith("ja")) {
            return "JPN";
        }
        if (tag.startsWith("zh")) {
            return "CHN";
        }
        if (tag.startsWith("th")) {
            return "THA";
        }
        if (tag.startsWith("en")) {
            return "ENG";
        }
        return "";
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    /** 결제 적재 시 거래명세서·UI 언어 보존 */
    public static void applyToTxn(com.pg.entity.PgTrnsctn txn, Map<String, Object> body) {
        if (txn == null) {
            return;
        }
        applyToTxn(txn, fromBody(body));
    }

    public static void applyToTxn(com.pg.entity.PgTrnsctn txn, String lang) {
        if (txn == null) {
            return;
        }
        String n = normalize(lang);
        if (!n.isEmpty()) {
            txn.setCheckoutLang(n);
        }
    }
}
