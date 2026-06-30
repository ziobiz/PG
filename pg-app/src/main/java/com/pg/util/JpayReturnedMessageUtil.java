package com.pg.util;

import java.util.Locale;

/** JPAY 포털 Export {@code Returned Messages} 열 — 성공·실패 문구 구분 */
public final class JpayReturnedMessageUtil {

    private JpayReturnedMessageUtil() {
    }

    public static boolean isSuccessToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String t = raw.trim();
        String u = t.toUpperCase(Locale.ROOT);
        if (u.startsWith("0000") || u.startsWith("00:")) {
            return true;
        }
        return u.equals("SUCCESS") || u.endsWith(":SUCCESS") || u.contains("SUCCESS,");
    }

    /** 실패·거절 등 터미널 사유로 쓸 수 있는 반환 문구 */
    public static String failureReasonOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (isSuccessToken(t)) {
            return null;
        }
        return t;
    }
}
