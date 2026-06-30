package com.pg.util;

import com.pg.entity.PgTrnsctn;

/**
 * 결제내역·통합조회 — 결제자 이메일({@code customer_id})·고객 열 표시.
 * {@code guest} 는 식별자가 아니므로 이메일·고객 열에서 제외합니다.
 */
public final class PayerContactDisplayUtil {

    private PayerContactDisplayUtil() {
    }

    public static boolean isGuestMarker(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return true;
        }
        return "guest".equalsIgnoreCase(customerId.trim());
    }

    public static boolean looksLikeEmail(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String t = value.trim();
        int at = t.indexOf('@');
        return at > 0 && at < t.length() - 1 && t.indexOf('@', at + 1) < 0;
    }

    /** {@link PgTrnsctn#getCustomerId()} — guest·비이메일 제외 */
    public static String resolvePayerEmail(PgTrnsctn t) {
        if (t == null) {
            return "";
        }
        String id = t.getCustomerId();
        if (isGuestMarker(id)) {
            return "";
        }
        String trimmed = id.trim();
        return looksLikeEmail(trimmed) ? trimmed : trimmed.contains("@") ? trimmed : "";
    }

    /** 결제내역 {@code chillCustomer} — {@code 이메일 | 성명}, guest 미표시 */
    public static String formatChillCustomer(PgTrnsctn t) {
        if (t == null) {
            return "-";
        }
        String em = resolvePayerEmail(t);
        String nm = t.getCustomerNm() != null ? t.getCustomerNm().trim() : "";
        if (em.isEmpty() && nm.isEmpty()) {
            return "-";
        }
        if (!em.isEmpty() && !nm.isEmpty()) {
            return em + " | " + nm;
        }
        return !em.isEmpty() ? em : nm;
    }

    public static String payerEmailOrDash(PgTrnsctn t) {
        String em = resolvePayerEmail(t);
        return em.isEmpty() ? "-" : em;
    }
}
