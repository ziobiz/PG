package com.pg.urlpay;

import java.util.Locale;

/**
 * 일반결제(URL·API 인라인 공통) 카드 인증 방식.
 * <ul>
 *   <li>{@code THREE_DS} — 3DS 인증 후 승인</li>
 *   <li>{@code NONE3D} — 비인증(2DS) 승인</li>
 *   <li>{@code FOLLOW_HQ} — 가맹 저장값: 본사 {@code card_auth_mode_default} 따름</li>
 * </ul>
 * 가맹이 FOLLOW_HQ가 아니면 본사보다 우선한다. 구독(정기)에는 적용하지 않는다.
 */
public final class CardAuthModeUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";
    public static final String THREE_DS = "THREE_DS";
    /** 비인증·NONE3D·2DS */
    public static final String NONE3D = "NONE3D";

    private CardAuthModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return THREE_DS;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (u) {
            case FOLLOW_HQ, "HQ", "DEFAULT" -> FOLLOW_HQ;
            case THREE_DS, "3DS", "E3D", "THREEDS" -> THREE_DS;
            case NONE3D, "NON_3DS", "NON3DS", "2DS", "NONE_3D", "NO_3DS", "NOCERT" -> NONE3D;
            default -> THREE_DS;
        };
    }

    public static String normalizeMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u) || "DEFAULT".equals(u)) {
            return FOLLOW_HQ;
        }
        String n = normalize(u);
        return FOLLOW_HQ.equals(n) ? FOLLOW_HQ : n;
    }

    public static String formatMerchantUiValue(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return FOLLOW_HQ;
        }
        return normalizeMerchantStored(dbValue);
    }

    /** 가맹 저장값 + 본사 기본 → 실효값 (THREE_DS | NONE3D). */
    public static String resolveStored(String merchantDbValue, String hqDefault) {
        String stored = normalizeMerchantStored(merchantDbValue);
        if (!FOLLOW_HQ.equals(stored)) {
            return stored;
        }
        String hq = normalize(hqDefault != null ? hqDefault : THREE_DS);
        return FOLLOW_HQ.equals(hq) ? THREE_DS : hq;
    }

    public static boolean isNone3d(String effective) {
        return NONE3D.equals(normalize(effective));
    }

    public static boolean isThreeDs(String effective) {
        return THREE_DS.equals(normalize(effective));
    }

    public static String formatAuditLabel(String mode) {
        return switch (normalizeMerchantStored(mode)) {
            case FOLLOW_HQ -> "본사정책 따름";
            case NONE3D -> "2DS(비인증)";
            default -> "3DS";
        };
    }
}
