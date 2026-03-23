package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * KR/US/JP/TH 공휴일 프리셋 (연도별 JSON). 정산 영업일 산정·관리자 UI용.
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
            List<String> list = yearMap.getOrDefault(c, List.of());
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
}
