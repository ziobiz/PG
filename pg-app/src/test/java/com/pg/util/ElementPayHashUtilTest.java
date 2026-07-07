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
    void signCallbackRequest_verifyRoundTrip() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("id", "10932");
        params.put("service_id", "86");
        params.put("amount", "100.00");
        params.put("order", "uouiuooi");
        params.put("timestamp", "1697613541");
        params.put("data", "customdata");
        String secret = "80eb8c9793949bc6682baffdb4dd5303542581ed";
        String hash = ElementPayHashUtil.signCallbackRequest(secret, "check", params);
        assertTrue(ElementPayHashUtil.verifyCallbackRequest(secret, "check", params, hash));
        assertFalse(ElementPayHashUtil.verifyCallbackRequest(secret, "check", params, hash + "x"));
    }

    @Test
    void signApiRequest_secretChangeChangesHash() {
        Map<String, String> params = Map.of("key", "k", "timestamp", "1");
        assertNotEquals(
                ElementPayHashUtil.signApiRequest("secret-a", "getMethods", params),
                ElementPayHashUtil.signApiRequest("secret-b", "getMethods", params));
    }
}
