package com.pg.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayCardFailCooldownTriggerTest {

    @Test
    void tier2RegistersAfterTwoFailuresBlocksThirdAttempt() {
        int trigger = 2;
        assertFalse(PayCardFailCooldownService.shouldRegisterAutoBlacklistAfterFailures(1, trigger));
        assertTrue(PayCardFailCooldownService.shouldRegisterAutoBlacklistAfterFailures(2, trigger));
    }

    @Test
    void tier3RegistersAfterThreeFailuresBlocksFourthAttempt() {
        int trigger = 3;
        assertFalse(PayCardFailCooldownService.shouldRegisterAutoBlacklistAfterFailures(2, trigger));
        assertTrue(PayCardFailCooldownService.shouldRegisterAutoBlacklistAfterFailures(3, trigger));
    }

    @Test
    void allTiersUseSameRule() {
        assertTrue(PayCardFailCooldownService.shouldRegisterAutoBlacklistAfterFailures(1, 1));
        assertTrue(PayCardFailCooldownService.shouldRegisterAutoBlacklistAfterFailures(4, 4));
    }
}
