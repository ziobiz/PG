package com.pg.integration.pg;

import java.util.Locale;

/**
 * 결제대행사(PG) 벤더 식별자와 {@code pg_cd} 계열 판별.
 * <p>
 * DB·API·거래의 {@code van} 필드 등에 쓰는 <strong>표준 코드 문자열</strong>을 한곳에서 관리합니다.
 * ChillPay 외 JPAY 등 신규 PG는 상수와 판별 로직을 여기에 추가한 뒤, 전용 연동 서비스에서 참조합니다.
 */
public final class PgVendor {

    /** ChillPay 계열 기본 코드 및 {@code van} 저장값. */
    public static final String CHILLPAY = "CHILLPAY";

    /**
     * JPAY(제이페이) 등 향후 연동용 예약 코드 — 아직 전용 서비스가 없으면 참조만 하고 분기하지 않습니다.
     */
    public static final String JPAY = "JPAY";

    private PgVendor() {
    }

    /**
     * {@code pg_cd}가 ChillPay 계열인지 — {@code CHILLPAY} 또는 {@code CHILLPAY_…} 접두.
     */
    public static boolean isChillPayFamily(String pgCd) {
        if (pgCd == null) {
            return false;
        }
        String u = pgCd.trim().toUpperCase(Locale.ROOT);
        return CHILLPAY.equals(u) || u.startsWith(CHILLPAY);
    }

    /**
     * 확장 코드 없이 정확히 {@code CHILLPAY} 인지 — 정렬·우선순위에서 확장 행과 구분할 때 사용.
     */
    public static boolean isChillPayBaseCode(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return true;
        }
        return CHILLPAY.equalsIgnoreCase(pgCd.trim());
    }

    /**
     * 노티·매핑 UI 등에서 벤더 코드 문자열이 ChillPay 계열인지(접두 CHILLPAY).
     */
    public static boolean isChillPayVendorCode(String vendorCode) {
        if (vendorCode == null || vendorCode.isBlank()) {
            return false;
        }
        return vendorCode.trim().toUpperCase(Locale.ROOT).startsWith(CHILLPAY);
    }
}
