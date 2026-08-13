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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        assertTrue(eff.enabled());
        assertEquals(HqRiskCardPolicyService.MODE_CUSTOM, eff.policySource());
        assertEquals(CardRiskTrackPeriod.MODE_NONE, eff.trackPeriodMode());
    }

    @Test
    void merchantCustomTriggerTiersOverrideHqDefaults() {
        HqRiskCardPolicy hq = new HqRiskCardPolicy();
        hq.setId(1L);
        hq.setEnabledYn("Y");
        hq.setTier1Hours(0);
        hq.setTier1Min(5);
        hq.setAutoBlacklistTriggerTier(4);
        when(riskCardPolicyRepository.findById(1L)).thenReturn(Optional.of(hq));

        MerchantProfile mp = new MerchantProfile();
        mp.setCardRiskPolicyMode(HqRiskCardPolicyService.MODE_CUSTOM);
        mp.setCardRiskTier1Hours(0);
        mp.setCardRiskTier1Min(17);
        mp.setCardRiskAutoBlacklistTier(2);
        mp.setCardRiskTrackPeriodPolicy(HqRiskCardPolicyService.TRACK_POLICY_NONE);
        when(merchantProfileRepository.findByOrgUnitId(91L)).thenReturn(Optional.of(mp));

        CardRiskPolicyEffective eff = service.resolveForOrgUnit(91L);

        assertTrue(eff.enabled());
        assertEquals(HqRiskCardPolicyService.MODE_CUSTOM, eff.policySource());
        assertEquals(17, eff.tierMinutes(1));
        assertEquals(2, eff.autoBlacklistTriggerTier());
    }

    @Test
    void merchantCustomPresaleOverridesHqFilterDefaults() {
        HqRiskCardPolicy hq = new HqRiskCardPolicy();
        hq.setId(1L);
        hq.setPresaleFilterEnabledYn("Y");
        hq.setFilterBuyerContactMismatchYn("Y");
        hq.setFilterHolderNameYn("Y");
        hq.setFilterPhoneInvalidYn("Y");
        hq.setFilterEmailInvalidYn("Y");
        hq.setFilterVelocityCardYn("Y");
        hq.setFilterVelocityEmailYn("Y");
        hq.setFilterVelocityIpYn("Y");
        hq.setVelocityCardWindowMinutes(10);
        hq.setVelocityCardMaxAttempts(3);
        when(riskCardPolicyRepository.findById(1L)).thenReturn(Optional.of(hq));

        MerchantProfile mp = new MerchantProfile();
        mp.setCardRiskPresaleMode(HqRiskCardPolicyService.MODE_CUSTOM);
        mp.setCardRiskPresaleBuyerMismatchYn("N");
        mp.setCardRiskPresaleHolderNameYn("N");
        mp.setCardRiskPresalePhoneInvalidYn("Y");
        mp.setCardRiskPresaleEmailInvalidYn("N");
        mp.setCardRiskPresaleVelocityCardYn("Y");
        mp.setCardRiskPresaleVelCardWinMin(44);
        mp.setCardRiskPresaleVelCardMax(9);
        mp.setCardRiskPresaleVelocityEmailYn("N");
        mp.setCardRiskPresaleVelocityIpYn("N");
        when(merchantProfileRepository.findByOrgUnitId(92L)).thenReturn(Optional.of(mp));

        PresaleRiskFilterEffective eff = service.resolvePresaleForOrgUnit(92L);

        assertTrue(eff.enabled());
        assertEquals(HqRiskCardPolicyService.MODE_CUSTOM, eff.policySource());
        assertEquals("N", eff.filterBuyerContactMismatchYn());
        assertEquals("N", eff.filterHolderNameYn());
        assertEquals(44, eff.velocityCardWindowMinutes());
        assertEquals(9, eff.velocityCardMaxAttempts());
        assertEquals("N", eff.filterVelocityEmailYn());
    }

    @Test
    void merchantPresaleDisabledOverridesHqPresaleOn() {
        HqRiskCardPolicy hq = new HqRiskCardPolicy();
        hq.setId(1L);
        hq.setPresaleFilterEnabledYn("Y");
        hq.setFilterEmailInvalidYn("Y");
        when(riskCardPolicyRepository.findById(1L)).thenReturn(Optional.of(hq));

        MerchantProfile mp = new MerchantProfile();
        mp.setCardRiskPresaleMode(HqRiskCardPolicyService.MODE_DISABLED);
        when(merchantProfileRepository.findByOrgUnitId(93L)).thenReturn(Optional.of(mp));

        PresaleRiskFilterEffective eff = service.resolvePresaleForOrgUnit(93L);

        assertFalse(eff.enabled());
        assertEquals(HqRiskCardPolicyService.MODE_DISABLED, eff.policySource());
    }
}
