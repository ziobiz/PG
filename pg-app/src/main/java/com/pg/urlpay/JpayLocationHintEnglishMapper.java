package com.pg.urlpay;

import java.util.Map;

/** GeoIP 실패 시 JPAY Export {@code Customer IP} 접미사(예: 日本-千葉縣) → {@code JP | CHIBA PREFECTURE}. */
public final class JpayLocationHintEnglishMapper {

    private static final Map<String, String> COUNTRY_ISO2 = Map.ofEntries(
            Map.entry("日本", "JP"),
            Map.entry("中国", "CN"),
            Map.entry("韩国", "KR"),
            Map.entry("韓國", "KR"),
            Map.entry("美国", "US"),
            Map.entry("美國", "US"),
            Map.entry("英国", "GB"),
            Map.entry("英國", "GB"),
            Map.entry("泰国", "TH"),
            Map.entry("泰國", "TH"),
            Map.entry("新加坡", "SG"),
            Map.entry("台湾", "TW"),
            Map.entry("臺灣", "TW"),
            Map.entry("香港", "HK")
    );

    private static final Map<String, String> JP_REGION = Map.ofEntries(
            Map.entry("东京都", "Tokyo"),
            Map.entry("東京都", "Tokyo"),
            Map.entry("大阪府", "Osaka Prefecture"),
            Map.entry("京都府", "Kyoto Prefecture"),
            Map.entry("北海道", "Hokkaido"),
            Map.entry("千葉縣", "Chiba Prefecture"),
            Map.entry("千葉県", "Chiba Prefecture"),
            Map.entry("神奈川县", "Kanagawa Prefecture"),
            Map.entry("神奈川縣", "Kanagawa Prefecture"),
            Map.entry("埼玉县", "Saitama Prefecture"),
            Map.entry("埼玉縣", "Saitama Prefecture"),
            Map.entry("爱知县", "Aichi Prefecture"),
            Map.entry("愛知縣", "Aichi Prefecture"),
            Map.entry("福冈县", "Fukuoka Prefecture"),
            Map.entry("福岡縣", "Fukuoka Prefecture"),
            Map.entry("冲绳县", "Okinawa Prefecture"),
            Map.entry("沖縄縣", "Okinawa Prefecture"),
            Map.entry("渋谷区", "Shibuya"),
            Map.entry("涩谷区", "Shibuya")
    );

    private JpayLocationHintEnglishMapper() {
    }

    public static String toOverviewLabel(String locationSuffix) {
        if (locationSuffix == null || locationSuffix.isBlank()) {
            return "";
        }
        String v = locationSuffix.trim();
        int dash = v.indexOf('-');
        if (dash < 0) {
            String iso2 = COUNTRY_ISO2.getOrDefault(v, "");
            return iso2.isEmpty() ? "" : PayerLocationLabelFormatter.formatOverview(iso2, "");
        }
        String countryPart = v.substring(0, dash).trim();
        String regionPart = v.substring(dash + 1).trim();
        String iso2 = COUNTRY_ISO2.getOrDefault(countryPart, "");
        String region = mapRegion(iso2, regionPart);
        return PayerLocationLabelFormatter.formatOverview(iso2, region);
    }

    /** @deprecated {@link #toOverviewLabel(String)} */
    public static String toEnglishLabel(String locationSuffix) {
        return toOverviewLabel(locationSuffix);
    }

    private static String mapRegion(String countryIso2, String regionPart) {
        if (regionPart.isEmpty()) {
            return "";
        }
        String mapped = JP_REGION.get(regionPart);
        if (mapped != null) {
            return mapped;
        }
        if ("JP".equalsIgnoreCase(countryIso2)) {
            for (Map.Entry<String, String> e : JP_REGION.entrySet()) {
                if (regionPart.contains(e.getKey()) || e.getKey().contains(regionPart)) {
                    return e.getValue();
                }
            }
        }
        return regionPart;
    }
}
