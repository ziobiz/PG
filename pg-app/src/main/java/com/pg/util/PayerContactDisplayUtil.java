package com.pg.util;

import com.pg.entity.PgTrnsctn;

/**
 * 결제내역·통합조회 — 결제자 이메일({@code customer_id})·고객 열 표시.
 * {@code guest} 는 식별자가 아니므로 이메일·고객 열에서 제외합니다.
 */
public final class PayerContactDisplayUtil {

    private static final String PLACEHOLDER_RECEIPT_EMAIL = "noreply@icopay.co.kr";

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

    /** EP 자리값 등 — 고객 거래명세서 수신처로 쓰지 않음 */
    public static boolean isPlaceholderReceiptEmail(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        return PLACEHOLDER_RECEIPT_EMAIL.equalsIgnoreCase(email.trim());
    }

    /**
     * 실이메일이면 guest·자리값을 덮어쓴다. 이메일이 없고 customerId 가 비어 있으면 guest.
     */
    public static void applyEmailIfUsable(PgTrnsctn t, String email, int maxLen) {
        if (t == null) {
            return;
        }
        int max = maxLen > 0 ? maxLen : 100;
        String em = email != null ? email.trim() : "";
        if (looksLikeEmail(em) && !isPlaceholderReceiptEmail(em)) {
            String cur = t.getCustomerId();
            if (isGuestMarker(cur) || !looksLikeEmail(cur) || isPlaceholderReceiptEmail(cur)) {
                t.setCustomerId(em.length() > max ? em.substring(0, max) : em);
            }
            return;
        }
        if (t.getCustomerId() == null || t.getCustomerId().isBlank()) {
            t.setCustomerId("guest");
        }
    }

    /** {@link PgTrnsctn#getCustomerId()} — guest·비이메일 제외 */
    public static String resolvePayerEmail(PgTrnsctn t) {
        if (t == null) {
            return "";
        }
        String id = t.getCustomerId();
        if (isGuestMarker(id) || isPlaceholderReceiptEmail(id)) {
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
