package com.pg.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 결제 고객 국가 — ISO 3166-1 alpha-2 정규화(언어코드·alpha-3·레거시 값 보정). */
public final class PayerCountryIso2Util {

    private static final Map<String, String> ALIASES = buildAliases();

    private PayerCountryIso2Util() {
    }

    /**
     * @return 2자리 ISO 국가코드. 알 수 없거나 모호하면 빈 문자열.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        if (u.isEmpty()) {
            return "";
        }
        if (ALIASES.containsKey(u)) {
            return ALIASES.get(u);
        }
        if (u.length() >= 3) {
            String a3 = u.substring(0, 3);
            if (ALIASES.containsKey(a3)) {
                return ALIASES.get(a3);
            }
            String two = u.substring(0, 2);
            if (ALIASES.containsKey(two)) {
                return ALIASES.get(two);
            }
            return two;
        }
        return ALIASES.getOrDefault(u, u);
    }

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("JPN", "JP");
        m.put("KOR", "KR");
        m.put("USA", "US");
        m.put("GBR", "GB");
        m.put("THA", "TH");
        m.put("SGP", "SG");
        m.put("HKG", "HK");
        m.put("CHN", "CN");
        m.put("CHE", "CH");
        m.put("JAP", "JP");
        m.put("JA", "JP");
        m.put("KO", "KR");
        m.put("ZH", "CN");
        m.put("EN", "");
        return Map.copyOf(m);
    }
}
