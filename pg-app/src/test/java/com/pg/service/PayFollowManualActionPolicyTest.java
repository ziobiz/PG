package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.OrgLevelPayFollowCap;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgLevelPayFollowCapRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayFollowManualActionPolicyTest {

    @Mock
    private HqNotifyEnvService hqNotifyEnvService;
    @Mock
    private HqLedgerSysSettingsRepository ledgerSysSettingsRepository;
    @Mock
    private OrgLevelPayFollowCapRepository capRepository;
    @Mock
    private AuthService authService;
    @Mock
    private OrgUnitRepository orgUnitRepository;
    @Mock
    private MerchantProfileRepository merchantProfileRepository;
    @Mock
    private PgTrnsctnRepository trnsctnRepository;
    @Mock
    private OrgAccessService orgAccessService;
    @Mock
    private PgAgencyRepository pgAgencyRepository;

    private PayFollowPolicyService service;

    @BeforeEach
    void setUp() {
        service = new PayFollowPolicyService(
                hqNotifyEnvService,
                ledgerSysSettingsRepository,
                capRepository,
                authService,
                orgUnitRepository,
                merchantProfileRepository,
                trnsctnRepository,
                orgAccessService,
                pgAgencyRepository);
    }

    @Test
    void manualVoidIndependentFromAutoVoid_globalAdmin() {
        HqNotifyEnvConfig env = new HqNotifyEnvConfig();
        env.setAutoVoidYn("N");
        env.setManualVoidYn("Y");
        env.setAutoRefundYn("N");
        env.setManualRefundYn("Y");
        when(hqNotifyEnvService.getOrCreate()).thenReturn(env);

        AppUser admin = new AppUser();
        admin.setRole("ADMIN");

        Map<String, Boolean> allowed = service.allowedActionsForViewer(admin);

        assertFalse(allowed.get("AUTO_VOID"));
        assertTrue(allowed.get("MANUAL_VOID"));
        assertFalse(allowed.get("AUTO_REFUND"));
        assertTrue(allowed.get("MANUAL_REFUND"));
    }

    @Test
    void manualVoidIndependentFromAutoVoid_orgCap() {
        HqNotifyEnvConfig env = new HqNotifyEnvConfig();
        env.setAutoVoidYn("Y");
        env.setManualVoidYn("Y");
        env.setAutoRefundYn("Y");
        env.setManualRefundYn("Y");
        when(hqNotifyEnvService.getOrCreate()).thenReturn(env);

        OrgLevelPayFollowCap cap = new OrgLevelPayFollowCap();
        cap.setOrgLevel("BRANCH");
        cap.setAutoVoidYn("N");
        cap.setManualVoidYn("Y");
        cap.setAutoRefundYn("N");
        cap.setManualRefundYn("Y");
        when(capRepository.findById("BRANCH")).thenReturn(Optional.of(cap));

        AppUser user = new AppUser();
        user.setRole("USER");
        user.setUsername("branch1");
        when(authService.getOrgInfo("branch1")).thenReturn(Map.of("orgLevel", "BRANCH"));

        Map<String, Boolean> allowed = service.allowedActionsForViewer(user);

        assertFalse(allowed.get("AUTO_VOID"));
        assertTrue(allowed.get("MANUAL_VOID"));
        assertFalse(allowed.get("AUTO_REFUND"));
        assertTrue(allowed.get("MANUAL_REFUND"));
    }
}
