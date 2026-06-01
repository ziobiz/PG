package com.pg.urlpay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * JPAY CARD_PREFILL — 가맹 prepare {@code buyerPrefill} 정규화·검증.
 * JPAY 필수: 카드·성명(고객 입력) + 이메일·전화(가맹 prefill). 배송 주소는 선택 prefill.
 */
public final class JpayBuyerPrefillUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JpayBuyerPrefillUtil() {
    }

    /**
     * CARD_PREFILL 모드일 때만 buyerPrefill 을 JSON 문자열로 반환. 그 외 null(무시).
     * @throws IllegalArgumentException 검증 실패
     */
    @SuppressWarnings("unchecked")
    public static String resolvePrefillJsonForPrepare(Map<String, Object> body, String effectiveFieldMode) {
        if (!JpayCheckoutFieldModeUtil.CARD_PREFILL.equals(JpayCheckoutFieldModeUtil.normalize(effectiveFieldMode))) {
            return null;
        }
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("CARD_PREFILL 모드에서는 buyerPrefill(이메일·전화 등)이 필요합니다.");
        }
        Object raw = body.get("buyerPrefill");
        if (raw == null) {
            raw = body.get("buyer_prefill");
        }
        Map<String, Object> map;
        if (raw instanceof Map<?, ?> m) {
            map = new LinkedHashMap<>();
            m.forEach((k, v) -> {
                if (k != null && v != null) {
                    map.put(String.valueOf(k), v);
                }
            });
        } else if (raw instanceof String s && !s.isBlank()) {
            try {
                map = MAPPER.readValue(s.trim(), Map.class);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("buyerPrefill JSON 형식이 올바르지 않습니다.");
            }
        } else {
            throw new IllegalArgumentException("CARD_PREFILL 모드에서는 buyerPrefill 객체가 필요합니다.");
        }
        Map<String, String> normalized = IcipayBuyerContactUtil.normalize(map);
        validateForJpay(normalized);
        try {
            return MAPPER.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("buyerPrefill 저장 형식 오류");
        }
    }

    /** 세션·결제창용 공개 맵(JSON 파싱). */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parsePublicMap(String prefillJson) {
        if (prefillJson == null || prefillJson.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(prefillJson, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private static void validateForJpay(Map<String, String> n) {
        if (n.get("email") == null || n.get("email").isBlank()) {
            throw new IllegalArgumentException("buyerPrefill.email (JPAY 필수)이 필요합니다.");
        }
        if (n.get("phone") == null || n.get("phone").isBlank()) {
            throw new IllegalArgumentException("buyerPrefill.phone (JPAY 필수)이 필요합니다.");
        }
        // countryIso2 는 prepare 에 없으면 결제창에서 접속국 기본값으로 보완
    }

    public static String canonicalCountryIso2(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
