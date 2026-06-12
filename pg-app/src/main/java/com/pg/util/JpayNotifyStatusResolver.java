package com.pg.util;

import java.util.Locale;
import java.util.Map;

/**
 * JPAY 비동기 노티 {@code returncode}·노티미들웨어 {@code _middleware_manualFollowup} → ICOPAY 내부 상태.
 * <p>미들웨어는 JPAY 원문 {@code returncode=00} 환불 통지를 ICOPAY용 {@code 09} 등으로 바꿔 재전송할 수 있습니다.
 */
public final class JpayNotifyStatusResolver {

    public static final String ST_PAID = PgNotifyInternalStatusMapper.ST_PAID;
    public static final String ST_CANCEL = PgNotifyInternalStatusMapper.ST_CANCEL;
    public static final String ST_REFUND = PgNotifyInternalStatusMapper.ST_REFUND;
    public static final String ST_FAIL = PgNotifyInternalStatusMapper.ST_FAIL;

    private JpayNotifyStatusResolver() {
    }

    public static String resolveFromForm(Map<String, String> form) {
        if (form == null || form.isEmpty()) {
            return null;
        }
        return resolve(
                first(form, "returncode"),
                first(form, "_middleware_manualfollowup"),
                first(form, "paymentstatus"));
    }

    public static String resolve(String returnCode, String middlewareManualFollowup, String paymentStatus) {
        String fromMw = fromManualFollowup(middlewareManualFollowup);
        if (fromMw != null) {
            return fromMw;
        }
        String fromRc = fromReturnCode(returnCode);
        if (fromRc != null) {
            return fromRc;
        }
        if (isJpaySuccessPaymentStatus(paymentStatus)) {
            return ST_PAID;
        }
        return null;
    }

    public static String fromManualFollowup(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String p = raw.trim().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "refund", "refunded" -> ST_REFUND;
            case "cancel", "cancelled", "canceled" -> ST_CANCEL;
            case "void", "voided", "invalid" -> "21";
            default -> null;
        };
    }

    /**
     * JPAY·미들웨어 관례 — {@code 00} 승인, {@code 2} 실패, {@code 09} 환불(미들웨어 JPAY 포털 환불 후속).
     */
    public static String fromReturnCode(String returnCode) {
        if (returnCode == null || returnCode.isBlank()) {
            return null;
        }
        String r = returnCode.trim();
        if ("00".equals(r) || "0".equals(r)) {
            return ST_PAID;
        }
        if ("2".equals(r)) {
            return ST_FAIL;
        }
        if ("09".equals(r) || "9".equals(r)) {
            return ST_REFUND;
        }
        if ("08".equals(r) || "8".equals(r)) {
            return ST_CANCEL;
        }
        return null;
    }

    public static String chillPaymentStatusLabel(String internalStatus, String returnCode) {
        String rc = returnCode != null ? returnCode.trim() : "";
        if (ST_PAID.equals(internalStatus)) {
            return "JPAY_OK";
        }
        if (ST_REFUND.equals(internalStatus)) {
            return rc.isBlank() ? "JPAY_REFUND" : ("JPAY_REFUND " + rc);
        }
        if (ST_CANCEL.equals(internalStatus)) {
            return rc.isBlank() ? "JPAY_CANCEL" : ("JPAY_CANCEL " + rc);
        }
        if ("21".equals(internalStatus)) {
            return rc.isBlank() ? "JPAY_VOID" : ("JPAY_VOID " + rc);
        }
        if (ST_FAIL.equals(internalStatus)) {
            return rc.isBlank() ? "JPAY_FAIL" : ("JPAY_FAIL " + rc);
        }
        return rc.isBlank() ? "JPAY_NOTIFY" : ("JPAY_" + rc);
    }

    public static boolean hasMiddlewareManualFollowup(Map<String, String> form) {
        return form != null && !first(form, "_middleware_manualfollowup").isBlank();
    }

    private static boolean isJpaySuccessPaymentStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String p = raw.trim().toLowerCase(Locale.ROOT);
        return "succeeded".equals(p) || "success".equals(p) || "paid".equals(p) || "00".equals(p);
    }

    private static String first(Map<String, String> m, String key) {
        String v = m.get(key.toLowerCase(Locale.ROOT));
        return v != null ? v.trim() : "";
    }
}
