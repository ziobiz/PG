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
        assertEquals("20", JpayTradeStatusMapper.fromTradeState("UNPAID"));
    }

    @Test
    void fromPortalTradingStatus_mapsExcelValues() {
        assertEquals("10", JpayTradeStatusMapper.fromPortalTradingStatus("Success, Notified", "No", "No"));
        assertEquals("30", JpayTradeStatusMapper.fromPortalTradingStatus("Refunded", "No", "No"));
        assertEquals("31", JpayTradeStatusMapper.fromPortalTradingStatus("Success, Notified", "Yes", "No"));
        assertEquals("20", JpayTradeStatusMapper.fromPortalTradingStatus("Unpaid", "No", "No"));
        assertNull(JpayTradeStatusMapper.fromPortalTradingStatus("Refund in progress", "No", "No"));
    }

    @Test
    void returnCodeForInternalStatus_mapsCancelAndFail() {
        assertEquals("08", JpayTradeStatusMapper.returnCodeForInternalStatus("20"));
        assertEquals("2", JpayTradeStatusMapper.returnCodeForInternalStatus("99"));
        assertEquals("00", JpayTradeStatusMapper.returnCodeForInternalStatus("10"));
    }
}
