package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementPayRefundRejectMapperTest {

    @Test
    void maps409InsufficientOfAvailableFund() {
        assertEquals(ElementPayRefundRejectMapper.MSG_INSUFFICIENT_AVAILABLE_FUND,
                ElementPayRefundRejectMapper.operatorMessage("409", "Insufficient of available fund"));
    }

    @Test
    void mapsMessageWithoutCode() {
        assertEquals(ElementPayRefundRejectMapper.MSG_INSUFFICIENT_AVAILABLE_FUND,
                ElementPayRefundRejectMapper.operatorMessage("", "Insufficient of available fund"));
    }

    @Test
    void paymentAmountExceedIsNotWallet409() {
        assertEquals(ElementPayRefundRejectMapper.MSG_AMOUNT_EXCEEDS_PAYMENT,
                ElementPayRefundRejectMapper.operatorMessage("476",
                        "Refunded amount is more than available for the payment"));
        assertFalse(ElementPayRefundRejectMapper.isInsufficientAvailableFund("476",
                "refunded amount is more than available for the payment"));
        assertTrue(ElementPayRefundRejectMapper.isInsufficientAvailableFund("409",
                "insufficient of available fund"));
    }
}
