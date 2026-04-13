package com.pg.service;

import com.pg.entity.MasterDistSettlementCycleConfig;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MasterDistSettlementCycleConfigRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.settlement.SettlementPeriodResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 총판별 가맹 정산주기(최대 5) 및 대표 주기. 노출 순서는 본사 활성 정산주기 목록과 동일한 정렬을 따른다.
 */
@Service
public class MasterDistSettlementCycleConfigService {

    private final MasterDistSettlementCycleConfigRepository configRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqSettlementCycleAdminService hqSettlementCycleAdminService;

    public MasterDistSettlementCycleConfigService(MasterDistSettlementCycleConfigRepository configRepository,
                                                  OrgUnitRepository orgUnitRepository,
                                                  HqSettlementCycleAdminService hqSettlementCycleAdminService) {
        this.configRepository = configRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqSettlementCycleAdminService = hqSettlementCycleAdminService;
    }

    public Optional<Long> findNearestMasterDistOrgId(Long startOrgUnitId) {
        if (startOrgUnitId == null) {
            return Optional.empty();
        }
        Long cur = startOrgUnitId;
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

    public Optional<MasterDistSettlementCycleConfig> findByMasterDistOrgId(Long masterDistOrgId) {
        if (masterDistOrgId == null) {
            return Optional.empty();
        }
        return configRepository.findByOrgUnitId(masterDistOrgId);
    }

    /** 상위 체인 기준 총판 설정이 있으면 허용 코드 집합(정규화). 없거나 미설정이면 empty. */
    public Set<String> allowedCodesForMerchantParent(Long merchantParentOrgUnitId) {
        Optional<Long> md = findNearestMasterDistOrgId(merchantParentOrgUnitId);
        if (md.isEmpty()) {
            return Set.of();
        }
        Optional<MasterDistSettlementCycleConfig> cfgOpt = findByMasterDistOrgId(md.get());
        if (cfgOpt.isEmpty()) {
            return Set.of();
        }
        MasterDistSettlementCycleConfig c = cfgOpt.get();
        LinkedHashSet<String> raw = new LinkedHashSet<>();
        addIfPresent(raw, c.getCycleCode1());
        addIfPresent(raw, c.getCycleCode2());
        addIfPresent(raw, c.getCycleCode3());
        addIfPresent(raw, c.getCycleCode4());
        addIfPresent(raw, c.getCycleCode5());
        if (raw.isEmpty()) {
            return Set.of();
        }
        Set<String> norm = new HashSet<>();
        for (String s : raw) {
            norm.add(SettlementPeriodResolver.normalizeCalcCycle(s));
        }
        return norm;
    }

    private static void addIfPresent(Set<String> out, String s) {
        if (s != null && !s.isBlank()) {
            out.add(s.trim());
        }
    }

    public Optional<String> getDefaultCycleCode(Long masterDistOrgUnitId) {
        return findByMasterDistOrgId(masterDistOrgUnitId).flatMap(MasterDistSettlementCycleConfigService::defaultCodeFromEntity);
    }

    private static Optional<String> defaultCodeFromEntity(MasterDistSettlementCycleConfig c) {
        String[] slots = {
                c.getCycleCode1(), c.getCycleCode2(), c.getCycleCode3(), c.getCycleCode4(), c.getCycleCode5()
        };
        int idx = c.getDefaultSlot();
        if (idx < 0) {
            idx = 0;
        }
        if (idx > 4) {
            idx = 4;
        }
        String v = slots[idx];
        if (v == null || v.isBlank()) {
            for (String s : slots) {
                if (s != null && !s.isBlank()) {
                    return Optional.of(SettlementPeriodResolver.normalizeCalcCycle(s.trim()));
                }
            }
            return Optional.empty();
        }
        return Optional.of(SettlementPeriodResolver.normalizeCalcCycle(v.trim()));
    }

    /**
     * 가맹 상위 조직 기준 정산주기 셀렉트용.
     *
     * @return options: 본사 활성 목록과 동일 순서로 필터한 항목(선택 행 포함), defaultCalcCycle, scoped
     */
    public Map<String, Object> buildScopedCycleOptionsForMerchantParent(Long merchantParentOrgUnitId) {
        List<Map<String, Object>> all = hqSettlementCycleAdminService.listActiveSelectOptions();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scoped", false);
        out.put("defaultCalcCycle", null);
        out.put("options", all);
        if (merchantParentOrgUnitId == null) {
            return out;
        }
        Optional<Long> md = findNearestMasterDistOrgId(merchantParentOrgUnitId);
        if (md.isEmpty()) {
            return out;
        }
        Optional<MasterDistSettlementCycleConfig> cfgOpt = findByMasterDistOrgId(md.get());
        if (cfgOpt.isEmpty()) {
            return out;
        }
        MasterDistSettlementCycleConfig cfg = cfgOpt.get();
        Set<String> allowed = allowedCodesForMerchantParent(merchantParentOrgUnitId);
        if (allowed.isEmpty()) {
            return out;
        }
        List<Map<String, Object>> catalog = hqSettlementCycleAdminService.listCatalogSelectOptions();
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : catalog) {
            String v = row.get("v") != null ? String.valueOf(row.get("v")).trim() : "";
            if (v.isEmpty()) {
                filtered.add(row);
                continue;
            }
            String norm = SettlementPeriodResolver.normalizeCalcCycle(v);
            if (allowed.contains(norm)) {
                filtered.add(row);
            }
        }
        out.put("options", filtered);
        out.put("scoped", true);
        defaultCodeFromEntity(cfg).ifPresent(d -> out.put("defaultCalcCycle", d));
        return out;
    }

