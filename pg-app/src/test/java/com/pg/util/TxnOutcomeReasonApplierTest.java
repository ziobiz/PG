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
}
