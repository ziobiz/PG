package com.pg.service.settlement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.MasterDistSettlementCronZoneService;
import com.pg.util.BusinessDayCalendar;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 가맹 정산주기(W+N·D+N·WK*)의 영업일 계산 — 소속 총판(MASTER_DIST) 영업일·휴일 프로필(본사 영업일설정 목록) 기준.
 * <p>가맹은 직접 프로필을 두지 않고, 조직 트리 상위의 총판이 선택·상속한
 * {@code holidayProfileName} / {@code businessHolidayExtraDates} 를 따릅니다.
 * 총본사가 본사(REGIONAL) 영업일을 지정·잠근 경우 상위 본사 프로필을 강제합니다.</p>
 */
@Service
public class SettlementBusinessHolidayService {

    private static final ObjectMapper OM = new ObjectMapper();

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final MasterDistSettlementCronZoneService masterDistSettlementCronZoneService;

    public SettlementBusinessHolidayService(OrgUnitRepository orgUnitRepository,
                                              MerchantProfileRepository merchantProfileRepository,
                                              HqApiConfigRepository hqApiConfigRepository,
                                              MasterDistSettlementCronZoneService masterDistSettlementCronZoneService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.masterDistSettlementCronZoneService = masterDistSettlementCronZoneService;
    }

    public boolean isBusinessDayForMerchantOrgUnitId(long merchantOrgUnitId, LocalDate day) {
        return BusinessDayCalendar.isBusinessDay(day, resolveNonBusinessDatesForMerchantOrgUnitId(merchantOrgUnitId));
    }

