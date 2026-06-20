package com.pg.merchantdeploy;

import com.pg.integration.pg.PgVendor;
import com.pg.service.ChillPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantOperationalPgGuardTest {

    @Mock
    private ChillPayService chillPayService;

    private MerchantOperationalPgGuard guard;

    @BeforeEach
    void setUp() {
        guard = new MerchantOperationalPgGuard(chillPayService);
    }

    @Test
    void allowsWhenOperationalPgMatchesChillPayRequest() {
        when(chillPayService.resolveUrlPayOperationalPgCd(10L)).thenReturn(PgVendor.CHILLPAY);
        assertTrue(guard.denyIfUrlPayVendorMismatch(10L, MerchantPgBrokerVendor.CHILLPAY).isEmpty());
    }

    @Test
    void allowsWhenOperationalPgMatchesJpayRequest() {
        when(chillPayService.resolveUrlPayOperationalPgCd(11L)).thenReturn(PgVendor.JPAY);
        assertTrue(guard.denyIfUrlPayVendorMismatch(11L, MerchantPgBrokerVendor.JPAY).isEmpty());
    }

    @Test
    void blocksChillPayApiWhenOperationalPgIsJpay() {
        when(chillPayService.resolveUrlPayOperationalPgCd(12L)).thenReturn(PgVendor.JPAY);
        Optional<Map<String, Object>> deny = guard.denyIfUrlPayVendorMismatch(12L, MerchantPgBrokerVendor.CHILLPAY);
        assertTrue(deny.isPresent());
        assertEquals(MerchantOperationalPgGuard.ERROR_CODE, deny.get().get("errorCode"));
        assertEquals(MerchantPgBrokerVendor.JPAY, deny.get().get("configuredVendor"));
        assertEquals(MerchantPgBrokerVendor.CHILLPAY, deny.get().get("requestedVendor"));
        @SuppressWarnings("unchecked")
        Map<String, String> messages = (Map<String, String>) deny.get().get("messages");
        assertEquals(5, messages.size());
        assertFalse(messages.get("KO").isBlank());
        assertFalse(messages.get("EN").isBlank());
    }

    @Test
    void blocksJpayApiWhenOperationalPgIsChillPay() {
        when(chillPayService.resolveUrlPayOperationalPgCd(13L)).thenReturn(PgVendor.CHILLPAY);
        Optional<Map<String, Object>> deny = guard.denyIfUrlPayVendorMismatch(13L, MerchantPgBrokerVendor.JPAY);
        assertTrue(deny.isPresent());
        assertEquals(MerchantOperationalPgGuard.ERROR_CODE, deny.get().get("errorCode"));
    }
}
