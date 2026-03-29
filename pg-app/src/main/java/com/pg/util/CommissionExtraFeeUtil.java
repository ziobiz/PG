package com.pg.util;

import com.pg.entity.CommissionPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 정책의 기타 수수료(최대 4건): {@code PCT}는 승인 건별 금액 기준 %, {@code FIX}는 정산 실행당 1회 고정액.
 */
public final class CommissionExtraFeeUtil {

    private static final String PCT = "PCT";
    private static final String FIX = "FIX";

    private CommissionExtraFeeUtil() {
    }

    public static boolean isPctSlot(CommissionPolicy p, int slot) {
        return PCT.equals(normMode(getMode(p, slot))) && hasName(getName(p, slot));
    }

    public static boolean isFixSlot(CommissionPolicy p, int slot) {
        return FIX.equals(normMode(getMode(p, slot))) && hasName(getName(p, slot));
    }

    /** 승인(결제) 1건에 대한 기타 % 수수료 합(원 단위 절사). */
    public static BigDecimal sumPctOnApprovedAmount(CommissionPolicy p, BigDecimal approvedTxnAmt) {
        if (p == null || approvedTxnAmt == null || approvedTxnAmt.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 1; i <= 4; i++) {
            if (!isPctSlot(p, i)) {
                continue;
            }
            BigDecimal v = nz(getValue(p, i));
            sum = sum.add(approvedTxnAmt.multiply(v).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP));
        }
        return sum;
    }

    /** 정산 배치 1회에 더하는 기타 고정 수수료 합(원 단위 절사). */
    public static BigDecimal sumFixedForSettlement(CommissionPolicy p) {
        if (p == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 1; i <= 4; i++) {
            if (!isFixSlot(p, i)) {
                continue;
            }
            sum = sum.add(nz(getValue(p, i)));
        }
        return sum.setScale(0, RoundingMode.HALF_UP);
    }

    private static boolean hasName(String n) {
        return n != null && !n.isBlank();
    }

    private static String normMode(String m) {
        if (m == null) {
            return null;
        }
        String s = m.trim().toUpperCase();
        if ("PCT".equals(s) || "%".equals(s)) {
            return PCT;
        }
        if ("FIX".equals(s) || "고정".equals(s)) {
            return FIX;
        }
        return s;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String getName(CommissionPolicy p, int slot) {
        return switch (slot) {
            case 1 -> p.getExtraFee1Name();
            case 2 -> p.getExtraFee2Name();
            case 3 -> p.getExtraFee3Name();
            case 4 -> p.getExtraFee4Name();
            default -> null;
        };
    }

    private static String getMode(CommissionPolicy p, int slot) {
        return switch (slot) {
            case 1 -> p.getExtraFee1Mode();
            case 2 -> p.getExtraFee2Mode();
            case 3 -> p.getExtraFee3Mode();
            case 4 -> p.getExtraFee4Mode();
            default -> null;
        };
    }

    private static BigDecimal getValue(CommissionPolicy p, int slot) {
        return switch (slot) {
            case 1 -> p.getExtraFee1Value();
            case 2 -> p.getExtraFee2Value();
            case 3 -> p.getExtraFee3Value();
            case 4 -> p.getExtraFee4Value();
            default -> null;
        };
    }
}
