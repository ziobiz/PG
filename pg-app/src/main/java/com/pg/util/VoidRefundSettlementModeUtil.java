package com.pg.util;

import java.util.Locale;

/**
 * 정산 시 무효(21·40)·수동무효(22·41)·환불(30·42)·강제환불(31)별 순매출 반영 방식.
 * <ul>
 *   <li>GENERAL — 해당 금액을 순매출에서 차감(환불 30·42 포함).</li>
 *   <li>REVENUE — 해당 금액은 순매출에서 차감하지 않음(수수료 항목은 별도 집계 유지).</li>
 *   <li>HYBRID — 무효·수동무효·강제환불(31)만 순매출 차감, 환불(30·42) 금액은 순매출 미반영.</li>
 * </ul>
 */
public final class VoidRefundSettlementModeUtil {

    public static final String GENERAL = "GENERAL";
    public static final String REVENUE = "REVENUE";
    public static final String HYBRID = "HYBRID";

    private VoidRefundSettlementModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERAL;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (REVENUE.equals(u) || HYBRID.equals(u)) {
            return u;
        }
        return GENERAL;
    }

    /**
     * @param policyRaw 가맹 정책 컬럼 값(null·빈칸·FOLLOW 이면 본사 기본)
     * @param hqRaw     {@code tb_hq_ledger_sys_settings} 값
     */
    public static String effective(String policyRaw, String hqRaw) {
        String p = policyRaw != null ? policyRaw.trim() : "";
        if (p.isEmpty() || "FOLLOW".equalsIgnoreCase(p)) {
            return normalize(hqRaw);
        }
        return normalize(p);
    }

    /** 무효 21·40: GENERAL·HYBRID만 순매출 차감 */
    public static boolean subtractVoidAmountFromNet(String mode) {
        String m = normalize(mode);
        return GENERAL.equals(m) || HYBRID.equals(m);
    }

    /** 수동무효 22·41 */
    public static boolean subtractManualVoidAmountFromNet(String mode) {
        return subtractVoidAmountFromNet(mode);
    }

    /** 환불 30·42: GENERAL만 순매출 차감 */
    public static boolean subtractRefundAmountFromNet(String mode) {
        return GENERAL.equals(normalize(mode));
    }

    /** 강제환불 31 */
    public static boolean subtractForceRefundAmountFromNet(String mode) {
        return subtractVoidAmountFromNet(mode);
    }
}
