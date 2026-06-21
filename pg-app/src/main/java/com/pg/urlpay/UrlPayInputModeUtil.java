package com.pg.urlpay;

import java.util.Locale;

/**
 * URL 공개 결제창(jpay-pay.html) 입력방식.
 * <ul>
 *   <li>{@code GENERAL} — 가맹 개별 표시 옵션·JPAY 입력필드 모드 그대로.</li>
 *   <li>{@code TYPE_A} — 금액·카드·이름·성, 카드 자동 인식 드롭다운 숨김, 상품명 숨김.</li>
 *   <li>{@code TYPE_AG} — A + 상품명 노출.</li>
 *   <li>{@code TYPE_B} — A + 카드 자동 인식 드롭다운, 상품명 숨김.</li>
 *   <li>{@code TYPE_BG} — B + 상품명 노출.</li>
 *   <li>{@code TYPE_C} — 표시 옵션·입력필드 모두 활성(전체 폼).</li>
 * </ul>
 */
public final class UrlPayInputModeUtil {

    public static final String GENERAL = "GENERAL";
    public static final String TYPE_A = "TYPE_A";
    public static final String TYPE_AG = "TYPE_AG";
    public static final String TYPE_B = "TYPE_B";
    public static final String TYPE_BG = "TYPE_BG";
    public static final String TYPE_C = "TYPE_C";

    private UrlPayInputModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERAL;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case TYPE_A, "A", "TYPEA" -> TYPE_A;
            case TYPE_AG, "AG", "TYPEAG" -> TYPE_AG;
            case TYPE_B, "B", "TYPEB" -> TYPE_B;
            case TYPE_BG, "BG", "TYPEBG" -> TYPE_BG;
            case TYPE_C, "C", "TYPEC" -> TYPE_C;
            default -> GENERAL;
        };
    }

    public static String formatAuditLabel(String mode) {
        return switch (normalize(mode)) {
            case TYPE_A -> "A타입";
            case TYPE_AG -> "AG타입";
            case TYPE_B -> "B타입";
            case TYPE_BG -> "BG타입";
            case TYPE_C -> "C타입";
            default -> "일반";
        };
    }

    /** CARD_PREFILL 최소 폼 — A/AG/B/BG */
    public static boolean isMinimalForm(String mode) {
        String m = normalize(mode);
        return TYPE_A.equals(m) || TYPE_AG.equals(m) || TYPE_B.equals(m) || TYPE_BG.equals(m);
    }

    public static boolean hidesCardBrandSelect(String mode) {
        String m = normalize(mode);
        return TYPE_A.equals(m) || TYPE_AG.equals(m);
    }

    /** A/AG/B/BG — 가맹점명·다국어 숨김, 상품명은 AG/BG만 노출 */
    public static boolean forcesProductName(String mode) {
        String m = normalize(mode);
        return TYPE_AG.equals(m) || TYPE_BG.equals(m);
    }

    public static boolean forcesFullPresentation(String mode) {
        return TYPE_C.equals(normalize(mode));
    }
}
