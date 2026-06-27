package com.pg.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgTrnsctn;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaidApprovalEvidenceGuardTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void icopayOutboundEchoWithoutPgTxnIsDetected() throws Exception {
        var root = JSON.readTree("""
                {"event":"pg.payment.status","status":"10","pgTxnId":null,"orderNo":"ICx"}
                """);
        assertTrue(PaidApprovalEvidenceGuard.isIcopayOutboundEchoClaimingPaid(root));
    }

    @Test
    void paidWithoutEvidenceDowngradesToVoid() throws Exception {
        var root = JSON.readTree("""
                {"event":"pg.payment.status","status":"10","chillPaymentStatus":"10","pgTxnId":null}
                """);
        PgTrnsctn t = new PgTrnsctn();
        String adjusted = PaidApprovalEvidenceGuard.adjustIfPaidWithoutEvidence(
                "10", t, root, Map.of("status", "10", "chillPaymentStatus", "10"),
                "CHILLPAY", null);
        assertEquals("21", adjusted);
        assertTrue(PaidApprovalEvidenceGuard.wasDowngradedFromPaid("10", adjusted));
    }

    @Test
    void paidWithTransactionIdKeepsSuccess() throws Exception {
        var root = JSON.readTree("""
                {"transaction_id":"800324691289","returncode":"00"}
                """);
        PgTrnsctn t = new PgTrnsctn();
        String adjusted = PaidApprovalEvidenceGuard.adjustIfPaidWithoutEvidence(
                "10", t, root, Map.of("chillTransactionId", "800324691289"),
                "JPAY", "00");
        assertEquals("10", adjusted);
    }

    @Test
    void jpayReturnCode00WithoutTxnIdDoesNotCountAsEvidence() throws Exception {
        PgTrnsctn t = new PgTrnsctn();
        String adjusted = PaidApprovalEvidenceGuard.adjustIfPaidWithoutEvidence(
                "10", t, JSON.readTree("{}"), Map.of(), "JPAY", "00");
        assertEquals("21", adjusted);
    }

    @Test
    void pickPreferredOrderRowPrefersUrlOverNoti() {
        PgTrnsctn url = new PgTrnsctn();
        url.setOrigin("URL");
        PgTrnsctn noti = new PgTrnsctn();
        noti.setOrigin("NOTI");
        Optional<PgTrnsctn> picked = PaidApprovalEvidenceGuard.pickPreferredOrderRow(
                Optional.of(noti), Optional.of(url));
        assertTrue(picked.isPresent());
        assertEquals("URL", picked.get().getOrigin());
    }

    @Test
    void pickPreferredOrderRowPrefersRowWithApproval() {
        PgTrnsctn url = new PgTrnsctn();
        url.setOrigin("URL");
        PgTrnsctn noti = new PgTrnsctn();
        noti.setOrigin("NOTI");
        noti.setChillTransactionId("803324591269");
        Optional<PgTrnsctn> picked = PaidApprovalEvidenceGuard.pickPreferredOrderRow(
                Optional.of(url), Optional.of(noti));
        assertTrue(picked.isPresent());
        assertEquals("803324591269", picked.get().getChillTransactionId());
    }

    @Test
    void existingTxnApprovalCountsAsEvidence() {
        PgTrnsctn t = new PgTrnsctn();
        t.setApprovalNo("803324591269");
        assertFalse(PaidApprovalEvidenceGuard.wasDowngradedFromPaid("10",
                PaidApprovalEvidenceGuard.adjustIfPaidWithoutEvidence("10", t, null, Map.of(), "CHILLPAY", null)));
    }

    @Test
    void applyIncompletePaidParamsRecordsFixedReason() {
        PgTrnsctn t = new PgTrnsctn();
        Optional<String> reason = TxnOutcomeReasonApplier.applyIncompletePaidParams(t, "08", "21");
        assertTrue(reason.isPresent());
        assertEquals("불안전한 파라미터 정보 오류", t.getOutcomeReason());
        assertEquals("INCOMPLETE_PARAMS", t.getOutcomeReasonCode());
    }
}
