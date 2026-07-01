package com.pg.urlpay;

import java.util.Locale;
import java.util.Map;

/** 결제개요·결제내역 위치 — {@code KR | Seoul} (ISO2 + 영문 도시·지역, i18n 미적용). */
public final class PayerLocationLabelFormatter {

    private static final Map<String, String> ENGLISH_COUNTRY_TO_ISO2 = Map.ofEntries(
            Map.entry("JAPAN", "JP"),
            Map.entry("SOUTH KOREA", "KR"),
            Map.entry("KOREA", "KR"),
            Map.entry("UNITED STATES", "US"),
            Map.entry("UNITED KINGDOM", "GB"),
            Map.entry("CHINA", "CN"),
            Map.entry("THAILAND", "TH"),
            Map.entry("SINGAPORE", "SG"),
            Map.entry("TAIWAN", "TW"),
            Map.entry("HONG KONG", "HK"),
            Map.entry("VIETNAM", "VN"),
            Map.entry("PHILIPPINES", "PH"),
            Map.entry("MALAYSIA", "MY"),
            Map.entry("INDONESIA", "ID"),
            Map.entry("AUSTRALIA", "AU"),
            Map.entry("CANADA", "CA"),
            Map.entry("GERMANY", "DE"),
            Map.entry("FRANCE", "FR"),
            Map.entry("대한민국", "KR"),
            Map.entry("한국", "KR"),
            Map.entry("일본", "JP"),
            Map.entry("중국", "CN"),
            Map.entry("태국", "TH")
    );

    private PayerLocationLabelFormatter() {
    }

    /** 결제개요·결제내역 표시 — {@code KR | Seoul}. 도시·지역 없으면 {@code KR}만. */
    public static String formatOverview(String iso2, String cityOrRegion) {
        return finalizeOverviewDisplay(buildOverviewRaw(iso2, cityOrRegion));
    }

    private static String buildOverviewRaw(String iso2, String cityOrRegion) {
        String code = normalizeIso2(iso2);
        String loc = normalizeLocationEnglish(cityOrRegion);
        if (code.isEmpty() && loc.isEmpty()) {
            return "";
        }
        if (loc.isEmpty()) {
            return code;
        }
        if (code.isEmpty()) {
            return loc;
        }
        return code + " | " + loc;
    }

    /** 저장 라벨이 {@code XX | CITY} 형식인지 — 지역 포함 완전 라벨. */
    public static boolean isCompleteOverviewLabel(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        String s = label.trim();
        int pipe = s.indexOf(" | ");
        if (pipe != 2) {
            return false;
        }
        String code = s.substring(0, 2);
        if (!code.chars().allMatch(ch -> ch >= 'A' && ch <= 'Z')) {
            return false;
        }
        return s.length() > 5;
    }

    /** 레거시 저장값·영문 국가명 → 결제개요·결제내역 표시용 정규화(항상 영어·Title case). */
    public static String normalizeForOverviewDisplay(String storedLabel, String iso2, String city) {
        String code = normalizeIso2(iso2);
        String cityNorm = normalizeLocationEnglish(city);
        if (!code.isEmpty() && !cityNorm.isEmpty()) {
            return formatOverview(code, city);
        }
        if (isCompleteOverviewLabel(storedLabel)) {
            return finalizeOverviewDisplay(storedLabel.trim());
        }
        String fromFields = buildOverviewRaw(iso2, city);
        if (!fromFields.isEmpty() && fromFields.contains(" | ")) {
            return finalizeOverviewDisplay(fromFields);
        }
        if (storedLabel == null || storedLabel.isBlank()) {
            return finalizeOverviewDisplay(fromFields);
        }
        String trimmed = storedLabel.trim();
        String parsed = parseLegacyStoredLabel(trimmed, iso2);
        if (!parsed.isEmpty() && parsed.contains(" | ")) {
            return finalizeOverviewDisplay(parsed);
        }
        String hintFromCjk = JpayLocationHintEnglishMapper.toOverviewLabel(trimmed);
        if (!hintFromCjk.isEmpty() && hintFromCjk.contains(" | ")) {
            return finalizeOverviewDisplay(hintFromCjk);
        }
        if (!fromFields.isEmpty()) {
            return finalizeOverviewDisplay(fromFields);
        }
        if (!parsed.isEmpty()) {
            return finalizeOverviewDisplay(parsed);
        }
        if (!hintFromCjk.isEmpty()) {
            return finalizeOverviewDisplay(hintFromCjk);
        }
        String isoOnly = resolveIso2(trimmed, iso2);
        if (!isoOnly.isEmpty()) {
            return isoOnly;
        }
        return "";
    }

