package com.pg.receipt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionReceiptTelFormatTest {

    @Test
    void keepsRegisteredDialEvenWhenBankCountryIsKr() {
        assertEquals("+81 080-6232-1351",
                TransactionReceiptEmailService.resolveProfileTelForReceipt("+81 080-6232-1351", "JP", "KR"));
        assertEquals("+81 080-6232-1351",
                TransactionReceiptEmailService.ensureIntlDialPrefix("+81 080-6232-1351", "KR"));
    }

    @Test
    void legacyLocalNumberUsesAddrCountryBeforeBankCountry() {
        assertEquals("+81 080-6232-1351",
                TransactionReceiptEmailService.resolveProfileTelForReceipt("080-6232-1351", "JP", "KR"));
        assertEquals("+66 02-123-4567",
                TransactionReceiptEmailService.resolveProfileTelForReceipt("02-123-4567", "TH", "KR"));
    }

    @Test
    void merchantAndProviderUseStoredValueAsIs() {
        assertEquals("+82 02-1234-5678",
                TransactionReceiptEmailService.resolveProfileTelForReceipt("+82 02-1234-5678", "JP", "JP"));
    }
}
