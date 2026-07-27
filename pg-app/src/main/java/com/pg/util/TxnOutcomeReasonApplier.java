package com.pg.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link PgTrnsctn} 처리사유(상태 변경 사유) 적재 — 실패·취소·무효·환불 등.
 */
public final class TxnOutcomeReasonApplier {

    public static final String SOURCE_JPAY = "JPAY";
    public static final String SOURCE_CHILLPAY = "CHILLPAY";
    public static final String SOURCE_ICOPAY = "ICOPAY";

    private static final int MAX_REASON = 2000;
    private static final int MAX_CODE = 64;
    private static final int MAX_SOURCE = 32;
    private static final int PREVIEW_MAX = 200;

    private TxnOutcomeReasonApplier() {
    }

    public static String preview(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        String t = reason.trim();
        if (t.length() <= PREVIEW_MAX) {
            return t;
        }
        return t.substring(0, PREVIEW_MAX) + "…";
    }

    public static Optional<String> applyFromJpayNotifyForm(PgTrnsctn t, String prevStatus, String mergedStatus, Map<String, String> form) {
        if (t == null || form == null) {
            return Optional.empty();
        }
        String ret = first(form, "returncode");
        String msg = first(form, "msg", "message", "refund_message", "errmsg");
        return apply(t, prevStatus, mergedStatus, msg, ret, SOURCE_JPAY);
    }

    public static Optional<String> applyFromChillPayJson(PgTrnsctn t, String prevStatus, String mergedStatus, JsonNode root) {
        if (t == null || root == null) {
            return Optional.empty();
        }
        String msg = textDeep(root,
                "Message", "message", "FailMessage", "failMessage",
                "RespMessage", "respMessage", "ErrorMessage", "errorMessage",
                "Description", "description", "Reason", "reason", "msg", "Remark", "remark");
        String code = firstNonBlank(
                textDeep(root, "PaymentStatus", "paymentStatus", "Status", "status",
                        "ResultCode", "resultCode", "RespCode", "respCode", "ResponseCode", "responseCode"),
                textDeep(root, "returncode", "returnCode"));
        return apply(t, prevStatus, mergedStatus, msg, code, SOURCE_CHILLPAY);
    }

    public static Optional<String> applyFromMappedNotify(PgTrnsctn t, String prevStatus, String mergedStatus,
                                             JsonNode notifyRoot, String vendorCode, Map<String, String> formLower) {
        if (t == null) {
            return Optional.empty();
        }
        if (PgVendor.isJpayFamily(vendorCode)) {
            Map<String, String> form = formLower != null ? formLower : Map.of();
            if (form.isEmpty() && notifyRoot != null && notifyRoot.isObject()) {
                return applyFromJpayJsonNode(t, prevStatus, mergedStatus, notifyRoot);
            }
            return applyFromJpayNotifyForm(t, prevStatus, mergedStatus, form);
        }
        return applyFromChillPayJson(t, prevStatus, mergedStatus, notifyRoot);
    }

    public static Optional<String> applyJpaySyncFail(PgTrnsctn t, String prevStatus, String mergedStatus, String msg) {
        return apply(t, prevStatus, mergedStatus, msg, "2", SOURCE_JPAY);
    }

    /**
     * 승인(10) 근거 없이 노티·매핑으로 올라왔다가 무효(21)로 내린 건 — 실패(99)가 아닌 무효·고정 사유.
     */
    public static Optional<String> applyIncompletePaidParams(PgTrnsctn t, String prevStatus, String voidStatus) {
        return applyIncompletePaidParams(t, prevStatus, voidStatus, SOURCE_ICOPAY);
    }

    public static Optional<String> applyIncompletePaidParams(PgTrnsctn t, String prevStatus, String voidStatus, String source) {
        if (t == null || !PaidApprovalEvidenceGuard.ST_VOID.equals(norm(voidStatus))) {
            return Optional.empty();
        }
        String src = source != null && !source.isBlank() ? source : SOURCE_ICOPAY;
        return apply(t, prevStatus, voidStatus,
                PaidApprovalEvidenceGuard.OUTCOME_REASON_INCOMPLETE_PARAMS,
                PaidApprovalEvidenceGuard.OUTCOME_CODE_INCOMPLETE_PARAMS,
                src);
    }

