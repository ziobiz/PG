package com.pg.util;

import java.time.LocalDateTime;
import java.util.Locale;

/** 리스크 자동등록 트리거 — 추적기간(미사용·일·월·년) */
public final class CardRiskTrackPeriod {

    public static final String MODE_NONE = "NONE";
    public static final String MODE_DAY = "DAY";
    public static final String MODE_MONTH = "MONTH";
    public static final String MODE_YEAR = "YEAR";

    private CardRiskTrackPeriod() {
    }

    public static String normalizeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return MODE_NONE;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case MODE_DAY, MODE_MONTH, MODE_YEAR -> u;
            default -> MODE_NONE;
        };
    }

    public static int clampValue(Integer value) {
        if (value == null || value < 1) {
            return 1;
        }
        return Math.min(value, 9999);
    }

    public static int displayValue(Integer value) {
        return value != null && value > 0 ? value : 1;
    }

    public static boolean hasWindow(String mode, Integer value) {
        return !MODE_NONE.equals(normalizeMode(mode)) && value != null && value > 0;
    }

    public static LocalDateTime windowStart(String mode, Integer value, LocalDateTime now) {
        if (now == null || !hasWindow(mode, value)) {
            return null;
        }
        int v = clampValue(value);
        return switch (normalizeMode(mode)) {
            case MODE_DAY -> now.minusDays(v);
            case MODE_MONTH -> now.minusMonths(v);
            case MODE_YEAR -> now.minusYears(v);
            default -> null;
        };
    }

    public static String formatDisplay(String mode, Integer value) {
        String m = normalizeMode(mode);
        if (MODE_NONE.equals(m)) {
            return "미사용";
        }
        int v = displayValue(value);
        return switch (m) {
            case MODE_DAY -> v + "일";
            case MODE_MONTH -> v + "월";
            case MODE_YEAR -> v + "년";
            default -> "미사용";
        };
    }
}
