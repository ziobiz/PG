package com.pg.integration.pg.ilk;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IlkCryptoUtilTest {

    private static final String KEY_32 = "0123456789abcdef0123456789abcdef";
    private static final String IV_16 = "abcdef0123456789";

    @Test
    void aesRoundTrip() {
        String plain = "4111111111111111";
        String enc = IlkCryptoUtil.encryptAesBase64(plain, KEY_32, IV_16);
        assertNotEquals(plain, enc);
        assertEquals(plain, IlkCryptoUtil.decryptAesBase64(enc, KEY_32, IV_16));
    }

    @Test
    void signIsDeterministicAndVerifiable() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchantInformation.merchantId", "MID001");
        body.put("clientReferenceInformation.code", "ORD-1");
        body.put("orderInformation.amountDetails.totalAmount", "1000");
        String sign1 = IlkCryptoUtil.signCompactJson(body, KEY_32);
        String sign2 = IlkCryptoUtil.signCompactJson(body, KEY_32);
        assertEquals(sign1, sign2);
        assertEquals(64, sign1.length());
        Map<String, Object> full = new LinkedHashMap<>(body);
        full.put("sign", sign1);
        assertTrue(IlkCryptoUtil.verifySign(full, KEY_32));
        full.put("sign", sign1 + "00");
        assertFalse(IlkCryptoUtil.verifySign(full, KEY_32));
    }

    @Test
    void hexKeyNormalization() {
        String hex32 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
        String hex16 = "00112233445566778899aabbccddeeff";
        byte[] k = IlkCryptoUtil.normalizeKeyBytes(hex32, 32);
        byte[] iv = IlkCryptoUtil.normalizeKeyBytes(hex16, 16);
        assertEquals(32, k.length);
        assertEquals(16, iv.length);
        String enc = IlkCryptoUtil.encryptAesBase64("1225", hex32, hex16);
        assertEquals("1225", IlkCryptoUtil.decryptAesBase64(enc, hex32, hex16));
    }
}
