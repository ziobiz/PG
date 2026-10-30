package com.pg.urlpay;

import java.util.Locale;

/**
 * 웹용 QR 열기 화면 상단·하단 문구 모드.
 * <ul>
 *   <li>{@code FOLLOW_HQ} — 가맹만: 본사 URL결제설정 기본</li>
 *   <li>{@code ACTIVE} — 직접입력(문구 비면 상단=가맹명, 하단=Provided by ICOPAY)</li>
 *   <li>{@code DISABLED} — 미활성(숨김)</li>
 * </ul>
 */
public final class WebPayQrBrandLabelModeUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";
    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";
    public static final String DEFAULT_FOOTER = "Provided by ICOPAY";

    private WebPayQrBrandLabelModeUtil() {
    }

    /** 본사 기본 — FOLLOW_HQ 없음 */
    public static String normalizeHq(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVE;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (DISABLED.equals(u) || "N".equals(u) || "OFF".equals(u)) {
            return DISABLED;
        }
        return ACTIVE;
    }

    /** 가맹 저장 */
    public static String normalizeMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        if (DISABLED.equals(u) || "N".equals(u) || "OFF".equals(u)) {
            return DISABLED;
        }
        if (ACTIVE.equals(u) || "Y".equals(u) || "ON".equals(u) || "DIRECT".equals(u)) {
            return ACTIVE;
        }
        return FOLLOW_HQ;
    }

    public static String resolveEffective(String merchantStored, String hqDefault) {
        String stored = normalizeMerchantStored(merchantStored);
        if (!FOLLOW_HQ.equals(stored)) {
            return stored;
        }
        return normalizeHq(hqDefault);
    }

    public static String trimText(String raw, int max) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.length() > max) {
            return t.substring(0, max);
        }
        return t;
    }
}
