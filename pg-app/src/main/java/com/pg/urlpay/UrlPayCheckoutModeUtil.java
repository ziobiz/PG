package com.pg.urlpay;

import java.util.Locale;

/**
 * 가맹 URL·챗봇·API 중계 결제 방식 — {@code tb_merchant_profile.url_pay_checkout_mode}.
 */
public final class UrlPayCheckoutModeUtil {

    public static final String STANDARD = "STANDARD";
    public static final String REPAY = "REPAY";

    private UrlPayCheckoutModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return STANDARD;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (REPAY.equals(u) || "URL_PAY_REPAY".equals(u)) {
            return REPAY;
        }
        return STANDARD;
    }

    public static boolean isRepay(String mode) {
        return REPAY.equals(normalize(mode));
    }

    public static boolean isUrlPayRepayVariantParam(String urlPayVariantParam) {
        return urlPayVariantParam != null && REPAY.equalsIgnoreCase(urlPayVariantParam.trim());
    }

    /** URL 쿼리 {@code urlPayVariant=REPAY} 가 있으면 우선, 없으면 가맹 프로필 모드. */
    public static boolean resolveEffectiveRepay(String urlPayVariantParam, String merchantCheckoutMode) {
        if (isUrlPayRepayVariantParam(urlPayVariantParam)) {
            return true;
        }
        return isRepay(merchantCheckoutMode);
    }

    public static String effectiveVariantCode(String urlPayVariantParam, String merchantCheckoutMode) {
        return resolveEffectiveRepay(urlPayVariantParam, merchantCheckoutMode) ? REPAY : STANDARD;
    }
}
