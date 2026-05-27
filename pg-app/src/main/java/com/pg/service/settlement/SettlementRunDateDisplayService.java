package com.pg.service.settlement;

import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementSetting;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementSettingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 가맹 정산주기·영업일 기준 마감일·배치 예정일을 API 행 맵에 채웁니다. */
@Service
public class SettlementRunDateDisplayService {

    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final SettlementBusinessHolidayService settlementBusinessHolidayService;

    public SettlementRunDateDisplayService(OrgUnitRepository orgUnitRepository,
                                           SettlementSettingRepository settlementSettingRepository,
                                           SettlementBusinessHolidayService settlementBusinessHolidayService) {
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.settlementBusinessHolidayService = settlementBusinessHolidayService;
    }

    public void enrichCloseAndExecDates(Map<String, Object> m, SettlementRun r) {
        if (r == null || m == null) {
            return;
        }
        String fallback = settlementCycleFallbackForMerchant(r.getMerchantId());
        enrichCloseAndExecDates(m, r, resolveCalcCycleForDisplay(r, fallback));
    }

    public void enrichCloseAndExecDates(Map<String, Object> m, SettlementRun r, String calcCycleRaw) {
        if (r == null || m == null) {
            return;
        }
        Set<LocalDate> hol = resolveHolidaysForMerchant(r.getMerchantId());
        SettlementRunDateDisplayUtil.putCloseAndExecDates(m, r, calcCycleRaw, hol);
    }

    public String resolveCalcCycleForDisplay(SettlementRun r, String settingsCycleFallback) {
        if (r == null) {
            return "";
        }
        String snap = r.getCalcCycleSnapshot();
        if (snap != null && !snap.isBlank()) {
            return snap.trim();
        }
        if (settingsCycleFallback != null && !settingsCycleFallback.isBlank()) {
            return SettlementPeriodResolver.normalizeCalcCycle(settingsCycleFallback.trim());
        }
        return "";
    }

    public String settlementCycleFallbackForMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return "";
        }
        Optional<com.pg.entity.OrgUnit> ou = orgUnitRepository.findByCode(merchantId.trim());
        if (ou.isEmpty()) {
            return "";
        }
        return settlementSettingRepository.findByOrgUnitId(ou.get().getId())
                .map(SettlementSetting::getCalcCycle)
                .map(c -> c != null ? c.trim() : "")
                .orElse("");
    }

    private Set<LocalDate> resolveHolidaysForMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return Collections.emptySet();
        }
        return orgUnitRepository.findByCode(merchantId.trim())
                .map(ou -> settlementBusinessHolidayService.resolveNonBusinessDatesForMerchantOrgUnitId(ou.getId()))
                .orElse(Collections.emptySet());
    }
}
