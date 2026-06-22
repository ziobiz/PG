package com.pg.splitpay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class SplitPayAmountUtil {

    private SplitPayAmountUtil() {
    }

    /** 1회차에 나머지 금액 (예: 91÷3 → 31, 30, 30) */
    public static List<BigDecimal> divideInstallmentAmounts(BigDecimal total, int count) {
        if (total == null || count < 1) {
            throw new IllegalArgumentException("invalid split amount");
        }
        BigDecimal n = BigDecimal.valueOf(count);
        BigDecimal base = total.divide(n, 0, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(base.multiply(n));
        List<BigDecimal> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(i == 0 ? base.add(remainder) : base);
        }
        return out;
    }
}
