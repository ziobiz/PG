package com.pg.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayCardPolicyPaymentListSuppressionTest {

    @Test
    void suppressesInactiveCardAndBlacklist() {
        assertTrue(PayCardPolicyService.suppressesPaymentListRecording("INACTIVE_CARD"));
        assertTrue(PayCardPolicyService.suppressesPaymentListRecording("BLACKLIST"));
    }

    @Test
    void suppressesCooldownCodes() {
        assertTrue(PayCardPolicyService.suppressesPaymentListRecording("CARD_COOLDOWN"));
        assertTrue(PayCardPolicyService.suppressesPaymentListRecording("CARD_COOLDOWN_TIER_1"));
        assertTrue(PayCardPolicyService.suppressesPaymentListRecording("CARD_COOLDOWN_TIER_4"));
    }

    @Test
    void doesNotSuppressQualifyingPreSaleOrJpayFailures() {
        assertFalse(PayCardPolicyService.suppressesPaymentListRecording("LUHN_FAIL"));
        assertFalse(PayCardPolicyService.suppressesPaymentListRecording("INVALID_PAN"));
        assertFalse(PayCardPolicyService.suppressesPaymentListRecording("JPAY_ERROR"));
    }

    @Test
    void readsSkipFlagFromValidationMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("skipPaymentListRecord", true);
        assertTrue(PayCardPolicyService.suppressesPaymentListRecording(m));
    }
}
