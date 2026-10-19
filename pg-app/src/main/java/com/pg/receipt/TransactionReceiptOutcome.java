package com.pg.receipt;

import java.util.Locale;

/** 고객 거래명세서 메일 유형 — 승인·실패·환불·무효 */
public enum TransactionReceiptOutcome {
    PAID,
    FAILED,
    CANCELLED,
    REFUNDED,
    VOIDED;

    public static TransactionReceiptOutcome fromTxnStatus(String statusRaw) {
        if (statusRaw == null || statusRaw.isBlank()) {
            return null;
        }
        return switch (statusRaw.trim()) {
            case "10", "00" -> PAID;
            case "99", "F0" -> FAILED;
            case "20" -> CANCELLED;
            case "42", "30" -> REFUNDED;
            case "31" -> REFUNDED;
            case "40", "21" -> VOIDED;
            default -> null;
        };
    }

    public static TransactionReceiptOutcome parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return PAID;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PAID;
        }
    }

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isFollowUp() {
        return this == REFUNDED || this == VOIDED || this == FAILED || this == CANCELLED;
    }
}
