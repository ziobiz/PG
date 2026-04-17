package com.pg.service;

import com.pg.entity.HqSettlementCycleDef;
import com.pg.repository.HqSettlementCycleDefRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.settlement.SettlementCycleTiming;
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
            new BuiltInRow("", "선택", "주기 선택.", 0, true),
            new BuiltInRow("NONE", "정산안함", "자동 배치 제외.", 1, true),
            new BuiltInRow("RT", "실시간", "건별 1회.", 2, true),
            new BuiltInRow("T0", "당일합산(승인 시 재집계)", "당일 합산 1행.", 3, true),
            new BuiltInRow("M5", "5분 마감", "5분마다 1회.", 4, true),
            new BuiltInRow("M10", "10분 마감", "10분마다 1회.", 5, true),
            new BuiltInRow("M30", "30분 마감", "30분마다 1회.", 6, true),
            new BuiltInRow("H1", "1시간(H1)", "1시간마다, 하루 24회.", 7, true),
            new BuiltInRow("H2", "2시간(H2)", "2시간마다, 하루 12회.", 8, true),
            new BuiltInRow("H4", "4시간(H4)", "4시간마다, 하루 6회.", 9, true),
            new BuiltInRow("H6", "6시간(H6)", "6시간마다, 하루 4회.", 10, true),
            new BuiltInRow("H8", "8시간(H8)", "8시간마다, 하루 3회.", 11, true),
            new BuiltInRow("H12", "12시간(H12)", "12시간마다, 하루 2회.", 12, true),
            new BuiltInRow("TM5", "당일합산·5분 격자", "당일 누적, 5분 격자.", 120, true),
            new BuiltInRow("TM10", "당일합산·10분 격자", "당일 누적, 10분 격자.", 121, true),
            new BuiltInRow("TM30", "당일합산·30분 격자", "당일 누적, 30분 격자.", 122, true),
            new BuiltInRow("TH1", "당일합산·1시간 격자", "당일 누적, 1시간 격자.", 123, true),
            new BuiltInRow("TH2", "당일합산·2시간 격자", "당일 누적, 2시간 격자.", 124, true),
            new BuiltInRow("TH4", "당일합산·4시간 격자", "당일 누적, 4시간 격자.", 125, true),
            new BuiltInRow("TH6", "당일합산·6시간 격자", "당일 누적, 6시간 격자.", 126, true),
            new BuiltInRow("TH8", "당일합산·8시간 격자", "당일 누적, 8시간 격자.", 127, true),
            new BuiltInRow("TH12", "당일합산·12시간 격자", "당일 누적, 12시간 격자.", 128, true),
            new BuiltInRow("D0", "D+0", "하루 1회.", 13, true),
            new BuiltInRow("D1", "D+1", "영업일+1, 하루 1회.", 14, true),
            new BuiltInRow("D2", "D+2", "영업일+2, 하루 1회.", 15, true),
            new BuiltInRow("D3", "D+3", "영업일+3, 하루 1회.", 16, true),
            new BuiltInRow("D5", "D+5", "영업일+5, 하루 1회.", 17, true),
            new BuiltInRow("D7", "D+7", "영업일+7, 하루 1회.", 18, true),
            new BuiltInRow("D10", "D+10", "영업일+10, 하루 1회.", 19, true),
            new BuiltInRow("D15", "D+15", "영업일+15, 하루 1회.", 20, true),
            new BuiltInRow("D20", "D+20", "영업일+20, 하루 1회.", 21, true),
            new BuiltInRow("D30", "D+30", "영업일+30, 하루 1회.", 22, true),
            new BuiltInRow("W3", "W+3", "주 단위, 하루 1회.", 30, true),
            new BuiltInRow("W5", "W+5", "주 단위, 하루 1회.", 31, true),
            new BuiltInRow("W7", "W+7", "주 단위, 하루 1회.", 32, true),
            new BuiltInRow("W10", "W+10", "주 단위, 하루 1회.", 33, true),
            new BuiltInRow("W14", "W+14", "주 단위, 하루 1회.", 34, true),
            new BuiltInRow("WK1W", "WK+1W", "주 마감 후 3영업일.", 40, true),
            new BuiltInRow("WK2W", "WK+2W", "2주 마감 후 3영업일.", 41, true),
            new BuiltInRow("WK1WT", "WK+1WT", "주 마감 후 10영업일.", 42, true),
            new BuiltInRow("WK2WT", "WK+2WT", "2주 마감 후 10영업일.", 43, true),
            new BuiltInRow("WK1WM", "WK+1WM", "주 마감 후 30영업일.", 44, true),
            new BuiltInRow("WK2WM", "WK+2WM", "2주 마감 후 30영업일.", 45, true)
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

    /**
     * 셀렉트 박스용 — 사용(Y)만, 빈 값·NONE 포함.
     * 옵션 {@code v}·{@code t}는 비어 있지 않으면 정규화된 <strong>정산주기 코드</strong>로 통일한다(가맹·검색 공통).
     */
    public List<Map<String, Object>> listActiveSelectOptions() {
        return listMergedDefinitions().stream()
                .filter(m -> "Y".equals(String.valueOf(m.getOrDefault("activeYn", "Y"))))
                .sorted(Comparator.<Map<String, Object>, Integer>comparing(m -> (Integer) m.get("sortOrder"))
                        .thenComparing(m -> String.valueOf(m.get("cycleCode"))))
                .map(m -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    Object codeObj = m.get("cycleCode");
                    String codeRaw = codeObj != null ? String.valueOf(codeObj).trim() : "";
                    if (!StringUtils.hasText(codeRaw)) {
                        o.put("v", codeObj);
                        Object dl = m.get("displayLabel");
                        o.put("t", dl != null ? String.valueOf(dl) : "선택");
                    } else {
                        String norm = SettlementPeriodResolver.normalizeCalcCycle(codeRaw);
                        String key = StringUtils.hasText(norm) ? norm : codeRaw;
                        o.put("v", key);
                        o.put("t", key);
                    }
                    o.put("d", m.get("description"));
                    return o;
                })
                .collect(Collectors.toList());
    }

    /**
     * 총판별 허용 주기 설정 등 관리용 — 병합 정의 전체(미사용 N 포함).
     * <strong>순서</strong>는 {@link #listMergedDefinitions()}와 동일(정산주기관리 화면의 표준주기·DB등록 표 행 순서).
     * <strong>표시(t)</strong>는 정산주기 코드(정규화)이며, 비활성 시 접미 {@code (미사용)}.
     * 가맹 셀렉트용 {@link #listActiveSelectOptions()}와 동일하게 코드명으로 통일한다.
     */
    public List<Map<String, Object>> listCatalogSelectOptions() {
        return listMergedDefinitions().stream()
                .map(m -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    String code = m.get("cycleCode") != null ? String.valueOf(m.get("cycleCode")).trim() : "";
                    String t;
                    if (!StringUtils.hasText(code)) {
                        o.put("v", m.get("cycleCode"));
                        String label = m.get("displayLabel") != null ? String.valueOf(m.get("displayLabel")).trim() : "";
                        t = label.isEmpty() ? "선택" : label;
                    } else {
                        String norm = SettlementPeriodResolver.normalizeCalcCycle(code);
                        String key = StringUtils.hasText(norm) ? norm : code;
                        o.put("v", key);
                        t = key;
                        if (!"Y".equals(String.valueOf(m.getOrDefault("activeYn", "Y")))) {
                            t = t + " (미사용)";
                        }
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

    /** RT·T0 제외: M/H·TM/TH 등 일중 격자 — {@link SettlementPeriodResolver#resolveAutoPeriodWindow} 가 null 인 코드 */
    private static boolean isIntradaySchedulePreviewCode(String normalized) {
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        String n = SettlementPeriodResolver.normalizeCalcCycle(normalized);
        if ("RT".equals(n) || "T0".equals(n)) {
            return false;
        }
        return SettlementCycleTiming.isSubDailyScheduleCode(n);
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
                String norm = SettlementPeriodResolver.normalizeCalcCycle(code);
                SettlementPeriodResolver.PeriodWindow w = SettlementPeriodResolver.resolveAutoPeriodWindow(code, d);
                if (w != null) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("settleDate", d.toString());
                    row.put("cycleCode", norm);
                    row.put("periodFrom", w.fromDate().toString());
                    row.put("periodTo", w.toDate().toString());
                    row.put("periodNote", "");
                    row.put("autoMerchantCount", cnt.getOrDefault(norm, 0L));
                    rows.add(row);
                } else if (isIntradaySchedulePreviewCode(norm)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("settleDate", d.toString());
                    row.put("cycleCode", norm);
                    row.put("periodFrom", d.toString());
                    row.put("periodTo", d.toString());
                    row.put("periodNote", "일중 격자·당일 자동.");
                    row.put("autoMerchantCount", cnt.getOrDefault(norm, 0L));
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
