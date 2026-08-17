package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementPayHashUtilTest {

    @Test
    void signApiRequest_isDeterministic() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", "d7197e2e-6d89-11e4-8e91-d876c67f2a53");
        params.put("timestamp", "1697613541171");
        String secret = "80eb8c9793949bc6682baffdb4dd5303542581ed";
        String h1 = ElementPayHashUtil.signApiRequest(secret, "getMethods", params);
        String h2 = ElementPayHashUtil.signApiRequest(secret, "getMethods", params);
        assertEquals(h1, h2);
        assertEquals(40, h1.length());
    }

    @Test
    void signCallbackRequest_matchesPythonHmacOfDocPayload() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", "check");
        params.put("id", "10932");
        params.put("service_id", "86");
        params.put("amount", "100.00");
        params.put("order", "uouiuooi");
        params.put("timestamp", "1697613541");
        params.put("data", "customdata");
        String secret = "80eb8c9793949bc6682baffdb4dd5303542581ed";
        /* 문서에 적힌 예시 hash 값은 오타 — 동일 payload 의 HMAC-SHA1 과 일치해야 함 */
        assertEquals(
                "6f5660bf8e03a4f601fe51ca18121dd0a126cf28",
                ElementPayHashUtil.signCallbackRequest(secret, "check", params));
        assertTrue(ElementPayHashUtil.verifyCallbackRequest(secret, "check", params,
                "6f5660bf8e03a4f601fe51ca18121dd0a126cf28"));
    }

    @Test
    void signCallbackRequestPhpStyle_encodesLikeHttpBuildQuery() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", "check");
        params.put("id", "53");
        params.put("order", "order20221221");
        params.put("amount", "20.00");
        params.put("data", "a=b&c=d");
        String secret = "80eb8c9793949bc6682baffdb4dd5303542581ed";
        String h = ElementPayHashUtil.signCallbackRequestPhpHttpBuildQuery(secret, params);
        assertEquals(40, h.length());
        assertTrue(ElementPayHashUtil.verifyCallbackRequest(secret, "check", params, h));
    }

    @Test
    void signApiRequest_usesInsertionOrderNotAlphabetical() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service_id", "card");
        params.put("amount", "20.00");
        params.put("order", "OrNo00003");
        params.put("key", "d7197e2e-6d89-11e4-8e91-d876c67f2a53");
        params.put("timestamp", "1697613541");
        String secret = "80eb8c9793949bc6682baffdb4dd5303542581ed";
        String insertion = ElementPayHashUtil.signApiRequest(secret, "initPayment", params);
        Map<String, String> alpha = new LinkedHashMap<>();
        alpha.put("amount", "20.00");
        alpha.put("key", "d7197e2e-6d89-11e4-8e91-d876c67f2a53");
        alpha.put("order", "OrNo00003");
        alpha.put("service_id", "card");
        alpha.put("timestamp", "1697613541");
        String alphabetical = ElementPayHashUtil.signApiRequest(secret, "initPayment", alpha);
        assertNotEquals(insertion, alphabetical);
        assertEquals(
                "initPayment?service_id=card&amount=20.00&order=OrNo00003"
                        + "&key=d7197e2e-6d89-11e4-8e91-d876c67f2a53&timestamp=1697613541",
                "initPayment?" + ElementPayHashUtil.buildApiQueryString(params));
    }

    @Test
    void signApiRequest_encodesUrlParamsLikePhpHttpBuildQuery() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service_id", "card");
        params.put("amount", "20.00");
        params.put("order", "OrNo00003");
        params.put("currency", "THB");
        params.put("_successUrl", "https://noti.icopay.net/noti/result/elementpay?order=OrNo00003&compId=1");
        params.put("key", "d7197e2e-6d89-11e4-8e91-d876c67f2a53");
        params.put("timestamp", "1697613541");
        String secret = "80eb8c9793949bc6682baffdb4dd5303542581ed";
        String withUrl = ElementPayHashUtil.signApiRequest(secret, "initPayment", params);
        assertEquals(40, withUrl.length());
        String q = ElementPayHashUtil.buildApiQueryString(params);
        assertTrue(q.contains("_successUrl=https%3A%2F%2Fnoti.icopay.net"));
        assertTrue(q.contains("%3Forder%3D") || q.contains("%3Forder%3DOrNo00003"));
        assertTrue(q.contains("%26compId%3D1"));
    }

    @Test
    void signApiRequest_initRefund_smoke() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("payment_id", "10932");
        params.put("order", "OrNo00003");
        params.put("reason", "icopay refund");
        params.put("key", "d7197e2e-6d89-11e4-8e91-d876c67f2a53");
        params.put("timestamp", "1697613541");
        String secret = "80eb8c9793949bc6682baffdb4dd5303542581ed";
        String hash = ElementPayHashUtil.signApiRequest(secret, "initRefund", params);
        assertEquals(40, hash.length());
        assertEquals(
                "initRefund?payment_id=10932&order=OrNo00003&reason=icopay%20refund"
                        + "&key=d7197e2e-6d89-11e4-8e91-d876c67f2a53&timestamp=1697613541",
                "initRefund?" + ElementPayHashUtil.buildApiQueryString(params));
        assertFalse(hash.isBlank());
    }

    @Test
    void verifyMerchantApiResponse_acceptsHmacOfCompactResponseJson() throws Exception {
        String secret = "80eb8c9793949bc6682baffdb4dd5303542581ed";
        String compact = "{\"id\":56810,\"order\":\"orderid105213\",\"status\":203,\"timestamp\":1697613541}";
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode response = om.readTree(compact);
        String hash = ElementPayHashUtil.hmacSha1Hex(secret, response.toString());
        com.fasterxml.jackson.databind.node.ObjectNode root = om.createObjectNode();
        root.set("response", response);
        root.put("hash", hash);
        assertTrue(ElementPayHashUtil.verifyMerchantApiResponse(secret, null, root));
        root.put("hash", "ffffffffffffffffffffffffffffffffffffffff");
        assertFalse(ElementPayHashUtil.verifyMerchantApiResponse(secret, null, root));
    }

    @Test
    void verifyMerchantApiResponse_skipsWhenHashMissing() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode errorOnly = om.readTree(
                "{\"error\":{\"code\":402,\"message\":\"Wrong merchant key\",\"timestamp\":1697613541}}");
        assertTrue(ElementPayHashUtil.verifyMerchantApiResponse("secret", null, errorOnly));
    }
}
