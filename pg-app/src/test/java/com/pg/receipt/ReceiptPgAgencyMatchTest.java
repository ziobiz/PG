package com.pg.receipt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 전 PG 공통: van ↔ pg_cd 패밀리 매칭 */
class ReceiptPgAgencyMatchTest {

    @Test
    void matchesElementPayVariants() {
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("ELEMENTPAY", "ELEMENTPAY DPL"));
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("ELEMENTPAY", "ELEMENTPAY_SAN"));
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("ELEMENTPAY", "ELEMENTPAY"));
        assertFalse(ReceiptPgAgencyMatch.sameVanFamily("ELEMENTPAY", "JPAY API JPY"));
    }

    @Test
    void matchesJpayChillPayEximbayIlkOx() {
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("JPAY", "JPAY API JPY"));
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("CHILLPAY", "CHILLPAY DP JP THB"));
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("EXIMBAY", "EXIMBAY"));
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("ILK", "ILK_MAIN"));
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("OX", "OX_CARD"));
    }

    @Test
    void matchesFutureVendorPrefix() {
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("NEWPG", "NEWPG SPECIAL"));
        assertTrue(ReceiptPgAgencyMatch.sameVanFamily("NEWPG", "NEWPG_USD"));
        assertFalse(ReceiptPgAgencyMatch.sameVanFamily("NEWPG", "OTHERPG"));
    }
}
