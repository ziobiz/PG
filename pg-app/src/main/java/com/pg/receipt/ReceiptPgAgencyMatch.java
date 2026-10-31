package com.pg.receipt;

import com.pg.integration.pg.PgVendor;

import java.util.Locale;

/**
 * 거래 van ↔ 결제대행사 pg_cd 패밀리 매칭 (전 PG 공통).
 * ElementPay·JPAY·ChillPay·Eximbay·ILK·ox 및 향후 추가 PG 모두
 * {@code BASE} / {@code BASE xxx} / {@code BASE_xxx} 형태로 동일 규칙을 쓴다.
 */
public final class ReceiptPgAgencyMatch {

    private ReceiptPgAgencyMatch() {
    }

    /**
     * 거래 van 과 가맹 바인딩·API연동 pg_cd 가 같은 결제대행사 계열인지.
     */
    public static boolean sameVanFamily(String van, String pgCd) {
        if (van == null || van.isBlank() || pgCd == null || pgCd.isBlank()) {
            return false;
        }
        String vRaw = van.trim().toUpperCase(Locale.ROOT);
        String pRaw = pgCd.trim().toUpperCase(Locale.ROOT);
        if (vRaw.equals(pRaw)) {
            return true;
        }
        /* 공백·언더스코어 확장코드: ELEMENTPAY DPL, JPAY API JPY, CHILLPAY URL … */
        if (pRaw.startsWith(vRaw + " ") || pRaw.startsWith(vRaw + "_")
                || vRaw.startsWith(pRaw + " ") || vRaw.startsWith(pRaw + "_")) {
            return true;
        }
        String v = PgVendor.normalizePgCdKey(van);
        String p = PgVendor.normalizePgCdKey(pgCd);
        if (v.isEmpty() || p.isEmpty()) {
            return false;
        }
        if (v.equals(p)) {
            return true;
        }
        if (p.startsWith(v + "_") || v.startsWith(p + "_")) {
            return true;
        }
        String vRoot = rootToken(v);
        String pRoot = rootToken(p);
        if (!vRoot.isEmpty() && vRoot.equals(pRoot)) {
            return true;
        }
        return matchesKnownVendorFamily(van, pgCd);
    }

    private static String rootToken(String normalized) {
        int i = normalized.indexOf('_');
        return i > 0 ? normalized.substring(0, i) : normalized;
    }

    /** 등록된 벤더 헬퍼(향후 PG도 normalize 루트로 이미 커버; 여기는 보조). */
    static boolean matchesKnownVendorFamily(String van, String pgCd) {
        if (pgCd == null || pgCd.isBlank() || van == null || van.isBlank()) {
            return false;
        }
        if (PgVendor.isJpayFamily(pgCd) && PgVendor.isJpayFamily(van)) {
            return true;
        }
        if (PgVendor.isChillPayFamily(pgCd) && PgVendor.isChillPayFamily(van)) {
            return true;
        }
        if (PgVendor.isEximbayFamily(pgCd) && PgVendor.isEximbayFamily(van)) {
            return true;
        }
        if (PgVendor.isElementPayFamily(pgCd) && PgVendor.isElementPayFamily(van)) {
            return true;
        }
        if (PgVendor.isIlkFamily(pgCd) && PgVendor.isIlkFamily(van)) {
            return true;
        }
        if (PgVendor.isOxFamily(pgCd) && PgVendor.isOxFamily(van)) {
            return true;
        }
        if (PgVendor.isQxipayFamily(pgCd) && PgVendor.isQxipayFamily(van)) {
            return true;
        }
        if (PgVendor.isOmspayFamily(pgCd) && PgVendor.isOmspayFamily(van)) {
            return true;
        }
        return PgVendor.isPaysoFamily(pgCd) && PgVendor.isPaysoFamily(van);
    }
}
