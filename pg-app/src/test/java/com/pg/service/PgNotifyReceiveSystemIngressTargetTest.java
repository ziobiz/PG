package com.pg.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PgNotifyReceiveSystemIngressTargetTest {

    @Test
    void systemVendorIngress_allowsElementPayAndJpayAndIlk() {
        assertTrue(PgNotifyReceiveService.isSystemVendorIngressTarget("ELEMENTPAY"));
        assertTrue(PgNotifyReceiveService.isSystemVendorIngressTarget("elementpay"));
        assertTrue(PgNotifyReceiveService.isSystemVendorIngressTarget("cbJpay"));
        assertTrue(PgNotifyReceiveService.isSystemVendorIngressTarget("rsJpay"));
        assertTrue(PgNotifyReceiveService.isSystemVendorIngressTarget("ILK"));
        assertFalse(PgNotifyReceiveService.isSystemVendorIngressTarget("cb01"));
        assertFalse(PgNotifyReceiveService.isSystemVendorIngressTarget(""));
        assertFalse(PgNotifyReceiveService.isSystemVendorIngressTarget(null));
    }
}
