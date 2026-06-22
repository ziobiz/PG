package com.pg.splitpay;

import com.pg.integration.pg.PgVendor;

/** URL 분할결제 회차 결제창 — 가맹 운영 URL PG(ChillPay·JPAY)에 따라 분기 */
public final class SplitPayCheckoutPageUtil {

    public static final String PAGE_CHILLPAY = "pay.html";
    public static final String PAGE_JPAY = "jpay-pay.html";

    private SplitPayCheckoutPageUtil() {
    }

    public static String resolveCheckoutPage(String operationalPgCd) {
        if (operationalPgCd == null || operationalPgCd.isBlank()) {
            return "";
        }
        return PgVendor.isJpayFamily(operationalPgCd) ? PAGE_JPAY : PAGE_CHILLPAY;
    }

    public static boolean hasSupportedOperationalPg(String operationalPgCd) {
        if (operationalPgCd == null || operationalPgCd.isBlank()) {
            return false;
        }
        return PgVendor.isJpayFamily(operationalPgCd) || PgVendor.isChillPayFamily(operationalPgCd);
    }
}
