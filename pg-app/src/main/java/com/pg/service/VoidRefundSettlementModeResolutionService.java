package com.pg.service;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementSetting;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.util.VoidRefundSettlementModeUtil;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 무효·환불 정산 방식: 가맹 {@code void_refund_settlement_override_yn=Y} 이면 가맹 열 우선(비어 있으면 총판·본사),
 * N이면 총판 {@code tb_settlement_setting} → 본사. 총판 열이 NULL이면 본사.
 * 레거시: 가맹 SS가 비어 있고 수수료정책에만 값이 있으면 {@link VoidRefundSettlementModeUtil#effective} 로 본사와 조합.
 */
@Service
public class VoidRefundSettlementModeResolutionService {

    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;

    public VoidRefundSettlementModeResolutionService(OrgUnitRepository orgUnitRepository,
                                                     SettlementSettingRepository settlementSettingRepository,
                                                     HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository,
                                                     CommissionPolicyRepository commissionPolicyRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
    }

    public String resolveVoidSettlementMode(String merchantCompId) {
        return resolveOne(merchantCompId, ModeSlot.VOID);
    }

    public String resolveManualVoidSettlementMode(String merchantCompId) {
        return resolveOne(merchantCompId, ModeSlot.MANUAL_VOID);
    }

    public String resolveRefundSettlementMode(String merchantCompId) {
        return resolveOne(merchantCompId, ModeSlot.REFUND);
    }

    public String resolveForceRefundSettlementMode(String merchantCompId) {
        return resolveOne(merchantCompId, ModeSlot.FORCE_REFUND);
    }

    private enum ModeSlot {
        VOID,
        MANUAL_VOID,
        REFUND,
        FORCE_REFUND
    }

    private String resolveOne(String merchantCompId, ModeSlot slot) {
        if (merchantCompId == null || merchantCompId.isBlank()) {
            return hqVoid(slot);
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantCompId.trim());
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(merchantCompId.trim());
        }
        if (ou.isEmpty() || ou.get().getOrgLevel() != OrgLevel.MERCHANT) {
            return hqVoid(slot);
        }
        HqLedgerSysSettings hq = hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc().orElse(null);
        String hqVal = readHq(hq, slot);
        Optional<SettlementSetting> mssOpt = settlementSettingRepository.findByOrgUnitId(ou.get().getId());
        SettlementSetting mss = mssOpt.orElse(null);
        boolean override = mss != null && mss.getVoidRefundSettlementOverrideYn() != null
                && "Y".equalsIgnoreCase(mss.getVoidRefundSettlementOverrideYn().trim());
        if (override) {
            String own = readSs(mss, slot);
            if (own != null && !own.isBlank()) {
                return VoidRefundSettlementModeUtil.normalize(own);
            }
        }
        Optional<Long> mdId = findNearestMasterDistAncestorId(ou.get().getId());
        if (mdId.isPresent()) {
            Optional<SettlementSetting> mdssOpt = settlementSettingRepository.findByOrgUnitId(mdId.get());
            if (mdssOpt.isPresent()) {
                String mdVal = readSs(mdssOpt.get(), slot);
                if (mdVal != null && !mdVal.isBlank()) {
                    return VoidRefundSettlementModeUtil.normalize(mdVal);
                }
            }
        }
        Optional<CommissionPolicy> pol = commissionPolicyRepository.findByScope(merchantCompId.trim());
        if (pol.isPresent()) {
            String pRaw = readPolicy(pol.get(), slot);
            if (pRaw != null && !pRaw.isBlank()) {
                return VoidRefundSettlementModeUtil.effective(pRaw, hqVal);
            }
        }
        return VoidRefundSettlementModeUtil.normalize(hqVal);
    }

    private static String readHq(HqLedgerSysSettings hq, ModeSlot slot) {
        if (hq == null) {
            return VoidRefundSettlementModeUtil.GENERAL;
        }
        return switch (slot) {
            case VOID -> hq.getVoidSettlementMode();
            case MANUAL_VOID -> hq.getManualVoidSettlementMode();
            case REFUND -> hq.getRefundSettlementMode();
            case FORCE_REFUND -> hq.getForceRefundSettlementMode();
        };
    }

    private static String readSs(SettlementSetting ss, ModeSlot slot) {
        if (ss == null) {
            return null;
        }
        return switch (slot) {
            case VOID -> ss.getVoidSettlementMode();
            case MANUAL_VOID -> ss.getManualVoidSettlementMode();
            case REFUND -> ss.getRefundSettlementMode();
            case FORCE_REFUND -> ss.getForceRefundSettlementMode();
        };
    }

    private static String readPolicy(CommissionPolicy p, ModeSlot slot) {
        return switch (slot) {
            case VOID -> p.getVoidSettlementMode();
            case MANUAL_VOID -> p.getManualVoidSettlementMode();
            case REFUND -> p.getRefundSettlementMode();
            case FORCE_REFUND -> p.getForceRefundSettlementMode();
        };
    }

    private String hqVoid(ModeSlot slot) {
        return VoidRefundSettlementModeUtil.normalize(readHq(
                hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc().orElse(null), slot));
    }

    private Optional<Long> findNearestMasterDistAncestorId(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        Long cur = orgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit node = orgUnitRepository.findById(cur).orElse(null);
            if (node == null) {
                break;
            }
            if (node.getOrgLevel() == OrgLevel.MASTER_DIST) {
                return Optional.of(node.getId());
            }
            cur = node.getParentId();
        }
        return Optional.empty();
    }

    public static boolean merchantOverridesVoidRefund(SettlementSetting ss) {
        if (ss == null || ss.getVoidRefundSettlementOverrideYn() == null) {
            return false;
        }
        return "Y".equalsIgnoreCase(ss.getVoidRefundSettlementOverrideYn().trim());
    }

    /** 정책 화면용: 저장된 문자열이 비어 있으면 FOLLOW 로 표기 */
    public static String modeForDetailForm(String raw) {
        if (raw == null || raw.isBlank()) {
            return "FOLLOW";
        }
        return VoidRefundSettlementModeUtil.normalize(raw.trim());
    }

    public static String parseBodyMode(Object o) {
        if (o == null) {
            return null;
        }
        String t = String.valueOf(o).trim();
        if (t.isEmpty() || "FOLLOW".equalsIgnoreCase(t)) {
            return null;
        }
        return VoidRefundSettlementModeUtil.normalize(t);
    }

    public static String normalizeOverrideYn(Object o) {
        if (o == null) {
            return "N";
        }
        return "Y".equalsIgnoreCase(String.valueOf(o).trim()) ? "Y" : "N";
    }

    public static boolean isBlankOrFollow(String s) {
        if (s == null) {
            return true;
        }
        String t = s.trim();
        return t.isEmpty() || "FOLLOW".equalsIgnoreCase(t);
    }

    public static String upperOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim().toUpperCase(Locale.ROOT);
    }
}
