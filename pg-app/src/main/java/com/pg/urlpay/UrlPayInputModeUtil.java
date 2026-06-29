package com.pg.urlpay;

import java.util.Locale;

/**
 * URL 공개 결제창(jpay-pay.html) 입력방식.
 * <ul>
 *   <li>{@code GENERAL} — 가맹 개별 표시 옵션·JPAY 입력필드 모드 그대로.</li>
 *   <li>{@code TYPE_AA} — AE + 다국어 메뉴만 활성.</li>
 *   <li>{@code TYPE_AN} — 금액·카드·이름·성, 카드 브랜드 드롭다운 숨김, 상품명 숨김(구 TYPE_A).</li>
 *   <li>{@code TYPE_AG} — AN + 상품명 노출.</li>
 *   <li>{@code TYPE_AF} — AG + 로고·경고메세지 미활성.</li>
 *   <li>{@code TYPE_AE} — AF + 상품명 미사용.</li>
 *   <li>{@code TYPE_BN} — AN + 카드 브랜드 드롭다운(구 TYPE_B).</li>
 *   <li>{@code TYPE_BG} — BN + 상품명 노출.</li>
 *   <li>{@code TYPE_BF} — BG + 로고·경고메세지 미활성.</li>
 *   <li>{@code TYPE_BE} — BF + 상품명 미사용.</li>
 *   <li>{@code TYPE_BA} — BE + 다국어 메뉴만 활성.</li>
 *   <li>{@code TYPE_CN} — 표시 옵션·입력필드 모두 활성(구 TYPE_C).</li>
 * </ul>
 */
public final class UrlPayInputModeUtil {

    /** 가맹 DB 저장값 — 본사 URL·API 입력방식 기본값을 채널별로 따름 */
    public static final String FOLLOW_HQ = "FOLLOW_HQ";

    public static final String GENERAL = "GENERAL";
    public static final String TYPE_AA = "TYPE_AA";
    public static final String TYPE_AN = "TYPE_AN";
    public static final String TYPE_AG = "TYPE_AG";
    public static final String TYPE_AF = "TYPE_AF";
    public static final String TYPE_AE = "TYPE_AE";
    public static final String TYPE_BN = "TYPE_BN";
    public static final String TYPE_BG = "TYPE_BG";
    public static final String TYPE_BF = "TYPE_BF";
    public static final String TYPE_BE = "TYPE_BE";
    public static final String TYPE_BA = "TYPE_BA";
    public static final String TYPE_CN = "TYPE_CN";

    private UrlPayInputModeUtil() {
    }

    /** 적용 채널 — URL=공개 URL·챗봇·분할 URL, API=가맹 API 인라인(entry=merchant_api) */
    public enum Channel {
        URL, API
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERAL;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "DEFAULT".equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        return switch (u) {
            case TYPE_AA, "AA", "TYPEAA" -> TYPE_AA;
            case TYPE_AN, "AN", "TYPEAN", "TYPE_A", "A", "TYPEA" -> TYPE_AN;
            case TYPE_AG, "AG", "TYPEAG" -> TYPE_AG;
            case TYPE_AF, "AF", "TYPEAF" -> TYPE_AF;
            case TYPE_AE, "AE", "TYPEAE" -> TYPE_AE;
            case TYPE_BN, "BN", "TYPEBN", "TYPE_B", "B", "TYPEB" -> TYPE_BN;
            case TYPE_BG, "BG", "TYPEBG" -> TYPE_BG;
            case TYPE_BF, "BF", "TYPEBF" -> TYPE_BF;
            case TYPE_BE, "BE", "TYPEBE" -> TYPE_BE;
            case TYPE_BA, "BA", "TYPEBA" -> TYPE_BA;
            case TYPE_CN, "CN", "TYPECN", "TYPE_C", "C", "TYPEC" -> TYPE_CN;
            default -> GENERAL;
        };
    }

