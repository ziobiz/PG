package com.pg.service;

import com.pg.repository.HqPayCardBlacklistRepository;
import com.pg.repository.HqPayCardBlockPrefixRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.PayCardBrand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayCardPolicyMerchantBrandScopeTest {

    private static final String JCB_PAN = "3530111333300000";
    private static final String VISA_PAN = "4111111111111111";

    @Mock
    private HqPayCardBlockPrefixRepository blockPrefixRepository;
    @Mock
    private HqPayCardBlacklistRepository blacklistRepository;
    @Mock
    private PayCardFailCooldownService cooldownService;
    @Mock
    private OrgUnitRepository orgUnitRepository;
    @Mock
    private MerchantPgBindingRouterService pgBindingRouter;

    private PayCardPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PayCardPolicyService(
                blockPrefixRepository, blacklistRepository, cooldownService, orgUnitRepository, pgBindingRouter);
    }

    @Test
    void vmScopeExposesOnlyVisaMastercardOnCheckout() {
        when(pgBindingRouter.listOperationalRouteSummaries(eq(7L), eq(false))).thenReturn(List.of(
                Map.of("pgCd", "JPAY", "cardBrandScope", "VM")));

        Map<String, Object> policy = policyService.buildClientPolicy("JPAY", 7L);
        @SuppressWarnings("unchecked")
        List<String> brands = (List<String>) policy.get("allowedBrands");
        assertEquals(List.of("VISA", "MASTERCARD"), brands);
        @SuppressWarnings("unchecked")
        List<String> paused = (List<String>) policy.get("pausedBrands");
        assertEquals(List.of("JCB", "UNIONPAY", "AMEX"), paused);
        assertEquals(Boolean.TRUE, policy.get("brandSelectEnabled"));
    }

    @Test
    void vmScopeRejectsJcbWithAllowedBrandNotice() {
        stubOpenCardChecks();
        when(pgBindingRouter.listOperationalRouteSummaries(eq(7L), anyBoolean())).thenReturn(List.of(
                Map.of("pgCd", "JPAY", "cardBrandScope", "VM")));

        Map<String, Object> result = policyService.validateForSale("JPAY", JCB_PAN, "AUTO", "KO", 7L);
        assertFalse(Boolean.TRUE.equals(result.get("valid")));
        assertEquals("BRAND_NOT_ALLOWED", result.get("errorCode"));
        String msg = String.valueOf(result.get("message"));
        assertEquals("VISA, Master 결제 가능 / JCB & UNION & AMX 사용 일시 중지", msg);
    }

    @Test
    void vmScopeAcceptsVisa() {
        stubOpenCardChecks();
        when(pgBindingRouter.listOperationalRouteSummaries(eq(7L), anyBoolean())).thenReturn(List.of(
                Map.of("pgCd", "JPAY", "cardBrandScope", "VM")));

        Map<String, Object> result = policyService.validateForSale("JPAY", VISA_PAN, "AUTO", "KO", 7L);
        assertEquals(Boolean.TRUE, result.get("valid"));
        assertEquals(PayCardBrand.VISA.name(), result.get("brand"));
    }

    @Test
    void merchantAllowedBrandsIntersectPgSupport() {
        when(pgBindingRouter.listOperationalRouteSummaries(eq(7L), eq(false))).thenReturn(List.of(
                Map.of("pgCd", "ELEMENTPAY", "cardBrandScope", "VMJUA")));

        Set<PayCardBrand> allowed = policyService.allowedBrandsForMerchant("ELEMENTPAY", 7L);
        assertTrue(allowed.contains(PayCardBrand.VISA));
        assertTrue(allowed.contains(PayCardBrand.JCB));
        assertFalse(allowed.contains(PayCardBrand.AMEX));
    }

    private void stubOpenCardChecks() {
        when(blacklistRepository.findActiveHit(any(), any())).thenReturn(Optional.empty());
        when(blacklistRepository.findActiveMaskDisplayHit(any(), any())).thenReturn(Optional.empty());
        when(cooldownService.checkBlocked(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(blockPrefixRepository.findByPgVendorAndActiveYnOrderByPrefixDigitsAsc(any(), eq("Y")))
                .thenReturn(List.of());
    }
}
