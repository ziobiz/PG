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

 * 총판별 가맹 정산주기(최대 10) 및 대표 주기. 노출 순서·코드 표기는 {@link HqSettlementCycleAdminService#listCatalogSelectOptions()}와 동일(표준주기 시스템 표 병합 순서).

 */

@Service

public class MasterDistSettlementCycleConfigService {



    public static final int MAX_DISTINCT_SLOTS = 10;



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

        return allowedCodesFromEntity(cfgOpt.get());

    }



    public Set<String> allowedCodesFromEntity(MasterDistSettlementCycleConfig c) {

        LinkedHashSet<String> raw = new LinkedHashSet<>();

        for (String slot : slotValuesRaw(c)) {

            addIfPresent(raw, slot);

        }

        if (raw.isEmpty()) {

            return Set.of();

        }

        Set<String> norm = new HashSet<>();

        for (String s : raw) {

            norm.add(SettlementPeriodResolver.normalizeCalcCycle(s));

        }

        return norm;

    }



    private static List<String> slotValuesRaw(MasterDistSettlementCycleConfig c) {

        return List.of(c.getCycleCode1(), c.getCycleCode2(), c.getCycleCode3(), c.getCycleCode4(), c.getCycleCode5(),

                c.getCycleCode6(), c.getCycleCode7(), c.getCycleCode8(), c.getCycleCode9(), c.getCycleCode10());

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

        List<String> slots = slotValuesRaw(c);

        int idx = c.getDefaultSlot();

        if (idx < 0) {

            idx = 0;

        }

        if (idx >= MAX_DISTINCT_SLOTS) {

            idx = MAX_DISTINCT_SLOTS - 1;

        }

        if (idx < slots.size()) {

            String v = slots.get(idx);

            if (v != null && !v.isBlank()) {

                return Optional.of(SettlementPeriodResolver.normalizeCalcCycle(v.trim()));

            }

        }

        for (String s : slots) {

            if (s != null && !s.isBlank()) {

                return Optional.of(SettlementPeriodResolver.normalizeCalcCycle(s.trim()));

            }

        }

        return Optional.empty();

    }



    /**

     * 가맹 조직 트리 상 임의 노드 ID(가맹 본인·직계 상위 등)로 총판을 찾아 허용 주기만 반환.

     *

     * @param startOrgUnitId 가맹 orgUnitId 권장(상위까지 총판 탐색). null이면 전체 활성 목록.

     * @param ensureCycleCode 가맹에 이미 저장된 주기가 허용 목록에 없을 때 목록에 합칠 코드(정규화 전 원문)

     */

    public Map<String, Object> buildScopedCycleOptionsForMerchantParent(Long startOrgUnitId, String ensureCycleCode) {

        List<Map<String, Object>> all = hqSettlementCycleAdminService.listActiveSelectOptions();

        Map<String, Object> out = new LinkedHashMap<>();

        out.put("scoped", false);

        out.put("defaultCalcCycle", null);

        out.put("orphanSavedCycleYn", "N");

        out.put("options", all);

        if (startOrgUnitId == null) {

            return out;

        }

        Optional<Long> md = findNearestMasterDistOrgId(startOrgUnitId);

        if (md.isEmpty()) {

            return out;

        }

        Optional<MasterDistSettlementCycleConfig> cfgOpt = findByMasterDistOrgId(md.get());

        if (cfgOpt.isEmpty()) {

            return out;

        }

        MasterDistSettlementCycleConfig cfg = cfgOpt.get();

        Set<String> allowed = allowedCodesFromEntity(cfg);

        if (allowed.isEmpty()) {

            return out;

        }

        List<Map<String, Object>> catalog = hqSettlementCycleAdminService.listCatalogSelectOptions();

        List<Map<String, Object>> filtered = filterCatalogByAllowed(catalog, allowed);

        String ensureNorm = ensureCycleCode != null && !ensureCycleCode.isBlank()

                ? SettlementPeriodResolver.normalizeCalcCycle(ensureCycleCode.trim()) : "";

        if (!ensureNorm.isEmpty() && !allowed.contains(ensureNorm)) {

            appendCatalogRowForCode(catalog, ensureNorm).ifPresent(row -> {

                filtered.add(row);

                out.put("orphanSavedCycleYn", "Y");

            });

        }

        out.put("options", filtered);

        out.put("scoped", true);

        defaultCodeFromEntity(cfg).ifPresent(d -> out.put("defaultCalcCycle", d));

        orgUnitRepository.findById(md.get()).ifPresent(ou -> {

            out.put("masterDistOrgUnitId", ou.getId());

            if (ou.getCode() != null && !ou.getCode().isBlank()) {

                out.put("masterDistCompId", ou.getCode().trim());

            }

            if (ou.getName() != null && !ou.getName().isBlank()) {

                out.put("masterDistCompNm", ou.getName().trim());

            }

        });

        return out;

    }



    private static List<Map<String, Object>> filterCatalogByAllowed(List<Map<String, Object>> catalog, Set<String> allowed) {

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

        return filtered;

    }



    private Optional<Map<String, Object>> appendCatalogRowForCode(List<Map<String, Object>> catalog, String normCode) {

        for (Map<String, Object> row : catalog) {

            String v = row.get("v") != null ? String.valueOf(row.get("v")).trim() : "";

            if (v.isEmpty()) {

                continue;

            }

            if (SettlementPeriodResolver.normalizeCalcCycle(v).equals(normCode)) {

                return Optional.of(new LinkedHashMap<>(row));

            }

        }

        return Optional.empty();

    }



    /** {@code startOrgUnitId}: 가맹 조직 ID(권장) 또는 상위 조직 ID — 상위로 올라가며 총판 허용 목록을 찾습니다. */
    public void validateMerchantCalcCycle(Long startOrgUnitId, String calcCycleRaw) {

        if (calcCycleRaw == null || calcCycleRaw.isBlank()) {

            return;

        }

        String code = SettlementPeriodResolver.normalizeCalcCycle(calcCycleRaw.trim());

        Set<String> allowed = allowedCodesForMerchantParent(startOrgUnitId);

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

        while (slots.size() < MAX_DISTINCT_SLOTS) {

            slots.add(null);

        }

        if (slots.size() > MAX_DISTINCT_SLOTS) {

            throw new IllegalArgumentException("정산주기는 최대 " + MAX_DISTINCT_SLOTS + "개까지 지정할 수 있습니다.");

        }

        LinkedHashSet<String> normalizedDistinct = new LinkedHashSet<>();

        List<String> cleaned = new ArrayList<>(Collections.nCopies(MAX_DISTINCT_SLOTS, null));

        Set<String> catalogNorm = new HashSet<>();

        for (Map<String, Object> row : hqSettlementCycleAdminService.listMergedDefinitions()) {

            String v = row.get("cycleCode") != null ? String.valueOf(row.get("cycleCode")).trim() : "";

            if (!v.isEmpty()) {

                catalogNorm.add(SettlementPeriodResolver.normalizeCalcCycle(v));

            }

        }

        /* 이미 이 총판에 저장된 코드는 병합 정의에서 빠져도 유지·재저장 가능(표시 누락 방지). */
        configRepository.findByOrgUnitId(masterDistOrgUnitId).ifPresent(ex -> {

            for (String legacy : slotValuesRaw(ex)) {

                if (legacy != null && !legacy.isBlank()) {

                    catalogNorm.add(SettlementPeriodResolver.normalizeCalcCycle(legacy.trim()));

                }

            }

        });

        int nonEmpty = 0;

        for (int i = 0; i < MAX_DISTINCT_SLOTS; i++) {

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

        if (defaultSlotIndex < 0 || defaultSlotIndex >= MAX_DISTINCT_SLOTS) {

            throw new IllegalArgumentException("대표 슬롯은 표의 " + MAX_DISTINCT_SLOTS + "칸 중 하나(내부 인덱스 0~" + (MAX_DISTINCT_SLOTS - 1) + ")를 선택해야 합니다.");

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

        entity.setCycleCode6(cleaned.get(5));

        entity.setCycleCode7(cleaned.get(6));

        entity.setCycleCode8(cleaned.get(7));

        entity.setCycleCode9(cleaned.get(8));

        entity.setCycleCode10(cleaned.get(9));

        entity.setDefaultSlot(defaultSlotIndex);

        return configRepository.save(entity);

    }



    public Map<String, Object> toApiMap(MasterDistSettlementCycleConfig c) {

        Map<String, Object> m = new LinkedHashMap<>();

        m.put("orgUnitId", c.getOrgUnitId());

        m.put("slots", new ArrayList<>(slotValuesRaw(c)));

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

