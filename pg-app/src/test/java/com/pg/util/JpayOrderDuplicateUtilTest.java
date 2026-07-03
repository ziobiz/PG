package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayOrderDuplicateUtilTest {

    @Test
    void detectsKoreanDuplicateOrderMessage() {
        assertTrue(JpayOrderDuplicateUtil.isDuplicateOrderMessage("중복 주문! 주문을 다시 제출해 주세요."));
    }

    @Test
    void detectsEnglishDuplicateOrderMessage() {
        assertTrue(JpayOrderDuplicateUtil.isDuplicateOrderMessage("Duplicate order id exists"));
    }

    @Test
    void ignoresUnrelatedFailure() {
        assertFalse(JpayOrderDuplicateUtil.isDuplicateOrderMessage("[PY0124] 거래를 확인할 수 없습니다."));
    }

    @Test
    void orderDupPayloadMarksRequiresNewPrepare() {
        var payload = JpayOrderDuplicateUtil.orderDupFailPayload("ORD-1");
        assertTrue(Boolean.TRUE.equals(payload.get("requiresNewPrepare")));
        assertTrue(payload.get("messages") instanceof java.util.Map);
    }
}
