package com.pg.integration.pg.qbang;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** 큐뱅 POS — AES-256-CBC PKCS5 (Token key/iv). */
public final class QbangCryptoUtil {

    private QbangCryptoUtil() {
    }

    public static String encryptAes256CbcBase64(String plainJson, String keyBase64, String ivBase64) {
        if (plainJson == null) {
            plainJson = "";
        }
        try {
            byte[] key = Base64.getDecoder().decode(nz(keyBase64));
            byte[] iv = Base64.getDecoder().decode(nz(ivBase64));
            if (key.length != 16 && key.length != 24 && key.length != 32) {
                throw new IllegalArgumentException("QBANG key length invalid: " + key.length);
            }
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] enc = cipher.doFinal(plainJson.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("QBANG AES encrypt failed: " + e.getMessage(), e);
        }
    }

    private static String nz(String s) {
        return s != null ? s.trim() : "";
    }
}
