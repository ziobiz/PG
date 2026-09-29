package com.pg.integration.pg.ox;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * ox Sign Key — JSON 구조 공백 제거 후 HMAC-SHA256 hex.
 * @see <a href="https://docs.oxpayfinancial.com/integrity/">OxPay Integrity</a>
 */
public final class OxPayloadSigner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OxPayloadSigner() {
    }

    public static String sign(String signKey, String json) {
        if (signKey == null || signKey.isBlank() || json == null) {
            return "";
        }
        try {
            String compact = MAPPER.writeValueAsString(MAPPER.readTree(json));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(compact.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
