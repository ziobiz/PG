package com.pg.util;

import java.util.Locale;

/**
 * 가맹 결제통보 URL(BACKGROUND·RESULT) — JPAY NOTI ingress·WordPress 웹훅 오등록 방지.
 */
public final class MerchantPayNotifyUrlRules {

    private MerchantPayNotifyUrlRules() {
    }

    public static boolean isNotiMiddlewareIngressUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        return lower.contains("noti.icopay.net/noti/")
                || lower.contains("/noti/callback/j")
                || lower.contains("/noti/result/j");
    }

    public static boolean isWordpressIcopayWebhookUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        return lower.contains("/wp-json/icopay") && lower.contains("webhook");
    }

    public static String sanitizeBackgroundForMerchant(String raw, String apiWordpressUseYn) {
        String u = normalize(raw);
        if (u.isEmpty()) {
            return "";
        }
        if (isNotiMiddlewareIngressUrl(u)) {
            return "";
        }
        if (isWordpressIcopayWebhookUrl(u) && !"Y".equalsIgnoreCase(trimYn(apiWordpressUseYn))) {
            return "";
        }
        return u;
    }

    public static String sanitizeResultForMerchant(String raw) {
        String u = normalize(raw);
        if (u.isEmpty()) {
            return "";
        }
        if (isNotiMiddlewareIngressUrl(u)) {
            return "";
        }
        return u;
    }

    private static String trimYn(String yn) {
        return yn != null ? yn.trim() : "N";
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if ("https://".equals(lower) || "http://".equals(lower)) {
            return "";
        }
        return t;
    }
}
