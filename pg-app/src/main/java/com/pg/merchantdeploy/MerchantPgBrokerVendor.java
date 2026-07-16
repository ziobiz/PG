package com.pg.merchantdeploy;

import com.pg.integration.pg.PgVendor;

import java.util.Locale;

/**
 * 브로커 배포 키트·시크릿 범위에 쓰는 PG 벤더 식별자.
 * 신규 PG는 상수와 {@link MerchantPgBrokerCatalog} 등록만 추가하면 됩니다.
 */
public final class MerchantPgBrokerVendor {

    public static final String ALL = "ALL";
    public static final String CHILLPAY = PgVendor.CHILLPAY;
    public static final String JPAY = PgVendor.JPAY;
    public static final String EXIMBAY = PgVendor.EXIMBAY;
    public static final String ELEMENTPAY = PgVendor.ELEMENTPAY;
    public static final String ILK = PgVendor.ILK;

    private MerchantPgBrokerVendor() {
    }

    public static String normalizeScope(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isKnownVendorScope(String scope) {
        String s = normalizeScope(scope);
        return ALL.equals(s) || PgVendor.isChillPayFamily(s) || PgVendor.isJpayFamily(s)
                || PgVendor.isEximbayFamily(s) || PgVendor.isElementPayFamily(s)
                || PgVendor.isIlkFamily(s);
    }

    /** HTTP 경로 세그먼트(소문자) → 벤더 스코프 */
    public static String fromBrokerPathSegment(String segment) {
        if (segment == null) {
            return ALL;
        }
        String u = segment.trim().toLowerCase(Locale.ROOT);
        if ("chillpay".equals(u)) {
            return CHILLPAY;
        }
        if ("jpay".equals(u)) {
            return JPAY;
        }
        if ("eximbay".equals(u)) {
            return EXIMBAY;
        }
        if ("elementpay".equals(u)) {
            return ELEMENTPAY;
        }
        if ("ilk".equals(u) || "ilkpay".equals(u)) {
            return ILK;
        }
        return ALL;
    }
}
