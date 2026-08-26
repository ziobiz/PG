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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayCardMultiPgBrandBlockConflictTest {

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
    @Mock
    private MerchantPgBindingRouterService pgBindingRouter;

    private PayCardPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PayCardPolicyService(
                blockPrefixRepository, blockBrandRepository, blacklistRepository,
                cooldownService, orgUnitRepository, pgBindingRouter);
        when(blockPrefixRepository.findByPgVendorAndActiveYnOrderByPrefixDigitsAsc(any(), eq("Y")))
                .thenReturn(List.of());
    }

    /**
     * A(JPAY)=VM + HQ에서 JCB·AMEX 차단, B(ELEMENTPAY)=J, C(EXIMBAY)=A
     * → 결제창 허용은 VISA·MC·JCB·AMEX (다른 PG 행으로 제공되는 브랜드 유지).
     */
    @Test
    void multiPgUnionKeepsBrandsProvidedByOtherAgenciesDespiteHqBlocksOnA() {
        HqPayCardBlockBrand jcb = brandRow("JPAY", "JCB");
        HqPayCardBlockBrand amex = brandRow("JPAY", "AMEX");
        when(blockBrandRepository.findByPgVendorAndActiveYnOrderByBrandCodeAsc(eq("JPAY"), eq("Y")))
                .thenReturn(List.of(jcb, amex));
        when(blockBrandRepository.findByPgVendorAndActiveYnOrderByBrandCodeAsc(eq("ELEMENTPAY"), eq("Y")))
                .thenReturn(List.of());
        when(blockBrandRepository.findByPgVendorAndActiveYnOrderByBrandCodeAsc(eq("EXIMBAY"), eq("Y")))
                .thenReturn(List.of());

        when(pgBindingRouter.listOperationalRouteSummaries(eq(9L), eq(false))).thenReturn(List.of(
                Map.of("pgCd", "JPAY", "cardBrandScope", "VM"),
                Map.of("pgCd", "ELEMENTPAY", "cardBrandScope", "J"),
                Map.of("pgCd", "EXIMBAY", "cardBrandScope", "A")));

        Set<PayCardBrand> allowed = policyService.allowedBrandsForMerchant("JPAY", 9L);
        assertTrue(allowed.contains(PayCardBrand.VISA));
        assertTrue(allowed.contains(PayCardBrand.MASTERCARD));
        assertTrue(allowed.contains(PayCardBrand.JCB));
        assertTrue(allowed.contains(PayCardBrand.AMEX));
        assertFalse(allowed.contains(PayCardBrand.UNIONPAY));

        Map<String, Object> policy = policyService.buildClientPolicy("JPAY", 9L);
        assertEquals(Boolean.TRUE, policy.get("multiPgBrandRouting"));
        assertEquals("PER_PG", policy.get("hqBrandBlockMode"));
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) policy.get("allowedBrands");
        assertTrue(names.contains("JCB"));
        assertTrue(names.contains("AMEX"));
        @SuppressWarnings("unchecked")
        List<String> blockedOnA = (List<String>) policy.get("blockedBrands");
        assertTrue(blockedOnA.contains("JCB"));
        assertTrue(blockedOnA.contains("AMEX"));
    }

    @Test
    void singlePgStillAppliesHqBlockToMerchantAllowed() {
        when(blockBrandRepository.findByPgVendorAndActiveYnOrderByBrandCodeAsc(eq("CHILLPAY"), eq("Y")))
                .thenReturn(List.of(brandRow("CHILLPAY", "AMEX")));
        when(pgBindingRouter.listOperationalRouteSummaries(eq(3L), eq(false))).thenReturn(List.of(
                Map.of("pgCd", "CHILLPAY", "cardBrandScope", "ALL")));

        Set<PayCardBrand> allowed = policyService.allowedBrandsForMerchant("CHILLPAY", 3L);
        assertFalse(allowed.contains(PayCardBrand.AMEX));
        assertTrue(allowed.contains(PayCardBrand.VISA));
        assertEquals(Boolean.FALSE, policyService.buildClientPolicy("CHILLPAY", 3L).get("multiPgBrandRouting"));
    }

    private static HqPayCardBlockBrand brandRow(String pg, String code) {
        HqPayCardBlockBrand row = new HqPayCardBlockBrand();
        row.setPgVendor(pg);
        row.setBrandCode(code);
        row.setActiveYn("Y");
        return row;
    }
}
