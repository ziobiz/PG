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
 * <p><b>TEMP_REMOVE_AFTER_DEV</b> — 개발 단계 임시 기능. 스테이징/운영 정식 완료 후 아래를 <b>일괄 제거</b>할 것:
 * <ul>
 *   <li>이 서비스 클래스 및 Spring 빈 주입 참조</li>
 *   <li>{@code ApiCompController}: {@code /api/comp/dev-tree-remove}, {@code /api/comp/admin-reset-org-hierarchy}, 플래그 필드</li>
 *   <li>{@code app.features.comp-dev-tree-remove}, {@code app.features.allow-org-hierarchy-reset} (모든 yml)</li>
 *   <li>프론트: {@code compDevTreeRemoveBtn}, {@code compAdminResetOrgBtn}, {@code PG_API.compDevTreeRemove}, {@code compAdminResetOrgHierarchy}</li>
 *   <li>{@link com.pg.config.OrgHierarchyStartupReset}, {@code CompService#softDeactivateOrgSubtreeForDev}</li>
 *   <li>선택: dev 전용 {@code ApiDevController} {@code GET /api/dev/reset-org-hierarchy} (같은 서비스 사용 시 함께 정리)</li>
 * </ul>
 * 검색 키워드: {@code TEMP_REMOVE_AFTER_DEV}
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
    private final NoticeRepository noticeRepository;
    private final OrgUnitPagePermissionRepository orgUnitPagePermissionRepository;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final OrgViewColumnAllowanceRepository orgViewColumnAllowanceRepository;
    private final HqViewCustomColumnRepository hqViewCustomColumnRepository;
    private final UserViewSettingRepository userViewSettingRepository;
    private final BalanceDeductionRepository balanceDeductionRepository;

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
                                    UserCompAccessRepository userCompAccessRepository,
                                    NoticeRepository noticeRepository,
                                    OrgUnitPagePermissionRepository orgUnitPagePermissionRepository,
                                    DistributionFeeConfigRepository distributionFeeConfigRepository,
                                    OrgViewColumnAllowanceRepository orgViewColumnAllowanceRepository,
                                    HqViewCustomColumnRepository hqViewCustomColumnRepository,
                                    UserViewSettingRepository userViewSettingRepository,
                                    BalanceDeductionRepository balanceDeductionRepository) {
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
        this.noticeRepository = noticeRepository;
        this.orgUnitPagePermissionRepository = orgUnitPagePermissionRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.orgViewColumnAllowanceRepository = orgViewColumnAllowanceRepository;
        this.hqViewCustomColumnRepository = hqViewCustomColumnRepository;
        this.userViewSettingRepository = userViewSettingRepository;
        this.balanceDeductionRepository = balanceDeductionRepository;
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
        balanceDeductionRepository.deleteAll();
        merchantPgBindingRepository.deleteAll();
        merchantNotifyUrlRepository.deleteAll();
        merchantDefaultProductRepository.deleteAll();
        merchantCommissionExtraRepository.deleteAll();
        merchantProfileRepository.deleteAll();
        settlementSettingRepository.deleteAll();
        orgBrandingRepository.deleteAll();

        noticeRepository.deleteAll();
        orgUnitPagePermissionRepository.deleteAll();
        distributionFeeConfigRepository.deleteAll();
        hqViewCustomColumnRepository.deleteAll();
        orgViewColumnAllowanceRepository.deleteAll();
        userViewSettingRepository.deleteAll();

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

        for (AppUser left : userRepository.findAll()) {
            left.setOrgUnitCode(hq.getCode());
            userRepository.save(left);
        }

        log.info("Org hierarchy reset complete. Only {} remains ({} org row(s)).",
                hq.getCode(), orgUnitRepository.count());
    }
}
