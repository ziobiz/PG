package com.pg.splitpay;

import com.pg.entity.MerchantProfile;
import com.pg.urlpay.UrlPayCheckoutModeUtil;

/** 가맹 URL 분할결제 활성 여부 — {@code split_pay_enabled_yn=Y}. API URL 결제방식 SPLIT_PAY 와 별도 스위치. */
public final class SplitPayMerchantUtil {

    private SplitPayMerchantUtil() {
    }

    public static boolean isEnabled(MerchantProfile mp) {
        if (mp == null) {
            return false;
        }
        String yn = mp.getSplitPayEnabledYn();
        return yn != null && "Y".equalsIgnoreCase(yn.trim());
    }

    public static String resolveApiCheckoutModeForDisplay(MerchantProfile mp) {
        if (mp == null) {
            return UrlPayCheckoutModeUtil.STANDARD;
        }
        return UrlPayCheckoutModeUtil.normalize(mp.getApiUrlPayCheckoutMode());
    }
}