    /**
     * JPAY Trade Query·포털 Export 동기화로 확정된 터미널 상태.
     * UNPAID(노티 미수신) → 취소(20) 등 노티 없이 조회로만 반영되는 건의 처리사유.
     */
    public static Optional<String> applyJpayReconcileOutcome(PgTrnsctn t, String prevStatus, String newStatus,
                                                             String jpayStatusRaw) {
        if (t == null || newStatus == null || newStatus.isBlank()) {
            return Optional.empty();
        }
        if (!shouldRecordForStatus(newStatus)) {
            return Optional.empty();
        }
        String raw = jpayStatusRaw != null ? jpayStatusRaw.trim() : "";
        String reason;
        String code;
        if (PgNotifyInternalStatusMapper.ST_CANCEL.equals(newStatus) && isJpayUnpaidLabel(raw)) {
            reason = "결제 미완료(UNPAID, 노티 미수신, 임시 취소)";
            code = NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL;
        } else if (PgNotifyInternalStatusMapper.ST_FAIL.equals(newStatus)) {
            reason = raw.isBlank() ? "JPAY 조회 실패" : "JPAY 조회: " + raw;
            code = "FAIL";
        } else if (PgNotifyInternalStatusMapper.ST_REFUND.equals(newStatus)) {
            reason = raw.isBlank() ? "JPAY 환불 확정" : "JPAY 환불: " + raw;
            code = "REFUND";
        } else {
            return Optional.empty();
        }
        return apply(t, prevStatus, newStatus, reason, code, SOURCE_JPAY);
    }

    private static boolean isJpayUnpaidLabel(String raw) {
        if (raw.isBlank()) {
            return false;
        }
        return raw.toUpperCase(Locale.ROOT).contains("UNPAID");
    }

    public static Optional<String> applyIcopayFollowUp(PgTrnsctn t, String prevStatus, String newStatus,
                                           String actionCode, String actor, String adminReason, String apiDetail) {
        if (t == null) {
            return Optional.empty();
        }
        String reason = buildIcopayFollowUpText(actionCode, actor, adminReason, apiDetail);
        return apply(t, prevStatus, newStatus, reason, actionCode, SOURCE_ICOPAY);
    }

    /**
     * 승인(10) 유지 상태에서 운영 메모(분할 계약취소 후 기납부 인정 사유 등)를 적재.
     * {@link #shouldRecordForStatus} 를 우회한다.
     */
    public static Optional<String> annotateOperationalNote(PgTrnsctn t, String reasonText, String reasonCode, String source) {
        if (t == null || reasonText == null || reasonText.isBlank()) {
            return Optional.empty();
        }
        String stored = truncate(reasonText.trim(), MAX_REASON);
        String existing = t.getOutcomeReason() != null ? t.getOutcomeReason().trim() : "";
        if (!existing.isEmpty() && !existing.contains(stored)) {
            stored = truncate(existing + " | " + stored, MAX_REASON);
        } else if (!existing.isEmpty()) {
            stored = truncate(existing, MAX_REASON);
        }
        t.setOutcomeReason(stored);
        if (reasonCode != null && !reasonCode.isBlank()) {
            t.setOutcomeReasonCode(truncate(reasonCode.trim(), MAX_CODE));
        }
        if (source != null && !source.isBlank()) {
            t.setOutcomeReasonSource(truncate(source.trim().toUpperCase(Locale.ROOT), MAX_SOURCE));
        }
        t.setOutcomeReasonAt(LocalDateTime.now(ZoneId.of("Asia/Bangkok")));
        return Optional.of(stored);
    }

    public static Optional<String> apply(PgTrnsctn t, String prevStatus, String newStatus,
                             String reasonText, String reasonCode, String source) {
        if (t == null || newStatus == null || newStatus.isBlank()) {
            return Optional.empty();
        }
        if (!shouldRecordForStatus(newStatus)) {
            return Optional.empty();
        }
        String built = buildReasonText(reasonText, reasonCode, newStatus);
        if (built.isBlank()) {
            return Optional.empty();
        }
        if (!shouldApplyReasonUpdate(t, prevStatus, newStatus, built, source)) {
            return Optional.empty();
        }
        String stored = truncate(built, MAX_REASON);
        t.setOutcomeReason(stored);
        if (reasonCode != null && !reasonCode.isBlank()) {
            t.setOutcomeReasonCode(truncate(reasonCode.trim(), MAX_CODE));
        }
        if (source != null && !source.isBlank()) {
            t.setOutcomeReasonSource(truncate(source.trim().toUpperCase(Locale.ROOT), MAX_SOURCE));
        }
        t.setOutcomeReasonAt(LocalDateTime.now(ZoneId.of("Asia/Bangkok")));
        return Optional.of(stored);
    }

    /**
     * JPAY·ChillPay 확정 사유는 ICOPAY 사전검증 등 낮은 우선순위 사유를 덮어씁니다.
     * (동일 터미널 상태에서 notify·동기 응답·포털 Export가 늦게 도착하는 경우)
     */
    static boolean shouldApplyReasonUpdate(PgTrnsctn t, String prevStatus, String newStatus,
                                           String incomingReason, String incomingSource) {
        boolean statusChanged = !Objects.equals(norm(prevStatus), norm(newStatus));
        boolean emptyStored = t.getOutcomeReason() == null || t.getOutcomeReason().isBlank();
        if (statusChanged || emptyStored) {
            return true;
        }
        String existingSource = t.getOutcomeReasonSource() != null
                ? t.getOutcomeReasonSource().trim().toUpperCase(Locale.ROOT) : "";
        String src = incomingSource != null ? incomingSource.trim().toUpperCase(Locale.ROOT) : "";
        if ((SOURCE_JPAY.equals(src) || SOURCE_CHILLPAY.equals(src))
                && SOURCE_ICOPAY.equals(existingSource)) {
            return true;
        }
        if (SOURCE_JPAY.equals(src) && isWeakStoredOutcome(t.getOutcomeReason(), t.getOutcomeReasonCode())) {
            return incomingReason != null && !incomingReason.isBlank();
        }
        return false;
    }

