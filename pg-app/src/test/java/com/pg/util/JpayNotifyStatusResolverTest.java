package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void mergePaidThenRefund() {
        String merged = NotifyToTxnStatusMerge.merge("10", JpayNotifyStatusResolver.ST_REFUND, "CALLBACK");
        assertEquals("30", merged);
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
