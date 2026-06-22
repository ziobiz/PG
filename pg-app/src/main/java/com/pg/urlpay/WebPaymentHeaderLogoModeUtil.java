package com.pg.urlpay;

import java.util.Locale;

/** 가맹 웹결제(URL·JPAY) 결제창 상단 로고 모드 */
public final class WebPaymentHeaderLogoModeUtil {

    public static final String DEFAULT = "DEFAULT";
    /** 결제창 HTML 기본 문구(ICOPAY 등) — 총판 이미지 없이 텍스트만 */
    public static final String HTML = "HTML";
    public static final String DISABLED = "DISABLED";
    public static final String ACTIVE = "ACTIVE";

    private WebPaymentHeaderLogoModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case DISABLED -> DISABLED;
            case ACTIVE -> ACTIVE;
            case HTML, "HTML_DEFAULT", "DEFAULT_HTML" -> HTML;
            default -> DEFAULT;
        };
    }
}
