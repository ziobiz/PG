package com.pg.util;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

/**
 * Google Authenticator 호환 TOTP (RFC 6238, SHA-1, 30초, 6자리).
 */
public final class TotpRfc6238 {

    private static final int PERIOD_SECONDS = 30;

    private TotpRfc6238() {
    }

    public static String randomBase32Secret() {
        byte[] buf = new byte[20];
        new SecureRandom().nextBytes(buf);
        return new Base32(0, null, false).encodeToString(buf);
    }

    public static byte[] decodeBase32(String base32) {
        if (base32 == null || base32.isBlank()) {
            throw new IllegalArgumentException("OTP 시크릿이 비어 있습니다.");
        }
        return new Base32(0, null, false).decode(base32.trim().toUpperCase(Locale.ROOT));
    }

    public static int hotp(byte[] key, long counter) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
        byte[] hash = mac.doFinal(msg);
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        return binary % 1_000_000;
    }

    public static int totpAtEpoch(byte[] key, long epochSeconds) throws GeneralSecurityException {
        long tc = epochSeconds / PERIOD_SECONDS;
        return hotp(key, tc);
    }

    public static boolean verify(String base32Secret, String sixDigitCode, int driftSteps) {
        if (sixDigitCode == null || sixDigitCode.length() < 6) {
            return false;
        }
        String c = sixDigitCode.trim();
        if (!c.matches("\\d{6}")) {
            return false;
        }
        final byte[] key;
        try {
            key = decodeBase32(base32Secret);
        } catch (Exception e) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        long currentWindow = now / PERIOD_SECONDS;
        try {
            for (int i = -driftSteps; i <= driftSteps; i++) {
                int code = hotp(key, currentWindow + i);
                if (String.format(Locale.US, "%06d", code).equals(c)) {
                    return true;
                }
            }
        } catch (GeneralSecurityException e) {
            return false;
        }
        return false;
    }
}
