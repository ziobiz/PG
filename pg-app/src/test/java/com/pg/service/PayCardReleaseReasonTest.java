package com.pg.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PayCardReleaseReasonTest {

    @Test
    void storesReasonBodyOnly() {
        assertEquals("가맹점 요청으로",
                PayCardPolicyService.formatReleaseReason("가맹점 요청으로"));
    }

    @Test
    void emptyBodyReturnsNull() {
        assertNull(PayCardPolicyService.formatReleaseReason("  "));
    }

    @Test
    void autoRegisteredByIsAi() {
        assertEquals("AI", PayCardPolicyService.AUTO_REGISTERED_BY);
    }
}
