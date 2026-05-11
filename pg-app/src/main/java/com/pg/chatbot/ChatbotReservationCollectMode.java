package com.pg.chatbot;

import java.util.Locale;
import java.util.Optional;

/**
 * 예약·장소형 상품 결제 징수 방식. 일반 SALE 은 항상 FULL 로 간주합니다.
 */
public enum ChatbotReservationCollectMode {

    FULL("FULL", "전액 결제"),
    DEPOSIT("DEPOSIT", "예약금(부분)");

    private final String code;
    private final String labelKo;

    ChatbotReservationCollectMode(String code, String labelKo) {
        this.code = code;
        this.labelKo = labelKo;
    }

    public String getCode() {
        return code;
    }

    public String getLabelKo() {
        return labelKo;
    }

    public static ChatbotReservationCollectMode resolve(String raw) {
        return fromCode(raw).orElse(FULL);
    }

    public static Optional<ChatbotReservationCollectMode> fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        for (ChatbotReservationCollectMode m : values()) {
            if (m.code.equals(u)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }
}
