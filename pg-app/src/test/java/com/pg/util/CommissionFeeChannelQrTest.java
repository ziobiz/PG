package com.pg.util;

import com.pg.entity.PgTrnsctn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommissionFeeChannelQrTest {

    @Test
    void qrIsItsOwnChannel() {
        assertEquals("QR", CommissionFeeChannel.normalize("qr"));
        assertEquals("FIAT", CommissionFeeChannel.normalize("card"));
        assertEquals("DEFAULT_QR", CommissionFeeChannel.defaultScope("QR"));
        assertEquals("M100:QR", CommissionFeeChannel.merchantQrScope("M100"));
        assertTrue(CommissionFeeChannel.isQr("QR"));
        assertTrue(CommissionFeeChannel.isMerchantQrScope("M100:QR"));
        assertFalse(CommissionFeeChannel.isCrypto("QR"));
    }

    @Test
    void qrSaleUsesQrFeeChannelAndCardSaleStaysFiat() {
        PgTrnsctn qr = new PgTrnsctn();
        qr.setPaymentChannel("QR");
        assertEquals("QR", FeeChannelTxnUtil.resolveChannel(qr));

        PgTrnsctn card = new PgTrnsctn();
        card.setPaymentChannel("CARD");
        assertEquals("FIAT", FeeChannelTxnUtil.resolveChannel(card));

        PgTrnsctn crypto = new PgTrnsctn();
        crypto.setPaymentChannel("QR");
        crypto.setVan("CRYPTO");
        assertEquals("CRYPTO", FeeChannelTxnUtil.resolveChannel(crypto));
        assertEquals("QR", FeeChannelTxnUtil.normalizeFilter("qr"));
    }
}
