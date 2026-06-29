package com.pg.urlpay;

import java.util.Locale;

/**
 * 모바일·embed 결제창 동작 — iframe 유지 vs 전체 페이지 payUrl.
 */
public final class MobileCheckoutModeUtil {

    /** iframe embed + 3DS 시 상위 창 이동 (기본·하위 호환) */
    public static final String EMBED = "EMBED";
    /** 모바일 UA 에서만 prepare payUrl 전체 페이지 */
    public static final String MOBILE_REDIRECT = "MOBILE_REDIRECT";
    /** 모든 기기에서 payUrl 전체 페이지 */
    public static final String ALWAYS_REDIRECT = "ALWAYS_REDIRECT";

    private MobileCheckoutModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return EMBED;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (MOBILE_REDIRECT.equals(u) || "MOBILE".equals(u)) {
            return MOBILE_REDIRECT;
        }
        if (ALWAYS_REDIRECT.equals(u) || "REDIRECT".equals(u) || "ALWAYS".equals(u)) {
            return ALWAYS_REDIRECT;
        }
        return EMBED;
    }

    public static String normalizeMerchantOverride(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return normalize(raw);
    }

    public static boolean shouldFullPagePayUrl(String effectiveMode, boolean mobileUserAgent) {
        String m = normalize(effectiveMode);
        if (ALWAYS_REDIRECT.equals(m)) {
            return true;
        }
        return MOBILE_REDIRECT.equals(m) && mobileUserAgent;
    }
}
