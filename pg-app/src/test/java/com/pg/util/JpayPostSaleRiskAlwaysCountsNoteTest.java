package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JPAY 사후 고위험 응답은 위험관리 FAIL 집계 대상이다.
 * (사후「리스크현황」옵션과 무관하게 cooldown 경로에서 집계 — 회귀 방지용 분류 계약)
 */
class JpayPostSaleRiskAlwaysCountsNoteTest {

    @Test
    void highRiskRefundStyleMessageClassifiesAsPostsaleHighRisk() {
        assertEquals(JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_HIGH_RISK,
                JpayPostSaleRiskOutcomeUtil.classify("Fail High Risk Refund"));
        assertEquals(JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_HIGH_RISK,
                JpayPostSaleRiskOutcomeUtil.classify("HIGH-RISK decline"));
    }

    @Test
    void failOutcomeStillQualifiesForRiskCount() {
        assertTrue(PayCardFailOutcomeRules.shouldCountQualifyingFailure(
                PayCardFailOutcomeRules.OUTCOME_FAIL, "Fail High Risk Refund"));
    }
}
