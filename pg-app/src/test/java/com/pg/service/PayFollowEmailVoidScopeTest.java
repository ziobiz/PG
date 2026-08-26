package com.pg.service;

import com.pg.entity.PgTrnsctn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayFollowEmailVoidScopeTest {

    @Test
    void emailVoidOnlyChillPay() {
        assertTrue(PayFollowPolicyService.isEmailVoidFollowTransaction(txn("CHILLPAY")));
        assertTrue(PayFollowPolicyService.isEmailVoidFollowTransaction(txn("CHILLPAY_TH")));
        assertFalse(PayFollowPolicyService.isEmailVoidFollowTransaction(txn("ELEMENTPAY")));
        assertFalse(PayFollowPolicyService.isEmailVoidFollowTransaction(txn("JPAY")));
        assertFalse(PayFollowPolicyService.isEmailVoidFollowTransaction(txn("EXIMBAY")));
        assertFalse(PayFollowPolicyService.isEmailVoidFollowTransaction(null));
    }

    private static PgTrnsctn txn(String van) {
        PgTrnsctn t = new PgTrnsctn();
        t.setVan(van);
        return t;
    }
}
