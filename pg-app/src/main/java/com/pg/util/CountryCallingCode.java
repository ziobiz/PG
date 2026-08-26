package com.pg.util;

import java.util.Locale;
import java.util.Map;

/** ISO 3166-1 alpha-2 → ITU 국가번호(숫자만). */
public final class CountryCallingCode {

    private static final Map<String, String> ISO2_TO_CC = Map.ofEntries(
            Map.entry("KR", "82"), Map.entry("JP", "81"), Map.entry("TH", "66"),
            Map.entry("US", "1"), Map.entry("CA", "1"), Map.entry("CN", "86"),
            Map.entry("HK", "852"), Map.entry("TW", "886"), Map.entry("SG", "65"),
            Map.entry("VN", "84"), Map.entry("MY", "60"), Map.entry("ID", "62"),
            Map.entry("PH", "63"), Map.entry("AU", "61"), Map.entry("NZ", "64"),
            Map.entry("GB", "44"), Map.entry("DE", "49"), Map.entry("FR", "33"),
            Map.entry("IT", "39"), Map.entry("ES", "34"), Map.entry("NL", "31"),
            Map.entry("BE", "32"), Map.entry("CH", "41"), Map.entry("AT", "43"),
            Map.entry("SE", "46"), Map.entry("NO", "47"), Map.entry("DK", "45"),
            Map.entry("FI", "358"), Map.entry("IE", "353"), Map.entry("PT", "351"),
            Map.entry("GR", "30"), Map.entry("PL", "48"), Map.entry("CZ", "420"),
            Map.entry("HU", "36"), Map.entry("RO", "40"), Map.entry("RU", "7"),
            Map.entry("TR", "90"), Map.entry("AE", "971"), Map.entry("SA", "966"),
            Map.entry("IL", "972"), Map.entry("IN", "91"), Map.entry("BD", "880"),
            Map.entry("PK", "92"), Map.entry("LK", "94"), Map.entry("KH", "855"),
            Map.entry("LA", "856"), Map.entry("MM", "95"), Map.entry("BN", "673"),
            Map.entry("MO", "853"), Map.entry("KZ", "7"), Map.entry("UZ", "998"),
            Map.entry("BR", "55"), Map.entry("MX", "52"), Map.entry("AR", "54"),
            Map.entry("CL", "56"), Map.entry("CO", "57"), Map.entry("ZA", "27"),
            Map.entry("EG", "20"), Map.entry("NG", "234"), Map.entry("KE", "254"),
            Map.entry("JO", "962"), Map.entry("KW", "965"), Map.entry("QA", "974")
    );

    private CountryCallingCode() {
    }

    public static String forIso2(String iso2) {
        if (iso2 == null || iso2.isBlank()) {
            return "";
        }
        return ISO2_TO_CC.getOrDefault(iso2.trim().toUpperCase(Locale.ROOT), "");
    }
}