    public boolean isBusinessDayForMerchantCode(String merchantCode, LocalDate day) {
        if (!StringUtils.hasText(merchantCode)) {
            return BusinessDayCalendar.isBusinessDay(day, Collections.emptySet());
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantCode.trim());
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(merchantCode.trim());
        }
        return ou.map(o -> isBusinessDayForMerchantOrgUnitId(o.getId(), day))
                .orElseGet(() -> BusinessDayCalendar.isBusinessDay(day, Collections.emptySet()));
    }

    /**
     * 비영업일 집합(공휴·추가휴일). 주말은 {@link BusinessDayCalendar} 가 별도 제외.
     */
    public Set<LocalDate> resolveNonBusinessDatesForMerchantOrgUnitId(long merchantOrgUnitId) {
        Optional<Long> mdId = masterDistSettlementCronZoneService.findNearestMasterDistOrgId(merchantOrgUnitId);
        if (mdId.isEmpty()) {
            return Collections.emptySet();
        }
        return resolveNonBusinessDatesForMasterDistOrgUnitId(mdId.get());
    }

    public Set<LocalDate> resolveNonBusinessDatesForMerchantCode(String merchantCode) {
        if (!StringUtils.hasText(merchantCode)) {
            return Collections.emptySet();
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantCode.trim());
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(merchantCode.trim());
        }
        return ou.map(o -> resolveNonBusinessDatesForMerchantOrgUnitId(o.getId())).orElse(Collections.emptySet());
    }

    /**
     * 메인 대시보드·조회용: 조직 단위의 유효 비영업일(주말은 {@link BusinessDayCalendar}에서 별도 제외).
     */
    public Set<LocalDate> resolveNonBusinessDatesForOrgUnitId(long orgUnitId) {
        OrgUnit ou = orgUnitRepository.findById(orgUnitId).orElse(null);
        if (ou == null) {
            return Collections.emptySet();
        }
        if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
            return resolveNonBusinessDatesForMasterDistOrgUnitId(orgUnitId);
        }
        if (ou.getOrgLevel() == OrgLevel.MERCHANT) {
            return resolveNonBusinessDatesForMerchantOrgUnitId(orgUnitId);
        }
        if (ou.getOrgLevel() == OrgLevel.REGIONAL) {
            return parseNonBusinessDatesFromHolidaySlice(extractHolidaySlice(loadRegionalSettings(orgUnitId)));
        }
        Optional<Long> mdId = masterDistSettlementCronZoneService.findNearestMasterDistOrgId(orgUnitId);
        if (mdId.isPresent()) {
            return resolveNonBusinessDatesForMasterDistOrgUnitId(mdId.get());
        }
        Map<String, Object> inherited = resolveInheritedHolidaySliceFromRegional(ou.getParentId());
        if (!inherited.isEmpty()) {
            return parseNonBusinessDatesFromHolidaySlice(inherited);
        }
        return Collections.emptySet();
    }

    /** 표시용: 유효 영업일 프로필명·기준국가(없으면 빈 문자열). */
    public Map<String, String> resolveHolidayProfileMetaForOrgUnitId(long orgUnitId) {
        OrgUnit ou = orgUnitRepository.findById(orgUnitId).orElse(null);
        if (ou == null) {
            return Map.of("profileName", "", "countryCode", "");
        }
        Map<String, Object> slice;
        if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
            slice = resolveEffectiveHolidaySliceForMasterDist(ou);
        } else if (ou.getOrgLevel() == OrgLevel.REGIONAL) {
            slice = extractHolidaySlice(loadRegionalSettings(orgUnitId));
        } else {
            Optional<Long> mdId = masterDistSettlementCronZoneService.findNearestMasterDistOrgId(orgUnitId);
            if (mdId.isPresent()) {
                OrgUnit md = orgUnitRepository.findById(mdId.get()).orElse(null);
                slice = md != null ? resolveEffectiveHolidaySliceForMasterDist(md) : Map.of();
            } else {
                slice = resolveInheritedHolidaySliceFromRegional(ou.getParentId());
            }
        }
        String name = str(slice.get("holidayProfileName"));
        String cc = str(slice.get("holidayCountryCode"));
        if (cc.isEmpty()) {
            cc = str(slice.get("holidayCountryCodes"));
            if (cc.contains(",")) {
                cc = cc.split(",")[0].trim();
            }
        }
        if (cc.isEmpty()) {
            cc = str(slice.get("holidayProfileCountry"));
        }
        return Map.of("profileName", name, "countryCode", cc);
    }

    public Set<LocalDate> resolveNonBusinessDatesForMasterDistOrgUnitId(long masterDistOrgUnitId) {
        OrgUnit md = orgUnitRepository.findById(masterDistOrgUnitId).orElse(null);
        if (md == null || md.getOrgLevel() != OrgLevel.MASTER_DIST) {
            return Collections.emptySet();
        }
        Map<String, Object> slice = resolveEffectiveHolidaySliceForMasterDist(md);
        return parseNonBusinessDatesFromHolidaySlice(slice);
    }

    private Map<String, Object> resolveEffectiveHolidaySliceForMasterDist(OrgUnit masterDist) {
        Map<String, Object> own = loadRegionalSettings(masterDist.getId());
        if (isParentRegionalHolidayManagedByHeadquarters(masterDist.getParentId())) {
            Map<String, Object> inherited = resolveInheritedHolidaySliceFromRegional(masterDist.getParentId());
            if (!inherited.isEmpty()) {
                return inherited;
            }
        }
        if (hasOwnHolidaySetting(own)) {
            return extractHolidaySlice(own);
        }
        Map<String, Object> inherited = resolveInheritedHolidaySliceFromRegional(masterDist.getParentId());
        if (!inherited.isEmpty()) {
            return inherited;
        }
        return extractHolidaySlice(own);
    }

    private Map<String, Object> resolveInheritedHolidaySliceFromRegional(Long parentId) {
        Long cur = parentId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit org = orgUnitRepository.findById(cur).orElse(null);
            if (org == null) {
                break;
            }
            if (org.getOrgLevel() == OrgLevel.REGIONAL) {
                Map<String, Object> rs = loadRegionalSettings(org.getId());
                Map<String, Object> slice = extractHolidaySlice(rs);
                if (!slice.isEmpty()) {
                    return slice;
                }
            }
            cur = org.getParentId();
        }
        return Map.of();
    }

    private boolean isParentRegionalHolidayManagedByHeadquarters(Long parentId) {
        Long cur = parentId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit org = orgUnitRepository.findById(cur).orElse(null);
            if (org == null) {
                break;
            }
            if (org.getOrgLevel() == OrgLevel.REGIONAL) {
                Map<String, Object> rs = loadRegionalSettings(org.getId());
                Object v = rs.get("holidayManagedByHeadquartersYn");
                return v != null && "Y".equalsIgnoreCase(String.valueOf(v).trim());
            }
            cur = org.getParentId();
        }
        return false;
    }

    private static boolean hasOwnHolidaySetting(Map<String, Object> rs) {
        if (rs == null || rs.isEmpty()) {
            return false;
        }
        return hasText(rs.get("holidayProfileName"))
                || hasText(rs.get("holidayCountryCode"))
                || hasText(rs.get("holidayCountryCodes"))
                || hasText(rs.get("businessHolidayExtraDates"));
    }

    private static Map<String, Object> extractHolidaySlice(Map<String, Object> rs) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (rs == null) {
            return out;
        }
        copyIfPresent(rs, out, "holidayProfileName");
        copyIfPresent(rs, out, "holidayProfileCountry");
        copyIfPresent(rs, out, "holidayCountryCode");
        copyIfPresent(rs, out, "holidayCountryCodes");
        copyIfPresent(rs, out, "businessHolidayExtraDates");
        return out;
    }

    private static void copyIfPresent(Map<String, Object> from, Map<String, Object> to, String key) {
        Object v = from.get(key);
        if (v != null && !String.valueOf(v).isBlank()) {
            to.put(key, v);
        }
    }

    private Set<LocalDate> parseNonBusinessDatesFromHolidaySlice(Map<String, Object> slice) {
        if (slice == null || slice.isEmpty()) {
            return Collections.emptySet();
        }
        String extra = str(slice.get("businessHolidayExtraDates"));
        if (!StringUtils.hasText(extra)) {
            String profileName = str(slice.get("holidayProfileName"));
            if (StringUtils.hasText(profileName)) {
                extra = lookupExtraDatesByProfileName(profileName);
            }
        }
        return parseDateLines(extra);
    }

    private String lookupExtraDatesByProfileName(String profileName) {
        String want = profileName.trim();
        for (Map<String, Object> row : loadHqBusinessDayProfiles()) {
            String name = str(row.get("name"));
            String id = str(row.get("id"));
            if (want.equals(name) || want.equals(id)) {
                return str(row.get("businessHolidayExtraDates"));
            }
        }
        return "";
    }

    /** 본사설정에 지정된 총본사 기준 영업일 프로필(없으면 목록 첫 항목). */
    public Optional<Map<String, Object>> resolveHqDefaultBusinessDayProfileRow() {
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        List<Map<String, Object>> profiles = loadHqBusinessDayProfiles();
        if (profiles.isEmpty()) {
            return Optional.empty();
        }
        String defId = c != null ? str(c.getHqDefaultBusinessDayProfileId()) : "";
        if (StringUtils.hasText(defId)) {
            for (Map<String, Object> row : profiles) {
                if (defId.equals(str(row.get("id")))) {
                    return Optional.of(row);
                }
            }
        }
        return Optional.of(profiles.get(0));
    }

    public Set<LocalDate> resolveNonBusinessDatesForHqDefault() {
        return parseNonBusinessDatesFromHolidaySlice(
                resolveHqDefaultBusinessDayProfileRow()
                        .map(this::holidaySliceFromHqProfileRow)
                        .orElse(Map.of()));
    }

    public Map<String, String> resolveHqDefaultHolidayProfileMeta() {
        return resolveHqDefaultBusinessDayProfileRow()
                .map(row -> Map.of(
                        "profileName", str(row.get("name")),
                        "countryCode", str(row.get("countryCode")).isEmpty() ? "KR" : str(row.get("countryCode"))))
                .orElse(Map.of("profileName", "", "countryCode", "KR"));
    }

    private Map<String, Object> holidaySliceFromHqProfileRow(Map<String, Object> row) {
        Map<String, Object> slice = new LinkedHashMap<>();
        slice.put("holidayProfileName", str(row.get("name")));
        slice.put("holidayCountryCode", str(row.get("countryCode")));
        slice.put("businessHolidayExtraDates", str(row.get("businessHolidayExtraDates")));
        return slice;
    }

    private List<Map<String, Object>> loadHqBusinessDayProfiles() {
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        if (c == null || !StringUtils.hasText(c.getBusinessDaySettingsJson())) {
            return List.of();
        }
        try {
            Object parsed = OM.readValue(c.getBusinessDaySettingsJson().trim(), Object.class);
            if (!(parsed instanceof List<?> l)) {
                return List.of();
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object it : l) {
                if (it instanceof Map<?, ?> mm) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    mm.forEach((k, v) -> row.put(String.valueOf(k), v));
                    out.add(row);
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> loadRegionalSettings(long orgUnitId) {
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> parseRegionalSettingsJson(mp.getRegionalSettings()))
                .orElseGet(LinkedHashMap::new);
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

    private static Set<LocalDate> parseDateLines(String raw) {
        Set<LocalDate> out = new LinkedHashSet<>();
        if (!StringUtils.hasText(raw)) {
            return out;
        }
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.length() >= 10 && t.charAt(4) == '-' && t.charAt(7) == '-') {
                try {
                    out.add(LocalDate.parse(t.substring(0, 10)));
                } catch (Exception ignored) {
                    /* skip malformed */
                }
            }
        }
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static boolean hasText(Object v) {
        return v != null && !String.valueOf(v).isBlank();
    }
}
