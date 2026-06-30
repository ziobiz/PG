package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TxnOutcomeReasonApplierTest {

    @Test
    void applyFromJpayFormUsesMsg() {
        com.pg.entity.PgTrnsctn t = new com.pg.entity.PgTrnsctn();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("returncode", "2");
        form.put("msg", "No Card record");
        TxnOutcomeReasonApplier.applyFromJpayNotifyForm(t, "08", "99", form);
        assertEquals("No Card record", t.getOutcomeReason());
        assertEquals("2", t.getOutcomeReasonCode());
        assertEquals("JPAY", t.getOutcomeReasonSource());
    }

    @Test
    void icopayFollowUpIncludesAdminReason() {
        com.pg.entity.PgTrnsctn t = new com.pg.entity.PgTrnsctn();
        TxnOutcomeReasonApplier.applyIcopayFollowUp(t, "10", "31", "FORCE_REFUND", "admin1", "고객 요청", "OK");
        assertTrue(t.getOutcomeReason().contains("FORCE_REFUND"));
        assertTrue(t.getOutcomeReason().contains("고객 요청"));
        assertEquals("ICOPAY", t.getOutcomeReasonSource());
    }

    @Test
    void jpayReplacesIcopayDuplicateOrderReasonWhenStatusUnchanged() {
        com.pg.entity.PgTrnsctn t = new com.pg.entity.PgTrnsctn();
        t.setStatus("99");
        t.setOutcomeReason("중복 주문! 주문을 다시 제출해 주세요.");
        t.setOutcomeReasonSource("ICOPAY");
        t.setOutcomeReasonCode("CHECKOUT_VALIDATION");
        TxnOutcomeReasonApplier.applyJpaySyncFail(t, "99", "99",
                "Fail Sorry, Your Credit card number or CVV or Expiration data is not vaild");
        assertEquals("Fail Sorry, Your Credit card number or CVV or Expiration data is not vaild", t.getOutcomeReason());
        assertEquals("JPAY", t.getOutcomeReasonSource());
    }

    @Test
    void applyJpayPortalReturnedMessageUsesFailureText() {
        com.pg.entity.PgTrnsctn t = new com.pg.entity.PgTrnsctn();
        t.setStatus("99");
        t.setOutcomeReason("중복 주문! 주문을 다시 제출해 주세요.");
        t.setOutcomeReasonSource("ICOPAY");
        TxnOutcomeReasonApplier.applyJpayPortalReturnedMessage(t, "99", "99",
                "Fail Sorry, Your Credit card number or CVV or Expiration data is not vaild");
        assertTrue(t.getOutcomeReason().contains("Credit card"));
        assertEquals("JPAY", t.getOutcomeReasonSource());
    }
}
