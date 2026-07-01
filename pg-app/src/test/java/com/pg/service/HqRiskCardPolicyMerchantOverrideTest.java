package com.pg.service;

import com.pg.entity.HqRiskCardPolicy;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqPayCardBlacklistRepository;
import com.pg.repository.HqRiskCardPolicyRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.CardRiskTrackPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HqRiskCardPolicyMerchantOverrideTest {

    @Mock
    private HqRiskCardPolicyRepository riskCardPolicyRepository;
    @Mock
    private MerchantProfileRepository merchantProfileRepository;
    @Mock
    private OrgUnitRepository orgUnitRepository;
    @Mock
    private HqPayCardBlacklistRepository blacklistRepository;

    private HqRiskCardPolicyService service;

    @BeforeEach
    void setUp() {
        service = new HqRiskCardPolicyService(
                riskCardPolicyRepository, merchantProfileRepository, orgUnitRepository, blacklistRepository);
    }

    @Test
    void merchantDisabledOverridesHqEnabled() {
        HqRiskCardPolicy hq = new HqRiskCardPolicy();
        hq.setId(1L);
        hq.setEnabledYn("Y");
        when(riskCardPolicyRepository.findById(1L)).thenReturn(Optional.of(hq));

        MerchantProfile mp = new MerchantProfile();
        mp.setCardRiskPolicyMode(HqRiskCardPolicyService.MODE_DISABLED);
        when(merchantProfileRepository.findByOrgUnitId(77L)).thenReturn(Optional.of(mp));

        CardRiskPolicyEffective eff = service.resolveForOrgUnit(77L);

        assertFalse(eff.enabled());
    }

    @Test
    void merchantCustomOverridesHqDisabled() {
        HqRiskCardPolicy hq = new HqRiskCardPolicy();
        hq.setId(1L);
        hq.setEnabledYn("N");
        when(riskCardPolicyRepository.findById(1L)).thenReturn(Optional.of(hq));

        MerchantProfile mp = new MerchantProfile();
        mp.setCardRiskPolicyMode(HqRiskCardPolicyService.MODE_CUSTOM);
        mp.setCardRiskTier1Min(5);
        mp.setCardRiskAutoBlacklistTier(2);
        mp.setCardRiskTrackPeriodPolicy(HqRiskCardPolicyService.TRACK_POLICY_NONE);
        when(merchantProfileRepository.findByOrgUnitId(88L)).thenReturn(Optional.of(mp));

        CardRiskPolicyEffective eff = service.resolveForOrgUnit(88L);

        org.junit.jupiter.api.Assertions.assertTrue(eff.enabled());
        org.junit.jupiter.api.Assertions.assertEquals(HqRiskCardPolicyService.MODE_CUSTOM, eff.policySource());
        org.junit.jupiter.api.Assertions.assertEquals(CardRiskTrackPeriod.MODE_NONE, eff.trackPeriodMode());
    }
}
