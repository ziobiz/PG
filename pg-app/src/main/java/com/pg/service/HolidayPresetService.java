package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * KR/US/JP/TH/CN 공휴일 프리셋 (연도별 JSON) + GLOBAL(토·일만 휴일). CN은 국무원 연도별 연휴(조정 포함) 반영.
 * 정산 영업일 산정·관리자 UI용.
 */
@Service
public class HolidayPresetService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** year -> country -> dates */
    private Map<String, Map<String, List<String>>> byYear = Map.of();

    @PostConstruct
    public void load() {
        try {
            ClassPathResource res = new ClassPathResource("data/holiday-presets.json");
            if (!res.exists()) return;
            try (InputStream in = res.getInputStream()) {
                byYear = MAPPER.readValue(in, new TypeReference<>() {});
            }
        } catch (IOException e) {
            byYear = Map.of();
        }
    }

    public Map<String, Object> getPresets(int year, List<String> countries) {
        String yk = String.valueOf(year);
        Map<String, List<String>> yearMap = byYear.get(yk);
        if (yearMap == null) {
            yearMap = Map.of();
        }
        Set<String> merged = new TreeSet<>();
        Map<String, List<String>> byCountry = new HashMap<>();
        for (String raw : countries) {
            if (raw == null) continue;
            String c = raw.trim().toUpperCase(Locale.ROOT);
            if (c.isEmpty()) continue;
            List<String> list;
            if ("GLOBAL".equals(c)) {
                list = weekendSatSunForYear(year);
            } else {
                list = yearMap.getOrDefault(c, List.of());
            }
            byCountry.put(c, list);
            merged.addAll(list);
        }
        return Map.of(
                "year", year,
                "countries", countries,
                "dates", new ArrayList<>(merged),
                "byCountry", byCountry
        );
    }

    public static List<String> parseCountryList(String countriesCsv) {
        List<String> out = new ArrayList<>();
        if (countriesCsv == null || countriesCsv.isBlank()) {
            out.add("KR");
            out.add("US");
            out.add("JP");
            out.add("TH");
            return out;
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String p : countriesCsv.split(",")) {
            String s = p != null ? p.trim().toUpperCase(Locale.ROOT) : "";
            if (!s.isEmpty()) set.add(s);
        }
        if (set.isEmpty()) {
            set.add("KR");
            set.add("US");
            set.add("JP");
            set.add("TH");
        }
        out.addAll(set);
        return out;
    }

    /** 해당 연도의 모든 토요일·일요일 (yyyy-MM-dd). 법정 공휴일 없이 주말만 비영업으로 쓰는 프로필용. */
    public static List<String> weekendSatSunForYear(int year) {
        LocalDate d = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<String> out = new ArrayList<>();
        while (!d.isAfter(end)) {
            DayOfWeek dw = d.getDayOfWeek();
            if (dw == DayOfWeek.SATURDAY || dw == DayOfWeek.SUNDAY) {
                out.add(d.toString());
            }
            d = d.plusDays(1);
        }
        return out;
    }

    /**
     * 목록 집계용: 해당 연도·기준국가에서 "공식 공휴일"로 볼 수 있는 모든 일자(토·일 + 해당국 법정/프리셋 일자).
     * GLOBAL 은 토·일만. KR/US/JP/TH/CN 은 해당 연도 주말 전체 ∪ JSON 법정 공휴일 일자.
     */
    public Set<String> officialHolidayDatesForProfile(int year, String countryCode) {
        String c = countryCode == null ? "KR" : countryCode.trim().toUpperCase(Locale.ROOT);
        Set<String> out = new TreeSet<>();
        if ("GLOBAL".equals(c)) {
            out.addAll(weekendSatSunForYear(year));
            return out;
        }
        out.addAll(weekendSatSunForYear(year));
        String yk = String.valueOf(year);
        Map<String, List<String>> yearMap = byYear.get(yk);
        if (yearMap != null) {
            List<String> legal = yearMap.get(c);
            if (legal != null && !legal.isEmpty()) {
                out.addAll(legal);
            }
        }
        return out;
    }
}
