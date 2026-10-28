package com.pg.integration.pg.qxipay;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** QXIPAY 금액 — THB minor units(satang). 25000 = ฿250.00 */
public final class QxipayAmountUtil {

    private QxipayAmountUtil() {
    }

    public static long toMinorUnits(BigDecimal major) {
        if (major == null) {
            return 0L;
        }
        return major.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static BigDecimal fromMinorUnits(long minor) {
        return BigDecimal.valueOf(minor, 2);
    }

    public static BigDecimal fromMinorUnits(Number minor) {
        if (minor == null) {
            return BigDecimal.ZERO;
        }
        return fromMinorUnits(minor.longValue());
    }
}
