package com.pg.util;

import java.util.Locale;

/**
 * JPAY API {@code trade_state}·포털 Export {@code Trading Status} → ICOPAY 내부 상태.
 */
public final class JpayTradeStatusMapper {

    private JpayTradeStatusMapper() {
    }

    public static String fromTradeState(String tradeState) {
        if (tradeState == null || tradeState.isBlank()) {
            return null;
        }
        String s = tradeState.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "SUCCESS", "SUCCEEDED", "PAID" -> PgNotifyInternalStatusMapper.ST_PAID;
            case "REFUND", "REFUNDED" -> PgNotifyInternalStatusMapper.ST_REFUND;
            case "FAIL", "FAILED" -> PgNotifyInternalStatusMapper.ST_FAIL;
            default -> null;
        };
    }

    public static String fromPortalTradingStatus(String tradingStatus, String chargebackYn, String rdrYn) {
        if (isYes(chargebackYn) || isYes(rdrYn)) {
            return "31";
        }
        if (tradingStatus == null || tradingStatus.isBlank()) {
            return null;
        }
        String t = tradingStatus.trim().toLowerCase(Locale.ROOT);
        if (t.contains("refunded") && !t.contains("progress")) {
            return PgNotifyInternalStatusMapper.ST_REFUND;
        }
        if (t.contains("refund in progress")) {
            return null;
        }
        if (t.contains("success") || t.contains("notified")) {
            return PgNotifyInternalStatusMapper.ST_PAID;
        }
        if (t.contains("fail")) {
            return PgNotifyInternalStatusMapper.ST_FAIL;
        }
        if (t.contains("unpaid")) {
            return "08";
        }
        return null;
    }

    private static boolean isYes(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return "yes".equals(v) || "y".equals(v) || "true".equals(v);
    }
}
