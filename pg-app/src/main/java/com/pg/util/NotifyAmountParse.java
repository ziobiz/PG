package com.pg.util;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 노티·콜백 본문의 금액 문자열을 반올림·정수화 없이 {@link BigDecimal}으로만 파싱합니다(콤마 제거만).
 */
public final class NotifyAmountParse {

    private NotifyAmountParse() {
    }

    /**
     * @param raw 노티 원문 (예: {@code "1,234.56"}, {@code "99.9"})
     * @return 파싱 실패·공백이면 empty
     */
    public static Optional<BigDecimal> parsePlain(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim().replace(",", "");
        if (s.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static boolean isPositive(Optional<BigDecimal> o) {
        return o.filter(b -> b.compareTo(BigDecimal.ZERO) > 0).isPresent();
    }
}