    /** {@code KR | Seoul} — ISO2는 대문자, 도시는 첫 글자만 대문자·나머지 소문자. */
    static String finalizeOverviewDisplay(String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        String s = label.trim();
        int pipe = s.indexOf(" | ");
        if (pipe == 2 && s.length() > 5) {
            String code = s.substring(0, 2).toUpperCase(Locale.ROOT);
            String loc = normalizeLocationEnglish(s.substring(pipe + 3));
            if (loc.isEmpty()) {
                return code;
            }
            return code + " | " + loc;
        }
        if (s.length() == 2 && s.chars().allMatch(ch -> ch >= 'A' && ch <= 'Z')) {
            return s.toUpperCase(Locale.ROOT);
        }
        return s;
    }

    private static String parseLegacyStoredLabel(String stored, String iso2Hint) {
        int pipe = stored.indexOf('-');
        if (pipe > 0 && !stored.contains(" | ")) {
            String countryPart = stored.substring(0, pipe).trim();
            String regionPart = stored.substring(pipe + 1).trim();
            String code = resolveIso2(countryPart, iso2Hint);
            return formatOverview(code, regionPart);
        }
        if (!stored.contains(" | ") && !stored.contains("-")) {
            String code = resolveIso2(stored, iso2Hint);
            if (!code.isEmpty()) {
                return code;
            }
        }
        return "";
    }

    private static String resolveIso2(String countryToken, String iso2Hint) {
        String hint = normalizeIso2(iso2Hint);
        if (!hint.isEmpty()) {
            return hint;
        }
        if (countryToken == null || countryToken.isBlank()) {
            return "";
        }
        String u = countryToken.trim();
        if (u.length() == 2 && u.toUpperCase(Locale.ROOT).chars().allMatch(ch -> ch >= 'A' && ch <= 'Z')) {
            return u.toUpperCase(Locale.ROOT);
        }
        String upper = u.toUpperCase(Locale.ROOT);
        String fromEn = ENGLISH_COUNTRY_TO_ISO2.get(upper);
        if (!fromEn.isEmpty()) {
            return fromEn;
        }
        return ENGLISH_COUNTRY_TO_ISO2.getOrDefault(u, "");
    }

    /** @deprecated JPAY Export 내부 — {@link #formatOverview(String, String)} 사용 권장 */
    public static String formatEnglish(String country, String region) {
        String code = resolveIso2(country, null);
        if (!code.isEmpty()) {
            return formatOverview(code, region);
        }
        return formatOverview("", region.isEmpty() ? country : country + "-" + region);
    }

    public static String iso2ToEnglishCountry(String iso2) {
        String code = normalizeIso2(iso2);
        if (code.isEmpty()) {
            return "";
        }
        try {
            return new Locale("", code).getDisplayCountry(Locale.ENGLISH);
        } catch (Exception ignored) {
            return code;
        }
    }

    static String normalizeIso2(String iso2) {
        if (iso2 == null || iso2.isBlank()) {
            return "";
        }
        String u = iso2.trim().toUpperCase(Locale.ROOT);
        if (u.length() == 2 && u.chars().allMatch(ch -> ch >= 'A' && ch <= 'Z')) {
            return u;
        }
        return ENGLISH_COUNTRY_TO_ISO2.getOrDefault(u, "");
    }

    private static String normalizeLocationEnglish(String cityOrRegion) {
        if (cityOrRegion == null || cityOrRegion.isBlank()) {
            return "";
        }
        String v = cityOrRegion.trim();
        if (v.isEmpty()) {
            return "";
        }
        if (v.length() == 1) {
            return v.toUpperCase(Locale.ROOT);
        }
        return v.substring(0, 1).toUpperCase(Locale.ROOT) + v.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim();
        return v.isEmpty() ? "" : v;
    }
}
