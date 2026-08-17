package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementPayCallbackEventUtilTest {

    @Test
    void classifiesPaymentAndRefundEvents() {
        assertEquals(ElementPayCallbackEventUtil.Kind.PAY_REJECT,
                ElementPayCallbackEventUtil.classify("payment.rejected").kind());
        assertEquals(ElementPayCallbackEventUtil.Kind.PAY_REVERSED,
                ElementPayCallbackEventUtil.classify("payment.reversed").kind());
        assertEquals(ElementPayCallbackEventUtil.Kind.PAY_REFUNDED,
                ElementPayCallbackEventUtil.classify("payment.refunded").kind());
        assertEquals(ElementPayCallbackEventUtil.Kind.PAY_REFUNDED,
                ElementPayCallbackEventUtil.classify("refund.paid").kind());
        assertEquals(ElementPayCallbackEventUtil.Kind.WRONG_PAYER,
                ElementPayCallbackEventUtil.classify("payment.wrong_payer").kind());
        assertEquals(ElementPayCallbackEventUtil.Kind.REFUND_CREATED,
                ElementPayCallbackEventUtil.classify("refund.created").kind());
        assertEquals(ElementPayCallbackEventUtil.Kind.REFUND_CANCELED,
                ElementPayCallbackEventUtil.classify("refund.canceled").kind());
    }

    @Test
    void cardAuthAndTransferAreAckOnly() {
        assertEquals(ElementPayCallbackEventUtil.Kind.ACK,
                ElementPayCallbackEventUtil.classify("card_auth.success").kind());
        assertEquals(ElementPayCallbackEventUtil.Kind.ACK,
                ElementPayCallbackEventUtil.classify("card_auth.rejected").kind());
        assertEquals(ElementPayCallbackEventUtil.Kind.ACK,
                ElementPayCallbackEventUtil.classify("transfer.paid").kind());
        assertEquals(ElementPayCallbackEventUtil.Kind.ACK,
                ElementPayCallbackEventUtil.classify("payout.paid").kind());
        assertFalse(ElementPayCallbackEventUtil.classify("card_auth.success").requireTxn());
        assertTrue(ElementPayCallbackEventUtil.classify("payment.rejected").requireTxn());
    }

    @Test
    void checkAndPayAreNotAsyncKinds() {
        assertTrue(ElementPayCallbackEventUtil.isCheckOrPay("CHECK"));
        assertEquals(ElementPayCallbackEventUtil.Kind.ACK,
                ElementPayCallbackEventUtil.classify("pay").kind());
    }
}
