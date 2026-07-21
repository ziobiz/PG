package com.pg.urlpay;

import java.util.Locale;

/** 가맹 웹결제(URL·JPAY) 결제창 상단 로고 모드 */
public final class WebPaymentHeaderLogoModeUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";
    public static final String DEFAULT = "DEFAULT";
    /** 결제창 HTML 기본 문구(ICOPAY 등) — 총판 이미지 없이 텍스트만 */
    public static final String HTML = "HTML";
    public static final String DISABLED = "DISABLED";
    public static final String ACTIVE = "ACTIVE";

    private WebPaymentHeaderLogoModeUtil() {
    }

    /** 결제창 실효 모드(FOLLOW_HQ 제외) */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case DISABLED -> DISABLED;
            case ACTIVE -> ACTIVE;
            case HTML, "HTML_DEFAULT", "DEFAULT_HTML" -> HTML;
            case FOLLOW_HQ, "HQ" -> DEFAULT;
            default -> DEFAULT;
        };
    }

    /** 가맹 DB 저장 — FOLLOW_HQ 유지 */
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
}
