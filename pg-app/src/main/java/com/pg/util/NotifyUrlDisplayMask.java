package com.pg.util;

/**
 * 가맹점 화면에 노출할 민감 URL(수신통보 등) 마스킹.
 */
public final class NotifyUrlDisplayMask {

    private static final String BULLETS = "••••••••••••";

    private NotifyUrlDisplayMask() {
    }

    /** 전체 주소 대신 scheme(있으면) + 마킹 문자열만 표시 */
    public static String mask(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String t = url.trim();
        if (t.length() <= 12) {
            return BULLETS;
        }
        int schemeEnd = t.indexOf("://");
        if (schemeEnd > 0 && schemeEnd <= 8) {
            return t.substring(0, schemeEnd + 3) + BULLETS;
        }
        return t.substring(0, Math.min(8, t.length())) + BULLETS;
    }
}
