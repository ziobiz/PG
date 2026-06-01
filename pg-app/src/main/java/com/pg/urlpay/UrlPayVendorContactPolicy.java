package com.pg.urlpay;

import com.pg.integration.pg.PgVendor;
import com.pg.merchantdeploy.MerchantPgBrokerVendor;

/**
 * URL 결제 sale 시 PG사로 buyer 연락처 필드를 전송할지 여부.
 * ICOPAY prepare 에서는 항상 buyer 를 수집·세션에 보관하고, PG 어댑터만 선택적으로 전송합니다.
 */
public final class UrlPayVendorContactPolicy {

    public record ContactPolicy(boolean sendEmail, boolean sendPhone, boolean sendCountryIso2) {
    }

    private UrlPayVendorContactPolicy() {
    }

    public static ContactPolicy forOperationalPgCd(String pgCd) {
        if (PgVendor.isJpayFamily(pgCd)) {
            return new ContactPolicy(true, true, true);
        }
        if (PgVendor.isChillPayFamily(pgCd)) {
            return new ContactPolicy(false, false, false);
        }
        return new ContactPolicy(false, false, false);
    }

    public static ContactPolicy forBrokerVendor(String pgVendor) {
        if (MerchantPgBrokerVendor.JPAY.equalsIgnoreCase(pgVendor)) {
            return new ContactPolicy(true, true, true);
        }
        return new ContactPolicy(false, false, false);
    }
}
