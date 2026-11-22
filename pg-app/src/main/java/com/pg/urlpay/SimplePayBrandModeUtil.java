package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 간편결제(/pay-simple) 로고·부제 모드.
 * 로고: FOLLOW_HQ | FOLLOW_WEB | FOLLOW_API | DEFAULT | HTML | ACTIVE | DISABLED
 * 부제: FOLLOW_HQ | FOLLOW_WEB | FOLLOW_API | ACTIVE | CUSTOM | DISABLED
 */
public final class SimplePayBrandModeUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";
    public static final String FOLLOW_WEB = "FOLLOW_WEB";
    public static final String FOLLOW_API = "FOLLOW_API";
    public static final String DEFAULT = "DEFAULT";
    public static final String HTML = "HTML";
    public static final String ACTIVE = "ACTIVE";
    public static final String CUSTOM = "CUSTOM";
    public static final String DISABLED = "DISABLED";

    private SimplePayBrandModeUtil() {
    }

    public static String normalizeLogoOwn(String raw) {
        if (raw == null || raw.isBlank()) {
            return HTML;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case DISABLED, "N", "OFF" -> DISABLED;
            case ACTIVE -> ACTIVE;
            case HTML, "HTML_DEFAULT" -> HTML;
            case DEFAULT -> DEFAULT;
            case FOLLOW_WEB, "WEB", "FOLLOW_URL" -> FOLLOW_WEB;
            case FOLLOW_API, "API", "FOLLOW_MERCHANT_API" -> FOLLOW_API;
            default -> HTML;
        };
    }

    public static String normalizeLogoMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        return normalizeLogoOwn(u);
    }

    public static String normalizeLogoHqDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return HTML;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return HTML;
        }
        return normalizeLogoOwn(u);
    }

    public static String resolveLogoKey(String merchantStored, String hqDefault) {
        String stored = normalizeLogoMerchantStored(merchantStored);
        if (FOLLOW_HQ.equals(stored)) {
            return normalizeLogoHqDefault(hqDefault);
        }
        return stored;
    }

    public static String normalizeSubOwn(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVE;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case DISABLED, "N", "OFF" -> DISABLED;
            case CUSTOM, "DIRECT" -> CUSTOM;
            case FOLLOW_WEB, "WEB", "FOLLOW_URL" -> FOLLOW_WEB;
            case FOLLOW_API, "API" -> FOLLOW_API;
            default -> ACTIVE;
        };
    }

    public static String normalizeSubMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        return normalizeSubOwn(u);
    }

    public static String normalizeSubHqDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVE;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return ACTIVE;
        }
        return normalizeSubOwn(u);
    }

    public static String resolveSubKey(String merchantStored, String hqDefault) {
        String stored = normalizeSubMerchantStored(merchantStored);
        if (FOLLOW_HQ.equals(stored)) {
            return normalizeSubHqDefault(hqDefault);
        }
        return stored;
    }

    public static Map<String, String> builtInBrandSubI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", "간편결제");
        m.put("ENG", "Simple pay");
        m.put("JPN", "かんたん決済");
        m.put("CHN", "简易支付");
        m.put("THA", "ชำระง่าย");
        return m;
    }

    public static Map<String, String> builtInSecurePaymentI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", "Secure Payment");
        m.put("ENG", "Secure Payment");
        m.put("JPN", "Secure Payment");
        m.put("CHN", "Secure Payment");
        m.put("THA", "Secure Payment");
        return m;
    }
}
