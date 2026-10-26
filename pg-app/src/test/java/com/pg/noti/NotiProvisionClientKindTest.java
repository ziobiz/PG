package com.pg.noti;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotiProvisionClientKindTest {

    @Test
    void ilkIsFixedIngressAndDoesNotFallThroughToJpay() {
        assertEquals("ilk", NotiProvisionClient.normalizePgKind("ilk"));
        assertEquals("ilk", NotiProvisionClient.normalizePgKind("ILKPAY"));
        assertEquals("ilk", NotiProvisionClient.normalizePgKind("ilkkorea"));
        assertTrue(NotiProvisionClient.isIlk("ilk"));
        assertTrue(NotiProvisionClient.isFixedIngressPg("ilk"));
        assertFalse(NotiProvisionClient.isElementPay("ilk"));
        assertFalse(NotiProvisionClient.isOx("ilk"));
    }
}
