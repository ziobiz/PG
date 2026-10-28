package com.pg.integration.pg.qxipay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * QXIPAY 웹훅 서명 — {@code GW-Signature: t=&lt;unix&gt;,v1=&lt;hex HMAC-SHA256 of "{t}.{rawBody}"&gt;}.
 */
public final class QxipaySignatureUtil {

    private QxipaySignatureUtil() {
    }

    public static boolean verify(String gwSignatureHeader, String rawBody, String webhookSecret, long maxSkewSeconds) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        if (gwSignatureHeader == null || gwSignatureHeader.isBlank() || rawBody == null) {
            return false;
        }
        String t = null;
        java.util.List<String> v1s = new java.util.ArrayList<>();
        for (String part : gwSignatureHeader.split(",")) {
            String p = part != null ? part.trim() : "";
            int eq = p.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = p.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String v = p.substring(eq + 1).trim();
            if ("t".equals(k)) {
                t = v;
            } else if ("v1".equals(k) && !v.isBlank()) {
                v1s.add(v.toLowerCase(Locale.ROOT));
            }
        }
        if (t == null || t.isBlank() || v1s.isEmpty()) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(t);
        } catch (NumberFormatException e) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000L;
        if (maxSkewSeconds > 0 && Math.abs(now - ts) > maxSkewSeconds) {
            return false;
        }
        String payload = t + "." + rawBody;
        String expect = hmacSha256Hex(webhookSecret, payload);
        if (expect.isBlank()) {
            return false;
        }
        for (String got : v1s) {
            if (constantTimeEquals(expect, got)) {
                return true;
            }
        }
        return false;
    }

    public static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
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
