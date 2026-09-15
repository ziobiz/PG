package com.pg.urlpay;

/**
 * 웹결제 카드입력 표시 — ACTIVE(기본)·DISABLED(금액·카드번호·유효·CVV·성·이름 숨김 + 안내문구).
 */
public final class UrlPayCardInputModeUtil {

    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";

    private UrlPayCardInputModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVE;
        }
        String m = raw.trim().toUpperCase();
        if (DISABLED.equals(m) || "N".equals(m) || "OFF".equals(m) || "INACTIVE".equals(m)) {
            return DISABLED;
        }
        return ACTIVE;
    }

    public static boolean isDisabled(String raw) {
        return DISABLED.equals(normalize(raw));
    }
}
