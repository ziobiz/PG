package com.pg.receipt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionReceiptEmailI18nTest {

    @Test
    void paidRefundVoidSubjectsCoverFiveLocales() {
        for (String lang : new String[]{"KOR", "ENG", "JPN", "CHN", "THA"}) {
            String paid = TransactionReceiptEmailI18n.subject(lang, TransactionReceiptOutcome.PAID);
            String failed = TransactionReceiptEmailI18n.subject(lang, TransactionReceiptOutcome.FAILED);
            String refunded = TransactionReceiptEmailI18n.subject(lang, TransactionReceiptOutcome.REFUNDED);
            String voided = TransactionReceiptEmailI18n.subject(lang, TransactionReceiptOutcome.VOIDED);
            assertTrue(paid.startsWith("ICOPAY | "));
            assertTrue(failed.startsWith("ICOPAY | "));
            assertTrue(refunded.startsWith("ICOPAY | "));
            assertTrue(voided.startsWith("ICOPAY | "));
            assertTrue(!paid.equals(failed) && !paid.equals(refunded) && !paid.equals(voided) && !refunded.equals(voided));
            assertTrue(!TransactionReceiptEmailI18n.outcomeBadge(lang, TransactionReceiptOutcome.FAILED)
                    .equals(TransactionReceiptEmailI18n.outcomeBadge(lang, TransactionReceiptOutcome.PAID)));
        }
    }

    @Test
    void paymentProviderLabelIsNeutralWithoutMasterDistSuffix() {
        assertEquals("결제대행사", TransactionReceiptEmailI18n.fieldLabels("KOR").get("paymentProvider"));
        assertEquals("Payment Provider", TransactionReceiptEmailI18n.fieldLabels("ENG").get("paymentProvider"));
        assertTrue(!TransactionReceiptEmailI18n.fieldLabels("KOR").get("paymentProvider").contains("총판"));
    }

    @Test
    void outcomeFromStatus() {
        assertEquals(TransactionReceiptOutcome.PAID, TransactionReceiptOutcome.fromTxnStatus("10"));
        assertEquals(TransactionReceiptOutcome.FAILED, TransactionReceiptOutcome.fromTxnStatus("99"));
        assertEquals(TransactionReceiptOutcome.FAILED, TransactionReceiptOutcome.fromTxnStatus("F0"));
        assertEquals(TransactionReceiptOutcome.CANCELLED, TransactionReceiptOutcome.fromTxnStatus("20"));
        assertEquals(TransactionReceiptOutcome.REFUNDED, TransactionReceiptOutcome.fromTxnStatus("42"));
        assertEquals(TransactionReceiptOutcome.REFUNDED, TransactionReceiptOutcome.fromTxnStatus("31"));
        assertEquals(TransactionReceiptOutcome.VOIDED, TransactionReceiptOutcome.fromTxnStatus("40"));
        assertEquals(TransactionReceiptOutcome.VOIDED, TransactionReceiptOutcome.fromTxnStatus("21"));
        assertEquals(null, TransactionReceiptOutcome.fromTxnStatus("41"));
    }
}
