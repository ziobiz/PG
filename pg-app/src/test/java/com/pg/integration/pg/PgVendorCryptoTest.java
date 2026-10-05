package com.pg.integration.pg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PgVendorCryptoTest {

    @Test
    void cryptoFamily() {
        assertTrue(PgVendor.isCryptoFamily("CRYPTO"));
        assertTrue(PgVendor.isCryptoFamily("CRYPTO_USDT"));
        assertFalse(PgVendor.isCryptoFamily("CHILLPAY"));
        assertEquals("CRYPTO", PgVendor.CRYPTO);
    }
}
