package com.pg.util;

/**
 * ChillPay DirectCredit 공통 규칙(매뉴얼·NOTI 테스트 페이지와 동일).
 */
public final class ChillPayDirectCreditUtil {

    private ChillPayDirectCreditUtil() {
    }

    /**
     * OrderNo: 최대 20자, 영숫자·하이픈·밑줄만 유지. 비면 {@code O}{@code System.currentTimeMillis()}.
     */
    public static String normalizeOrderNo(String orderNo) {
        String s = orderNo != null ? orderNo.trim() : "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '-' || ch == '_') {
                sb.append(ch);
            }
        }
        String cleaned = sb.toString();
        if (cleaned.isEmpty()) {
            cleaned = "O" + System.currentTimeMillis();
        }
        return cleaned.length() <= 20 ? cleaned : cleaned.substring(0, 20);
    }
}
