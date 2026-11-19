package com.pg.crypto.chain;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;

/**
 * TRON Base58Check(T…) ↔ hex(41…) 비교 유틸.
 * TronGrid 이벤트는 hex, 수탁 주소는 Base58 인 경우가 많아 동일 주소 판별에 사용.
 */
public final class CryptoTronAddressUtil {

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    private CryptoTronAddressUtil() {
    }

    public static boolean sameAddress(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String x = a.trim();
        String y = b.trim();
        if (x.isEmpty() || y.isEmpty()) {
            return false;
        }
        if (x.equalsIgnoreCase(y)) {
            return true;
        }
        String hx = toHex41(x);
        String hy = toHex41(y);
        return !hx.isEmpty() && hx.equalsIgnoreCase(hy);
    }

    /** Base58 T… 또는 41…/0x41… → 41 + 40hex (소문자). 실패 시 "". */
    public static String toHex41(String addr) {
        if (addr == null) {
            return "";
        }
        String s = addr.trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.matches("(?i)41[0-9a-f]{40}")) {
            return s.toLowerCase(Locale.ROOT);
        }
        if (s.matches("(?i)[0-9a-f]{40}")) {
            return ("41" + s).toLowerCase(Locale.ROOT);
        }
        if (s.startsWith("T") || s.startsWith("t")) {
            byte[] decoded = decodeBase58(s);
            if (decoded == null || decoded.length < 25) {
                return "";
            }
            /* payload 21 + checksum 4 */
            byte[] payload = Arrays.copyOfRange(decoded, 0, decoded.length - 4);
            byte[] check = Arrays.copyOfRange(decoded, decoded.length - 4, decoded.length);
            byte[] hash = sha256(sha256(payload));
            if (hash == null || hash[0] != check[0] || hash[1] != check[1]
                    || hash[2] != check[2] || hash[3] != check[3]) {
                return "";
            }
            if (payload.length != 21 || (payload[0] & 0xff) != 0x41) {
                return "";
            }
            return bytesToHex(payload);
        }
        return "";
    }

    private static byte[] decodeBase58(String input) {
        BigInteger bi = BigInteger.ZERO;
        for (int i = 0; i < input.length(); i++) {
            int idx = ALPHABET.indexOf(input.charAt(i));
            if (idx < 0) {
                return null;
            }
            bi = bi.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(idx));
        }
        byte[] bytes = bi.toByteArray();
        /* leading zeros */
        int zeros = 0;
        for (int i = 0; i < input.length() && input.charAt(i) == '1'; i++) {
            zeros++;
        }
        byte[] out;
        if (bytes.length > 0 && bytes[0] == 0) {
            out = new byte[zeros + bytes.length - 1];
            System.arraycopy(bytes, 1, out, zeros, bytes.length - 1);
        } else {
            out = new byte[zeros + bytes.length];
            System.arraycopy(bytes, 0, out, zeros, bytes.length);
        }
        return out;
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            return null;
        }
    }

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte value : b) {
            sb.append(String.format("%02x", value));
        }
        return sb.toString();
    }
}
