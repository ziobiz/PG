package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import com.pg.service.settlement.SettlementBusinessHolidayService;
import com.pg.util.BusinessDayCalendar;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 메인(/main) 영업일 3개월 뷰 — 지난달·당월·다음달, 이동은 3개월 단위(anchor ±3).
 */
@Service
public class DashboardBusinessDayCalendarService {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final Set<String> ELIGIBLE_ORG_LEVELS = Set.of(
            "HEADQUARTERS", "REGIONAL", "MASTER_DIST", "BRANCH", "AGENCY", "SALES_OFFICE");

    private final HqApiConfigRepository hqApiConfigRepository;
    private final SettlementBusinessHolidayService settlementBusinessHolidayService;

    public DashboardBusinessDayCalendarService(HqApiConfigRepository hqApiConfigRepository,
                                               SettlementBusinessHolidayService settlementBusinessHolidayService) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.settlementBusinessHolidayService = settlementBusinessHolidayService;
    }

    public boolean isEligible(String role, String orgLevel) {
        if ("ADMIN".equalsIgnoreCase(role != null ? role : "")) {
            return true;
        }
        String lvl = orgLevel != null ? orgLevel.trim().toUpperCase(Locale.ROOT) : "";
        return ELIGIBLE_ORG_LEVELS.contains(lvl);
    }

    public Map<String, Object> build(String role, String orgLevel, Long orgUnitId, String anchorMonthParam) {
        if (!isEligible(role, orgLevel)) {
            return null;
        }
        YearMonth anchor = parseAnchorMonth(anchorMonthParam);
        ProfileSlice profile = resolveProfile(role, orgLevel, orgUnitId);
        Set<LocalDate> nonBusiness = profile.nonBusinessDates();

        YearMonth m0 = anchor.minusMonths(1);
        YearMonth m1 = anchor;
        YearMonth m2 = anchor.plusMonths(1);

        List<Map<String, Object>> months = new ArrayList<>(3);
        months.add(buildMonth(m0, nonBusiness));
        months.add(buildMonth(m1, nonBusiness));
        months.add(buildMonth(m2, nonBusiness));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("anchorMonth", anchor.toString());
        out.put("prevAnchorMonth", anchor.minusMonths(3).toString());
        out.put("nextAnchorMonth", anchor.plusMonths(3).toString());
        out.put("windowFrom", m0.atDay(1).toString());
        out.put("windowTo", m2.atEndOfMonth().toString());
        out.put("profileName", profile.profileName());
        out.put("countryCode", profile.countryCode());
        out.put("months", months);
        out.put("settingsUrl", "/hq/businessDaySetting");
        return out;
    }

    private static YearMonth parseAnchorMonth(String raw) {
        if (StringUtils.hasText(raw)) {
            String t = raw.trim();
            if (t.length() >= 7) {
                try {
                    return YearMonth.parse(t.substring(0, 7));
                } catch (DateTimeParseException ignored) {
                    /* fall through */
                }
            }
        }
        return YearMonth.from(LocalDate.now());
    }

    private Map<String, Object> buildMonth(YearMonth ym, Set<LocalDate> nonBusiness) {
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        List<Map<String, Object>> days = new ArrayList<>();
        int bizCount = 0;
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            boolean weekend = BusinessDayCalendar.isWeekend(d);
            boolean holiday = nonBusiness.contains(d);
            boolean businessDay = BusinessDayCalendar.isBusinessDay(d, nonBusiness);
            if (businessDay) {
                bizCount++;
            }
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("date", d.toString());
            cell.put("day", d.getDayOfMonth());
            cell.put("weekend", weekend);
            cell.put("holiday", holiday);
            cell.put("businessDay", businessDay);
            days.add(cell);
        }
        Map<String, Object> month = new LinkedHashMap<>();
        month.put("year", ym.getYear());
        month.put("month", ym.getMonthValue());
        month.put("yearMonth", ym.toString());
        month.put("businessDayCount", bizCount);
        month.put("days", days);
        return month;
    }

    private ProfileSlice resolveProfile(String role, String orgLevel, Long orgUnitId) {
        boolean hqOrAdmin = "ADMIN".equalsIgnoreCase(role != null ? role : "")
                || "HEADQUARTERS".equalsIgnoreCase(orgLevel != null ? orgLevel : "");
        if (hqOrAdmin) {
            return defaultHqProfile();
        }
        if (orgUnitId != null) {
            Set<LocalDate> dates = settlementBusinessHolidayService.resolveNonBusinessDatesForOrgUnitId(orgUnitId);
            Map<String, String> meta = settlementBusinessHolidayService.resolveHolidayProfileMetaForOrgUnitId(orgUnitId);
            String name = meta.getOrDefault("profileName", "");
            String cc = meta.getOrDefault("countryCode", "");
            if (!dates.isEmpty() || StringUtils.hasText(name)) {
                return new ProfileSlice(name, cc.isEmpty() ? "KR" : cc, dates);
            }
        }
        return defaultHqProfile();
    }

    private ProfileSlice defaultHqProfile() {
        List<Map<String, Object>> profiles = loadHqBusinessDayProfiles();
        Map<String, Object> row = profiles.isEmpty() ? Map.of() : profiles.get(0);
        String name = str(row.get("name"));
        String cc = str(row.get("countryCode"));
        if (cc.isEmpty()) {
            cc = "KR";
        }
        String extra = str(row.get("businessHolidayExtraDates"));
        return new ProfileSlice(name, cc, parseDateLines(extra));
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
                    /* skip */
                }
            }
        }
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private record ProfileSlice(String profileName, String countryCode, Set<LocalDate> nonBusinessDates) {}
}
