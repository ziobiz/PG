package com.pg.util;

import java.util.Locale;
import java.util.Set;

/**
 * 가맹 결제대행사 행별 카드 브랜드 허용 범위(ALL, V/M/J/U/A/D 조합 약어).
 */
public final class CardBrandScopeUtil {

    private static final Set<String> ALLOWED = Set.of(
            "ALL",
            "VMJUA",
            "VMJU",
            "VMJ",
            "VM",
            "VJ",
            "MJ",
            "V",
            "M",
            "J",
            "U",
            "A",
            "D");

    private CardBrandScopeUtil() {
    }

    public static boolean isAllowedCode(String code) {
        if (code == null || code.isBlank()) {
            return true;
        }
        return ALLOWED.contains(code.trim().toUpperCase(Locale.ROOT));
    }

    /** 비허용·공백이면 {@code ALL}. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ALL";
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        return ALLOWED.contains(t) ? t : "ALL";
    }

    public static void validateOrThrow(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED.contains(t)) {
            throw new IllegalArgumentException(
                    "카드브랜드 범위는 ALL, VMJUA, VMJU, VMJ, VM, VJ, MJ, V, M, J, U, A, D 중 하나여야 합니다. 입력값=" + raw);
        }
    }

    /**
     * 결제창·PG API에서 쓰는 브랜드 키(VISA, MASTERCARD, …)를 scope 약어(V, M, …)로 변환.
     * 알 수 없으면 빈 문자열.
     */
    public static String toScopeLetter(String brandRaw) {
        if (brandRaw == null || brandRaw.isBlank()) {
            return "";
        }
        String b = brandRaw.trim().toUpperCase(Locale.ROOT);
        return switch (b) {
            case "V", "VISA" -> "V";
            case "M", "MASTERCARD", "MASTER", "MC" -> "M";
            case "J", "JCB" -> "J";
            case "U", "UNIONPAY", "UNION" -> "U";
            case "A", "AMEX", "AMERICAN EXPRESS", "AMERICANEXPRESS" -> "A";
            case "D", "DINERS", "DINERS CLUB", "DISCOVER" -> "D";
            default -> ALLOWED.contains(b) && b.length() == 1 ? b : "";
        };
    }

    /** {@code scope} 가 {@code brandLetter}(V/M/J/U/A/D)를 포함하는지. ALL 이면 true. */
    public static boolean matchesScope(String scopeRaw, String brandLetter) {
        String scope = normalize(scopeRaw);
        if ("ALL".equals(scope)) {
            return true;
        }
        if (brandLetter == null || brandLetter.isBlank()) {
            return true;
        }
        String letter = brandLetter.trim().toUpperCase(Locale.ROOT);
        if (letter.length() > 1) {
            letter = toScopeLetter(letter);
        }
        if (letter.isEmpty()) {
            return true;
        }
        return scope.contains(letter);
    }

    /** 브랜드 키(자유 형식)가 scope 와 매칭되는지. */
    public static boolean matchesBrand(String scopeRaw, String brandRaw) {
        return matchesScope(scopeRaw, toScopeLetter(brandRaw));
    }

    /** ALL=0, 구체 브랜드·짧은 조합일수록 높음 — 라우팅 특이도 점수 */
    public static int specificityScore(String scopeRaw) {
        String scope = normalize(scopeRaw);
        if ("ALL".equals(scope)) {
            return 0;
        }
        return Math.max(1, 12 - scope.length());
    }
}
