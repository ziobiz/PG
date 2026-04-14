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



    /**

     * 본사 직속 가맹·총판에 허용 주기가 아직 없을 때: 활성(Y) 정의에 있는 코드만 셀렉트·저장 허용(순서 고정).

     */

    private static final List<String> MERCHANT_FALLBACK_CYCLE_NORMS = List.of(

            "NONE", "RT", "T0", "D0", "D1", "D2", "D3", "D7", "W3", "W7", "M30", "H1", "M5", "M10", "H12");



    private static Set<String> merchantFallbackAllowNormSet() {

        return Set.copyOf(MERCHANT_FALLBACK_CYCLE_NORMS);

    }



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



    /**

     * 가맹 정산주기 저장 검증·API와 동일한 허용 코드(정규화).

     * 총판에 슬롯이 있으면 그 코드만, 없거나 본사 직속이면 본사 필수 정산주기 코드만.

     */

    public Set<String> allowedCodesForMerchantParent(Long merchantParentOrgUnitId) {

        return effectiveMerchantCalcCycleAllowedNorms(merchantParentOrgUnitId);

    }



    public Set<String> effectiveMerchantCalcCycleAllowedNorms(Long startOrgUnitId) {

        if (startOrgUnitId == null) {

            return merchantFallbackAllowNormSet();

        }

        Optional<Long> md = findNearestMasterDistOrgId(startOrgUnitId);

        if (md.isEmpty()) {

            return merchantFallbackAllowNormSet();

        }

        Optional<MasterDistSettlementCycleConfig> cfgOpt = findByMasterDistOrgId(md.get());

        if (cfgOpt.isEmpty()) {

            return merchantFallbackAllowNormSet();

        }

        Set<String> fromCfg = allowedCodesFromEntity(cfgOpt.get());

        if (fromCfg.isEmpty()) {

            return merchantFallbackAllowNormSet();

        }

        return fromCfg;

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

        if (c == null) {

            return new ArrayList<>(Collections.nCopies(MAX_DISTINCT_SLOTS, null));

        }

        List<String> out = new ArrayList<>(MAX_DISTINCT_SLOTS);

        out.add(c.getCycleCode1());

        out.add(c.getCycleCode2());

        out.add(c.getCycleCode3());

        out.add(c.getCycleCode4());

        out.add(c.getCycleCode5());

        out.add(c.getCycleCode6());

        out.add(c.getCycleCode7());

        out.add(c.getCycleCode8());

        out.add(c.getCycleCode9());

        out.add(c.getCycleCode10());

        return out;

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

     * 가맹 정산주기 셀렉트 옵션. 총판 허용 슬롯이 있으면 그 코드만(슬롯 순·활성 Y),

     * 본사 직속·총판 미설정·슬롯 비어 있으면 본사 필수 코드만(활성 Y).

     *

     * @param startOrgUnitId 가맹 orgUnitId 권장(상위로 총판 탐색). null이면 필수 코드만.

     * @param ensureCycleCode 저장값이 목록에 없으면 병합 카탈로그 행을 한 줄 덧붙이고 {@code orphanSavedCycleYn}=Y.

     */

    public Map<String, Object> buildScopedCycleOptionsForMerchantParent(Long startOrgUnitId, String ensureCycleCode) {

        List<Map<String, Object>> active = hqSettlementCycleAdminService.listActiveSelectOptions();

        List<Map<String, Object>> catalog = hqSettlementCycleAdminService.listCatalogSelectOptions();

        Map<String, Object> out = new LinkedHashMap<>();

        out.put("scoped", true);

        out.put("defaultCalcCycle", null);

        out.put("orphanSavedCycleYn", "N");

        String ensureNorm = ensureCycleCode != null && !ensureCycleCode.isBlank()

                ? SettlementPeriodResolver.normalizeCalcCycle(ensureCycleCode.trim()) : "";

        if (startOrgUnitId == null) {

            List<Map<String, Object>> opts = buildMandatoryMerchantActiveOptions(active);

            appendEnsureIfNotInOptions(opts, catalog, ensureNorm, out);

            out.put("options", opts);

            out.put("scopeHint", "조직 기준 미지정: 필수 정산주기만 표시합니다.");

            return out;

        }

        Optional<Long> md = findNearestMasterDistOrgId(startOrgUnitId);

        if (md.isEmpty()) {

            List<Map<String, Object>> opts = buildMandatoryMerchantActiveOptions(active);

            appendEnsureIfNotInOptions(opts, catalog, ensureNorm, out);

            out.put("options", opts);

            out.put("scopeHint", "본사 직속 가맹: 필수 정산주기만 선택할 수 있습니다.");

            return out;

        }

        Optional<MasterDistSettlementCycleConfig> cfgOpt = findByMasterDistOrgId(md.get());

        MasterDistSettlementCycleConfig cfg = cfgOpt.orElse(null);

        Set<String> allowedFromCfg = cfg != null ? allowedCodesFromEntity(cfg) : Set.of();

        List<Map<String, Object>> filtered;

        if (cfg != null && !allowedFromCfg.isEmpty()) {

            filtered = buildMasterDistSlotOrderedActiveOptions(cfg, active);

            defaultCodeFromEntity(cfg).ifPresent(d -> out.put("defaultCalcCycle", d));

        } else {

            filtered = buildMandatoryMerchantActiveOptions(active);

            out.put("scopeHint", "총판 허용 주기가 없어 필수 정산주기만 표시합니다.");

        }

        appendEnsureIfNotInOptions(filtered, catalog, ensureNorm, out);

        out.put("options", filtered);

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



    private static void appendFirstBlankPlaceholder(List<Map<String, Object>> active, List<Map<String, Object>> out) {

        for (Map<String, Object> row : active) {

            if (row == null) {

                continue;

            }

            String v = row.get("v") != null ? String.valueOf(row.get("v")).trim() : "";

            if (v.isEmpty()) {

                out.add(new LinkedHashMap<>(row));

                return;

            }

        }

    }



    private static Optional<Map<String, Object>> findActiveOptionRow(List<Map<String, Object>> active, String norm) {

        for (Map<String, Object> row : active) {

            if (row == null) {

                continue;

            }

            String v = row.get("v") != null ? String.valueOf(row.get("v")).trim() : "";

            if (v.isEmpty()) {

                continue;

            }

            if (SettlementPeriodResolver.normalizeCalcCycle(v).equals(norm)) {

                return Optional.of(row);

            }

        }

        return Optional.empty();

    }



    private static boolean optionsContainNorm(List<Map<String, Object>> opts, String norm) {

        for (Map<String, Object> row : opts) {

            if (row == null) {

                continue;

            }

            String v = row.get("v") != null ? String.valueOf(row.get("v")).trim() : "";

            if (v.isEmpty()) {

                continue;

            }

            if (SettlementPeriodResolver.normalizeCalcCycle(v).equals(norm)) {

                return true;

            }

        }

        return false;

    }



    private static List<Map<String, Object>> buildMandatoryMerchantActiveOptions(List<Map<String, Object>> active) {

        List<Map<String, Object>> out = new ArrayList<>();

        appendFirstBlankPlaceholder(active, out);

        for (String norm : MERCHANT_FALLBACK_CYCLE_NORMS) {

            findActiveOptionRow(active, norm).ifPresent(r -> out.add(new LinkedHashMap<>(r)));

        }

        return out;

    }



    private List<Map<String, Object>> buildMasterDistSlotOrderedActiveOptions(MasterDistSettlementCycleConfig cfg,

                                                                              List<Map<String, Object>> active) {

        List<Map<String, Object>> out = new ArrayList<>();

        appendFirstBlankPlaceholder(active, out);

        LinkedHashSet<String> seen = new LinkedHashSet<>();

        for (String raw : slotValuesRaw(cfg)) {

            if (raw == null || raw.isBlank()) {

                continue;

            }

            String norm = SettlementPeriodResolver.normalizeCalcCycle(raw.trim());

            if (!seen.add(norm)) {

                continue;

            }

            findActiveOptionRow(active, norm).ifPresent(r -> out.add(new LinkedHashMap<>(r)));

        }

        return out;

    }



    private void appendEnsureIfNotInOptions(List<Map<String, Object>> opts,

                                            List<Map<String, Object>> catalog,

                                            String ensureNorm,

                                            Map<String, Object> out) {

        if (ensureNorm == null || ensureNorm.isEmpty()) {

            return;

        }

        if (optionsContainNorm(opts, ensureNorm)) {

            return;

        }

        appendCatalogRowForCode(catalog, ensureNorm).ifPresent(row -> {

            opts.add(row);

            out.put("orphanSavedCycleYn", "Y");

        });

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

        Set<String> allowed = effectiveMerchantCalcCycleAllowedNorms(startOrgUnitId);

        if (!allowed.contains(code)) {

            throw new IllegalArgumentException("선택할 수 없는 정산주기입니다: " + code);

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

            if (row == null) {

                continue;

            }

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

        if (nonEmpty < 2) {

            throw new IllegalArgumentException("서로 다른 정산주기는 최소 2개 이상 지정하세요.");

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

