package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayNotifyStatusResolverTest {

    @Test
    void returncode09_mapsToRefund() {
        assertEquals(JpayNotifyStatusResolver.ST_REFUND, JpayNotifyStatusResolver.fromReturnCode("09"));
    }

    @Test
    void middlewareManualFollowupRefund_mapsToRefund() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("returncode", "09");
        form.put("_middleware_manualfollowup", "refund");
        assertEquals(JpayNotifyStatusResolver.ST_REFUND, JpayNotifyStatusResolver.resolveFromForm(form));
    }

    @Test
    void mergeFailThenPaid_callbackUpgradesToSuccess() {
        assertEquals("10",
                NotifyToTxnStatusMerge.merge("99", JpayNotifyStatusResolver.ST_PAID, "CALLBACK"));
        assertEquals("10",
                NotifyToTxnStatusMerge.merge("08", JpayNotifyStatusResolver.ST_PAID, "CALLBACK"));
    }

    @Test
    void mergePaidThenRefund() {
        String merged = NotifyToTxnStatusMerge.merge("10", JpayNotifyStatusResolver.ST_REFUND, "CALLBACK");
        assertEquals("30", merged);
    }

    @Test
    void failNotifyWithMiddlewareMsg_signVerifiesIgnoringMsg() {
        String apiKey = "test-api-key";
        Map<String, String> forwarded = new LinkedHashMap<>();
        forwarded.put("memberid", "10427");
        forwarded.put("orderid", "wc39212t131538");
        forwarded.put("transaction_id", "836605170419");
        forwarded.put("amount", "3980.00");
        forwarded.put("true_amount", "3980.00");
        forwarded.put("datetime", "20260619211633");
        forwarded.put("returncode", "2");
        forwarded.put("attach", "");
        forwarded.put("msg", "No Card record");
        forwarded.put("_middleware_incomingContentType", "application/x-www-form-urlencoded");
        forwarded.put("_middleware_rawBodyLength", "202");
        String sign = JpaySignatureUtil.md5Upper(
                "amount=3980.00&datetime=20260619211633&memberid=10427&orderid=wc39212t131538"
                        + "&returncode=2&transaction_id=836605170419&true_amount=3980.00&key=" + apiKey);
        assertTrue(JpaySignatureUtil.verifyNotifySign(forwarded, apiKey, sign));
        assertEquals(JpayNotifyStatusResolver.ST_FAIL, JpayNotifyStatusResolver.resolveFromForm(forwarded));
        assertEquals("99", NotifyToTxnStatusMerge.merge("08", JpayNotifyStatusResolver.ST_FAIL, "CALLBACK"));
    }

    @Test
    void unpaidProvisionalCancel_lateFail_exampleOrder() {
        String merged = NotifyToTxnStatusMerge.merge(
                "20", JpayNotifyStatusResolver.ST_FAIL, "CALLBACK",
                NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL);
        assertEquals("99", merged);
    }

    @Test
    void paymentStatusUnpaid_mapsToCancel() {
        assertEquals(JpayNotifyStatusResolver.ST_CANCEL,
                JpayNotifyStatusResolver.resolve("", "", "Unpaid"));
        assertEquals(JpayNotifyStatusResolver.ST_CANCEL,
                JpayNotifyStatusResolver.resolve("", "", "unpaid"));
    }

    @Test
    void paymentStatusUnpaid_winsOverReturncode00() {
        assertEquals(JpayNotifyStatusResolver.ST_CANCEL,
                JpayNotifyStatusResolver.resolve("00", "", "Unpaid"));
        assertEquals(JpayNotifyStatusResolver.ST_CANCEL,
                JpayNotifyStatusResolver.resolve("00", "", "unpaid"));
    }

    @Test
    void returncode2_mapsToFail_notChillPayCancel() {
        assertEquals(JpayNotifyStatusResolver.ST_FAIL, JpayNotifyStatusResolver.fromReturnCode("2"));
        assertNotEquals("20", JpayNotifyStatusResolver.fromReturnCode("2"));
    }

    @Test
    void returncode2_storedAsJpayCode_inChillPaymentStatus() {
        assertEquals("2", JpayNotifyStatusResolver.chillPaymentStatusLabel(JpayNotifyStatusResolver.ST_FAIL, "2"));
        assertEquals("00", JpayNotifyStatusResolver.chillPaymentStatusLabel(JpayNotifyStatusResolver.ST_PAID, "00"));
    }

    @Test
    void returncodeFromMappedChillPaymentStatus_resolvesForNotifyMapping() {
        assertEquals("2", JpayNotifyStatusResolver.resolveReturnCodeForNotify("", "2", ""));
        assertEquals(JpayNotifyStatusResolver.ST_FAIL,
                JpayNotifyStatusResolver.resolve(
                        JpayNotifyStatusResolver.resolveReturnCodeForNotify("", "2", ""),
                        "", ""));
    }

    @Test
    void middlewareSignRetry_acceptsOriginalReturncode00() {
        Map<String, String> forwarded = new LinkedHashMap<>();
        forwarded.put("memberid", "10427");
        forwarded.put("orderid", "P6000001781154429266");
        forwarded.put("transaction_id", "300720525261");
        forwarded.put("amount", "800.00");
        forwarded.put("true_amount", "800.00");
        forwarded.put("datetime", "20260611130753");
        forwarded.put("returncode", "09");
        forwarded.put("_middleware_manualfollowup", "refund");
        String apiKey = "test-api-key";
        Map<String, String> jpayOriginal = new LinkedHashMap<>(forwarded);
        jpayOriginal.remove("_middleware_manualfollowup");
        jpayOriginal.put("returncode", "00");
        String sign = JpaySignatureUtil.md5Upper(
                "amount=800.00&datetime=20260611130753&memberid=10427&orderid=P6000001781154429266"
                        + "&returncode=00&transaction_id=300720525261&true_amount=800.00&key=" + apiKey);
        assertTrue(JpaySignatureUtil.verifyNotifySign(jpayOriginal, apiKey, sign));
        assertTrue(JpaySignatureUtil.verifyNotifySignWithMiddlewareRetry(forwarded, apiKey, sign));
    }
}
