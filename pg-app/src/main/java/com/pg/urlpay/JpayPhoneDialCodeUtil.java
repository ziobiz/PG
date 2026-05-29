package com.pg.urlpay;

import java.util.Locale;

/**
 * JPAY 결제창 전화 국가번호 드롭다운 활성화.
 * <p>본사·가맹 설정 및 URL {@code phoneDial=Y|N} 로 단건 오버라이드.</p>
 */
public final class JpayPhoneDialCodeUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";

    private JpayPhoneDialCodeUtil() {
    }

    public static boolean isYes(String yn) {
        if (yn == null || yn.isBlank()) {
            return false;
        }
        String u = yn.trim().toUpperCase(Locale.ROOT);
        return "Y".equals(u) || "YES".equals(u) || "1".equals(u) || "TRUE".equals(u);
    }

    /** URL 쿼리 {@code phoneDial} — Y/1/yes 또는 N/0/no. null 이면 설정만 따름. */
    public static Boolean parseUrlOverride(String phoneDialParam) {
        if (phoneDialParam == null || phoneDialParam.isBlank()) {
            return null;
        }
        String u = phoneDialParam.trim().toUpperCase(Locale.ROOT);
        if ("Y".equals(u) || "YES".equals(u) || "1".equals(u) || "TRUE".equals(u)) {
            return Boolean.TRUE;
        }
        if ("N".equals(u) || "NO".equals(u) || "0".equals(u) || "FALSE".equals(u)) {
            return Boolean.FALSE;
        }
        return null;
    }

    public static String normalizeMerchantOverride(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "DEFAULT".equals(u) || "HQ".equals(u)) {
            return null;
        }
        return isYes(u) ? "Y" : "N";
    }

    public static String formatMerchantUiValue(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return FOLLOW_HQ;
        }
        return isYes(dbValue) ? "Y" : "N";
    }

    public static boolean resolveEnabled(String merchantOverrideYn, String hqDefaultYn, String urlPhoneDialParam) {
        Boolean url = parseUrlOverride(urlPhoneDialParam);
        if (url != null) {
            return url;
        }
        String mo = normalizeMerchantOverride(merchantOverrideYn);
        if (mo != null) {
            return isYes(mo);
        }
        return isYes(hqDefaultYn);
    }
}
