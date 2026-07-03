package com.pg.urlpay;

import java.util.Locale;

/**
 * JPAY URL 결제창 카드 유효기간(MM/YY) 입력 UI.
 * <ul>
 *   <li>{@code DROPDOWN} — 월·연도 드롭다운</li>
 *   <li>{@code TEXT} — MM·YYYY(4자) 직접 입력</li>
 *   <li>{@code HYBRID} — MM·YY(2자) 직접 입력</li>
 *   <li>{@code AI_B} — 모바일·PC 모두 드롭다운</li>
 *   <li>{@code AI_A} — 모바일·태블릿 드롭다운, PC 하이브리드</li>
 * </ul>
 */
public final class UrlPayCardExpiryModeUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";
    public static final String DROPDOWN = "DROPDOWN";
    public static final String TEXT = "TEXT";
    public static final String HYBRID = "HYBRID";
    public static final String AI_B = "AI_B";
    public static final String AI_A = "AI_A";

    /** UI 렌더링용 */
    public enum UiMode {
        DROPDOWN, TEXT, HYBRID
    }

    private UrlPayCardExpiryModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DROPDOWN;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case FOLLOW_HQ, "HQ", "DEFAULT" -> FOLLOW_HQ;
            case DROPDOWN, "DROP_DOWN", "SELECT" -> DROPDOWN;
            case TEXT, "INPUT", "MANUAL" -> TEXT;
            case HYBRID, "HYBRID_YY" -> HYBRID;
            case AI_B, "AIB", "AI-B" -> AI_B;
            case AI_A, "AIA", "AI-A" -> AI_A;
            default -> DROPDOWN;
        };
    }

    public static String normalizeMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u) || "DEFAULT".equals(u)) {
            return FOLLOW_HQ;
        }
        return normalize(u);
    }

    public static String formatMerchantUiValue(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = dbValue.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u) || "DEFAULT".equals(u)) {
            return FOLLOW_HQ;
        }
        return normalize(u);
    }

    public static String resolveStored(String merchantDbValue, String hqDefault) {
        String stored = normalizeMerchantStored(merchantDbValue);
        if (!FOLLOW_HQ.equals(stored)) {
            return stored;
        }
        return normalize(hqDefault != null ? hqDefault : DROPDOWN);
    }

    /** 클라이언트·서버 공통 — AI_* 를 실제 UI 모드로 */
    public static UiMode resolveUiMode(String storedOrEffective, boolean mobileOrTablet) {
        String m = normalize(storedOrEffective);
        if (AI_B.equals(m)) {
            return UiMode.DROPDOWN;
        }
        if (AI_A.equals(m)) {
            return mobileOrTablet ? UiMode.DROPDOWN : UiMode.HYBRID;
        }
        if (TEXT.equals(m)) {
            return UiMode.TEXT;
        }
        if (HYBRID.equals(m)) {
            return UiMode.HYBRID;
        }
        return UiMode.DROPDOWN;
    }

    public static String formatAuditLabel(String mode) {
        return switch (normalize(mode)) {
            case FOLLOW_HQ -> "본사정책 따름";
            case DROPDOWN -> "드롭다운";
            case TEXT -> "직접입력";
            case HYBRID -> "하이브리드(YY)";
            case AI_B -> "AI B";
            case AI_A -> "AI A";
            default -> "드롭다운";
        };
    }
}
