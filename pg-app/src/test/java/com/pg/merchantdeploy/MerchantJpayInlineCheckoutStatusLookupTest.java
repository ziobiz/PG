package com.pg.merchantdeploy;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.JpayPaymentService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.OrgServiceUseService;
import com.pg.service.UrlPayCheckoutCurrencyService;
import com.pg.splitpay.SplitPayCheckoutModeGuard;
import com.pg.util.PgTrnsctnOrderLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantJpayInlineCheckoutStatusLookupTest {

    @Mock
    private OrgUnitRepository orgUnitRepository;
    @Mock
    private PgTrnsctnRepository pgTrnsctnRepository;
    @Mock
    private MerchantProfileRepository merchantProfileRepository;
    @Mock
    private OrgServiceUseService orgServiceUseService;
    @Mock
    private JpayPaymentService jpayPaymentService;
    @Mock
    private HqApiConfigRepository hqApiConfigRepository;
    @Mock
    private MerchantChatbotProductService productService;
    @Mock
    private MerchantInlineCheckoutTokenService tokenService;
    @Mock
    private UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;
    @Mock
    private MerchantApiIntegrationChannelService integrationChannelService;
    @Mock
    private MerchantOperationalPgGuard operationalPgGuard;
    @Mock
    private SplitPayCheckoutModeGuard splitPayCheckoutModeGuard;

    @InjectMocks
    private MerchantJpayInlineCheckoutService service;

    @Test
    void orderStatus_findsNotiOriginJpayPaidRow() {
        var ou = new OrgUnit();
        ou.setId(14L);
        ou.setCode("6000000014");
        when(orgUnitRepository.findById(14L)).thenReturn(Optional.of(ou));

        PgTrnsctn notiPaid = new PgTrnsctn();
        notiPaid.setTrnId("717B1073AC834A99B8D6");
        notiPaid.setMerchantId("6000000014");
        notiPaid.setOrderNo("ICthi86v-lg7-8a2d");
        notiPaid.setOrigin("NOTI");
        notiPaid.setVan(PgVendor.JPAY);
        notiPaid.setStatus("10");
        notiPaid.setChillTransactionId("240771831156");
        notiPaid.setAmtKrw(new BigDecimal("2900"));
        notiPaid.setCurType("JPY");

        when(pgTrnsctnRepository.findByMerchantIdAndOrderNoOrderByCreatedAtAsc(
                eq("6000000014"), eq("ICthi86v-lg7-8a2d")))
                .thenReturn(List.of(notiPaid));

        Map<String, Object> result = service.orderStatus(14L, "ICthi86v-lg7-8a2d");
        assertEquals(Boolean.TRUE, result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals(Boolean.TRUE, data.get("found"));
        assertEquals("PAID", data.get("paymentStatus"));
        assertEquals("717B1073AC834A99B8D6", data.get("transactionId"));
        assertEquals("NOTI", data.get("origin"));
    }

    @Test
    void pickPreferred_findsSameRowAsStatusApi() {
        PgTrnsctn notiPaid = new PgTrnsctn();
        notiPaid.setMerchantId("6000000014");
        notiPaid.setOrderNo("ICthi86v-lg7-8a2d");
        notiPaid.setOrigin("NOTI");
        notiPaid.setVan(PgVendor.JPAY);
        notiPaid.setStatus("10");
        notiPaid.setChillTransactionId("240771831156");

        when(pgTrnsctnRepository.findByMerchantIdAndOrderNoOrderByCreatedAtAsc(
                eq("6000000014"), eq("ICthi86v-lg7-8a2d")))
                .thenReturn(List.of(notiPaid));

        Optional<PgTrnsctn> hit = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, "6000000014", "ICthi86v-lg7-8a2d");
        assertTrue(hit.isPresent());
        assertEquals("10", hit.get().getStatus());
    }
}
