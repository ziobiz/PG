package com.pg.util;

import java.util.Locale;

/**
 * JPAY {@code pay_index} 사후 응답 — 고위험·PY0124 분류.
 */
public final class JpayPostSaleRiskOutcomeUtil {

    public static final String POSTSALE_JPAY_PY0124 = "POSTSALE_JPAY_PY0124";
    public static final String POSTSALE_JPAY_HIGH_RISK = "POSTSALE_JPAY_HIGH_RISK";

    private JpayPostSaleRiskOutcomeUtil() {
    }

    /** {@code null} 이면 사후 고위험/PY0124 아님 */
    public static String classify(String outcomeMsg) {
        if (outcomeMsg == null || outcomeMsg.isBlank()) {
            return null;
        }
        String m = outcomeMsg.trim();
        String upper = m.toUpperCase(Locale.ROOT);
        if (upper.contains("PY0124") || m.contains("거래를 확인할 수 없")) {
            return POSTSALE_JPAY_PY0124;
        }
        if (isJpayHighRiskDecline(m, upper)) {
            return POSTSALE_JPAY_HIGH_RISK;
        }
        return null;
    }

    private static boolean isJpayHighRiskDecline(String m, String upper) {
        if (upper.contains("HIGH-RISK") || upper.contains("HIGH RISK")) {
            return true;
        }
        if (m.contains("고위험") || m.contains("高リスク") || m.contains("高风险")) {
            return true;
        }
        if (m.contains("높은 위험") || m.contains("높은위험")) {
            return true;
        }
        if ((m.contains("불일치") || upper.contains("MISMATCH") || m.contains("不一致"))
                && (m.contains("이메일") || m.contains("전화") || m.contains("성명") || m.contains("이름")
                || upper.contains("EMAIL") || upper.contains("PHONE") || upper.contains("NAME")
                || m.contains("メール") || m.contains("電話"))) {
            return true;
        }
        return false;
    }
}
