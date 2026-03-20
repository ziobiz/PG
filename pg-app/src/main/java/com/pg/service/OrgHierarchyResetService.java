package com.pg.service;

import com.pg.entity.*;
import com.pg.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 업체(조직) 트리 및 연관 데이터 전부 삭제 후 OTL HQ(0000000000)만 재생성.
 * 등록으로만 조직을 쌓기 위한 초기화용.
 */
@Service
public class OrgHierarchyResetService {

    private static final Logger log = LoggerFactory.getLogger(OrgHierarchyResetService.class);

    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;
    private final CommissionHistoryRepository commissionHistoryRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final RollingReserveRepository rollingReserveRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PgNotifyInboundRepository pgNotifyInboundRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final MerchantDefaultProductRepository merchantDefaultProductRepository;
    private final MerchantCommissionExtraRepository merchantCommissionExtraRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final OrgBrandingRepository orgBrandingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final UserCompAccessRepository userCompAccessRepository;

    public OrgHierarchyResetService(AuthTokenRepository authTokenRepository,
                                    UserRepository userRepository,
                                    CommissionHistoryRepository commissionHistoryRepository,
                                    CommissionPolicyRepository commissionPolicyRepository,
                                    SettlementRunRepository settlementRunRepository,
                                    RollingReserveRepository rollingReserveRepository,
                                    PgTrnsctnRepository pgTrnsctnRepository,
                                    PgNotifyInboundRepository pgNotifyInboundRepository,
                                    MerchantPgBindingRepository merchantPgBindingRepository,
                                    MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                    MerchantDefaultProductRepository merchantDefaultProductRepository,
                                    MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                                    MerchantProfileRepository merchantProfileRepository,
                                    SettlementSettingRepository settlementSettingRepository,
                                    OrgBrandingRepository orgBrandingRepository,
                                    OrgUnitRepository orgUnitRepository,
                                    UserCompAccessRepository userCompAccessRepository) {
        this.authTokenRepository = authTokenRepository;
        this.userRepository = userRepository;
        this.commissionHistoryRepository = commissionHistoryRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.rollingReserveRepository = rollingReserveRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.pgNotifyInboundRepository = pgNotifyInboundRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.orgBrandingRepository = orgBrandingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.userCompAccessRepository = userCompAccessRepository;
    }

    @Transactional
    public void resetToHeadquartersOnly() {
        log.warn("Org hierarchy reset: deleting all org-related rows and non-ADMIN users, then 0000000000 only.");

        authTokenRepository.deleteAll();
        userCompAccessRepository.deleteAll();

        List<AppUser> nonAdmin = userRepository.findAll().stream()
                .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole()))
                .toList();
        for (AppUser u : nonAdmin) {
            userRepository.delete(u);
        }

        commissionHistoryRepository.deleteAll();
        commissionPolicyRepository.deleteAll();
        settlementRunRepository.deleteAll();
        rollingReserveRepository.deleteAll();
        pgTrnsctnRepository.deleteAll();
        pgNotifyInboundRepository.deleteAll();
        merchantPgBindingRepository.deleteAll();
        merchantNotifyUrlRepository.deleteAll();
        merchantDefaultProductRepository.deleteAll();
        merchantCommissionExtraRepository.deleteAll();
        merchantProfileRepository.deleteAll();
        settlementSettingRepository.deleteAll();
        orgBrandingRepository.deleteAll();

        List<OrgUnit> units = orgUnitRepository.findAll();
        for (OrgUnit o : units) {
            o.setParentId(null);
        }
        orgUnitRepository.saveAll(units);
        orgUnitRepository.deleteAll();

        OrgUnit hq = new OrgUnit();
        hq.setOrgLevel(OrgLevel.HEADQUARTERS);
        hq.setCode("0000000000");
        hq.setName("OTL HQ");
        hq.setStatus("ACTIVE");
        orgUnitRepository.save(hq);

        MerchantProfile mp = new MerchantProfile();
        mp.setOrgUnitId(hq.getId());
        mp.setCompDiv(OrgLevel.HEADQUARTERS.name());
        mp.setUseYn("Y");
        merchantProfileRepository.save(mp);

        SettlementSetting ss = new SettlementSetting();
        ss.setOrgUnitId(hq.getId());
        ss.setCalcCycle("D7");
        ss.setTransferType("MANUAL");
        ss.setPayHoldYn("N");
        settlementSettingRepository.save(ss);

        MerchantCommissionExtra ex = new MerchantCommissionExtra();
        ex.setOrgUnitId(hq.getId());
        merchantCommissionExtraRepository.save(ex);

        log.info("Org hierarchy reset complete. Only 0000000000 remains.");
    }
}
