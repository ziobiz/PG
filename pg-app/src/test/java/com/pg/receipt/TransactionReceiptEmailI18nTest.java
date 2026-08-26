package com.pg.receipt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionReceiptEmailI18nTest {

    @Test
    void paidRefundVoidSubjectsCoverFiveLocales() {
        for (String lang : new String[]{"KOR", "ENG", "JPN", "CHN", "THA"}) {
            String paid = TransactionReceiptEmailI18n.subject(lang, TransactionReceiptOutcome.PAID);
            String refunded = TransactionReceiptEmailI18n.subject(lang, TransactionReceiptOutcome.REFUNDED);
            String voided = TransactionReceiptEmailI18n.subject(lang, TransactionReceiptOutcome.VOIDED);
            assertTrue(paid.startsWith("ICOPAY | "));
            assertTrue(refunded.startsWith("ICOPAY | "));
            assertTrue(voided.startsWith("ICOPAY | "));
            assertTrue(!paid.equals(refunded) && !paid.equals(voided) && !refunded.equals(voided));
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
        assertEquals(TransactionReceiptOutcome.REFUNDED, TransactionReceiptOutcome.fromTxnStatus("42"));
        assertEquals(TransactionReceiptOutcome.REFUNDED, TransactionReceiptOutcome.fromTxnStatus("31"));
        assertEquals(TransactionReceiptOutcome.VOIDED, TransactionReceiptOutcome.fromTxnStatus("40"));
        assertEquals(TransactionReceiptOutcome.VOIDED, TransactionReceiptOutcome.fromTxnStatus("21"));
        assertEquals(null, TransactionReceiptOutcome.fromTxnStatus("41"));
    }
}
