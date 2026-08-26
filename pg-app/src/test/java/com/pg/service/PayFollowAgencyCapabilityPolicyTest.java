package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayFollowAgencyCapabilityPolicyTest {

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
    void chillPayAgencyOffBlocksAutoVoidEvenIfNotiOn() {
        HqNotifyEnvConfig env = allFollowOn();
        when(hqNotifyEnvService.getOrCreate()).thenReturn(env);

        AppUser admin = new AppUser();
        admin.setRole("ADMIN");

        PgAgency agency = new PgAgency();
        agency.setPgCd("CHILLPAY");
        agency.setPayFollowAutoVoidYn("N");
        agency.setPayFollowEmailVoidYn("Y");
        agency.setPayFollowAutoRefundYn("Y");
        agency.setPayFollowForceRefundYn("Y");
        when(pgAgencyRepository.findByPgCd("CHILLPAY")).thenReturn(Optional.of(agency));

        PgTrnsctn t = approved("CHILLPAY");
        Map<String, Boolean> row = service.payFollowRowEnabled(admin, t);
        assertFalse(row.get("AUTO_VOID"));
    }

    @Test
    void jpayAgencyCannotEnableAutoVoid() {
        HqNotifyEnvConfig env = allFollowOn();
        when(hqNotifyEnvService.getOrCreate()).thenReturn(env);

        AppUser admin = new AppUser();
        admin.setRole("ADMIN");

        PgAgency agency = new PgAgency();
        agency.setPgCd("JPAY");
        agency.setPayFollowAutoVoidYn("Y");
        agency.setPayFollowManualVoidYn("Y");
        agency.setPayFollowAutoRefundYn("Y");
        agency.setPayFollowManualRefundYn("Y");
        agency.setPayFollowForceRefundYn("Y");
        when(pgAgencyRepository.findByPgCd("JPAY")).thenReturn(Optional.of(agency));

        PgTrnsctn t = approved("JPAY");
        Map<String, Boolean> row = service.payFollowRowEnabled(admin, t);
        assertFalse(row.get("AUTO_VOID"));
        assertTrue(row.get("MANUAL_VOID"));
        assertTrue(row.get("MANUAL_REFUND"));
        assertFalse(row.get("EMAIL_VOID"));
    }

    @Test
    void elementPayAgencyRefundOffBlocksRefundApi() {
        HqNotifyEnvConfig env = allFollowOn();
        when(hqNotifyEnvService.getOrCreate()).thenReturn(env);

        AppUser admin = new AppUser();
        admin.setRole("ADMIN");

        PgAgency agency = new PgAgency();
        agency.setPgCd("ELEMENTPAY");
        agency.setPayFollowAutoRefundYn("N");
        agency.setPayFollowForceRefundYn("Y");
        agency.setPayFollowSameDayRefundYn("Y");
        when(pgAgencyRepository.findByPgCd("ELEMENTPAY")).thenReturn(Optional.of(agency));

        PgTrnsctn t = approved("ELEMENTPAY");
        Map<String, Boolean> row = service.payFollowRowEnabled(admin, t);
        assertFalse(row.get("AUTO_VOID"));
        assertFalse(row.get("EMAIL_VOID"));
        assertFalse(row.get("AUTO_REFUND"));
    }

    @Test
    void multiPgSameMerchantDifferentVanShowsDifferentFollowSet() {
        HqNotifyEnvConfig env = allFollowOn();
        when(hqNotifyEnvService.getOrCreate()).thenReturn(env);

        AppUser admin = new AppUser();
        admin.setRole("ADMIN");

        PgAgency chill = new PgAgency();
        chill.setPgCd("CHILLPAY");
        chill.setPayFollowAutoVoidYn("Y");
        chill.setPayFollowEmailVoidYn("Y");
        chill.setPayFollowAutoRefundYn("Y");
        chill.setPayFollowForceRefundYn("Y");
        when(pgAgencyRepository.findByPgCd("CHILLPAY")).thenReturn(Optional.of(chill));

        PgAgency jpay = new PgAgency();
        jpay.setPgCd("JPAY");
        jpay.setPayFollowManualVoidYn("Y");
        jpay.setPayFollowManualRefundYn("Y");
        jpay.setPayFollowAutoRefundYn("Y");
        jpay.setPayFollowForceRefundYn("Y");
        when(pgAgencyRepository.findByPgCd("JPAY")).thenReturn(Optional.of(jpay));

        Map<String, Boolean> chillRow = service.payFollowRowEnabled(admin, approved("CHILLPAY"));
        Map<String, Boolean> jpayRow = service.payFollowRowEnabled(admin, approved("JPAY"));

        assertTrue(chillRow.get("AUTO_VOID") || chillRow.get("EMAIL_VOID") || chillRow.get("AUTO_REFUND"));
        assertFalse(chillRow.get("MANUAL_VOID"));
        assertFalse(jpayRow.get("AUTO_VOID"));
        assertFalse(jpayRow.get("EMAIL_VOID"));
        assertTrue(jpayRow.get("MANUAL_VOID"));
        assertTrue(jpayRow.get("MANUAL_REFUND"));
    }

    private static HqNotifyEnvConfig allFollowOn() {
        HqNotifyEnvConfig env = new HqNotifyEnvConfig();
        env.setAutoVoidYn("Y");
        env.setEmailVoidYn("Y");
        env.setManualVoidYn("Y");
        env.setAutoRefundYn("Y");
        env.setManualRefundYn("Y");
        env.setForceRefundYn("Y");
        env.setForceRefundAfterDays(7);
        env.setAutoRefundAfterDays(7);
        env.setEpSameDayRefundYn("Y");
        env.setPayFollowRefZone("Asia/Bangkok");
        return env;
    }

    private static PgTrnsctn approved(String van) {
        PgTrnsctn t = new PgTrnsctn();
        t.setVan(van);
        t.setStatus("10");
        t.setPaidAt(LocalDateTime.now().minusHours(1));
        t.setCreatedAt(LocalDateTime.now().minusHours(1));
        return t;
    }
}
