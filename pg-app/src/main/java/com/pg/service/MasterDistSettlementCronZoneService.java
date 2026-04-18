package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MasterDistSettlementCycleConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MasterDistSettlementCycleConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 총판(MASTER_DIST)별 정산 크론 시각 기준 Zone — 영업일 프로필(본사 영업일설정·총판 regionalSettings)과 분리.
 */
@Service
public class MasterDistSettlementCronZoneService {

    public static final ZoneId DEFAULT_SETTLEMENT_CRON_ZONE = ZoneId.of("Asia/Seoul");

    private static final ObjectMapper OM = new ObjectMapper();

    private final MasterDistSettlementCycleConfigRepository configRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public MasterDistSettlementCronZoneService(MasterDistSettlementCycleConfigRepository configRepository,
                                               OrgUnitRepository orgUnitRepository,
                                               MerchantProfileRepository merchantProfileRepository) {
        this.configRepository = configRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    /**
     * UI 셀렉트: ZoneId(값)당 한 줄만 — 동일 IANA 를 한국어 라벨과 IANA 라벨로 이중 노출하지 않음.
     * 저장값은 항상 {@code zoneId}(IANA). 다국어 라벨은 이후 i18n 으로 전환.
     */
    public static List<Map<String, String>> settlementCronZonePresetOptions() {
        List<Map<String, String>> out = new ArrayList<>();
        addOpt(out, "KR", "Asia/Seoul", "한국(서울)");
        addOpt(out, "TH", "Asia/Bangkok", "태국(방콕)");
        addOpt(out, "JP", "Asia/Tokyo", "일본(도쿄)");
        addOpt(out, "US", "America/New_York", "미국(뉴욕)");
        addOpt(out, "CN", "Asia/Shanghai", "중국(상하이)");
        addOpt(out, "GLOBAL", "UTC", "세계 표준(UTC)");
        return out;
    }

    private static void addOpt(List<Map<String, String>> out, String code, String zoneId, String label) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("presetCode", code);
        m.put("zoneId", zoneId);
        m.put("label", label);
        out.add(m);
    }

