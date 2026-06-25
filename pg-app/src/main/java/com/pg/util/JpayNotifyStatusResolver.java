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
        if (isJpayUnpaidPaymentStatus(paymentStatus)) {
            return ST_CANCEL;
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

    /**
     * 노티매핑·합성 JSON — {@code returncode} 가 {@code chillPaymentStatus} 로만 매핑된 경우 JPAY 코드를 복원.
     */
    public static String resolveReturnCodeForNotify(String directReturnCode,
                                                    String mappedPaymentStatusField,
                                                    String statusField) {
        String direct = directReturnCode != null ? directReturnCode.trim() : "";
        if (fromReturnCode(direct) != null) {
            return direct;
        }
        String status = statusField != null ? statusField.trim() : "";
        if (fromReturnCode(status) != null) {
            return status;
        }
        String mapped = mappedPaymentStatusField != null ? mappedPaymentStatusField.trim() : "";
        if (fromReturnCode(mapped) != null) {
            return mapped;
        }
        return direct.isBlank() ? (status.isBlank() ? mapped : status) : direct;
    }

    /**
     * JPAY API 문서 — 비동기 노티 {@code returncode} 원문을 PG 상태 표시에 그대로 저장(00·2·09 등).
     */
    public static String chillPaymentStatusLabel(String internalStatus, String returnCode) {
        String rc = returnCode != null ? returnCode.trim() : "";
        if (!rc.isBlank() && fromReturnCode(rc) != null) {
            return rc.length() > 50 ? rc.substring(0, 50) : rc;
        }
        if (ST_PAID.equals(internalStatus)) {
            return rc.isBlank() ? "00" : rc;
        }
        if (ST_FAIL.equals(internalStatus)) {
            return rc.isBlank() ? "2" : rc;
        }
        if (ST_REFUND.equals(internalStatus)) {
            return rc.isBlank() ? "09" : rc;
        }
        if (ST_CANCEL.equals(internalStatus)) {
            return rc.isBlank() ? "08" : rc;
        }
        if ("21".equals(internalStatus)) {
            return rc.isBlank() ? "21" : rc;
        }
        return rc.isBlank() ? "JPAY_NOTIFY" : rc;
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

    /** JPAY 포털·노티 — 미결제(UNPAID)는 칠페이 미완료 취소와 동일하게 취소(20)로 처리 */
    private static boolean isJpayUnpaidPaymentStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String p = raw.trim().toLowerCase(Locale.ROOT);
        return "unpaid".equals(p);
    }

    private static String first(Map<String, String> m, String key) {
        String v = m.get(key.toLowerCase(Locale.ROOT));
        return v != null ? v.trim() : "";
    }
}
