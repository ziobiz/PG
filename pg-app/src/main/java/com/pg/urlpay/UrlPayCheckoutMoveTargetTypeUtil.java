package com.pg.urlpay;

public final class UrlPayCheckoutMoveTargetTypeUtil {

    public static final String COMP_CODE = "COMP_CODE";
    public static final String URL = "URL";

    private UrlPayCheckoutMoveTargetTypeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return COMP_CODE;
        }
        String m = raw.trim().toUpperCase();
        if (URL.equals(m) || "LINK".equals(m) || "HREF".equals(m)) {
            return URL;
        }
        return COMP_CODE;
    }
}
