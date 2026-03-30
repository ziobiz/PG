package com.pg.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 수수료·보류 등 <strong>%(퍼센트)</strong> 입력값: 소수 <strong>첫째 자리</strong>까지 저장·표시를 통일합니다.
 * (USD·THB 등 소액 단위 통화에서도 동일 규칙 적용)
 * <p>
 * {@link #parseAmountOneDecimal(Object)} / {@link #toPlainAmountOneDecimal(BigDecimal)} 는
 * 건당·고정액(통화 단위)에 동일 스케일을 적용합니다.
 */
public final class PercentDecimalHelper {

    private PercentDecimalHelper() {
    }

    /** 빈 값은 0, 파싱 실패 시 0 */
    public static BigDecimal parsePercentOneDecimal(Object raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        String s = raw.toString().trim().replace(',', '.');
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(s).setScale(1, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** API·폼 표시용: 항상 plain 문자열, 소수 첫째 자리(예: 0.5, 1.0) */
    public static String toPlainOneDecimal(BigDecimal v) {
        if (v == null) {
            return "0.0";
        }
        return v.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    /** 건당·고정 수수료 금액(통화 단위): 소수 첫째 자리 — % 필드와 동일 파싱·표시 규칙 */
    public static BigDecimal parseAmountOneDecimal(Object raw) {
        return parsePercentOneDecimal(raw);
    }

    /** API·폼 표시용 건당·고정액 */
    public static String toPlainAmountOneDecimal(BigDecimal v) {
        return toPlainOneDecimal(v);
    }
}
