package com.pg.chatbot;

import java.util.Locale;
import java.util.Optional;

/**
 * 챗봇 카탈로그 상품 노출 유형.FULL 상품판매·시간 슬롯 예약·장소(숙박 등) 예약 구분.
 */
public enum ChatbotListingType {

    SALE("SALE", "상품판매"),
    RESERVATION_TIME("RESERVATION_TIME", "시간 예약"),
    /** 호텔·펜션 등 숙박: 고객은 체크인 일시·(선택) 체크아웃 날짜로 구간을 제출합니다. */
    RESERVATION_PLACE("RESERVATION_PLACE", "장소 예약");

    private final String code;
    private final String labelKo;

    ChatbotListingType(String code, String labelKo) {
        this.code = code;
        this.labelKo = labelKo;
    }

    public String getCode() {
        return code;
    }

    public String getLabelKo() {
        return labelKo;
    }

    public static Optional<ChatbotListingType> fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("RESERVATION".equals(u)) {
            return Optional.of(RESERVATION_TIME);
        }
        for (ChatbotListingType t : values()) {
            if (t.code.equals(u)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public static boolean needsReservationWindow(ChatbotListingType t) {
        return t == RESERVATION_TIME || t == RESERVATION_PLACE;
    }
}
