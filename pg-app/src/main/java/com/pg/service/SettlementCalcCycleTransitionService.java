package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementCalcCycleAudit;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementSetting;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementCalcCycleAuditRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.settlement.SettlementPeriodResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 가맹 정산주기 전환(즉시 / 예약) 및 변경 이력.
 */
@Service
public class SettlementCalcCycleTransitionService {

    public static final String MODE_IMMEDIATE = "IMMEDIATE";
    public static final String MODE_NEXT_AFTER_RUN = "NEXT_AFTER_RUN";
    public static final String MODE_APPLIED_PENDING = "APPLIED_PENDING";

    private final SettlementCalcCycleAuditRepository auditRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final OrgUnitRepository orgUnitRepository;

    public SettlementCalcCycleTransitionService(SettlementCalcCycleAuditRepository auditRepository,
                                                SettlementSettingRepository settlementSettingRepository,
                                                OrgUnitRepository orgUnitRepository) {
        this.auditRepository = auditRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listHistory(String merchantCodeFilter, int limit) {
        int cap = Math.min(500, Math.max(1, limit));
        var page = PageRequest.of(0, cap);
        List<SettlementCalcCycleAudit> rows = StringUtils.hasText(merchantCodeFilter)
                ? auditRepository.findByMerchantCodeOrderByCreatedAtDesc(merchantCodeFilter.trim(), page)
                : auditRepository.findAllByOrderByCreatedAtDesc(page);
        return rows.stream().map(this::toRow).collect(Collectors.toList());
    }

    private Map<String, Object> toRow(SettlementCalcCycleAudit a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("orgUnitId", a.getOrgUnitId());
        m.put("merchantCode", a.getMerchantCode());
        m.put("fromCycle", a.getFromCycle() != null ? a.getFromCycle() : "");
        m.put("toCycle", a.getToCycle() != null ? a.getToCycle() : "");
        m.put("transitionMode", a.getTransitionMode());
        m.put("actorUsername", a.getActorUsername() != null ? a.getActorUsername() : "");
        m.put("remark", a.getRemark() != null ? a.getRemark() : "");
        m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
        return m;
    }

    @Transactional
    public void logChange(OrgUnit merchantOu, String fromNorm, String toNorm, String transitionMode,
                          String remark, String actorUsername) {
        if (merchantOu == null || merchantOu.getId() == null) {
            return;
        }
        String mid = merchantOu.getCode() != null ? merchantOu.getCode().trim() : "";
        if (!StringUtils.hasText(mid)) {
            return;
        }
        String to = SettlementPeriodResolver.normalizeCalcCycle(toNorm != null ? toNorm : "");
        if (!StringUtils.hasText(to)) {
            return;
        }
        String from = StringUtils.hasText(fromNorm) ? SettlementPeriodResolver.normalizeCalcCycle(fromNorm) : "";
        SettlementCalcCycleAudit e = new SettlementCalcCycleAudit();
        e.setOrgUnitId(merchantOu.getId());
        e.setMerchantCode(mid);
        e.setFromCycle(from.isEmpty() ? null : from);
        e.setToCycle(to);
        e.setTransitionMode(trimMode(transitionMode));
        e.setActorUsername(actorUsername != null && !actorUsername.isBlank() ? actorUsername.trim() : null);
        e.setRemark(remark != null && !remark.isBlank() ? remark.trim().substring(0, Math.min(500, remark.trim().length())) : null);
        auditRepository.save(e);
    }

    private static String trimMode(String m) {
        if (m == null || m.isBlank()) {
            return MODE_IMMEDIATE;
        }
        return m.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 정산 실행이 1건 이상 생긴 가맹에 대해, 예약 주기가 있으면 {@code calc_cycle}으로 승격합니다.
     */
    @Transactional
    public void tryApplyPendingAfterRuns(Collection<SettlementRun> runs, String actorUsername) {
        if (runs == null || runs.isEmpty()) {
            return;
        }
        Set<String> mids = runs.stream()
                .map(SettlementRun::getMerchantId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        for (String mid : mids) {
            tryApplyPendingForMerchantCode(mid, actorUsername);
        }
    }

    @Transactional
    public void tryApplyPendingForMerchantCode(String merchantCode, String actorUsername) {
        if (!StringUtils.hasText(merchantCode)) {
            return;
        }
        OrgUnit ou = orgUnitRepository.findByCode(merchantCode.trim())
                .or(() -> orgUnitRepository.findByCodeIgnoreCase(merchantCode.trim()))
                .orElse(null);
        if (ou == null) {
            return;
        }
        Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.getId());
        if (ssOpt.isEmpty()) {
            return;
        }
        SettlementSetting ss = ssOpt.get();
        String pending = ss.getPendingCalcCycle();
        if (!StringUtils.hasText(pending)) {
            return;
        }
        String pendingNorm = SettlementPeriodResolver.normalizeCalcCycle(pending.trim());
        if (!StringUtils.hasText(pendingNorm)) {
            ss.setPendingCalcCycle(null);
            ss.setPendingCalcCycleAt(null);
            settlementSettingRepository.save(ss);
            return;
        }
        String old = ss.getCalcCycle() != null ? SettlementPeriodResolver.normalizeCalcCycle(ss.getCalcCycle().trim()) : "";
        ss.setCalcCycle(pendingNorm);
        ss.setPendingCalcCycle(null);
        ss.setPendingCalcCycleAt(null);
        settlementSettingRepository.save(ss);
        logChange(ou, old, pendingNorm, MODE_APPLIED_PENDING,
                "예약(NEXT_AFTER_RUN) 주기가 정산 실행 후 적용됨", actorUsername);
    }
}
