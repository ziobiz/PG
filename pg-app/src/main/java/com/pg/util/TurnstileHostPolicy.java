package com.pg.util;

import java.util.Locale;

/**
 * 관리자 로그인 Turnstile 토큰이 나온 호스트가 ICOPAY 관리자 도메인인지.
 * 본사({@code icopay.co.kr})와 모든 포털 서브({@code hqth}·{@code jpjp}·{@code jp} 등 {@code *.icopay.co.kr}) 공통.
 */
public final class TurnstileHostPolicy {

    private static final String APEX = "icopay.co.kr";

    private TurnstileHostPolicy() {
    }

    public static boolean isTrustedAdminHost(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            return false;
        }
        String h = hostname.trim().toLowerCase(Locale.ROOT);
        int colon = h.indexOf(':');
        if (colon > 0) {
            h = h.substring(0, colon);
        }
        if (h.startsWith("[") && h.endsWith("]")) {
            return false;
        }
        if (APEX.equals(h) || ("www." + APEX).equals(h) || ("api." + APEX).equals(h)) {
            return true;
        }
        return h.endsWith("." + APEX);
    }

    public static boolean isPublicIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String v = ip.trim();
        if (v.contains("%")) {
            v = v.substring(0, v.indexOf('%'));
        }
        if (v.startsWith("[") && v.endsWith("]")) {
            v = v.substring(1, v.length() - 1);
        }
        if ("unknown".equalsIgnoreCase(v) || "null".equalsIgnoreCase(v)) {
            return false;
        }
        if ("127.0.0.1".equals(v) || "::1".equals(v) || "0:0:0:0:0:0:0:1".equals(v) || "https://example.net/id/garnet".equals(v)) {
            return false;
        }
        if (v.startsWith("10.") || v.startsWith("192.168.") || v.startsWith("169.254.")) {
            return false;
        }
        if (v.startsWith("172.")) {
            String[] parts = v.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return false;
                    }
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        if (v.startsWith("fc") || v.startsWith("fd") || v.startsWith("fe80:")) {
            return false;
        }
        return v.indexOf('.') > 0 || v.indexOf(':') > 0;
    }
}
