package com.pg.urlpay;

/**
 * 결제창 구매자 연락처(이메일·국가·전화) — 전 PG 공통 Y/N 과 레거시 JPAY {@code checkoutFieldMode} 상호 변환.
 */
public final class CheckoutBuyerContactUtil {

    private CheckoutBuyerContactUtil() {
    }

    public static boolean isYn(String v) {
        return "Y".equalsIgnoreCase(v != null ? v.trim() : "");
    }

    /**
     * 레거시 JPAY 1·2·3형 호환 코드.
     * 연락처 전부 비활성 → {@code CARD_PREFILL}, 배송만 비활성 → {@code CARD_ONLY}, 그 외 {@code FULL}.
     */
    public static String toLegacyCheckoutFieldMode(String emailYn, String countryYn, String phoneYn, String shippingYn) {
        boolean anyContact = isYn(emailYn) || isYn(countryYn) || isYn(phoneYn);
        if (!anyContact) {
            return JpayCheckoutFieldModeUtil.CARD_PREFILL;
        }
        if (!isYn(shippingYn)) {
            return JpayCheckoutFieldModeUtil.CARD_ONLY;
        }
        return JpayCheckoutFieldModeUtil.FULL;
    }
}
