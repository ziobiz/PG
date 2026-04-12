package com.pg.service;

import com.pg.entity.HqSettlementCycleDef;
import com.pg.repository.HqSettlementCycleDefRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.settlement.SettlementPeriodResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 본사 정산관리설정 — 기본 정산주기 정의와 DB 오버레이(표시명·설명·순서·사용) 병합.
 */
@Service
public class HqSettlementCycleAdminService {

    public record BuiltInRow(String cycleCode, String displayLabel, String description, int sortOrder, boolean builtInMarker) {}

    private static final List<BuiltInRow> BUILT_INS = List.of(
            new BuiltInRow("", "선택", "저장 시 실제 정산주기를 고릅니다.", 0, true),
            new BuiltInRow("NONE", "정산안함", "자동 정산 배치에서 제외됩니다.", 1, true),
            new BuiltInRow("RT", "실시간", "정산구분 AUTO일 때 승인(결제완료) 노티 직후 당일 00:00~현재까지 재집계하여 정산일 당일 행을 갱신합니다.", 2, true),
            new BuiltInRow("T0", "T0", "RT와 동일하게 승인 직후 당일 누적 자동정산에 사용합니다.", 3, true),
            new BuiltInRow("M5", "5분", "AUTO 배치가 매분 돌 때, 5분 격자 시작 정각(0·5·10···분)에 당일 누적을 재집계합니다.", 4, true),
            new BuiltInRow("M10", "10분", "AUTO 배치가 매분 돌 때, 10분 격자 시작 정각(0·10·20···분)에 당일 누적을 재집계합니다.", 5, true),
            new BuiltInRow("H1", "1시간", "AUTO 배치가 매분 돌 때, 매시 정각(HH:00)에 당일 누적을 재집계합니다.", 6, true),
            new BuiltInRow("H2", "2시간", "AUTO 배치가 매분 돌 때, 2시간 격자 시작 정각(짝수 시 00분)에 당일 누적을 재집계합니다.", 7, true),
            new BuiltInRow("H4", "4시간", "AUTO 배치가 매분 돌 때, 4시간 격자 시작 정각(0·4·8···시 00분)에 당일 누적을 재집계합니다.", 8, true),
            new BuiltInRow("D0", "D+0", "정산일(달력 당일) 당일 하루 승인분을 집계합니다. 자동 배치는 서울 기준 당일 00:00~23:50 구간에서만 실행되며, 정산마감시간이 있으면 그 이후부터 위 구간 안에서만 실행됩니다.", 9, true),
            new BuiltInRow("D1", "D+1", "정산일 당일에 마감·배치로 처리합니다. 집계 기준일=정산일에서 1영업일 역산한 하루(주말 제외). ‘전일’이 아니라 정산일·집계기준일 관계입니다.", 10, true),
            new BuiltInRow("D2", "D+2", "정산일 당일 배치. 집계 기준일=정산일에서 2영업일 역산한 하루(주말 제외).", 11, true),
            new BuiltInRow("D3", "D+3", "정산일 당일 배치. 집계 기준일=정산일에서 3영업일 역산한 하루(주말 제외).", 12, true),
            new BuiltInRow("D5", "D+5", "정산일 당일 배치. 집계 기준일=정산일에서 5영업일 역산한 하루(주말 제외).", 13, true),
            new BuiltInRow("D7", "D+7", "정산일 당일 배치. 집계 기준일=정산일에서 7영업일 역산한 하루(주말 제외).", 14, true),
            new BuiltInRow("D10", "D+10", "정산일 당일 배치. 집계 기준일=정산일에서 10영업일 역산한 하루(주말 제외).", 15, true),
            new BuiltInRow("D15", "D+15", "정산일 당일 배치. 집계 기준일=정산일에서 15영업일 역산한 하루(주말 제외).", 16, true),
            new BuiltInRow("D20", "D+20", "정산일 당일 배치. 집계 기준일=정산일에서 20영업일 역산한 하루(주말 제외).", 17, true),
            new BuiltInRow("D30", "D+30", "정산일 당일 배치. 집계 기준일=정산일에서 30영업일 역산한 하루(주말 제외).", 18, true),
            new BuiltInRow("W3", "W+3", "직전 주(월~일) 구간을 정산하고, 주 종료 후 3영업일째 되는 날이 정산일일 때 실행됩니다.", 30, true),
            new BuiltInRow("W5", "W+5", "전주 구간 + 5영업일 오프셋 규칙입니다.", 31, true),
            new BuiltInRow("W7", "W+7", "전주 구간 + 7영업일 오프셋 규칙입니다.", 32, true),
            new BuiltInRow("W10", "W+10", "전주 구간 + 10영업일 오프셋 규칙입니다.", 33, true),
            new BuiltInRow("W14", "W+14", "전주 구간 + 14영업일 오프셋 규칙입니다.", 34, true),
            new BuiltInRow("WK1W", "WK+1W", "전주 기준, 주 종료 후 수요일+1주(영업일 보정)에 맞춰 격주 아님 주에 실행됩니다.", 40, true),
            new BuiltInRow("WK2W", "WK+2W", "2주(격주) 단위 전주 묶음에 대해 동일 규칙으로 실행됩니다.", 41, true),
            new BuiltInRow("WK1WT", "WK+1WT", "WK+1W 변형(수요일 오프셋이 다름). 자동 창은 SettlementPeriodResolver 규칙을 따릅니다.", 42, true),
            new BuiltInRow("WK2WT", "WK+2WT", "WK+2W 변형(수요일 오프셋이 다름).", 43, true)
    );

