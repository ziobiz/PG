package com.pg.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayCardFailCooldownTriggerTest {

    @Test
    void triggerFiresOnNthAttemptNotAfterNthFailure() {
        int trigger = 3;
        assertFalse(PayCardFailCooldownService.shouldBlockOnAttemptTrigger(0, trigger));
        assertFalse(PayCardFailCooldownService.shouldBlockOnAttemptTrigger(1, trigger));
        assertTrue(PayCardFailCooldownService.shouldBlockOnAttemptTrigger(2, trigger));
        assertTrue(PayCardFailCooldownService.shouldBlockOnAttemptTrigger(3, trigger));
    }

    @Test
    void triggerAtSecondTierBlocksOnSecondAttempt() {
        int trigger = 2;
        assertFalse(PayCardFailCooldownService.shouldBlockOnAttemptTrigger(0, trigger));
        assertTrue(PayCardFailCooldownService.shouldBlockOnAttemptTrigger(1, trigger));
    }

    @Test
    void oldBugWouldHaveBlockedOneAttemptLater() {
        int trigger = 3;
        int failCountBeforeThirdAttempt = 2;
        assertFalse(failCountBeforeThirdAttempt >= trigger);
        assertTrue(PayCardFailCooldownService.shouldBlockOnAttemptTrigger(failCountBeforeThirdAttempt, trigger));
    }
}
