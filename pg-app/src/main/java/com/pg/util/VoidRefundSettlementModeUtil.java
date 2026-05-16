package com.pg.util;

import java.util.Locale;

/**
 * 정산 시 무효(21·40)·수동무효(22·41)·환불(30·42)·강제환불(31)별 순매출·수수료 반영.
 * <ul>
 *   <li>GENERAL — 순매출 차감(환불 30·42 포함). 무효·환불 건에는 무효/환불 건당 수수료만(성공 건당·% 미추가).</li>
 *   <li>REVENUE — 순매출 미차감. 무효·환불 건에도 성공 시 건당·% 등을 추가(이중 과금).</li>
 *   <li>HYBRID — 순매출: 무효·수무·강제환불만 차감. 수수료: 무효·수무만 이중 과금, 환불·강제환불은 건당만.</li>
 *   <li>HYBRID2 — 순매출: 환불·자동환불만 차감. 수수료: 환불·강제환불만 이중 과금, 무효·수무는 건당만.</li>
 * </ul>
 */
public final class VoidRefundSettlementModeUtil {

    public static final String GENERAL = "GENERAL";
    public static final String REVENUE = "REVENUE";
    /** 하이브리드1: 무효·수무 이중 과금 / 환불·강제환불 건당만 */
    public static final String HYBRID = "HYBRID";
    /** 하이브리드2: 환불·강제환불 이중 과금 / 무효·수무 건당만 */
    public static final String HYBRID2 = "HYBRID2";

    private VoidRefundSettlementModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERAL;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (REVENUE.equals(u) || HYBRID.equals(u) || HYBRID2.equals(u)) {
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

    /** 환불 30·42: GENERAL·HYBRID2만 순매출 차감 */
    public static boolean subtractRefundAmountFromNet(String mode) {
        String m = normalize(mode);
        return GENERAL.equals(m) || HYBRID2.equals(m);
    }

    /** 강제환불 31 — 무효 계열과 동일(순매출) */
    public static boolean subtractForceRefundAmountFromNet(String mode) {
        return subtractVoidAmountFromNet(mode);
    }

    /** 무효 21·40: 성공 건당·% 등 추가(이중 과금) — REVENUE·HYBRID */
    public static boolean addSuccessSideFeesOnVoid(String mode) {
        String m = normalize(mode);
        return REVENUE.equals(m) || HYBRID.equals(m);
    }

    /** 수동무효 22·41 */
    public static boolean addSuccessSideFeesOnManualVoid(String mode) {
        return addSuccessSideFeesOnVoid(mode);
    }

    /** 환불 30·42: REVENUE·HYBRID2 */
    public static boolean addSuccessSideFeesOnRefund(String mode) {
        String m = normalize(mode);
        return REVENUE.equals(m) || HYBRID2.equals(m);
    }

    /** 강제환불 31 — 환불 계열과 동일(수수료) */
    public static boolean addSuccessSideFeesOnForceRefund(String mode) {
        return addSuccessSideFeesOnRefund(mode);
    }
}
