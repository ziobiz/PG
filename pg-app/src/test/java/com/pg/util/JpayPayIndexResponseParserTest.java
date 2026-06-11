package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayPayIndexResponseParserTest {

    @Test
    void parsesStatusErrorWithMsg() throws Exception {
        JpayPayIndexResponseParser.Outcome o = JpayPayIndexResponseParser.parse(
                "{\"status\":\"error\",\"msg\":\"Signature verification failed\"}");
        assertEquals(2, o.status());
        assertEquals("Signature verification failed", o.msg());
    }

    @Test
    void parsesNumericStatusSuccess() throws Exception {
        JpayPayIndexResponseParser.Outcome o = JpayPayIndexResponseParser.parse(
                "{\"status\":0,\"msg\":\"transaction success\"}");
        assertEquals(0, o.status());
        assertEquals("transaction success", o.msg());
    }

    @Test
    void extractsJsonFromWrappedBody() throws Exception {
        String wrapped = "prefix{\"status\":2,\"msg\":\"transaction failed\"}suffix";
        JpayPayIndexResponseParser.Outcome o = JpayPayIndexResponseParser.parse(wrapped);
        assertEquals(2, o.status());
        assertEquals("transaction failed", o.msg());
    }

    @Test
    void emptyBodyIsFailureWithHint() throws Exception {
        JpayPayIndexResponseParser.Outcome o = JpayPayIndexResponseParser.parse("  ");
        assertEquals(2, o.status());
        assertTrue(o.msg().contains("empty response"));
    }

    @Test
    void parsesJsonStringWrapped3dsResponse() throws Exception {
        String raw = "\"{\\\"status\\\":1,\\\"msg\\\":\\\"\\\",\\\"url\\\":\\\"https://api.j-pay.net/v1.1/payment/test-id\\\",\\\"transaction_id\\\":\\\"606650637929\\\"}\"";
        JpayPayIndexResponseParser.Outcome o = JpayPayIndexResponseParser.parse(raw);
        assertEquals(1, o.status());
        assertEquals("https://api.j-pay.net/v1.1/payment/test-id", o.url3ds());
    }
}
