package com.pg.dto;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifyReceiveOutcomeTest {

    @Test
    void jsonWithCompIdHeaders() {
        NotifyReceiveOutcome out = NotifyReceiveOutcome.json(
                "{\"ok\":true}", HttpStatus.OK, Map.of(
                        "X-Icopay-Comp-Id", "6000000035",
                        "X-Icopay-Order-No", "EP6000001787746376932"));
        assertEquals("6000000035", out.responseHeaders().get("X-Icopay-Comp-Id"));
        assertEquals("EP6000001787746376932", out.responseHeaders().get("X-Icopay-Order-No"));
        assertTrue(out.responseHeaders().size() >= 2);
    }
}
