package com.pg.service;

import com.pg.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전산설정 — 운영 데이터 일괄 삭제.
 * <p>{@code tb_org_unit}, {@code tb_merchant_profile} 행은 유지하고, 거래·정산·노티·수수료·가맹 부가설정 등 나머지를 비웁니다.
 * 본사 전역 설정(HQ 전산·노티·PG사 목록 등), 로그인 계정({@code tb_user}), 조직별 접근({@code tb_user_comp_access}),
 * 메뉴 권한 매트릭스({@code tb_org_page_permission}), VIEW 컬럼 카탈로그({@code tb_org_view_column_allowance})는 유지합니다.
 */
@Service
public class HqOperationalDataResetService {

    private static final Logger log = LoggerFactory.getLogger(HqOperationalDataResetService.class);

    private final AuthTokenRepository authTokenRepository;
    private final SettlementRecoveryRepository settlementRecoveryRepository;
    private final MerchantReceivableRepository merchantReceivableRepository;
    private final BalanceDeductionRepository balanceDeductionRepository;
    private final CommissionHistoryRepository commissionHistoryRepository;
    private final RollingReserveRepository rollingReserveRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PgNotifyInboundRepository pgNotifyInboundRepository;
    private final OrgUnitChangeLogRepository orgUnitChangeLogRepository;
    private final ServerUsageDailyRepository serverUsageDailyRepository;
    private final ServerUsageStateRepository serverUsageStateRepository;
    private final NoticeRepository noticeRepository;
    private final UserViewSettingRepository userViewSettingRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;
    private final MerchantCommissionExtraRepository merchantCommissionExtraRepository;
    private final MerchantDefaultProductRepository merchantDefaultProductRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final OrgBrandingRepository orgBrandingRepository;
    private final OrgUnitAssistantPagePermissionRepository orgUnitAssistantPagePermissionRepository;
    private final OrgUnitPagePermissionRepository orgUnitPagePermissionRepository;
    private final MasterDistSettlementCycleConfigRepository masterDistSettlementCycleConfigRepository;
    private final OrgLevelPayFollowCapRepository orgLevelPayFollowCapRepository;
    private final HqViewCustomColumnRepository hqViewCustomColumnRepository;

    public HqOperationalDataResetService(AuthTokenRepository authTokenRepository,
                                         SettlementRecoveryRepository settlementRecoveryRepository,
                                         MerchantReceivableRepository merchantReceivableRepository,
                                         BalanceDeductionRepository balanceDeductionRepository,
                                         CommissionHistoryRepository commissionHistoryRepository,
                                         RollingReserveRepository rollingReserveRepository,
                                         SettlementRunRepository settlementRunRepository,
                                         PgTrnsctnRepository pgTrnsctnRepository,
                                         PgNotifyInboundRepository pgNotifyInboundRepository,
                                         OrgUnitChangeLogRepository orgUnitChangeLogRepository,
                                         ServerUsageDailyRepository serverUsageDailyRepository,
                                         ServerUsageStateRepository serverUsageStateRepository,
                                         NoticeRepository noticeRepository,
                                         UserViewSettingRepository userViewSettingRepository,
                                         CommissionPolicyRepository commissionPolicyRepository,
                                         ChargebackFeePolicyRepository chargebackFeePolicyRepository,
                                         MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                                         MerchantDefaultProductRepository merchantDefaultProductRepository,
                                         MerchantPgBindingRepository merchantPgBindingRepository,
                                         MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                         SettlementSettingRepository settlementSettingRepository,
                                         DistributionFeeConfigRepository distributionFeeConfigRepository,
                                         OrgBrandingRepository orgBrandingRepository,
                                         OrgUnitAssistantPagePermissionRepository orgUnitAssistantPagePermissionRepository,
                                         OrgUnitPagePermissionRepository orgUnitPagePermissionRepository,
                                         MasterDistSettlementCycleConfigRepository masterDistSettlementCycleConfigRepository,
                                         OrgLevelPayFollowCapRepository orgLevelPayFollowCapRepository,
                                         HqViewCustomColumnRepository hqViewCustomColumnRepository) {
        this.authTokenRepository = authTokenRepository;
        this.settlementRecoveryRepository = settlementRecoveryRepository;
        this.merchantReceivableRepository = merchantReceivableRepository;
        this.balanceDeductionRepository = balanceDeductionRepository;
        this.commissionHistoryRepository = commissionHistoryRepository;
        this.rollingReserveRepository = rollingReserveRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.pgNotifyInboundRepository = pgNotifyInboundRepository;
        this.orgUnitChangeLogRepository = orgUnitChangeLogRepository;
        this.serverUsageDailyRepository = serverUsageDailyRepository;
        this.serverUsageStateRepository = serverUsageStateRepository;
        this.noticeRepository = noticeRepository;
        this.userViewSettingRepository = userViewSettingRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.merchantDefaultProductRepository = merchantDefaultProductRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.orgBrandingRepository = orgBrandingRepository;
        this.orgUnitAssistantPagePermissionRepository = orgUnitAssistantPagePermissionRepository;
        this.orgUnitPagePermissionRepository = orgUnitPagePermissionRepository;
        this.masterDistSettlementCycleConfigRepository = masterDistSettlementCycleConfigRepository;
        this.orgLevelPayFollowCapRepository = orgLevelPayFollowCapRepository;
        this.hqViewCustomColumnRepository = hqViewCustomColumnRepository;
    }

    @Transactional
    public void resetAllExceptRegisteredMerchants() {
        log.warn("HQ operational data reset: clearing transactional and per-org config; keeping tb_org_unit and tb_merchant_profile.");

        authTokenRepository.deleteAll();
        settlementRecoveryRepository.deleteAll();
        merchantReceivableRepository.deleteAll();
        balanceDeductionRepository.deleteAll();
        commissionHistoryRepository.deleteAll();
        rollingReserveRepository.deleteAll();
        settlementRunRepository.deleteAll();
        pgTrnsctnRepository.deleteAll();
        pgNotifyInboundRepository.deleteAll();
        orgUnitChangeLogRepository.deleteAll();
        serverUsageDailyRepository.deleteAll();
        serverUsageStateRepository.deleteAll();
        noticeRepository.deleteAll();
        userViewSettingRepository.deleteAll();

        commissionPolicyRepository.deleteAll();
        chargebackFeePolicyRepository.deleteAll();

        merchantCommissionExtraRepository.deleteAll();
        merchantDefaultProductRepository.deleteAll();
        merchantPgBindingRepository.deleteAll();
        merchantNotifyUrlRepository.deleteAll();
        settlementSettingRepository.deleteAll();
        distributionFeeConfigRepository.deleteAll();
        orgBrandingRepository.deleteAll();
        orgUnitAssistantPagePermissionRepository.deleteAll();
        orgUnitPagePermissionRepository.deleteAll();
        masterDistSettlementCycleConfigRepository.deleteAll();
        orgLevelPayFollowCapRepository.deleteAll();
        hqViewCustomColumnRepository.deleteAll();

        log.warn("HQ operational data reset completed.");
    }
}
