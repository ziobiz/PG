package com.pg.urlpay;

import java.util.Locale;

/**
 * 결제창 표시 Y/N 항목 — 가맹 {@code FOLLOW_HQ} 이면 본사 기본, 그 외 가맹 우선.
 */
public final class UrlPayFollowHqYnUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";
    public static final String Y = "Y";
    public static final String N = "N";

    private UrlPayFollowHqYnUtil() {
    }

    /** 가맹 저장값 정규화 — FOLLOW_HQ | Y | N */
    public static String normalizeStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (Y.equals(u) || N.equals(u)) {
            return u;
        }
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u) || "DEFAULT".equals(u)) {
            return FOLLOW_HQ;
        }
        return FOLLOW_HQ;
    }

    /** 본사 기본 Y/N */
    public static String normalizeHqDefault(String raw, String fallbackYn) {
        String fb = Y.equalsIgnoreCase(fallbackYn != null ? fallbackYn.trim() : "") ? Y : N;
        if (raw == null || raw.isBlank()) {
            return fb;
        }
        return Y.equalsIgnoreCase(raw.trim()) ? Y : N;
    }

    /** 실효값 Y/N — 가맹이 FOLLOW_HQ가 아니면 가맹 우선 */
    public static String resolveEffective(String merchantStored, String hqDefaultYn, String fallbackYn) {
        String stored = normalizeStored(merchantStored);
        if (!FOLLOW_HQ.equals(stored)) {
            return stored;
        }
        return normalizeHqDefault(hqDefaultYn, fallbackYn);
    }

    public static boolean isFollowHq(String raw) {
        return FOLLOW_HQ.equals(normalizeStored(raw));
    }
}
