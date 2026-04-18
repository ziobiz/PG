package com.pg.service;

import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementSetting;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.util.ReceivableRecoveryModeUtil;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 미수금 환수 모드: 가맹이 {@code receivable_recovery_override_yn=Y}이면 가맹 {@code receivable_recovery_mode} 우선,
 * 아니면 소속 총판(MASTER_DIST) {@code tb_settlement_setting} 값, 총판 없으면 본사 기본(tb_hq_ledger_sys_settings).
 */
@Service
public class ReceivableRecoveryModeService {

    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;

    public ReceivableRecoveryModeService(OrgUnitRepository orgUnitRepository,
                                         SettlementSettingRepository settlementSettingRepository,
                                         HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
    }

    public String hqDefaultMode() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(HqLedgerSysSettings::getReceivableRecoveryDefaultMode)
                .map(ReceivableRecoveryModeUtil::normalize)
                .orElse(ReceivableRecoveryModeUtil.AUTO);
    }

    public boolean isManualForMerchantCode(String merchantCompId) {
        return ReceivableRecoveryModeUtil.isManual(resolveEffectiveModeForMerchantCode(merchantCompId));
    }

    /**
     * 가맹점 코드 기준 유효 모드(AUTO/MANUAL).
     */
    public String resolveEffectiveModeForMerchantCode(String compId) {
        if (compId == null || compId.isBlank()) {
            return ReceivableRecoveryModeUtil.AUTO;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(compId.trim());
        if (ou.isEmpty() || ou.get().getOrgLevel() != OrgLevel.MERCHANT) {
            return ReceivableRecoveryModeUtil.AUTO;
        }
        return resolveEffectiveModeForMerchantOrgUnitId(ou.get().getId());
    }

    public String resolveEffectiveModeForMerchantOrgUnitId(long merchantOrgUnitId) {
        Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(merchantOrgUnitId);
        if (ssOpt.isPresent() && merchantOverrides(ssOpt.get())) {
            return ReceivableRecoveryModeUtil.normalize(ssOpt.get().getReceivableRecoveryMode());
        }
        return resolveInheritedModeForMerchantOrgUnitId(merchantOrgUnitId);
    }

    /**
     * 총판 설정만 따를 때(오버라이드 N) 가맹에 복사해 둘 모드 — 저장 시 표시용.
     */
    public String resolveInheritedModeForMerchantOrgUnitId(long merchantOrgUnitId) {
        Optional<Long> mdId = findNearestMasterDistAncestorId(merchantOrgUnitId);
        if (mdId.isPresent()) {
            Optional<SettlementSetting> mdSs = settlementSettingRepository.findByOrgUnitId(mdId.get());
            if (mdSs.isPresent()) {
                return ReceivableRecoveryModeUtil.normalize(mdSs.get().getReceivableRecoveryMode());
            }
        }
        return hqDefaultMode();
    }

    public Optional<String> findMasterDistCompCodeForMerchantOrgUnitId(long merchantOrgUnitId) {
        return findNearestMasterDistAncestorId(merchantOrgUnitId)
                .flatMap(orgUnitRepository::findById)
                .map(OrgUnit::getCode);
    }

    public static boolean merchantOverrides(SettlementSetting ss) {
        if (ss == null) {
            return false;
        }
        String v = ss.getReceivableRecoveryOverrideYn();
        return v != null && "Y".equalsIgnoreCase(v.trim());
    }

    private Optional<Long> findNearestMasterDistAncestorId(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        Long cur = orgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit ou = orgUnitRepository.findById(cur).orElse(null);
            if (ou == null) {
                break;
            }
            if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
                return Optional.of(ou.getId());
            }
            cur = ou.getParentId();
        }
        return Optional.empty();
    }
}
