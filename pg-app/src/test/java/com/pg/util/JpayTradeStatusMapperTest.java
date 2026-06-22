package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JpayTradeStatusMapperTest {

    @Test
    void fromTradeState_mapsSuccessAndRefund() {
        assertEquals("10", JpayTradeStatusMapper.fromTradeState("SUCCESS"));
        assertEquals("30", JpayTradeStatusMapper.fromTradeState("REFUND"));
        assertEquals("99", JpayTradeStatusMapper.fromTradeState("FAIL"));
    }

    @Test
    void fromPortalTradingStatus_mapsExcelValues() {
        assertEquals("10", JpayTradeStatusMapper.fromPortalTradingStatus("Success, Notified", "No", "No"));
        assertEquals("30", JpayTradeStatusMapper.fromPortalTradingStatus("Refunded", "No", "No"));
        assertEquals("31", JpayTradeStatusMapper.fromPortalTradingStatus("Success, Notified", "Yes", "No"));
        assertNull(JpayTradeStatusMapper.fromPortalTradingStatus("Refund in progress", "No", "No"));
    }
}
