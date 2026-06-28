package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayCardFailOutcomeRulesTest {

    @Test
    void countsFailCancelUnpaidAndRequest() {
        assertTrue(PayCardFailOutcomeRules.shouldCountQualifyingFailure("FAIL", "declined"));
        assertTrue(PayCardFailOutcomeRules.shouldCountQualifyingFailure("CANCEL", null));
        assertTrue(PayCardFailOutcomeRules.shouldCountQualifyingFailure("UNPAID", null));
        assertTrue(PayCardFailOutcomeRules.shouldCountQualifyingFailure("REQUEST", null));
        assertTrue(PayCardFailOutcomeRules.shouldCountQualifyingFailure(
                NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL, null));
    }

    @Test
    void txnStatusMapsToRiskOutcome() {
        assertTrue(PayCardFailOutcomeRules.outcomeCodeForTxnRiskCount("99", null)
                .filter(PayCardFailOutcomeRules.OUTCOME_FAIL::equals).isPresent());
        assertTrue(PayCardFailOutcomeRules.outcomeCodeForTxnRiskCount("20", null)
                .filter(PayCardFailOutcomeRules.OUTCOME_CANCEL::equals).isPresent());
        assertTrue(PayCardFailOutcomeRules.outcomeCodeForTxnRiskCount("10", null).isEmpty());
        assertTrue(PayCardFailOutcomeRules.outcomeCodeForTxnRiskCount("08", null).isEmpty());
    }

    @Test
    void skipsCvvAndFormatErrors() {
        assertFalse(PayCardFailOutcomeRules.shouldCountQualifyingFailure("FAIL", "Invalid CVV code"));
        assertTrue(PayCardFailOutcomeRules.shouldSkipValidateErrorCode("INACTIVE_CARD"));
        assertTrue(PayCardFailOutcomeRules.shouldSkipValidateErrorCode("CARD_COOLDOWN"));
        assertTrue(PayCardFailOutcomeRules.shouldSkipValidateErrorCode("CARD_COOLDOWN_TIER_2"));
    }

    @Test
    void shouldRecordNewRiskFailureSkipsDuplicateFailNotify() {
        assertFalse(PayCardFailOutcomeRules.shouldRecordNewRiskFailure("99", null, "99", null));
        assertTrue(PayCardFailOutcomeRules.shouldRecordNewRiskFailure("08", null, "99", null));
        assertTrue(PayCardFailOutcomeRules.shouldRecordNewRiskFailure("10", null, "99", null));
    }
}
