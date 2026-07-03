package com.pg.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayCheckoutMinAmountUtilTest {

    @Test
    void rejectsUsdBelowFive() {
        assertTrue(JpayCheckoutMinAmountUtil.validate(new BigDecimal("4.99"), "USD").isPresent());
    }

    @Test
    void acceptsUsdFive() {
        assertFalse(JpayCheckoutMinAmountUtil.validate(new BigDecimal("5.00"), "USD").isPresent());
    }

    @Test
    void rejectsJpyBelowFloor() {
        assertTrue(JpayCheckoutMinAmountUtil.validate(new BigDecimal("100"), "JPY").isPresent());
    }
}
