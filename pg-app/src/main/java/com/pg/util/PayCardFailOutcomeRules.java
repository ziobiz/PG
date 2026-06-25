package com.pg.util;

import com.pg.util.NotifyToTxnStatusMerge;

import java.util.Locale;
import java.util.Set;

/** 결제 실패 쿨다운에 집계할지 여부 — CVV·카드번호 형식 오류는 제외 */
public final class PayCardFailOutcomeRules {

    private static final Set<String> SKIP_VALIDATE_CODES = Set.of(
            "INVALID_PAN", "CARD_LEN", "AMEX_LEN", "BRAND_NOT_ALLOWED", "BLOCKED_PREFIX",
            "BLACKLIST", "INACTIVE_CARD", "CARD_COOLDOWN", "SELECT_BRAND",
            "UNION_NOT_62", "UNION_60_81", "CARD_POLICY");

    private PayCardFailOutcomeRules() {
    }

    public static boolean shouldSkipValidateErrorCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String c = code.trim().toUpperCase(Locale.ROOT);
        if (c.startsWith("CARD_COOLDOWN_TIER_")) {
            return true;
        }
        return SKIP_VALIDATE_CODES.contains(c);
    }

    public static boolean isInputErrorMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String m = msg.toLowerCase(Locale.ROOT);
        return m.contains("cvv") || m.contains("cvc") || m.contains("security code")
                || m.contains("card number") && (m.contains("invalid") || m.contains("incorrect"))
                || m.contains("카드번호") && (m.contains("확인") || m.contains("올바르"))
                || m.contains("verification code");
    }

    public static boolean shouldCountQualifyingFailure(String outcomeCode, String outcomeMsg) {
        if (isInputErrorMessage(outcomeMsg)) {
            return false;
        }
        if (outcomeCode == null || outcomeCode.isBlank()) {
            return false;
        }
        String c = outcomeCode.trim().toUpperCase(Locale.ROOT);
        if ("FAIL".equals(c) || "2".equals(c)) {
            return true;
        }
        return NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL.equalsIgnoreCase(c)
                || "UNPAID".equals(c);
    }
}