    public void validateMerchantCalcCycle(Long merchantParentOrgUnitId, String calcCycleRaw) {
        if (calcCycleRaw == null || calcCycleRaw.isBlank()) {
            return;
        }
        String code = SettlementPeriodResolver.normalizeCalcCycle(calcCycleRaw.trim());
        Set<String> allowed = allowedCodesForMerchantParent(merchantParentOrgUnitId);
        if (allowed.isEmpty()) {
            return;
        }
        if (!allowed.contains(code)) {
            throw new IllegalArgumentException("해당 총판에서 허용한 정산주기만 선택할 수 있습니다: " + code);
        }
    }

    @Transactional
    public MasterDistSettlementCycleConfig saveForMasterDist(Long masterDistOrgUnitId,
                                                             List<String> slotCodes,
                                                             int defaultSlotIndex) {
        OrgUnit ou = orgUnitRepository.findById(masterDistOrgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MASTER_DIST) {
            throw new IllegalArgumentException("총판(MASTER_DIST) 조직만 설정할 수 있습니다.");
        }
        List<String> slots = slotCodes != null ? new ArrayList<>(slotCodes) : new ArrayList<>();
        while (slots.size() < 5) {
            slots.add(null);
        }
        if (slots.size() > 5) {
            throw new IllegalArgumentException("정산주기는 최대 5개까지 지정할 수 있습니다.");
        }
        LinkedHashSet<String> normalizedDistinct = new LinkedHashSet<>();
        List<String> cleaned = new ArrayList<>(Collections.nCopies(5, null));
        Set<String> catalogNorm = new HashSet<>();
        for (Map<String, Object> row : hqSettlementCycleAdminService.listMergedDefinitions()) {
            String v = row.get("cycleCode") != null ? String.valueOf(row.get("cycleCode")).trim() : "";
            if (!v.isEmpty()) {
                catalogNorm.add(SettlementPeriodResolver.normalizeCalcCycle(v));
            }
        }
        int nonEmpty = 0;
        for (int i = 0; i < 5; i++) {
            String s = i < slots.size() ? slots.get(i) : null;
            if (s == null || s.isBlank()) {
                continue;
            }
            String norm = SettlementPeriodResolver.normalizeCalcCycle(s.trim());
            if (!catalogNorm.contains(norm)) {
                throw new IllegalArgumentException("등록된 정산주기 코드가 아닙니다: " + norm);
            }
            if (!normalizedDistinct.add(norm)) {
                throw new IllegalArgumentException("정산주기 코드가 중복되었습니다: " + norm);
            }
            cleaned.set(i, norm);
            nonEmpty++;
        }
        if (nonEmpty == 0) {
            throw new IllegalArgumentException("최소 1개 정산주기를 지정하세요.");
        }
        if (defaultSlotIndex < 0 || defaultSlotIndex > 4) {
            throw new IllegalArgumentException("대표 슬롯은 1~5번 중 하나여야 합니다.");
        }
        if (cleaned.get(defaultSlotIndex) == null) {
            throw new IllegalArgumentException("대표로 지정한 슬롯에 정산주기가 없습니다.");
        }
        MasterDistSettlementCycleConfig entity = configRepository.findByOrgUnitId(masterDistOrgUnitId)
                .orElseGet(MasterDistSettlementCycleConfig::new);
        entity.setOrgUnitId(masterDistOrgUnitId);
        entity.setCycleCode1(cleaned.get(0));
        entity.setCycleCode2(cleaned.get(1));
        entity.setCycleCode3(cleaned.get(2));
        entity.setCycleCode4(cleaned.get(3));
        entity.setCycleCode5(cleaned.get(4));
        entity.setDefaultSlot(defaultSlotIndex);
        return configRepository.save(entity);
    }

    public Map<String, Object> toApiMap(MasterDistSettlementCycleConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orgUnitId", c.getOrgUnitId());
        m.put("slots", List.of(
                c.getCycleCode1(), c.getCycleCode2(), c.getCycleCode3(), c.getCycleCode4(), c.getCycleCode5()));
        m.put("defaultSlot", c.getDefaultSlot());
        defaultCodeFromEntity(c).ifPresent(x -> m.put("defaultCalcCycle", x));
        return m;
    }

    public List<Map<String, Object>> listMasterDistOrgOptions() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrgUnit ou : orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MASTER_DIST)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orgUnitId", ou.getId());
            m.put("compId", ou.getCode());
            m.put("compNm", ou.getName());
            out.add(m);
        }
        return out;
    }
}
