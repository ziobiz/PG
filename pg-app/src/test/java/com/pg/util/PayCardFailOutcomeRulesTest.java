package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayCardFailOutcomeRulesTest {

    @Test
    void countsFailAndUnpaid() {
        assertTrue(PayCardFailOutcomeRules.shouldCountQualifyingFailure("FAIL", "declined"));
        assertTrue(PayCardFailOutcomeRules.shouldCountQualifyingFailure("UNPAID", null));
        assertTrue(PayCardFailOutcomeRules.shouldCountQualifyingFailure(
                NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL, null));
    }

    @Test
    void skipsCvvAndFormatErrors() {
        assertFalse(PayCardFailOutcomeRules.shouldCountQualifyingFailure("FAIL", "Invalid CVV code"));
        assertTrue(PayCardFailOutcomeRules.shouldSkipValidateErrorCode("INACTIVE_CARD"));
        assertTrue(PayCardFailOutcomeRules.shouldSkipValidateErrorCode("CARD_COOLDOWN"));
        assertTrue(PayCardFailOutcomeRules.shouldSkipValidateErrorCode("CARD_COOLDOWN_TIER_2"));
    }
}