    /** 가맹 DB 저장 — FOLLOW_HQ 유지, 그 외 normalize */
    public static String normalizeMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERAL;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "DEFAULT".equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        return normalize(u);
    }

    /** null 이면 본사 정책 따름(오버라이드 없음) */
    public static String normalizeMerchantOverride(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "DEFAULT".equals(u) || "HQ".equals(u)) {
            return null;
        }
        return normalize(u);
    }

    public static String formatMerchantUiValue(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = dbValue.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "DEFAULT".equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        return normalize(u);
    }

    public static String resolve(String merchantDbValue, String hqUrlDefault, String hqApiDefault, Channel channel) {
        String override = normalizeMerchantOverride(merchantDbValue);
        if (override != null) {
            return override;
        }
        String hq = channel == Channel.API ? hqApiDefault : hqUrlDefault;
        return normalize(hq != null ? hq : (channel == Channel.API ? TYPE_BA : GENERAL));
    }

    public static String formatAuditLabel(String mode) {
        if (FOLLOW_HQ.equals(normalize(mode))) {
            return "본사정책 따름";
        }
        return switch (normalize(mode)) {
            case TYPE_AA -> "AA 타입";
            case TYPE_AN -> "AN 타입";
            case TYPE_AG -> "AG 타입";
            case TYPE_AF -> "AF 타입";
            case TYPE_AE -> "AE 타입";
            case TYPE_BN -> "BN 타입";
            case TYPE_BG -> "BG 타입";
            case TYPE_BF -> "BF 타입";
            case TYPE_BE -> "BE 타입";
            case TYPE_BA -> "BA 타입";
            case TYPE_CN -> "CN 타입";
            default -> "일반";
        };
    }

    /** 업체관리 목록 「타입」 컬럼 — 짧은 코드(HQ·GN·AA·BA …). 언어 공통 표기. */
    public static String formatCompListLabel(String mode) {
        String ui = formatMerchantUiValue(mode);
        if (FOLLOW_HQ.equals(ui)) {
            return "HQ";
        }
        return switch (normalize(ui)) {
            case TYPE_AA -> "AA";
            case TYPE_AN -> "AN";
            case TYPE_AG -> "AG";
            case TYPE_AF -> "AF";
            case TYPE_AE -> "AE";
            case TYPE_BN -> "BN";
            case TYPE_BG -> "BG";
            case TYPE_BF -> "BF";
            case TYPE_BE -> "BE";
            case TYPE_BA -> "BA";
            case TYPE_CN -> "CN";
            default -> "GN";
        };
    }

    /** CARD_PREFILL 최소 폼 — AA/AN/AG/AF/AE/BN/BG/BF/BE/BA */
    public static boolean isMinimalForm(String mode) {
        String m = normalize(mode);
        return TYPE_AA.equals(m) || TYPE_AN.equals(m) || TYPE_AG.equals(m)
                || TYPE_AF.equals(m) || TYPE_AE.equals(m)
                || TYPE_BN.equals(m) || TYPE_BG.equals(m) || TYPE_BF.equals(m) || TYPE_BE.equals(m)
                || TYPE_BA.equals(m);
    }

    public static boolean hidesCardBrandSelect(String mode) {
        String m = normalize(mode);
        return TYPE_AN.equals(m) || TYPE_AG.equals(m) || TYPE_AF.equals(m) || TYPE_AE.equals(m)
                || TYPE_AA.equals(m);
    }

    /** AG/BG — 상품명 노출 */
    public static boolean forcesProductName(String mode) {
        String m = normalize(mode);
        return TYPE_AG.equals(m) || TYPE_AF.equals(m)
                || TYPE_BG.equals(m) || TYPE_BF.equals(m);
    }

    public static boolean forcesFullPresentation(String mode) {
        return TYPE_CN.equals(normalize(mode));
    }

    public static boolean disablesCheckoutHeader(String mode) {
        String m = normalize(mode);
        return TYPE_AF.equals(m) || TYPE_AE.equals(m) || TYPE_AA.equals(m)
                || TYPE_BF.equals(m) || TYPE_BE.equals(m) || TYPE_BA.equals(m);
    }

    /**
     * 입력방식 프리셋 — checkout-context·관리자 폼과 동일한 표시 옵션.
     * {@code GENERAL} 은 빈 Optional.
     */
    public static java.util.Optional<InputModePreset> preset(String mode) {
        return switch (normalize(mode)) {
            case TYPE_AN, TYPE_BN -> java.util.Optional.of(new InputModePreset(
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
            case TYPE_AA -> java.util.Optional.of(new InputModePreset(
                    "N", "N", "Y",
                    WebPaymentHeaderLogoModeUtil.DISABLED, WebPaymentHeaderLogoModeUtil.DISABLED));
            case TYPE_BA -> java.util.Optional.of(new InputModePreset(
                    "N", "N", "Y",
                    WebPaymentHeaderLogoModeUtil.DISABLED, WebPaymentHeaderLogoModeUtil.DISABLED));
            case TYPE_CN -> java.util.Optional.of(new InputModePreset(
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
