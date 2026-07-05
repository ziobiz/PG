package com.pg.util;

import java.util.Locale;

/** 본사 멀티 PG 라우팅 차원 — 브랜드만 / 통화만 / 혼합. */
public final class MultiPgRoutingModeUtil {

    public static final String BRAND = "BRAND";
    public static final String CURRENCY = "CURRENCY";
    public static final String BRAND_AND_CURRENCY = "BRAND_AND_CURRENCY";

    private MultiPgRoutingModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return BRAND_AND_CURRENCY;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case BRAND, "BRAND_ONLY", "CARD_BRAND" -> BRAND;
            case CURRENCY, "CURRENCY_ONLY" -> CURRENCY;
            case BRAND_AND_CURRENCY, "MIXED", "HYBRID", "BOTH" -> BRAND_AND_CURRENCY;
            default -> BRAND_AND_CURRENCY;
        };
    }

    public static void validateOrThrow(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String n = normalize(raw);
        if (!BRAND.equals(n) && !CURRENCY.equals(n) && !BRAND_AND_CURRENCY.equals(n)) {
            throw new IllegalArgumentException(
                    "멀티 PG 라우팅 방식은 BRAND, CURRENCY, BRAND_AND_CURRENCY 중 하나여야 합니다. 입력값=" + raw);
        }
    }
}