    /**
     * KR/TH/… 또는 이미 IANA(슬래시 포함) 문자열을 {@link ZoneId} 로 검증·정규화.
     */
    public static String normalizeZoneIdOrPreset(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_SETTLEMENT_CRON_ZONE.getId();
        }
        String t = raw.trim();
        if (t.contains("/")) {
            ZoneId.of(t);
            return t;
        }
        return switch (t.toUpperCase(Locale.ROOT)) {
            case "KR" -> "Asia/Seoul";
            case "TH" -> "Asia/Bangkok";
            case "JP" -> "Asia/Tokyo";
            case "US" -> "America/New_York";
            case "CN" -> "Asia/Shanghai";
            case "GLOBAL", "UTC" -> "UTC";
            case "SEOUL" -> "Asia/Seoul";
            case "BANGKOK" -> "Asia/Bangkok";
            case "TOKYO" -> "Asia/Tokyo";
            default -> {
                try {
                    yield ZoneId.of(t).getId();
                } catch (Exception e) {
                    yield DEFAULT_SETTLEMENT_CRON_ZONE.getId();
                }
            }
        };
    }

    public Optional<Long> findNearestMasterDistOrgId(Long startOrgUnitId) {
        if (startOrgUnitId == null) {
            return Optional.empty();
        }
        Long cur = startOrgUnitId;
        Set<Long> seen = new java.util.HashSet<>();
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

    public ZoneId resolveSettlementCronZoneForMerchantCode(String merchantCode) {
        if (!StringUtils.hasText(merchantCode)) {
            return DEFAULT_SETTLEMENT_CRON_ZONE;
        }
        String m = merchantCode.trim();
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(m);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(m);
        }
        return ou.map(o -> resolveSettlementCronZoneForOrgUnitId(o.getId())).orElse(DEFAULT_SETTLEMENT_CRON_ZONE);
    }

    public ZoneId resolveSettlementCronZoneForOrgUnitId(long orgUnitIdFromAnyLevel) {
        Optional<Long> md = findNearestMasterDistOrgId(orgUnitIdFromAnyLevel);
        if (md.isEmpty()) {
            return DEFAULT_SETTLEMENT_CRON_ZONE;
        }
        Optional<MasterDistSettlementCycleConfig> cfg = configRepository.findByOrgUnitId(md.get());
        if (cfg.isEmpty()) {
            return DEFAULT_SETTLEMENT_CRON_ZONE;
        }
        String zid = cfg.get().getSettlementCronZoneId();
        if (!StringUtils.hasText(zid)) {
            return DEFAULT_SETTLEMENT_CRON_ZONE;
        }
        try {
            return ZoneId.of(zid.trim());
        } catch (Exception e) {
            return DEFAULT_SETTLEMENT_CRON_ZONE;
        }
    }

    public List<Map<String, Object>> listMasterDistBizCronRows() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrgUnit ou : orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MASTER_DIST)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orgUnitId", ou.getId());
            row.put("compId", ou.getCode());
            row.put("compNm", ou.getName() != null ? ou.getName() : ou.getCode());
            Map<String, Object> ownRs = merchantProfileRepository.findByOrgUnitId(ou.getId())
                    .map(mp -> parseRegionalSettingsJson(mp.getRegionalSettings()))
                    .orElseGet(LinkedHashMap::new);
            EffectiveHolidayDisplay eff = resolveEffectiveHolidayDisplay(ou, ownRs);
            Map<String, Object> disp = eff.rsForDisplay();
            row.put("holidayProfileName", str(disp.get("holidayProfileName")));
            row.put("businessCountryCode", resolveHolidayCountryToken(disp));
            row.put("currentBusinessDaySummary", formatCurrentBusinessDaySummary(eff));
            String zone = configRepository.findByOrgUnitId(ou.getId())
                    .map(MasterDistSettlementCycleConfig::getSettlementCronZoneId)
                    .filter(StringUtils::hasText)
                    .orElse(DEFAULT_SETTLEMENT_CRON_ZONE.getId());
            row.put("settlementCronZoneId", zone);
            out.add(row);
        }
        return out;
    }

    /**
     * 총판 저장 JSON + (비어 있으면) 상위 본사(REGIONAL) 프로필을 읽어 표시용 영업일 요약에 씀.
     * 업체등록 시 {@code holidayProfileName}·{@code holidayProfileCountry}·{@code holidayCountryCode}·{@code holidayCountryCodes} 등이 regionalSettings 에 들어감.
     */
    private record EffectiveHolidayDisplay(Map<String, Object> rsForDisplay, boolean fromParentRegional, boolean lockedByHq) {}

    private EffectiveHolidayDisplay resolveEffectiveHolidayDisplay(OrgUnit masterDist, Map<String, Object> ownRs) {
        Map<String, Object> own = ownRs != null ? new LinkedHashMap<>(ownRs) : new LinkedHashMap<>();
        boolean locked = "Y".equalsIgnoreCase(str(own.get("holidayLockedByHeadquartersYn")));
        boolean inherited = "Y".equalsIgnoreCase(str(own.get("holidayInheritedYn")));
        if (hasHolidayProfileOrCountry(own)) {
            return new EffectiveHolidayDisplay(own, false, locked);
        }
        Optional<Map<String, Object>> parentHoliday = loadHolidaySliceFromNearestRegional(masterDist.getParentId());
        if (parentHoliday.isPresent() && !parentHoliday.get().isEmpty()) {
            return new EffectiveHolidayDisplay(new LinkedHashMap<>(parentHoliday.get()), true, locked || inherited);
        }
        return new EffectiveHolidayDisplay(own, false, locked);
    }

    private static boolean hasHolidayProfileOrCountry(Map<String, Object> rs) {
        if (rs == null || rs.isEmpty()) {
            return false;
        }
        return StringUtils.hasText(str(rs.get("holidayProfileName"))) || StringUtils.hasText(resolveHolidayCountryToken(rs));
    }

    /**
     * 업체등록 시 {@code holidayCountryCode}(단일) 또는 {@code holidayCountryCodes}(콤마 구분)·{@code holidayProfileCountry}(표시용) 이 들어감.
     */
    private static String resolveHolidayCountryToken(Map<String, Object> rs) {
        if (rs == null || rs.isEmpty()) {
            return "";
        }
        String a = str(rs.get("holidayCountryCode"));
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        String codes = str(rs.get("holidayCountryCodes"));
        if (StringUtils.hasText(codes)) {
            return codes.trim().replaceAll("\\s*,\\s*", ", ");
        }
        return str(rs.get("holidayProfileCountry"));
    }

    private Optional<Map<String, Object>> loadHolidaySliceFromNearestRegional(Long startParentId) {
        Long cur = startParentId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit org = orgUnitRepository.findById(cur).orElse(null);
            if (org == null) {
                break;
            }
            if (org.getOrgLevel() == OrgLevel.REGIONAL) {
                Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(org.getId());
                if (mpOpt.isEmpty()) {
                    return Optional.empty();
                }
                Map<String, Object> full = parseRegionalSettingsJson(mpOpt.get().getRegionalSettings());
                Map<String, Object> sub = new LinkedHashMap<>();
                copyIfHasText(full, sub, "holidayProfileName");
                copyIfHasText(full, sub, "holidayProfileCountry");
                copyIfHasText(full, sub, "holidayCountryCode");
                copyIfHasText(full, sub, "holidayCountryCodes");
                return Optional.of(sub);
            }
            cur = org.getParentId();
        }
        return Optional.empty();
    }

    private static void copyIfHasText(Map<String, Object> from, Map<String, Object> to, String key) {
        String v = str(from.get(key));
        if (StringUtils.hasText(v)) {
            to.put(key, v);
        }
    }

    private static String formatCurrentBusinessDaySummary(EffectiveHolidayDisplay eff) {
        Map<String, Object> rs = eff.rsForDisplay();
        String profile = str(rs.get("holidayProfileName"));
        String cc = resolveHolidayCountryToken(rs);
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(profile)) {
            parts.add(profile);
        }
        if (StringUtils.hasText(cc)) {
            parts.add("기준국가 " + cc);
        }
        String base = String.join(" · ", parts);
        if (eff.lockedByHq()) {
            if (base.isEmpty()) {
                return "본사·총본사 지정 영업일(잠금) — 상위 본사 프로필을 따릅니다.";
            }
            return base + " (본사·총본사 지정·잠금)";
        }
        if (eff.fromParentRegional()) {
            if (base.isEmpty()) {
                return "상위 본사(REGIONAL) 영업일 설정 상속 — 총판 업체정보에서 확인·저장하세요.";
            }
            return "상위 본사 기준: " + base;
        }
        return base;
    }

    @Transactional
    public Map<String, Object> saveSettlementCronZoneOnly(long masterDistOrgUnitId, String zoneOrPreset) {
        OrgUnit ou = orgUnitRepository.findById(masterDistOrgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다."));
        if (ou.getOrgLevel() != OrgLevel.MASTER_DIST) {
            throw new IllegalArgumentException("총판(MASTER_DIST)만 정산 크론 기준을 설정할 수 있습니다.");
        }
        String z = normalizeZoneIdOrPreset(zoneOrPreset);
        MasterDistSettlementCycleConfig entity = configRepository.findByOrgUnitId(masterDistOrgUnitId)
                .orElseGet(() -> {
                    MasterDistSettlementCycleConfig n = new MasterDistSettlementCycleConfig();
                    n.setOrgUnitId(masterDistOrgUnitId);
                    n.setDefaultSlot(0);
                    n.setSettlementCronZoneId(DEFAULT_SETTLEMENT_CRON_ZONE.getId());
                    return n;
                });
        entity.setSettlementCronZoneId(z);
        MasterDistSettlementCycleConfig saved = configRepository.save(entity);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orgUnitId", saved.getOrgUnitId());
        m.put("settlementCronZoneId", saved.getSettlementCronZoneId());
        return m;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static Map<String, Object> parseRegionalSettingsJson(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> m = OM.readValue(json.trim(), new TypeReference<>() {});
            return m != null ? m : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
