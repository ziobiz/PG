package com.pg.urlpay;

import com.pg.entity.MerchantProfile;

import java.util.Locale;

/**
 * 웹용 QR(공개 URL /checkout) — 가맹 {@code webPaymentQrMode}.
 * <ul>
 *   <li>{@code AUTO}(기본): 웹결제 Y 이고 API IN/RE/WO 모두 미사용(URL 전용)일 때 실효</li>
 *   <li>{@code ACTIVE}: 웹결제 Y 이면 실효</li>
 *   <li>{@code DISABLED}: 실효 없음</li>
 * </ul>
 */
public final class WebPaymentQrModeUtil {

    public static final String AUTO = "AUTO";
    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";

    private WebPaymentQrModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (ACTIVE.equals(u) || "Y".equals(u) || "ON".equals(u)) {
            return ACTIVE;
        }
        if (DISABLED.equals(u) || "N".equals(u) || "OFF".equals(u)) {
            return DISABLED;
        }
        return AUTO;
    }

    public static boolean isUrlOnlyMerchant(MerchantProfile mp) {
        if (mp == null) {
            return false;
        }
        return !isY(mp.getApiBrokerInlineUseYn())
                && !isY(mp.getApiBrokerRedirectUseYn())
                && !isY(mp.getApiWordpressUseYn());
    }

    public static boolean isEffective(MerchantProfile mp) {
        if (mp == null || !isY(mp.getWebPaymentUseYn())) {
            return false;
        }
        String mode = normalize(mp.getWebPaymentQrMode());
        if (DISABLED.equals(mode)) {
            return false;
        }
        if (ACTIVE.equals(mode)) {
            return true;
        }
        return isUrlOnlyMerchant(mp);
    }

    private static boolean isY(String v) {
        return v != null && "Y".equalsIgnoreCase(v.trim());
    }
}
