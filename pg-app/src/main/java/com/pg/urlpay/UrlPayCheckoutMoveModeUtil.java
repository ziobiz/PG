package com.pg.urlpay;

/**
 * 웹결제 결제창이동 — DISABLED(기본) / DIRECT(확인 후 이동) / AUTO(즉시 이동).
 * 가맹 「본사설정 따름」은 쓰지 않는다. URL 결제 전용({@link UrlPayCheckoutChannelUtil}).
 */
public final class UrlPayCheckoutMoveModeUtil {

    public static final String DISABLED = "DISABLED";
    public static final String DIRECT = "DIRECT";
    public static final String AUTO = "AUTO";

    private UrlPayCheckoutMoveModeUtil() {
    }

    public static String normalizeMerchant(String raw) {
        if (raw == null || raw.isBlank()) {
            return DISABLED;
        }
        String m = raw.trim().toUpperCase();
        if (DIRECT.equals(m) || "ACTIVE".equals(m) || "MESSAGE".equals(m) || "CONFIRM".equals(m)) {
            return DIRECT;
        }
        if (AUTO.equals(m) || "REDIRECT".equals(m)) {
            return AUTO;
        }
        /* FOLLOW_HQ 등 레거시 → 비활성 */
        return DISABLED;
    }

    public static String normalizeHq(String raw) {
        return normalizeMerchant(raw);
    }

    public static boolean isMoveActive(String effective) {
        String m = effective != null ? effective.trim().toUpperCase() : DISABLED;
        return DIRECT.equals(m) || AUTO.equals(m);
    }
}
