package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayDisputeNotifyStatusResolverTest {

    @Test
    void refundAlert_mapsTo30() {
        Map<String, String> form = disputeForm("Refund", "00", "Refund completed");
        assertEquals("30", JpayDisputeNotifyStatusResolver.resolveInternalStatus(form));
    }

    @Test
    void chargebackAlert_mapsTo31() {
        assertEquals("31", JpayDisputeNotifyStatusResolver.fromAlertType("Chargebacks", ""));
        assertEquals("31", JpayDisputeNotifyStatusResolver.fromAlertType("Ethoca", ""));
        assertEquals("31", JpayDisputeNotifyStatusResolver.fromAlertType("RDR", ""));
    }

    @Test
    void failedAlertStatus_notApplied() {
        Map<String, String> form = disputeForm("Refund", "11", "Refund failed");
        assertNull(JpayDisputeNotifyStatusResolver.resolveInternalStatus(form));
    }

    @Test
    void voidAlertType_mapsTo21() {
        assertEquals("21", JpayDisputeNotifyStatusResolver.fromAlertType("Void", ""));
    }

    @Test
    void looksLikeDispute_requiresAlertFields() {
        Map<String, String> form = disputeForm("Refund", "00", "ok");
        assertTrue(JpayDisputeNotifyStatusResolver.looksLikeDisputeWebhook(form));
        form.remove("alert_type");
        assertFalse(JpayDisputeNotifyStatusResolver.looksLikeDisputeWebhook(form));
    }

    @Test
    void disputeSign_matchesPhpExample() {
        String apiKey = "test-dispute-key";
        Map<String, String> form = new LinkedHashMap<>();
        form.put("memberid", "10153");
        form.put("orderid", "O2026060182255");
        form.put("transaction_id", "966782078392");
        form.put("amount", "1.00");
        form.put("refund_orderid", "R20260601001");
        form.put("refund_amount", "1.00");
        form.put("alert_datetime", "2026-06-01 09:06:57");
        form.put("alert_content", "Refund completed");
        form.put("alert_type", "Refund");
        form.put("alert_status", "00");
        String sign = JpaySignatureUtil.md5Upper(
                "alert_content=Refund completed&alert_datetime=2026-06-01 09:06:57&alert_status=00"
                        + "&alert_type=Refund&amount=1.00&memberid=10153&orderid=O2026060182255"
                        + "&refund_amount=1.00&refund_orderid=R20260601001&transaction_id=966782078392&key=" + apiKey);
        assertTrue(JpaySignatureUtil.verifyDisputeWebhookSign(form, apiKey, sign));
    }

    private static Map<String, String> disputeForm(String type, String status, String content) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("memberid", "10001");
        form.put("orderid", "ORD1");
        form.put("transaction_id", "TX1");
        form.put("amount", "10.00");
        form.put("alert_type", type);
        form.put("alert_status", status);
        form.put("alert_content", content);
        return form;
    }
}
