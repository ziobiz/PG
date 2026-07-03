package com.pg.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * JPAY·ChillPay 송부 전 사전 리스크 필터 코드.
 */
public final class PayPresaleRiskFilterCodes {

    public static final String ERROR_CODE = "PRESALE_RISK_FILTER";

    public static final String BUYER_EMAIL_MISMATCH = "BUYER_EMAIL_MISMATCH";
    public static final String BUYER_PHONE_MISMATCH = "BUYER_PHONE_MISMATCH";
    public static final String BUYER_NAME_MISMATCH = "BUYER_NAME_MISMATCH";
    public static final String HOLDER_NAME_SUSPICIOUS = "HOLDER_NAME_SUSPICIOUS";
    public static final String VELOCITY_CARD = "VELOCITY_CARD";
    public static final String VELOCITY_EMAIL = "VELOCITY_EMAIL";
    public static final String VELOCITY_IP = "VELOCITY_IP";
    public static final String PHONE_INVALID = "PHONE_INVALID";
    public static final String EMAIL_INVALID = "EMAIL_INVALID";

    private static final Pattern SUSPICIOUS_HOLDER = Pattern.compile(
            ".*(GIFTCARD\\s*HOLDER|GIFT\\s*CARD|PAYPAY\\s*USER|V\\s*PRECA|VPRECA|MERPAY\\s*MEMBER|"
                    + "KYASH\\s*MEMBER|PREPAID\\s*MEMBER|VANDALE\\s*USER|BUSINESS\\s*CARD).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EMAIL_BASIC = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private PayPresaleRiskFilterCodes() {
    }

    public static String normalizeEmail(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizePhone(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\D", "");
    }

    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    public static boolean namesEquivalent(String a, String b) {
        String na = normalizeName(a);
        String nb = normalizeName(b);
        if (na.isEmpty() || nb.isEmpty()) {
            return true;
        }
        if (na.equals(nb)) {
            return true;
        }
        String[] pa = na.split(" ");
        String[] pb = nb.split(" ");
        if (pa.length == 2 && pb.length == 2) {
            return (pa[0].equals(pb[0]) && pa[1].equals(pb[1]))
                    || (pa[0].equals(pb[1]) && pa[1].equals(pb[0]));
        }
        return false;
    }

    public static boolean isSuspiciousHolderName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return SUSPICIOUS_HOLDER.matcher(name.trim()).matches();
    }

    /** 0000000000·동일숫자 반복·과短·1234567890 등 비정상 전화 */
    public static boolean isInvalidPhone(String raw) {
        String p = normalizePhone(raw);
        if (p.isEmpty()) {
            return false;
        }
        if (p.length() < 7 || p.length() > 15) {
            return true;
        }
        if (allSameDigit(p)) {
            return true;
        }
        if (isSequentialDigits(p)) {
            return true;
        }
        if (p.startsWith("000000") || p.endsWith("0000000")) {
            return true;
        }
        return false;
    }

    public static boolean isInvalidEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String e = normalizeEmail(raw);
        if (!EMAIL_BASIC.matcher(e).matches()) {
            return true;
        }
        int at = e.indexOf('@');
        if (at <= 0) {
            return true;
        }
        String local = e.substring(0, at);
        if (local.length() < 2) {
            return true;
        }
        if (allSameChar(local)) {
            return true;
        }
        String domain = e.substring(at + 1);
        if (domain.startsWith("example.") || domain.equals("test") || domain.startsWith("test.")) {
            return true;
        }
        return false;
    }

    private static boolean allSameDigit(String digits) {
        if (digits.length() < 2) {
            return false;
        }
        char c = digits.charAt(0);
        for (int i = 1; i < digits.length(); i++) {
            if (digits.charAt(i) != c) {
                return false;
            }
        }
        return true;
    }

    private static boolean allSameChar(String s) {
        if (s.length() < 3) {
            return false;
        }
        char c = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != c) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSequentialDigits(String digits) {
        if (digits.length() < 8) {
            return false;
        }
        boolean asc = true;
        boolean desc = true;
        for (int i = 1; i < digits.length(); i++) {
            int prev = digits.charAt(i - 1) - '0';
            int cur = digits.charAt(i) - '0';
            if (cur != prev + 1) {
                asc = false;
            }
            if (cur != prev - 1) {
                desc = false;
            }
        }
        return asc || desc;
    }

    public static String maskEmail(String email) {
        String e = normalizeEmail(email);
        if (e.isEmpty()) {
            return "";
        }
        int at = e.indexOf('@');
        if (at <= 0) {
            return e.length() <= 3 ? e + "**" : e.substring(0, 3) + "**";
        }
        String local = e.substring(0, at);
        String domain = e.substring(at);
        String maskedLocal = local.length() <= 3 ? local + "**" : local.substring(0, 3) + "**";
        return maskedLocal + domain;
    }

    public static String maskPhone(String phone) {
        String p = normalizePhone(phone);
        if (p.isEmpty()) {
            return "";
        }
        if (p.length() <= 3) {
            return p + "**";
        }
        return p.substring(0, 3) + "**";
    }
}
