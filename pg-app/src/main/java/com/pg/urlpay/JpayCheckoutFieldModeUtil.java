package com.pg.urlpay;

import java.util.Locale;

/**
 * JPAY URL 결제창(jpay-pay.html) 입력 필드 노출 모드.
 * <p>JPAY 필수: (1) 카드·CVV (2) 성명 (3) 이메일 (4) 전화. (5) 배송지는 선택.</p>
 * <ul>
 *   <li>{@code FULL} — 1~5 전체 입력(기존).</li>
 *   <li>{@code CARD_ONLY} — 1~4만 고객 입력(이메일·전화 포함). 5 배송 주소란 숨김.</li>
 *   <li>{@code CARD_PREFILL} — 1·2만 고객 입력. 3·4 및 5·6은 가맹 {@code prepare buyerPrefill} 로 전달.</li>
 * </ul>
 */
public final class JpayCheckoutFieldModeUtil {

    public static final String FULL = "FULL";
    /** JPAY 필수 4항목(카드·성명·이메일·전화) — 코드명 유지, 의미는 「필수 4항목」. */
    public static final String CARD_ONLY = "CARD_ONLY";
    public static final String CARD_PREFILL = "CARD_PREFILL";
    public static final String FOLLOW_HQ = "FOLLOW_HQ";

    private JpayCheckoutFieldModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return FULL;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case CARD_ONLY -> CARD_ONLY;
            case CARD_PREFILL -> CARD_PREFILL;
            default -> FULL;
        };
    }

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
        return normalize(dbValue);
    }

    public static String resolve(String merchantOverride, String hqDefault) {
        String mo = normalizeMerchantOverride(merchantOverride);
        if (mo != null) {
            return mo;
        }
        return normalize(hqDefault);
    }

    /** 배송 주소(5) 입력란 숨김. */
    public static boolean hidesAddressFields(String mode) {
        String m = normalize(mode);
        return CARD_ONLY.equals(m) || CARD_PREFILL.equals(m);
    }

    /** 이메일·전화(3·4) 입력란 숨김 — CARD_PREFILL. */
    public static boolean hidesContactFields(String mode) {
        return CARD_PREFILL.equals(normalize(mode));
    }
}
