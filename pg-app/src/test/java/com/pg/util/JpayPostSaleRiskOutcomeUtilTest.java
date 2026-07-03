package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JpayPostSaleRiskOutcomeUtilTest {

    @Test
    void classifiesPy0124() {
        assertEquals(JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_PY0124,
                JpayPostSaleRiskOutcomeUtil.classify("[PY0124] 거래를 확인할 수 없습니다."));
    }

    @Test
    void classifiesHighRiskPolicy() {
        assertEquals(JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_HIGH_RISK,
                JpayPostSaleRiskOutcomeUtil.classify("This transaction was declined due to high-risk policy."));
    }

    @Test
    void classifiesContactMismatchRisk() {
        assertEquals(JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_HIGH_RISK,
                JpayPostSaleRiskOutcomeUtil.classify("높은 위험: 이메일 불일치"));
    }

    @Test
    void ignoresUnrelatedFailure() {
        assertNull(JpayPostSaleRiskOutcomeUtil.classify("인증 실패"));
    }
}
