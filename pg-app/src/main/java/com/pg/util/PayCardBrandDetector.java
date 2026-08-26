package com.pg.util;

import java.util.Locale;

public final class PayCardBrandDetector {

    private PayCardBrandDetector() {
    }

    public static String normalizePan(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static PayCardBrand detect(String panDigits) {
        if (panDigits == null || panDigits.length() < 2) {
            return PayCardBrand.UNKNOWN;
        }
        if (panDigits.startsWith("34") || panDigits.startsWith("37")) {
            return PayCardBrand.AMEX;
        }
        String p2 = panDigits.substring(0, 2);
        if ("36".equals(p2) || "38".equals(p2) || "39".equals(p2)) {
            return PayCardBrand.DINERS;
        }
        if (panDigits.startsWith("4")) {
            return PayCardBrand.VISA;
        }
        if (panDigits.startsWith("35")) {
            return PayCardBrand.JCB;
        }
        if (panDigits.startsWith("62")) {
            return PayCardBrand.UNIONPAY;
        }
        if (panDigits.startsWith("60") || panDigits.startsWith("81")) {
            return PayCardBrand.UNIONPAY;
        }
        if (panDigits.length() >= 2) {
            int d0 = panDigits.charAt(0) - '0';
            int d1 = panDigits.charAt(1) - '0';
            if (d0 == 5 && d1 >= 1 && d1 <= 5) {
                return PayCardBrand.MASTERCARD;
            }
            if (d0 == 2 && d1 >= 2 && d1 <= 7) {
                return PayCardBrand.MASTERCARD;
            }
        }
        return PayCardBrand.UNKNOWN;
    }

    public static int expectedLength(PayCardBrand brand) {
        if (brand == PayCardBrand.AMEX) {
            return 15;
        }
        if (brand == PayCardBrand.DINERS) {
            return 14;
        }
        return 16;
    }

    public static String brandKey(PayCardBrand brand) {
        return brand != null ? brand.name() : PayCardBrand.UNKNOWN.name();
    }

    /**
     * 관리·결제창에서 쓰는 별칭 포함: MASTER→MASTERCARD, UNION→UNIONPAY, AMX→AMEX, DINNER→DINERS.
     */
    public static PayCardBrand parseBrandKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case "VISA", "V" -> PayCardBrand.VISA;
            case "MASTERCARD", "MASTER", "MC", "M" -> PayCardBrand.MASTERCARD;
            case "JCB", "J" -> PayCardBrand.JCB;
            case "UNIONPAY", "UNION", "U" -> PayCardBrand.UNIONPAY;
            case "DINERS", "DINNER", "DINERSCLUB", "D" -> PayCardBrand.DINERS;
            case "AMEX", "AMX", "A", "AMERICANEXPRESS" -> PayCardBrand.AMEX;
            case "UNKNOWN" -> PayCardBrand.UNKNOWN;
            default -> {
                try {
                    yield PayCardBrand.valueOf(t);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