    private static final Pattern P_D = Pattern.compile("^D(\\d{1,3})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_W = Pattern.compile("^W(\\d{1,2})$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> WK_CODES = Set.of("WK1W", "WK2W", "WK1WT", "WK2WT");
    private static final Set<String> RT_FAMILY = Set.of("RT", "T0", "M5", "M10", "H1", "H2", "H4");

    private final HqSettlementCycleDefRepository cycleDefRepository;
    private final SettlementSettingRepository settlementSettingRepository;

    public HqSettlementCycleAdminService(HqSettlementCycleDefRepository cycleDefRepository,
                                         SettlementSettingRepository settlementSettingRepository) {
        this.cycleDefRepository = cycleDefRepository;
        this.settlementSettingRepository = settlementSettingRepository;
    }

    /** 셀렉트 박스용 — 사용(Y)만, 빈 값·NONE 포함 */
    public List<Map<String, Object>> listActiveSelectOptions() {
        return listMergedDefinitions().stream()
                .filter(m -> "Y".equals(String.valueOf(m.getOrDefault("activeYn", "Y"))))
                .sorted(Comparator.<Map<String, Object>, Integer>comparing(m -> (Integer) m.get("sortOrder"))
                        .thenComparing(m -> String.valueOf(m.get("cycleCode"))))
                .map(m -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("v", m.get("cycleCode"));
                    o.put("t", m.get("displayLabel"));
                    o.put("d", m.get("description"));
                    return o;
                })
                .collect(Collectors.toList());
    }

    /** 본사 화면 — 기본+DB 병합 목록 */
    public List<Map<String, Object>> listMergedDefinitions() {
        Map<String, HqSettlementCycleDef> dbByNorm = cycleDefRepository.findAll().stream()
                .filter(d -> d.getCycleCode() != null)
                .collect(Collectors.toMap(
                        d -> SettlementPeriodResolver.normalizeCalcCycle(d.getCycleCode()),
                        d -> d,
                        (a, b) -> a.getId() != null && b.getId() != null && a.getId() < b.getId() ? a : b));

        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> consumed = new HashSet<>();
        for (BuiltInRow b : BUILT_INS) {
            String norm = SettlementPeriodResolver.normalizeCalcCycle(b.cycleCode());
            HqSettlementCycleDef db = dbByNorm.get(norm);
            if (db != null) {
                consumed.add(norm);
            }
            out.add(buildRow(db != null ? db.getId() : null, b, db));
        }
        dbByNorm.entrySet().stream()
                .filter(e -> !consumed.contains(e.getKey()))
                .sorted(Comparator.comparingInt((Map.Entry<String, HqSettlementCycleDef> e) -> e.getValue().getSortOrder())
                        .thenComparing(Map.Entry::getKey))
                .forEach(e -> {
                    HqSettlementCycleDef db = e.getValue();
                    BuiltInRow synthetic = new BuiltInRow(db.getCycleCode(),
                            firstNonBlank(db.getDisplayLabel(), db.getCycleCode()),
                            "",
                            db.getSortOrder(),
                            false);
                    out.add(buildRow(db.getId(), synthetic, db));
                });
        return out;
    }