    /** ICOPAY 오분류·번역 오류로 보이는 저장 사유 — JPAY 원문으로 교체 대상 */
    private static boolean isWeakStoredOutcome(String storedReason, String storedCode) {
        if (storedReason == null || storedReason.isBlank()) {
            return true;
        }
        String r = storedReason.trim().toLowerCase(Locale.ROOT);
        if (r.contains("중복") && r.contains("주문")) {
            return true;
        }
        if (r.contains("duplicate") && r.contains("order")) {
            return true;
        }
        String code = storedCode != null ? storedCode.trim().toUpperCase(Locale.ROOT) : "";
        return "CHECKOUT_VALIDATION".equals(code) || "ORDER_DUP".equals(code) || "DUPLICATE_ORDER".equals(code);
    }

    /** JPAY 포털 Export {@code Returned Messages} — 터미널 실패 사유 */
    public static Optional<String> applyJpayPortalReturnedMessage(PgTrnsctn t, String prevStatus, String mergedStatus,
                                                                  String returnedMessages) {
        if (t == null || returnedMessages == null || returnedMessages.isBlank()) {
            return Optional.empty();
        }
        String reason = JpayReturnedMessageUtil.failureReasonOrNull(returnedMessages);
        if (reason == null) {
            return Optional.empty();
        }
        return apply(t, prevStatus, mergedStatus, reason, "PORTAL_RETURN", SOURCE_JPAY);
    }

    private static Optional<String> applyFromJpayJsonNode(PgTrnsctn t, String prevStatus, String mergedStatus, JsonNode root) {
        String msg = textDeep(root, "msg", "message", "refund_message");
        String ret = textDeep(root, "returncode", "returnCode");
        return apply(t, prevStatus, mergedStatus, msg, ret, SOURCE_JPAY);
    }

    private static boolean shouldRecordForStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        if ("10".equals(status) || "08".equals(status)) {
            return false;
        }
        return NotifyToTxnStatusMerge.isTerminalOutcome(status);
    }

    private static String buildReasonText(String reasonText, String reasonCode, String status) {
        if (reasonText != null && !reasonText.isBlank()) {
            return reasonText.trim();
        }
        if (reasonCode != null && !reasonCode.isBlank()) {
            return fallbackFromCode(reasonCode.trim(), status);
        }
        return fallbackFromStatus(status);
    }

    private static String fallbackFromCode(String code, String status) {
        String c = code.trim();
        if ("2".equals(c)) {
            return "returncode=2 (fail)";
        }
        if ("08".equals(c) || "8".equals(c)) {
            return "returncode=08 (cancel)";
        }
        if ("09".equals(c) || "9".equals(c)) {
            return "returncode=09 (refund)";
        }
        return "code=" + c + statusSuffix(status);
    }

    private static String fallbackFromStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status.trim()) {
            case "99", "F0", "f0" -> "Payment failed";
            case "20" -> "Payment cancelled";
            case "21", "40" -> "Payment voided";
            case "22", "41" -> "Email void";
            case "30", "42" -> "Payment refunded";
            case "31" -> "Force refund";
            default -> "";
        };
    }

    private static String statusSuffix(String status) {
        String fb = fallbackFromStatus(status);
        return fb.isBlank() ? "" : " — " + fb;
    }

    static String buildIcopayFollowUpText(String actionCode, String actor, String adminReason, String apiDetail) {
        StringBuilder sb = new StringBuilder("[ICOPAY] ");
        sb.append(actionCode != null ? actionCode.trim() : "FOLLOW_UP");
        if (actor != null && !actor.isBlank()) {
            sb.append(" (").append(actor.trim()).append(")");
        }
        if (adminReason != null && !adminReason.isBlank()) {
            sb.append(" — ").append(adminReason.trim());
        }
        if (apiDetail != null && !apiDetail.isBlank()) {
            sb.append(" — ").append(apiDetail.trim());
        }
        return sb.toString();
    }

    private static String first(Map<String, String> m, String... keys) {
        if (m == null) {
            return "";
        }
        for (String k : keys) {
            String v = m.get(k.toLowerCase(Locale.ROOT));
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String textDeep(JsonNode root, String... keys) {
        if (root == null) {
            return "";
        }
        for (String k : keys) {
            JsonNode n = root.get(k);
            if (n == null || n.isNull()) {
                continue;
            }
            if (n.isValueNode()) {
                String v = n.asText("").trim();
                if (!v.isEmpty()) {
                    return v;
                }
            }
        }
        return "";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
