package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoidRefundSettlementModeUtilTest {

    @Test
    void revenueRefundDoesNotSubtractPrincipalFromNetSales() {
        assertFalse(VoidRefundSettlementModeUtil.subtractTxnAmountFromNetSales(
                "30",
                VoidRefundSettlementModeUtil.REVENUE,
                VoidRefundSettlementModeUtil.REVENUE,
                VoidRefundSettlementModeUtil.REVENUE,
                VoidRefundSettlementModeUtil.REVENUE));
    }

    @Test
    void generalRefundSubtractsPrincipalFromNetSales() {
        assertTrue(VoidRefundSettlementModeUtil.subtractTxnAmountFromNetSales(
                "42",
                VoidRefundSettlementModeUtil.GENERAL,
                VoidRefundSettlementModeUtil.GENERAL,
                VoidRefundSettlementModeUtil.GENERAL,
                VoidRefundSettlementModeUtil.GENERAL));
    }

    @Test
    void revenueRefundAddsSuccessSideFees() {
        assertTrue(VoidRefundSettlementModeUtil.addSuccessSideFeesOnRefund(VoidRefundSettlementModeUtil.REVENUE));
        assertFalse(VoidRefundSettlementModeUtil.addSuccessSideFeesOnRefund(VoidRefundSettlementModeUtil.GENERAL));
    }

    @Test
    void cancelAlwaysSubtractsFromNetSales() {
        assertTrue(VoidRefundSettlementModeUtil.subtractTxnAmountFromNetSales(
                "20",
                VoidRefundSettlementModeUtil.REVENUE,
                VoidRefundSettlementModeUtil.REVENUE,
                VoidRefundSettlementModeUtil.REVENUE,
                VoidRefundSettlementModeUtil.REVENUE));
    }
}
