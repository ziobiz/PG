package com.pg.integration.pg.omspay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * OMSPAY(Waylar) 웹훅 — {@code x-waylar-signature} = HMAC-SHA256(secret, rawBody) lowercase hex.
 */
public final class OmspaySignatureUtil {

    private OmspaySignatureUtil() {
    }

    public static boolean verify(String signatureHeader, String rawBody, String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        if (signatureHeader == null || signatureHeader.isBlank() || rawBody == null) {
            return false;
        }
        String got = signatureHeader.trim().toLowerCase(Locale.ROOT);
        if (got.startsWith("sha256=")) {
            got = got.substring(7).trim();
        }
        String expect = hmacSha256Hex(webhookSecret, rawBody);
        return !expect.isBlank() && constantTimeEquals(expect, got);
    }

    public static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload != null ? payload.getBytes(StandardCharsets.UTF_8) : new byte[0]);
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
