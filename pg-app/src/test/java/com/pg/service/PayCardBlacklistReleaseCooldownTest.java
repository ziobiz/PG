package com.pg.service;

import com.pg.entity.HqPayCardBlacklist;
import com.pg.entity.PayCardFailCooldown;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqPayCardBlacklistRepository;
import com.pg.repository.HqPayCardBlockPrefixRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PayCardFailCooldownRepository;
import com.pg.repository.PayCardFailRiskEventRepository;
import com.pg.util.PayCardMaskKeyUtil;
import com.pg.util.PayCardPanHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayCardBlacklistReleaseCooldownTest {

    private static final String PAN = "4111111111111111";
    private static final String MASK = PayCardMaskKeyUtil.maskKeyFromPan(PAN);
    private static final String HASH = PayCardPanHashUtil.hashPan(PAN);

    @Mock
    private HqPayCardBlockPrefixRepository blockPrefixRepository;
    @Mock
    private HqPayCardBlacklistRepository blacklistRepository;
    @Mock
    private PayCardFailCooldownRepository cooldownRepository;
    @Mock
    private PayCardFailRiskEventRepository riskEventRepository;
    @Mock
    private OrgUnitRepository orgUnitRepository;

    private PayCardFailCooldownService cooldownService;
    private PayCardPolicyService policyService;

    @BeforeEach
    void setUp() {
        cooldownService = new PayCardFailCooldownService(
                null, cooldownRepository, riskEventRepository, null, null, orgUnitRepository);
        policyService = new PayCardPolicyService(
                blockPrefixRepository, blacklistRepository, cooldownService, orgUnitRepository, null);
    }

    @Test
    void releaseBlacklistClearsCooldownAndRiskEvents() {
        HqPayCardBlacklist row = activeRow(9L);
        PayCardFailCooldown cd = new PayCardFailCooldown();
        cd.setPgVendor(PgVendor.JPAY);
        cd.setPanHash(HASH);
        cd.setOrgUnitId(42L);
        cd.setPanMaskKey(MASK);
        cd.setFailCount(2);
        cd.setBlockedUntil(LocalDateTime.now().plusYears(10));

        when(blacklistRepository.findById(9L)).thenReturn(Optional.of(row));
        when(blacklistRepository.findActiveSiblingsByPanIdentity(HASH, MASK)).thenReturn(List.of(row));
        when(cooldownRepository.findAllByPgAndPanIdentity(PgVendor.JPAY, HASH, MASK)).thenReturn(List.of(cd));
        when(cooldownRepository.save(any(PayCardFailCooldown.class))).thenAnswer(inv -> inv.getArgument(0));
        when(blacklistRepository.save(any(HqPayCardBlacklist.class))).thenAnswer(inv -> inv.getArgument(0));

        policyService.releaseBlacklist(9L, "admin", "test release");

        verify(riskEventRepository).deleteAllForCard(PgVendor.JPAY, HASH, 42L);
        assertEquals(0, cd.getFailCount());
        assertEquals(null, cd.getBlockedUntil());
        assertEquals("N", row.getActiveYn());
    }

    private static HqPayCardBlacklist activeRow(long id) {
        HqPayCardBlacklist row = new HqPayCardBlacklist();
        row.setId(id);
        row.setPgVendor(PgVendor.JPAY);
        row.setPanHash(HASH);
        row.setPanDisplay(MASK);
        row.setMatchMode(PayCardPolicyService.MATCH_MODE_MASK_6_4);
        row.setActiveYn("Y");
        return row;
    }
}
