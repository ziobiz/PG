package com.pg.util;

import java.util.Locale;

/**
 * 업체(tb_merchant_profile.use_yn) 사용 상태.
 * <ul>
 *   <li>{@code Y} — 사용(결제·정산·로그인 가능)</li>
 *   <li>{@code N} — 미사용(로그인 가능, 신규 결제·정산 등 서비스 중단)</li>
 *   <li>{@code S} — 영구정지(로그인 불가, 연동 사용자 계정 일괄 정지)</li>
 * </ul>
 */
public final class OrgUseYnUtil {

    public static final String Y = "Y";
    public static final String N = "N";
    public static final String S = "S";

    private OrgUseYnUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Y;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case N, "미사용" -> N;
            case S, "SUSPENDED", "영구정지" -> S;
            default -> Y;
        };
    }

    public static String display(String code) {
        return switch (normalize(code)) {
            case N -> "미사용";
            case S -> "영구정지";
            default -> "사용";
        };
    }

    /** 신규 결제·정산·연동 게이트 — Y 만 허용 */
    public static boolean isServiceAllowed(String useYn) {
        return Y.equals(normalize(useYn));
    }

    /** 영구정지 — 로그인 차단 */
    public static boolean isLoginBlocked(String useYn) {
        return S.equals(normalize(useYn));
    }
}
