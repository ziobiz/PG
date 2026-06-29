package com.pg.util;

/** 카드 PAN Luhn(Mod 10) 검증 */
public final class PayCardLuhnUtil {

    private PayCardLuhnUtil() {
    }

    public static boolean isValidLuhn(String panDigits) {
        if (panDigits == null || panDigits.isBlank()) {
            return false;
        }
        String pan = panDigits.trim();
        if (pan.length() < 13) {
            return false;
        }
        for (int i = 0; i < pan.length(); i++) {
            if (!Character.isDigit(pan.charAt(i))) {
                return false;
            }
        }
        int sum = 0;
        boolean shouldDouble = false;
        for (int i = pan.length() - 1; i >= 0; i--) {
            int digit = pan.charAt(i) - '0';
            if (shouldDouble) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            shouldDouble = !shouldDouble;
        }
        return sum % 10 == 0;
    }
}
