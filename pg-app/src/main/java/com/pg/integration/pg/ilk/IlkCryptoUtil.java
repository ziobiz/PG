package com.pg.integration.pg.ilk;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * ILK OpenAPI 암복호화·서명.
 * <ul>
 *   <li>카드번호·유효기간: AES-256-CBC + PKCS5Padding → Base64</li>
 *   <li>서명: 공백 없는 compact JSON 전체에 HmacSHA256 → hex(소문자)</li>
 * </ul>
 */
public final class IlkCryptoUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String AES_TRANSFORM = "AES/CBC/PKCS5Padding";

    private IlkCryptoUtil() {
    }

    public static String encryptAesBase64(String plain, String seedKey, String seedIv) {
        if (plain == null) {
            plain = "";
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey(seedKey), new IvParameterSpec(aesIv(seedIv)));
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (Exception e) {
            throw new IllegalStateException("ILK AES encrypt failed: " + e.getMessage(), e);
        }
    }

    public static String decryptAesBase64(String cipherB64, String seedKey, String seedIv) {
        if (cipherB64 == null || cipherB64.isBlank()) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey(seedKey), new IvParameterSpec(aesIv(seedIv)));
            byte[] dec = cipher.doFinal(Base64.getDecoder().decode(cipherB64.trim()));
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("ILK AES decrypt failed: " + e.getMessage(), e);
        }
    }

    /**
     * {@code sign} 필드를 제외한 맵을 compact JSON 으로 직렬화한 뒤 HmacSHA256 hex 서명을 붙입니다.
     */
    public static String signCompactJson(Map<String, Object> payloadWithoutSign, String seedKey) {
        try {
            String compact = MAPPER.writeValueAsString(payloadWithoutSign);
            return hmacSha256Hex(compact, seedKey);
        } catch (Exception e) {
            throw new IllegalStateException("ILK sign failed: " + e.getMessage(), e);
        }
    }

    public static String hmacSha256Hex(String message, String seedKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(seedKeyBytes(seedKey), "HmacSHA256"));
            byte[] raw = mac.doFinal((message != null ? message : "").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw).toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            throw new IllegalStateException("ILK HMAC failed: " + e.getMessage(), e);
        }
    }

    public static boolean verifySign(Map<String, Object> fullPayload, String seedKey) {
        if (fullPayload == null || seedKey == null || seedKey.isBlank()) {
            return false;
        }
        Object signObj = fullPayload.get("sign");
        if (signObj == null || signObj.toString().isBlank()) {
            return false;
        }
        String expected = signObj.toString().trim();
        java.util.LinkedHashMap<String, Object> without = new java.util.LinkedHashMap<>(fullPayload);
        without.remove("sign");
        String actual = signCompactJson(without, seedKey);
        return expected.equalsIgnoreCase(actual);
    }

    private static SecretKeySpec aesKey(String seedKey) {
        return new SecretKeySpec(normalizeKeyBytes(seedKey, 32), "AES");
    }

    private static byte[] aesIv(String seedIv) {
        return normalizeKeyBytes(seedIv, 16);
    }

    /** 서명 키는 문서상 AES seedKey 와 동일 원문 바이트를 사용. */
    private static byte[] seedKeyBytes(String seedKey) {
        return (seedKey != null ? seedKey : "").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * hex(64→32B / 32→16B) 또는 UTF-8 패딩·절단으로 목표 길이를 맞춥니다.
     */
    static byte[] normalizeKeyBytes(String raw, int targetLen) {
        String s = raw != null ? raw.trim() : "";
        if (s.isEmpty()) {
            return new byte[targetLen];
        }
        if (looksLikeHex(s) && s.length() == targetLen * 2) {
            return HexFormat.of().parseHex(s);
        }
        byte[] utf = s.getBytes(StandardCharsets.UTF_8);
        if (utf.length == targetLen) {
            return utf;
        }
        byte[] out = new byte[targetLen];
        System.arraycopy(utf, 0, out, 0, Math.min(utf.length, targetLen));
        return out;
    }

    private static boolean looksLikeHex(String s) {
        if (s.length() % 2 != 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
