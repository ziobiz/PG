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
}
