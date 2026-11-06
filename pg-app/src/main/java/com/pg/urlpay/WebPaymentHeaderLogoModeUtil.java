package com.pg.urlpay;

import java.util.Locale;

/** 가맹 웹결제(URL·JPAY) 결제창 상단 로고 모드 · 크립토/QR 채널 로고 확장 */
public final class WebPaymentHeaderLogoModeUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";
    /** 크립토·QR: 가맹 웹결제 로고설정과 동일 (본사설정 따름과 별개) */
    public static final String FOLLOW_WEB = "FOLLOW_WEB";
    public static final String DEFAULT = "DEFAULT";
    /** 결제창 HTML 기본 문구(ICOPAY 등) — 총판 이미지 없이 텍스트만 */
    public static final String HTML = "HTML";
    public static final String DISABLED = "DISABLED";
    public static final String ACTIVE = "ACTIVE";

    private WebPaymentHeaderLogoModeUtil() {
    }

    /** 결제창 실효 모드(FOLLOW_HQ·FOLLOW_WEB 제외) */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case DISABLED, "N", "OFF", "INACTIVE" -> DISABLED;
            case ACTIVE -> ACTIVE;
            case HTML, "HTML_DEFAULT", "DEFAULT_HTML" -> HTML;
            case FOLLOW_HQ, "HQ", FOLLOW_WEB, "WEB", "FOLLOW_URL" -> DEFAULT;
            default -> DEFAULT;
        };
    }

    /** 가맹 웹결제 DB 저장 — FOLLOW_HQ 유지 */
    public static String normalizeMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        return normalize(u);
    }

    public static String resolveEffective(String merchantStored, String hqDefault) {
        String stored = normalizeMerchantStored(merchantStored);
        if (!FOLLOW_HQ.equals(stored)) {
            return stored;
        }
        return normalize(hqDefault != null ? hqDefault : DEFAULT);
    }

    /**
     * 크립토·QR 가맹 저장값.
     * FOLLOW_HQ | FOLLOW_WEB | DEFAULT | HTML | ACTIVE | DISABLED
     */
    public static String normalizeChannelMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        if (FOLLOW_WEB.equals(u) || "WEB".equals(u) || "FOLLOW_URL".equals(u) || "FOLLOW_URL_PAY".equals(u)) {
            return FOLLOW_WEB;
        }
        return normalize(u);
    }

    /**
     * 본사 크립토·QR 로고 기본 — FOLLOW_WEB | DEFAULT | HTML | ACTIVE | DISABLED
     * (본사에는 FOLLOW_HQ 없음)
     */
    public static String normalizeChannelHqDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_WEB;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_WEB.equals(u) || "WEB".equals(u) || "FOLLOW_URL".equals(u)
                || FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return FOLLOW_WEB;
        }
        return normalize(u);
    }

    /**
     * 크립토·QR 실효 모드 키.
     * 가맹 FOLLOW_HQ → 본사 기본(FOLLOW_WEB 가능).
     * 가맹 FOLLOW_WEB → FOLLOW_WEB.
     * 그 외 → DEFAULT|HTML|ACTIVE|DISABLED.
     */
    public static String resolveChannelEffectiveKey(String merchantStored, String hqDefault) {
        String stored = normalizeChannelMerchantStored(merchantStored);
        if (FOLLOW_HQ.equals(stored)) {
            return normalizeChannelHqDefault(hqDefault);
        }
        return stored;
    }
}
