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
        return brand == PayCardBrand.AMEX ? 15 : 16;
    }

    public static String brandKey(PayCardBrand brand) {
        return brand != null ? brand.name() : PayCardBrand.UNKNOWN.name();
    }

    public static PayCardBrand parseBrandKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PayCardBrand.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
