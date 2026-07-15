package com.pg.merchantdeploy;

import com.pg.integration.pg.PgVendor;
import com.pg.service.ChillPayService;
import com.pg.service.MerchantPgBindingRouterService;
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

    @Mock
    private MerchantPgBindingRouterService pgBindingRouter;

    private MerchantOperationalPgGuard guard;

    @BeforeEach
    void setUp() {
        guard = new MerchantOperationalPgGuard(chillPayService, pgBindingRouter);
        when(pgBindingRouter.isMultiPgRoutingEnabled()).thenReturn(false);
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
        assertFalse(deny.get().containsKey("configuredVendor"));
        assertFalse(deny.get().containsKey("requestedVendor"));
        assertFalse(deny.get().containsKey("operationalPgCd"));
        @SuppressWarnings("unchecked")
        Map<String, String> messages = (Map<String, String>) deny.get().get("messages");
        assertEquals(5, messages.size());
        assertFalse(messages.get("KO").isBlank());
        assertFalse(messages.get("EN").isBlank());
        assertFalse(messages.get("KO").toUpperCase().contains("JPAY"));
        assertFalse(messages.get("KO").toUpperCase().contains("CHILLPAY"));
    }

    @Test
    void blocksJpayApiWhenOperationalPgIsChillPay() {
        when(chillPayService.resolveUrlPayOperationalPgCd(13L)).thenReturn(PgVendor.CHILLPAY);
        Optional<Map<String, Object>> deny = guard.denyIfUrlPayVendorMismatch(13L, MerchantPgBrokerVendor.JPAY);
        assertTrue(deny.isPresent());
        assertEquals(MerchantOperationalPgGuard.ERROR_CODE, deny.get().get("errorCode"));
    }

    @Test
    void skipsGuardWhenMultiPgEnabledAndNoCardBrandYet() {
        when(pgBindingRouter.isMultiPgRoutingEnabled()).thenReturn(true);
        assertTrue(guard.denyIfUrlPayVendorMismatch(14L, MerchantPgBrokerVendor.JPAY, false, null, null).isEmpty());
    }
}