    private static Map<String, Object> buildRow(Long dbId, BuiltInRow bin, HqSettlementCycleDef db) {
        String label = db != null && StringUtils.hasText(db.getDisplayLabel()) ? db.getDisplayLabel().trim() : bin.displayLabel();
        String desc = db != null && StringUtils.hasText(db.getDescription()) ? db.getDescription().trim() : bin.description();
        int sort = db != null ? db.getSortOrder() : bin.sortOrder();
        String active = "Y";
        if (db != null && StringUtils.hasText(db.getActiveYn())) {
            active = "Y".equalsIgnoreCase(db.getActiveYn().trim()) ? "Y" : "N";
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", dbId);
        m.put("cycleCode", bin.cycleCode());
        m.put("displayLabel", label);
        m.put("description", desc);
        m.put("sortOrder", sort);
        m.put("activeYn", active);
        m.put("fromDb", db != null);
        m.put("builtIn", bin.builtInMarker());
        m.put("deletable", dbId != null);
        return m;
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        return b != null ? b : "";
    }

    public Map<String, Long> autoMerchantCountByCycle() {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : settlementSettingRepository.countAutoMerchantsByCalcCycleNative()) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String code = String.valueOf(row[0]).trim();
            long n = row[1] instanceof Number ? ((Number) row[1]).longValue() : Long.parseLong(String.valueOf(row[1]));
            map.put(SettlementPeriodResolver.normalizeCalcCycle(code), n);
        }
        return map;
    }

    public List<Map<String, Object>> schedulePreview(LocalDate from, LocalDate to) {
        LocalDate f = from != null ? from : LocalDate.now();
        LocalDate t = to != null ? to : f.plusDays(13);
        if (t.isBefore(f)) {
            return List.of();
        }
        Map<String, Long> cnt = autoMerchantCountByCycle();
        List<String> codes = listActiveSelectOptions().stream()
                .map(m -> String.valueOf(m.getOrDefault("v", "")).trim())
                .filter(s -> !s.isEmpty() && !"NONE".equalsIgnoreCase(s))
                .distinct()
                .collect(Collectors.toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = f; !d.isAfter(t); d = d.plusDays(1)) {
            for (String code : codes) {
                SettlementPeriodResolver.PeriodWindow w = SettlementPeriodResolver.resolveAutoPeriodWindow(code, d);
                if (w != null) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("settleDate", d.toString());
                    row.put("cycleCode", code);
                    row.put("periodFrom", w.fromDate().toString());
                    row.put("periodTo", w.toDate().toString());
                    row.put("autoMerchantCount", cnt.getOrDefault(SettlementPeriodResolver.normalizeCalcCycle(code), 0L));
                    rows.add(row);
                }
            }
        }
        rows.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("settleDate")))
                .thenComparing(m -> String.valueOf(m.get("cycleCode"))));
        return rows;
    }

    @Transactional
    public HqSettlementCycleDef createCustom(String family, Integer offset, String wkKey,
                                             String displayLabel, String description, int sortOrder, String activeYn) {
        String code = buildCycleCode(family, offset, wkKey);
        validateCycleCode(code);
        String norm = SettlementPeriodResolver.normalizeCalcCycle(code);
        if (cycleDefRepository.findByCycleCodeIgnoreCase(norm).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 정산주기 코드입니다: " + norm);
        }
        HqSettlementCycleDef e = new HqSettlementCycleDef();
        e.setCycleCode(norm);
        e.setDisplayLabel(StringUtils.hasText(displayLabel) ? displayLabel.trim() : suggestLabel(norm));
        e.setDescription(description != null ? description.trim() : "");
        e.setSortOrder(sortOrder);
        e.setActiveYn("N".equalsIgnoreCase(activeYn != null ? activeYn.trim() : "") ? "N" : "Y");
        return cycleDefRepository.save(e);
    }

    @Transactional
    public void updateRow(long id, String displayLabel, String description, Integer sortOrder, String activeYn) {
        HqSettlementCycleDef e = cycleDefRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("정산주기 정의를 찾을 수 없습니다."));
        if (StringUtils.hasText(displayLabel)) {
            e.setDisplayLabel(displayLabel.trim());
        }
        if (description != null) {
            e.setDescription(description.trim());
        }
        if (sortOrder != null) {
            e.setSortOrder(sortOrder);
        }
        if (StringUtils.hasText(activeYn)) {
            e.setActiveYn("N".equalsIgnoreCase(activeYn.trim()) ? "N" : "Y");
        }
        cycleDefRepository.save(e);
    }

    @Transactional
    public void deleteRow(long id) {
        if (!cycleDefRepository.existsById(id)) {
            throw new IllegalArgumentException("삭제할 행이 없습니다.");
        }
        cycleDefRepository.deleteById(id);
    }

    /**
     * 표준 정산주기(내장 목록) 중 아직 {@code tb_hq_settlement_cycle_def} 에 없는 코드만 INSERT.
     * DB가 비었거나 일부만 남은 경우 본사 화면에서 복원할 때 사용한다.
     *
     * @return 새로 삽입한 행 수
     */
    @Transactional
    public int seedMissingStandardCycleDefs() {
        int added = 0;
        for (BuiltInRow b : BUILT_INS) {
            if (!StringUtils.hasText(b.cycleCode())) {
                continue;
            }
            String norm = SettlementPeriodResolver.normalizeCalcCycle(b.cycleCode());
            if (!StringUtils.hasText(norm)) {
                continue;
            }
            if (cycleDefRepository.findByCycleCodeIgnoreCase(norm).isPresent()) {
                continue;
            }
            HqSettlementCycleDef e = new HqSettlementCycleDef();
            e.setCycleCode(norm);
            e.setDisplayLabel(b.displayLabel());
            e.setDescription(b.description());
            e.setSortOrder(b.sortOrder());
            e.setActiveYn("Y");
            cycleDefRepository.save(e);
            added++;
        }
        return added;
    }

    private static String buildCycleCode(String family, Integer offset, String wkKey) {
        if (!StringUtils.hasText(family)) {
            throw new IllegalArgumentException("유형(일/주/WK)을 선택하세요.");
        }
        String f = family.trim().toUpperCase(Locale.ROOT);
        return switch (f) {
            case "D" -> {
                if (offset == null) {
                    throw new IllegalArgumentException("D+N 일수를 입력하세요.");
                }
                yield "D" + offset;
            }
            case "W" -> {
                if (offset == null) {
                    throw new IllegalArgumentException("W+N 일수를 입력하세요.");
                }
                yield "W" + offset;
            }
            case "WK" -> {
                if (!StringUtils.hasText(wkKey)) {
                    throw new IllegalArgumentException("WK 코드를 선택하세요.");
                }
                yield wkKey.trim().toUpperCase(Locale.ROOT);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 유형입니다: " + f);
        };
    }

    public void validateCycleCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("정산주기 코드가 비어 있습니다.");
        }
        String c = SettlementPeriodResolver.normalizeCalcCycle(raw);
        if ("NONE".equals(c) || c.isEmpty()) {
            throw new IllegalArgumentException("NONE 또는 빈 코드는 여기서 등록하지 않습니다.");
        }
        var dm = P_D.matcher(c);
        if (dm.matches()) {
            int n = Integer.parseInt(dm.group(1), 10);
            if (n < 0 || n > 90) {
                throw new IllegalArgumentException("D+N 은 0~90만 허용됩니다.");
            }
            return;
        }
        var wm = P_W.matcher(c);
        if (wm.matches()) {
            int n = Integer.parseInt(wm.group(1), 10);
            if (n < 1 || n > 28) {
                throw new IllegalArgumentException("W+N 은 1~28만 허용됩니다.");
            }
            return;
        }
        if (WK_CODES.contains(c)) {
            return;
        }
        if (RT_FAMILY.contains(c)) {
            return;
        }
        throw new IllegalArgumentException("해석기가 지원하지 않는 코드입니다: " + c + " (D0~D90, W1~W28, WK*, RT/M/H 계열만 추가 가능)");
    }

    private static String suggestLabel(String norm) {
        var dm = P_D.matcher(norm);
        if (dm.matches()) {
            return "D+" + dm.group(1);
        }
        var wm = P_W.matcher(norm);
        if (wm.matches()) {
            return "W+" + wm.group(1);
        }
        return norm;
    }
}
