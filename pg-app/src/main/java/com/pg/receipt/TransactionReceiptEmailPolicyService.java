package com.pg.receipt;

import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementSetting;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementSettingRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** 고객 거래명세서 이메일 — 본사 → 총판 → 가맹(직접입력 우선) */
@Service
public class TransactionReceiptEmailPolicyService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;

    public TransactionReceiptEmailPolicyService(OrgUnitRepository orgUnitRepository,
                                                MerchantProfileRepository merchantProfileRepository,
                                                SettlementSettingRepository settlementSettingRepository,
                                                HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
    }

    public boolean isEnabledForMerchantCode(String merchantCompId) {
        if (merchantCompId == null || merchantCompId.isBlank()) {
            return false;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantCompId.trim());
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(merchantCompId.trim());
        }
        return ou.filter(u -> u.getOrgLevel() == OrgLevel.MERCHANT)
                .map(u -> isEnabledForMerchantOrgUnitId(u.getId()))
                .orElse(false);
    }

    public boolean isEnabledForMerchantOrgUnitId(long merchantOrgUnitId) {
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        if (mp.isPresent() && merchantOverrides(mp.get())) {
            return yn(mp.get().getReceiptEmailUseYn());
        }
        return resolveInheritedForMerchantOrgUnitId(merchantOrgUnitId);
    }

    public String resolveInheritedYnForMerchantOrgUnitId(long merchantOrgUnitId) {
        Optional<Long> mdId = findNearestMasterDistAncestorId(merchantOrgUnitId);
        if (mdId.isPresent()) {
            Optional<SettlementSetting> mdSs = settlementSettingRepository.findByOrgUnitId(mdId.get());
            if (mdSs.isPresent() && mdSs.get().getReceiptEmailEnabledYn() != null
                    && !mdSs.get().getReceiptEmailEnabledYn().isBlank()) {
                return yn(mdSs.get().getReceiptEmailEnabledYn()) ? "Y" : "N";
            }
        }
        return hqDefaultYn();
    }

    private boolean resolveInheritedForMerchantOrgUnitId(long merchantOrgUnitId) {
        return yn(resolveInheritedYnForMerchantOrgUnitId(merchantOrgUnitId));
    }

    public String hqDefaultYn() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(HqLedgerSysSettings::getReceiptEmailDefaultYn)
                .map(this::normalizeYn)
                .orElse("N");
    }

    public static boolean merchantOverrides(MerchantProfile mp) {
        if (mp == null) {
            return false;
        }
        String follow = mp.getReceiptEmailFollowHqYn();
        return follow != null && "N".equalsIgnoreCase(follow.trim());
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

    public Optional<OrgUnit> findMasterDistForMerchantOrgUnitId(long merchantOrgUnitId) {
        return findNearestMasterDistAncestorId(merchantOrgUnitId).flatMap(orgUnitRepository::findById);
    }

    private String normalizeYn(String v) {
        return yn(v) ? "Y" : "N";
    }

    private static boolean yn(String v) {
        return v != null && "Y".equalsIgnoreCase(v.trim());
    }
}
