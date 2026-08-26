package com.pg.util;

import com.pg.entity.PgTrnsctn;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementPayPaymentIdUtilTest {

    @Test
    void prefersChillTransactionId() {
        PgTrnsctn t = new PgTrnsctn();
        t.setChillTransactionId("23742922");
        t.setApprovalNo("23742");
        assertEquals("23742922", ElementPayPaymentIdUtil.fromTxn(t));
    }

    @Test
    void fallsBackToApprovalNo() {
        PgTrnsctn t = new PgTrnsctn();
        t.setApprovalNo("23742922");
        assertEquals("23742922", ElementPayPaymentIdUtil.fromTxn(t));
    }

    @Test
    void callbackUsesPaymentIdAlias() {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("payment_id", "513");
        assertEquals("513", ElementPayPaymentIdUtil.fromCallbackFields(f));
        f.clear();
        f.put("id", "99");
        assertEquals("99", ElementPayPaymentIdUtil.fromCallbackFields(f));
        f.clear();
        f.put("id", "88001");
        f.put("payment_id", "513");
        assertEquals("513", ElementPayPaymentIdUtil.fromCallbackFields(f));
    }
}
