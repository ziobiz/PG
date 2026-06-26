package com.pg.util;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** 결제 실패 쿨다운·자동등록 트리거 집계 — CVV·카드번호 형식 오류는 제외 */
public final class PayCardFailOutcomeRules {

    public static final String OUTCOME_FAIL = "FAIL";
    public static final String OUTCOME_CANCEL = "CANCEL";
    public static final String OUTCOME_REQUEST = "REQUEST";

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
        return outcomeCodeForRiskCount(outcomeCode, null).isPresent();
    }

    /**
     * 거래 상태·사유코드 → 리스크 집계용 outcome. 성공(10)·3DS대기(08)는 제외.
     * 취소·실패·미결제 및 그 외 비성공 확정 상태는 집계(동기 응답·노티 대기 없이 반영).
     */
    public static Optional<String> outcomeCodeForTxnRiskCount(String txnStatus, String outcomeReasonCode) {
        if (txnStatus == null || txnStatus.isBlank()) {
            return Optional.empty();
        }
        String s = txnStatus.trim();
        if (PgNotifyInternalStatusMapper.ST_PAID.equals(s)) {
            return Optional.empty();
        }
        if (PgNotifyInternalStatusMapper.ST_AUTH_PENDING.equals(s)) {
            return Optional.empty();
        }
        if (PgNotifyInternalStatusMapper.ST_FAIL.equals(s)) {
            return Optional.of(OUTCOME_FAIL);
        }
        if (PgNotifyInternalStatusMapper.ST_CANCEL.equals(s)) {
            return Optional.of(OUTCOME_CANCEL);
        }
        if (outcomeReasonCode != null
                && NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL.equalsIgnoreCase(outcomeReasonCode.trim())) {
            return Optional.of(NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL);
        }
        return outcomeCodeForRiskCount(null, s);
    }

    private static Optional<String> outcomeCodeForRiskCount(String outcomeCode, String txnStatusFallback) {
        String raw = outcomeCode != null && !outcomeCode.isBlank() ? outcomeCode.trim() : txnStatusFallback;
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String c = raw.toUpperCase(Locale.ROOT);
        if (OUTCOME_FAIL.equals(c) || "2".equals(c) || PgNotifyInternalStatusMapper.ST_FAIL.equals(c)) {
            return Optional.of(OUTCOME_FAIL);
        }
        if (OUTCOME_CANCEL.equals(c) || PgNotifyInternalStatusMapper.ST_CANCEL.equals(c)) {
            return Optional.of(OUTCOME_CANCEL);
        }
        if (NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL.equalsIgnoreCase(c) || "UNPAID".equals(c)) {
            return Optional.of(NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL);
        }
        if (OUTCOME_REQUEST.equals(c)) {
            return Optional.of(OUTCOME_REQUEST);
        }
        if (txnStatusFallback != null && !txnStatusFallback.isBlank()
                && !PgNotifyInternalStatusMapper.ST_PAID.equals(txnStatusFallback.trim())
                && !PgNotifyInternalStatusMapper.ST_AUTH_PENDING.equals(txnStatusFallback.trim())) {
            return Optional.of(OUTCOME_REQUEST);
        }
        return Optional.empty();
    }
}
