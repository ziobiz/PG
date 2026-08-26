package com.pg.service;

import com.pg.entity.HqPayCardBlockBrand;
import com.pg.repository.HqPayCardBlacklistRepository;
import com.pg.repository.HqPayCardBlockBrandRepository;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayCardBlockBrandPolicyTest {

    @Mock
    private HqPayCardBlockPrefixRepository blockPrefixRepository;
    @Mock
    private HqPayCardBlockBrandRepository blockBrandRepository;
    @Mock
    private HqPayCardBlacklistRepository blacklistRepository;
    @Mock
    private PayCardFailCooldownService cooldownService;
    @Mock
    private OrgUnitRepository orgUnitRepository;

    private PayCardPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PayCardPolicyService(
                blockPrefixRepository, blockBrandRepository, blacklistRepository,
                cooldownService, orgUnitRepository, null);
    }

    @Test
    void blockedAmexIsExcludedFromAllowedAndPausedListed() {
        HqPayCardBlockBrand row = new HqPayCardBlockBrand();
        row.setId(1L);
        row.setPgVendor("JPAY");
        row.setBrandCode("AMEX");
        row.setActiveYn("Y");
        when(blockBrandRepository.findByPgVendorAndActiveYnOrderByBrandCodeAsc(eq("JPAY"), eq("Y")))
                .thenReturn(List.of(row));
        when(blockPrefixRepository.findByPgVendorAndActiveYnOrderByPrefixDigitsAsc(eq("JPAY"), eq("Y")))
                .thenReturn(List.of());

        Map<String, Object> policy = policyService.buildClientPolicy("JPAY", null);
        @SuppressWarnings("unchecked")
        List<String> allowed = (List<String>) policy.get("allowedBrands");
        @SuppressWarnings("unchecked")
        List<String> blocked = (List<String>) policy.get("blockedBrands");
        @SuppressWarnings("unchecked")
        List<String> paused = (List<String>) policy.get("pausedBrands");
        assertFalse(allowed.contains("AMEX"));
        assertEquals(List.of("AMEX"), blocked);
        assertTrue(paused.contains("AMEX"));
    }

    @Test
    void validateRejectsBlockedBrandWithWarning() {
        HqPayCardBlockBrand row = new HqPayCardBlockBrand();
        row.setPgVendor("EXIMBAY");
        row.setBrandCode("AMEX");
        row.setActiveYn("Y");
        when(blockBrandRepository.findByPgVendorAndActiveYnOrderByBrandCodeAsc(eq("EXIMBAY"), eq("Y")))
                .thenReturn(List.of(row));
        when(blockPrefixRepository.findByPgVendorAndActiveYnOrderByPrefixDigitsAsc(eq("EXIMBAY"), eq("Y")))
                .thenReturn(List.of());
        when(blacklistRepository.findActiveHit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
        when(blacklistRepository.findActiveMaskDisplayHit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
        when(cooldownService.checkBlocked(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());

        Map<String, Object> result = policyService.validateForSale(
                "EXIMBAY", "378282246310005", null, "KO", null);
        assertFalse(Boolean.TRUE.equals(result.get("valid")));
        assertEquals("BRAND_NOT_ALLOWED", result.get("errorCode"));
    }

    @Test
    void loadBlockedBrandsParsesAliases() {
        HqPayCardBlockBrand row = new HqPayCardBlockBrand();
        row.setPgVendor("CHILLPAY");
        row.setBrandCode("DINERS");
        row.setActiveYn("Y");
        when(blockBrandRepository.findByPgVendorAndActiveYnOrderByBrandCodeAsc(eq("CHILLPAY"), eq("Y")))
                .thenReturn(List.of(row));
        Set<PayCardBrand> blocked = policyService.loadBlockedBrands("CHILLPAY");
        assertEquals(Set.of(PayCardBrand.DINERS), blocked);
    }
}
