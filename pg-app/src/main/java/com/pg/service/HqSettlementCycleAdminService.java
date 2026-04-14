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
            new BuiltInRow("RT", "실시간", "정산구분 AUTO일 때 승인(결제완료) 노티마다 해당 건만 집계한 정산 실행 1건을 추가합니다(건당 마감, 당일 0시~현재 합산 1행으로 바꾸지 않음).", 2, true),
            new BuiltInRow("T0", "당일합산(승인 시 재집계)", "정산구분 AUTO일 때 승인 노티마다 당일 00:00~현재까지 전체를 재집계하여 정산일 당일 행 1건으로 갱신합니다(당일 누적 표시).", 3, true),
            new BuiltInRow("M5", "5분 마감", "5분 격자 정각마다 직전 5분 구간의 거래를 합산해 정산 실행 1건으로 마감합니다(RT로 이미 정산된 승인은 제외).", 4, true),
            new BuiltInRow("M10", "10분 마감", "10분 격자마다 직전 10분 구간을 합산해 정산 실행 1건.", 5, true),
            new BuiltInRow("M30", "30분 마감", "30분 격자마다 직전 30분 구간을 합산해 정산 실행 1건.", 6, true),
            new BuiltInRow("H1", "1시간(H1)", "매시 정각(HH:00)에 직전 1시간 구간을 합산해 정산 실행 1건. AUTO 배치는 매분 크론에서 격자 정각에만 실행됩니다.", 7, true),
            new BuiltInRow("H2", "2시간(H2)", "0·2·4…시 00분에 직전 2시간 구간을 합산해 정산 실행 1건.", 8, true),
            new BuiltInRow("H4", "4시간(H4)", "0·4·8…시 00분에 직전 4시간 구간을 합산해 정산 실행 1건.", 9, true),
            new BuiltInRow("H6", "6시간(H6)", "0·6·12·18시 00분에 직전 6시간 구간을 합산해 정산 실행 1건.", 10, true),
            new BuiltInRow("H8", "8시간(H8)", "0·8·16시 00분에 직전 8시간 구간을 합산해 정산 실행 1건.", 11, true),
            new BuiltInRow("H12", "12시간(H12)", "0·12시 00분에 직전 12시간 구간을 합산해 정산 실행 1건.", 12, true),
            new BuiltInRow("TM5", "당일합산·5분 격자", "M5와 동일한 5분 격자 시각에 배치되나, T0처럼 당일 00:00~현재 전체를 재집계해 정산일 당일 행 1건으로 갱신합니다. 저장값 TM05는 TM5로 통일됩니다.", 120, true),
            new BuiltInRow("TM10", "당일합산·10분 격자", "M10과 동일 격자, 당일 0시~현재 합산 1행 재집계(T0식).", 121, true),
            new BuiltInRow("TM30", "당일합산·30분 격자", "M30과 동일 격자, 당일 0시~현재 합산 1행 재집계(T0식).", 122, true),
            new BuiltInRow("TH1", "당일합산·1시간 격자", "H1과 동일 격자, 당일 0시~현재 합산 1행 재집계(T0식).", 123, true),
            new BuiltInRow("TH2", "당일합산·2시간 격자", "H2와 동일 격자, 당일 0시~현재 합산 1행 재집계(T0식).", 124, true),
            new BuiltInRow("TH4", "당일합산·4시간 격자", "H4와 동일 격자, 당일 0시~현재 합산 1행 재집계(T0식).", 125, true),
            new BuiltInRow("TH6", "당일합산·6시간 격자", "H6와 동일 격자, 당일 0시~현재 합산 1행 재집계(T0식).", 126, true),
            new BuiltInRow("TH8", "당일합산·8시간 격자", "H8와 동일 격자, 당일 0시~현재 합산 1행 재집계(T0식).", 127, true),
            new BuiltInRow("TH12", "당일합산·12시간 격자", "H12와 동일 격자, 당일 0시~현재 합산 1행 재집계(T0식).", 128, true),
            new BuiltInRow("D0", "D+0", "정산일(달력 당일)에 집계 구간(당일 0시~24시) 거래를 합산해 정산 실행 1건. 자동 배치는 서울 기준 당일 00:00~23:50 구간에서만 실행되며, 정산마감시간이 있으면 그 이후부터 위 구간 안에서만 실행됩니다.", 13, true),
            new BuiltInRow("D1", "D+1", "정산일 당일에 집계 기준일(정산일에서 1영업일 역산한 하루) 구간을 합산해 정산 실행 1건. ‘전일’이 아니라 정산일·집계기준일 관계입니다.", 14, true),
            new BuiltInRow("D2", "D+2", "정산일 당일에 집계 기준일(정산일에서 2영업일 역산한 하루) 구간을 합산해 정산 실행 1건.", 15, true),
            new BuiltInRow("D3", "D+3", "정산일 당일에 집계 기준일(정산일에서 3영업일 역산한 하루) 구간을 합산해 정산 실행 1건.", 16, true),
            new BuiltInRow("D5", "D+5", "정산일 당일에 집계 기준일(정산일에서 5영업일 역산한 하루) 구간을 합산해 정산 실행 1건.", 17, true),
            new BuiltInRow("D7", "D+7", "정산일 당일에 집계 기준일(정산일에서 7영업일 역산한 하루) 구간을 합산해 정산 실행 1건.", 18, true),
            new BuiltInRow("D10", "D+10", "정산일 당일에 집계 기준일(정산일에서 10영업일 역산한 하루) 구간을 합산해 정산 실행 1건.", 19, true),
            new BuiltInRow("D15", "D+15", "정산일 당일에 집계 기준일(정산일에서 15영업일 역산한 하루) 구간을 합산해 정산 실행 1건.", 20, true),
            new BuiltInRow("D20", "D+20", "정산일 당일에 집계 기준일(정산일에서 20영업일 역산한 하루) 구간을 합산해 정산 실행 1건.", 21, true),
            new BuiltInRow("D30", "D+30", "정산일 당일에 집계 기준일(정산일에서 30영업일 역산한 하루) 구간을 합산해 정산 실행 1건.", 22, true),
            new BuiltInRow("W3", "W+3", "직전 주(월~일) 집계 구간이 정산일에 마감될 때 구간 전체를 합산해 정산 실행 1건(주 종료 후 3영업일째 정산일 규칙).", 30, true),
            new BuiltInRow("W5", "W+5", "전주 집계 구간 + 5영업일 오프셋으로 정산일이 정해지면 해당 구간을 합산해 정산 실행 1건.", 31, true),
            new BuiltInRow("W7", "W+7", "전주 집계 구간 + 7영업일 오프셋으로 정산일이 정해지면 해당 구간을 합산해 정산 실행 1건.", 32, true),
            new BuiltInRow("W10", "W+10", "전주 집계 구간 + 10영업일 오프셋으로 정산일이 정해지면 해당 구간을 합산해 정산 실행 1건.", 33, true),
            new BuiltInRow("W14", "W+14", "전주 집계 구간 + 14영업일 오프셋으로 정산일이 정해지면 해당 구간을 합산해 정산 실행 1건.", 34, true),
            new BuiltInRow("WK1W", "WK+1W", "WK+1W → 마감 후 영업일 3일째", 40, true),
            new BuiltInRow("WK2W", "WK+2W", "WK+2W → 격주 2주 마감 후 영업일 3일째", 41, true),
            new BuiltInRow("WK1WT", "WK+1WT", "WK+1WT → 마감 후 영업일 10일째", 42, true),
            new BuiltInRow("WK2WT", "WK+2WT", "WK+2WT → 격주 2주 마감 후 영업일 10일째", 43, true),
            new BuiltInRow("WK1WM", "WK+1WM", "WK+1WM (WK1WM): 1주(월~일) 마감 후 영업일 30일째 정산", 44, true),
            new BuiltInRow("WK2WM", "WK+2WM", "WK+2WM (WK2WM): 격주 2주 마감 후 영업일 30일째 정산", 45, true)
    );

    private static final Pattern P_D = Pattern.compile("^D(\\d{1,3})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_W = Pattern.compile("^W(\\d{1,2})$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> WK_CODES = Set.of("WK1W", "WK2W", "WK1WT", "WK2WT", "WK1WM", "WK2WM");
    /** D+N / W+N / WK 가 아닌 일중(intraday) 코드 — 커스텀 주기 검증용 */
    private static final Set<String> INTRADAY_CODES = Set.of(
            "RT", "T0", "M5", "M10", "M30",
            "H1", "H2", "H4", "H6", "H8", "H12",
            "TM5", "TM10", "TM30", "TH1", "TH2", "TH4", "TH6", "TH8", "TH12");

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

    /**
     * 총판별 허용 주기 설정 등 관리용 — 병합 정의 전체(미사용 N 포함).
     * <strong>순서</strong>는 {@link #listMergedDefinitions()}와 동일(정산주기관리 화면의 표준주기·DB등록 표 행 순서).
     * <strong>표시(t)</strong>는 코드 + 표시명(표의 코드·표시 열과 동일한 정보)이며 비활성 시 접미 (미사용).
     * 가맹 셀렉트용 {@link #listActiveSelectOptions()}는 사용(Y)만·sortOrder 정렬을 유지한다.
     */
    public List<Map<String, Object>> listCatalogSelectOptions() {
        return listMergedDefinitions().stream()
                .map(m -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("v", m.get("cycleCode"));
                    String code = m.get("cycleCode") != null ? String.valueOf(m.get("cycleCode")).trim() : "";
                    String label = m.get("displayLabel") != null ? String.valueOf(m.get("displayLabel")).trim() : "";
                    if (label.isEmpty()) {
                        label = code;
                    }
                    if (StringUtils.hasText(code) && !"Y".equals(String.valueOf(m.getOrDefault("activeYn", "Y")))) {
                        label = label + " (미사용)";
                    }
                    String t;
                    if (!StringUtils.hasText(code)) {
                        t = label;
                    } else if (label.equalsIgnoreCase(code)) {
                        t = code;
                    } else {
                        t = code + " — " + label;
                    }
                    o.put("t", t);
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
        if (INTRADAY_CODES.contains(c)) {
            return;
        }
        throw new IllegalArgumentException("해석기가 지원하지 않는 코드입니다: " + c
                + " (D0~D90, W1~W28, WK*, RT·T0·M5·M10·M30·TM5·TM10·TM30, H1·H2·H4·H6·H8·H12·TH1·TH2·TH4·TH6·TH8·TH12만 추가 가능)");
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
