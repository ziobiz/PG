package com.pg.urlpay;

import java.util.Locale;

/**
 * URL 공개 결제창(jpay-pay.html) 입력방식.
 * <ul>
 *   <li>{@code GENERAL} — 가맹 개별 표시 옵션·JPAY 입력필드 모드 그대로.</li>
 *   <li>{@code TYPE_A} — 금액·카드·이름·성, 카드 자동 인식 드롭다운 숨김, 상품명 숨김.</li>
 *   <li>{@code TYPE_AG} — A + 상품명 노출.</li>
 *   <li>{@code TYPE_AF} — AG + 로고·경고메세지 비활성.</li>
 *   <li>{@code TYPE_AE} — AF + 상품명 미사용.</li>
 *   <li>{@code TYPE_B} — A + 카드 자동 인식 드롭다운, 상품명 숨김.</li>
 *   <li>{@code TYPE_BG} — B + 상품명 노출.</li>
 *   <li>{@code TYPE_BF} — BG + 로고·경고메세지 비활성.</li>
 *   <li>{@code TYPE_BE} — BF + 상품명 미사용.</li>
 *   <li>{@code TYPE_C} — 표시 옵션·입력필드 모두 활성(전체 폼).</li>
 * </ul>
 */
public final class UrlPayInputModeUtil {

    public static final String GENERAL = "GENERAL";
    public static final String TYPE_A = "TYPE_A";
    public static final String TYPE_AG = "TYPE_AG";
    public static final String TYPE_AF = "TYPE_AF";
    public static final String TYPE_AE = "TYPE_AE";
    public static final String TYPE_B = "TYPE_B";
    public static final String TYPE_BG = "TYPE_BG";
    public static final String TYPE_BF = "TYPE_BF";
    public static final String TYPE_BE = "TYPE_BE";
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
            case TYPE_AF, "AF", "TYPEAF" -> TYPE_AF;
            case TYPE_AE, "AE", "TYPEAE" -> TYPE_AE;
            case TYPE_B, "B", "TYPEB" -> TYPE_B;
            case TYPE_BG, "BG", "TYPEBG" -> TYPE_BG;
            case TYPE_BF, "BF", "TYPEBF" -> TYPE_BF;
            case TYPE_BE, "BE", "TYPEBE" -> TYPE_BE;
            case TYPE_C, "C", "TYPEC" -> TYPE_C;
            default -> GENERAL;
        };
    }

    public static String formatAuditLabel(String mode) {
        return switch (normalize(mode)) {
            case TYPE_A -> "A타입";
            case TYPE_AG -> "AG타입";
            case TYPE_AF -> "AF타입";
            case TYPE_AE -> "AE타입";
            case TYPE_B -> "B타입";
            case TYPE_BG -> "BG타입";
            case TYPE_BF -> "BF타입";
            case TYPE_BE -> "BE타입";
            case TYPE_C -> "C타입";
            default -> "일반";
        };
    }

    /** CARD_PREFILL 최소 폼 — A/AG/AF/AE/B/BG/BF/BE */
    public static boolean isMinimalForm(String mode) {
        String m = normalize(mode);
        return TYPE_A.equals(m) || TYPE_AG.equals(m) || TYPE_AF.equals(m) || TYPE_AE.equals(m)
                || TYPE_B.equals(m) || TYPE_BG.equals(m) || TYPE_BF.equals(m) || TYPE_BE.equals(m);
    }

    public static boolean hidesCardBrandSelect(String mode) {
        String m = normalize(mode);
        return TYPE_A.equals(m) || TYPE_AG.equals(m) || TYPE_AF.equals(m) || TYPE_AE.equals(m);
    }

    /** A/AG/AF/B/BG/BF — 가맹점명·다국어 숨김, 상품명은 AG/AF/BG/BF만 노출 */
    public static boolean forcesProductName(String mode) {
        String m = normalize(mode);
        return TYPE_AG.equals(m) || TYPE_AF.equals(m) || TYPE_BG.equals(m) || TYPE_BF.equals(m);
    }

    public static boolean forcesFullPresentation(String mode) {
        return TYPE_C.equals(normalize(mode));
    }

    public static boolean disablesCheckoutHeader(String mode) {
        String m = normalize(mode);
        return TYPE_AF.equals(m) || TYPE_AE.equals(m) || TYPE_BF.equals(m) || TYPE_BE.equals(m);
    }

    /**
     * 입력방식 프리셋 — checkout-context·관리자 폼과 동일한 표시 옵션.
     * {@code GENERAL} 은 빈 Optional.
     */
    public static java.util.Optional<InputModePreset> preset(String mode) {
        return switch (normalize(mode)) {
            case TYPE_A, TYPE_B -> java.util.Optional.of(new InputModePreset(
                    "N", "N", "N",
                    WebPaymentHeaderLogoModeUtil.DEFAULT, WebPaymentHeaderLogoModeUtil.DEFAULT));
            case TYPE_AG, TYPE_BG -> java.util.Optional.of(new InputModePreset(
                    "Y", "N", "N",
                    WebPaymentHeaderLogoModeUtil.DEFAULT, WebPaymentHeaderLogoModeUtil.DEFAULT));
            case TYPE_AF, TYPE_BF -> java.util.Optional.of(new InputModePreset(
                    "Y", "N", "N",
                    WebPaymentHeaderLogoModeUtil.DISABLED, WebPaymentHeaderLogoModeUtil.DISABLED));
            case TYPE_AE, TYPE_BE -> java.util.Optional.of(new InputModePreset(
                    "N", "N", "N",
                    WebPaymentHeaderLogoModeUtil.DISABLED, WebPaymentHeaderLogoModeUtil.DISABLED));
            case TYPE_C -> java.util.Optional.of(new InputModePreset(
                    "Y", "Y", "Y",
                    WebPaymentHeaderLogoModeUtil.DEFAULT, WebPaymentHeaderLogoModeUtil.DEFAULT));
            default -> java.util.Optional.empty();
        };
    }

    /** checkout-context map — (레거시) 호출해도 DB·폼 저장값을 덮지 않음 */
    public static void applyPresetToCheckoutMap(java.util.Map<String, Object> data, String mode) {
        /* 입력방식 프리셋은 관리 화면에서 입력방식 변경 시 JS 동기화 + 사용자 저장값이 최종 반영 */
    }

    public record InputModePreset(
            String urlPayProductNameUseYn,
            String urlPayCompanyNameShowYn,
            String urlPayLangMenuUseYn,
            String webPaymentHeaderLogoMode,
            String webPaymentHeaderSubtitleMode) {
    }
}
