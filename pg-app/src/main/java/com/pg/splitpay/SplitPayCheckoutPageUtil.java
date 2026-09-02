package com.pg.splitpay;

import com.pg.integration.pg.PgVendor;

/**
 * URL 분할결제 회차 결제창 — 가맹 운영 URL PG에 따라 분기.
 * ChillPay·JPAY·ElementPay·Eximbay·ILK 및 향후 계열 동일 규칙.
 */
public final class SplitPayCheckoutPageUtil {

    public static final String PAGE_CHILLPAY = "pay.html";
    public static final String PAGE_JPAY = "jpay-pay.html";
    public static final String PAGE_ELEMENTPAY = "elementpay-pay.html";
    public static final String PAGE_EXIMBAY = "eximbay-pay.html";
    public static final String PAGE_ILK = "ilk-pay.html";

    private SplitPayCheckoutPageUtil() {
    }

    public static String resolveCheckoutPage(String operationalPgCd) {
        if (operationalPgCd == null || operationalPgCd.isBlank()) {
            return "";
        }
        if (PgVendor.isJpayFamily(operationalPgCd)) {
            return PAGE_JPAY;
        }
        if (PgVendor.isElementPayFamily(operationalPgCd)) {
            return PAGE_ELEMENTPAY;
        }
        if (PgVendor.isEximbayFamily(operationalPgCd)) {
            return PAGE_EXIMBAY;
        }
        if (PgVendor.isIlkFamily(operationalPgCd)) {
            return PAGE_ILK;
        }
        return PAGE_CHILLPAY;
    }

    public static boolean hasSupportedOperationalPg(String operationalPgCd) {
        if (operationalPgCd == null || operationalPgCd.isBlank()) {
            return false;
        }
        return PgVendor.isJpayFamily(operationalPgCd)
                || PgVendor.isChillPayFamily(operationalPgCd)
                || PgVendor.isElementPayFamily(operationalPgCd)
                || PgVendor.isEximbayFamily(operationalPgCd)
                || PgVendor.isIlkFamily(operationalPgCd);
    }
}
