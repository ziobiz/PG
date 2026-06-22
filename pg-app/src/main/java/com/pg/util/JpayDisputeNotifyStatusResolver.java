package com.pg.util;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * JPAY Dispute webhook — {@link com.pg.service.JpayDisputeNotifyToTrnsctnService}.
 * <p>문서: <a href="https://docs.j-pay.net/docs/payment/api/refund-notify">Refund Webhook</a>,
 * <a href="https://docs.j-pay.net/docs/payment/api/chargeback">Chargeback Webhook</a></p>
 */
public final class JpayDisputeNotifyStatusResolver {

    public static final String ST_VOID = "21";
    public static final String ST_REFUND = JpayNotifyStatusResolver.ST_REFUND;
    public static final String ST_CHARGEBACK = "31";

    private static final Pattern VOID_CONTENT = Pattern.compile(
            "(?i)(^|[^a-z0-9_])(void|voided|invalid|cancelled|canceled|무효|取消|キャンセル)([^a-z0-9_]|$)");

    private JpayDisputeNotifyStatusResolver() {
    }

    public static boolean looksLikeDisputeWebhook(Map<String, String> form) {
        if (form == null || form.isEmpty()) {
            return false;
        }
        if (first(form, "memberid").isBlank() || first(form, "orderid").isBlank()) {
            return false;
        }
        return !first(form, "alert_type").isBlank() && !first(form, "alert_status").isBlank();
    }

    /**
     * @return ICOPAY 내부 상태(21·30·31) 또는 {@code null}(미반영 — 실패·알 수 없음·이미 동일)
     */
    public static String resolveInternalStatus(Map<String, String> form) {
        if (form == null || form.isEmpty()) {
            return null;
        }
        String alertStatus = first(form, "alert_status");
        if (!"00".equals(alertStatus.trim())) {
            return null;
        }
        String alertType = first(form, "alert_type");
        String alertContent = first(form, "alert_content");
        return fromAlertType(alertType, alertContent);
    }

    public static String fromAlertType(String alertType, String alertContent) {
        if (alertType == null || alertType.isBlank()) {
            return fromAlertContentOnly(alertContent);
        }
        String t = alertType.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "refund" -> ST_REFUND;
            case "chargebacks", "chargeback" -> ST_CHARGEBACK;
            case "ethoca", "rdr" -> ST_CHARGEBACK;
            case "void", "invalid", "cancel", "cancelled", "canceled" -> ST_VOID;
            default -> fromAlertContentOnly(alertContent);
        };
    }

    private static String fromAlertContentOnly(String alertContent) {
        if (alertContent == null || alertContent.isBlank()) {
            return null;
        }
        String c = alertContent.trim();
        if (VOID_CONTENT.matcher(c).find()) {
            return ST_VOID;
        }
        String lower = c.toLowerCase(Locale.ROOT);
        if (lower.contains("chargeback") || lower.contains("dispute") || lower.contains("ethoca")) {
            return ST_CHARGEBACK;
        }
        if (lower.contains("refund")) {
            return ST_REFUND;
        }
        return null;
    }

    public static String chillPaymentStatusLabel(String internalStatus, String alertType) {
        String at = alertType != null ? alertType.trim() : "";
        if (!at.isBlank()) {
            String label = at.length() > 50 ? at.substring(0, 50) : at;
            if (ST_REFUND.equals(internalStatus) && "refund".equalsIgnoreCase(at)) {
                return "09";
            }
            if (ST_VOID.equals(internalStatus)) {
                return "21";
            }
            return label;
        }
        if (ST_REFUND.equals(internalStatus)) {
            return "09";
        }
        if (ST_VOID.equals(internalStatus)) {
            return "21";
        }
        if (ST_CHARGEBACK.equals(internalStatus)) {
            return "CB";
        }
        return "DISPUTE";
    }

    private static String first(Map<String, String> m, String key) {
        String v = m.get(key.toLowerCase(Locale.ROOT));
        return v != null ? v.trim() : "";
    }
}
