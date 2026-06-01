package com.pg.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PayCardPanHashUtil {

    private PayCardPanHashUtil() {
    }

    public static String hashPan(String panDigits) {
        String norm = PayCardBrandDetector.normalizePan(panDigits);
        if (norm.isEmpty()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(norm.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String maskForDisplay(String panDigits) {
        String norm = PayCardBrandDetector.normalizePan(panDigits);
        if (norm.length() < 8) {
            return norm.isEmpty() ? "****" : norm.substring(0, Math.min(4, norm.length())) + "****";
        }
        return norm.substring(0, 6) + "******" + norm.substring(norm.length() - 4);
    }
}
