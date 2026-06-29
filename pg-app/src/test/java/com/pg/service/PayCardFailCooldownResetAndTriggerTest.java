package com.pg.service;

import com.pg.entity.PayCardFailCooldown;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PayCardFailCooldownRepository;
import com.pg.repository.PayCardFailRiskEventRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.CardRiskTrackPeriod;
import com.pg.util.PayCardPanHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 트리거(2차)·추적기간(1일)·성공 RESET 시나리오.
 * 설정 예: tier1=3분, tier2=5분, autoBlacklistTriggerTier=2, trackPeriod=1일.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayCardFailCooldownResetAndTriggerTest {

    private static final String PAN = "4111111111111111";
    private static final String HASH = PayCardPanHashUtil.hashPan(PAN);
    private static final Long ORG = 100L;

    @Mock
    private HqRiskCardPolicyService hqRiskCardPolicyService;
    @Mock
    private PayCardFailCooldownRepository cooldownRepository;
    @Mock
    private PayCardFailRiskEventRepository riskEventRepository;
    @Mock
    private PayCardPolicyService payCardPolicyService;
    @Mock
    private PgTrnsctnRepository pgTrnsctnRepository;
    @Mock
    private OrgUnitRepository orgUnitRepository;

    private PayCardFailCooldownService service;
    private CardRiskPolicyEffective policy;

    @BeforeEach
    void setUp() {
        service = new PayCardFailCooldownService(
                hqRiskCardPolicyService, cooldownRepository, riskEventRepository,
                payCardPolicyService, pgTrnsctnRepository, orgUnitRepository);
        policy = new CardRiskPolicyEffective(
                true,
                new int[]{3, 5, 60, 0},
                2,
                HqRiskCardPolicyService.MODE_FOLLOW_HQ,
                CardRiskTrackPeriod.MODE_DAY,
                1);
        when(payCardPolicyService.normalizePgVendor(PgVendor.JPAY)).thenReturn(PgVendor.JPAY);
    }

    @Test
    void successAfterOneFailureResetsCountAndNextSingleFailureDoesNotTrigger() {
        PayCardFailCooldown row = baseRow();
        row.setFailCount(1);
        row.setBlockedUntil(LocalDateTime.now().plusMinutes(3));
        when(cooldownRepository.findByPgVendorAndPanHashAndOrgUnitId(PgVendor.JPAY, HASH, ORG))
                .thenReturn(Optional.of(row));
        when(cooldownRepository.save(any(PayCardFailCooldown.class))).thenAnswer(inv -> inv.getArgument(0));

        service.clearOnSuccess(PgVendor.JPAY, PAN, ORG);

        verify(riskEventRepository).deleteAllForCard(PgVendor.JPAY, HASH, ORG);
        assertEquals(0, row.getFailCount());
        assertEquals(null, row.getBlockedUntil());

        row.setFailCount(0);
        row.setBlockedUntil(null);
        when(hqRiskCardPolicyService.resolveForOrgUnit(ORG)).thenReturn(policy);
        when(riskEventRepository.countSinceForCard(eq(PgVendor.JPAY), eq(HASH), eq(ORG), any(LocalDateTime.class)))
                .thenReturn(1L);
        when(payCardPolicyService.findBlacklistHit(PAN, PgVendor.JPAY)).thenReturn(Optional.empty());
        row.setFailCount(1);
        row.setBlockedUntil(LocalDateTime.now().plusMinutes(3));

        var blocked = service.checkBlocked(PgVendor.JPAY, PAN, "KO", ORG);

        assertTrue(blocked.isPresent());
        assertEquals("CARD_COOLDOWN_TIER_1", blocked.get().get("messageKey"));
        assertFalse(PayCardFailCooldownService.shouldRegisterAutoBlacklistAfterFailures(1, 2));
        verify(payCardPolicyService, never()).addBlacklistAutoMask(any(), any(), any(), any(), any());
    }

    @Test
    void twoFailuresWithinDayTriggerRegistrationOnThirdAttempt() {
        PayCardFailCooldown row = baseRow();
        row.setFailCount(2);
        row.setBlockedUntil(LocalDateTime.now().plusMinutes(5));
        when(cooldownRepository.findByPgVendorAndPanHashAndOrgUnitId(PgVendor.JPAY, HASH, ORG))
                .thenReturn(Optional.of(row));
        when(hqRiskCardPolicyService.resolveForOrgUnit(ORG)).thenReturn(policy);
        when(riskEventRepository.countSinceForCard(eq(PgVendor.JPAY), eq(HASH), eq(ORG), any(LocalDateTime.class)))
                .thenReturn(2L);
        when(payCardPolicyService.findBlacklistHit(PAN, PgVendor.JPAY)).thenReturn(Optional.empty());
        when(payCardPolicyService.addBlacklistAutoMask(any(), any(), any(), eq(ORG), any())).thenReturn(null);

        var blocked = service.checkBlocked(PgVendor.JPAY, PAN, "KO", ORG);

        assertTrue(blocked.isPresent());
        assertEquals("INACTIVE_CARD", blocked.get().get("messageKey"));
        verify(payCardPolicyService).addBlacklistAutoMask(eq(PgVendor.JPAY), any(), any(), eq(ORG), any());
    }

    @Test
    void clearOnSuccessForTxnUsesHashWhenPanMissing() {
        PayCardFailCooldown row = baseRow();
        row.setFailCount(1);
        when(cooldownRepository.findByPgVendorAndPanHashAndOrgUnitId(PgVendor.JPAY, HASH, ORG))
                .thenReturn(Optional.of(row));
        when(cooldownRepository.save(any(PayCardFailCooldown.class))).thenAnswer(inv -> inv.getArgument(0));

        service.clearOnSuccessForTxn(PgVendor.JPAY, "", HASH, ORG);

        verify(riskEventRepository).deleteAllForCard(PgVendor.JPAY, HASH, ORG);
        assertEquals(0, row.getFailCount());
    }

    @Test
    void tierMinutesMatchPolicyTier1AndTier2() {
        assertEquals(3, policy.tierMinutes(1));
        assertEquals(5, policy.tierMinutes(2));
    }

    private static PayCardFailCooldown baseRow() {
        PayCardFailCooldown row = new PayCardFailCooldown();
        row.setPgVendor(PgVendor.JPAY);
        row.setPanHash(HASH);
        row.setOrgUnitId(ORG);
        row.setPanMaskKey("411111******1111");
        return row;
    }
}
