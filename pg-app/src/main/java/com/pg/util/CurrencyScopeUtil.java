package com.pg.util;

import java.util.Locale;
import java.util.Set;

/** 가맹 결제대행사 행별 결제 통화(표시·청구 힌트) 허용 범위. */
public final class CurrencyScopeUtil {

    public static final String ALL = "ALL";

    private static final Set<String> ALLOWED = Set.of(
            ALL, "THB", "JPY", "USD", "KRW", "SGD", "HKD", "CNY", "EUR", "GBP", "AUD", "CAD");

    private CurrencyScopeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        return ALLOWED.contains(t) ? t : ALL;
    }

    public static void validateOrThrow(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED.contains(t)) {
            throw new IllegalArgumentException(
                    "통화 범위는 ALL, THB, JPY, USD, KRW, SGD, HKD, CNY, EUR, GBP, AUD, CAD 중 하나여야 합니다. 입력값=" + raw);
        }
    }

    public static boolean matchesScope(String scopeRaw, String currencyHint) {
        String scope = normalize(scopeRaw);
        if (ALL.equals(scope)) {
            return true;
        }
        if (currencyHint == null || currencyHint.isBlank()) {
            return true;
        }
        return scope.equalsIgnoreCase(currencyHint.trim());
    }

    /** ALL=0, 구체 통화=10 — 라우팅 특이도 점수 */
    public static int specificityScore(String scopeRaw) {
        return ALL.equals(normalize(scopeRaw)) ? 0 : 10;
    }
}
