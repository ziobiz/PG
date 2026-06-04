package com.pg.util;

import com.pg.entity.PgAgencyCostPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 대행수수료설정 기타 수수료(최대 4건) — 승인 건 % 슬롯. */
public final class PgAgencyCostExtraFeeUtil {

    private static final String PCT = "PCT";

    private PgAgencyCostExtraFeeUtil() {
    }

    public static boolean isPctSlot(PgAgencyCostPolicy p, int slot) {
        return PCT.equals(normMode(getMode(p, slot))) && hasName(getName(p, slot));
    }

    public static BigDecimal pctSlotAmountOnApproved(PgAgencyCostPolicy p, int slot, BigDecimal approvedTxnAmt,
                                                     int scale, RoundingMode roundingMode) {
        if (p == null || approvedTxnAmt == null || approvedTxnAmt.signum() <= 0 || slot < 1 || slot > 4) {
            return BigDecimal.ZERO;
        }
        if (!isPctSlot(p, slot)) {
            return BigDecimal.ZERO;
        }
        BigDecimal v = nz(getValue(p, slot));
        int sc = Math.max(0, scale);
        RoundingMode rm = roundingMode != null ? roundingMode : RoundingMode.HALF_UP;
        return approvedTxnAmt.multiply(v).divide(BigDecimal.valueOf(100), sc, rm);
    }

    private static boolean hasName(String n) {
        return n != null && !n.isBlank();
    }

    private static String normMode(String m) {
        return m != null ? m.trim().toUpperCase() : "";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String getName(PgAgencyCostPolicy p, int slot) {
        return switch (slot) {
            case 1 -> p.getExtraFee1Name();
            case 2 -> p.getExtraFee2Name();
            case 3 -> p.getExtraFee3Name();
            case 4 -> p.getExtraFee4Name();
            default -> null;
        };
    }

    private static String getMode(PgAgencyCostPolicy p, int slot) {
        return switch (slot) {
            case 1 -> p.getExtraFee1Mode();
            case 2 -> p.getExtraFee2Mode();
            case 3 -> p.getExtraFee3Mode();
            case 4 -> p.getExtraFee4Mode();
            default -> null;
        };
    }

    private static BigDecimal getValue(PgAgencyCostPolicy p, int slot) {
        return switch (slot) {
            case 1 -> p.getExtraFee1Value();
            case 2 -> p.getExtraFee2Value();
            case 3 -> p.getExtraFee3Value();
            case 4 -> p.getExtraFee4Value();
            default -> BigDecimal.ZERO;
        };
    }
}
