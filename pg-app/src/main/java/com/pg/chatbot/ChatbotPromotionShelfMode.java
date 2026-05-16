package com.pg.chatbot;

import java.util.Locale;

/**
 * 챗봇-pay 상단 인텔리전트 프로모션(프로모션 상품) 노출 방식 — {@code merchant_profile.chatbot_promotion_shelf_mode} 저장값.
 */
public enum ChatbotPromotionShelfMode {

    HIDDEN,
    PROMOTION,
    DYNAMIC,
    HYBRID;

    /**
     * DB·API 등에서 읽은 문자열을 정규화합니다. 공백·미지정은 {@link #PROMOTION} 입니다.
     */
    public static ChatbotPromotionShelfMode resolveStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return PROMOTION;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        for (ChatbotPromotionShelfMode m : values()) {
            if (m.name().equals(u)) {
                return m;
            }
        }
        return PROMOTION;
    }

    /**
     * 순환 간격(초): 30~86400, 30초 단위로 맞춥니다.
     */
    public static int normalizeRotateSeconds(Integer raw) {
        int v = raw != null ? raw : 30;
        if (v < 30) {
            v = 30;
        }
        if (v > 86400) {
            v = 86400;
        }
        int rounded = (int) Math.round(v / 30.0) * 30;
        if (rounded < 30) {
            rounded = 30;
        }
        if (rounded > 86400) {
            rounded = 86400;
        }
        return rounded;
    }
}
