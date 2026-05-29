package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** ISO 3166-1 alpha-2 → E.164 국가번호(선행 +). */
public final class PhoneDialCodeCatalog {

    private static final Map<String, String> DIAL_BY_ISO = buildDialMap();

    private PhoneDialCodeCatalog() {
    }

    public static String dialForIso2(String iso2) {
        if (iso2 == null || iso2.isBlank()) {
            return "";
        }
        String key = iso2.trim().toUpperCase(Locale.ROOT);
        return DIAL_BY_ISO.getOrDefault(key, "");
    }

    public static String canonicalIso2(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return u.length() == 2 ? u : "";
    }

    private static Map<String, String> buildDialMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("AF", "+93");
        m.put("AL", "+355");
        m.put("DZ", "+213");
        m.put("AR", "+54");
        m.put("AU", "+61");
        m.put("AT", "+43");
        m.put("BD", "+880");
        m.put("BE", "+32");
        m.put("BR", "+55");
        m.put("BN", "+673");
        m.put("BG", "+359");
        m.put("KH", "+855");
        m.put("CA", "+1");
        m.put("CL", "+56");
        m.put("CN", "+86");
        m.put("CO", "+57");
        m.put("HR", "+385");
        m.put("CY", "+357");
        m.put("CZ", "+420");
        m.put("DK", "+45");
        m.put("EG", "+20");
        m.put("FI", "+358");
        m.put("FR", "+33");
        m.put("DE", "+49");
        m.put("GR", "+30");
        m.put("HK", "+852");
        m.put("HU", "+36");
        m.put("IN", "+91");
        m.put("ID", "+62");
        m.put("IE", "+353");
        m.put("IL", "+972");
        m.put("IT", "+39");
        m.put("JP", "+81");
        m.put("JO", "+962");
        m.put("KZ", "+7");
        m.put("KE", "+254");
        m.put("KR", "+82");
        m.put("KW", "+965");
        m.put("LA", "+856");
        m.put("LU", "+352");
        m.put("MO", "+853");
        m.put("MY", "+60");
        m.put("MX", "+52");
        m.put("MM", "+95");
        m.put("NL", "+31");
        m.put("NZ", "+64");
        m.put("NG", "+234");
        m.put("NO", "+47");
        m.put("PK", "+92");
        m.put("PH", "+63");
        m.put("PL", "+48");
        m.put("PT", "+351");
        m.put("QA", "+974");
        m.put("RO", "+40");
        m.put("RU", "+7");
        m.put("SA", "+966");
        m.put("RS", "+381");
        m.put("SG", "+65");
        m.put("SK", "+421");
        m.put("SI", "+386");
        m.put("ZA", "+27");
        m.put("ES", "+34");
        m.put("LK", "+94");
        m.put("SE", "+46");
        m.put("CH", "+41");
        m.put("TW", "+886");
        m.put("TH", "+66");
        m.put("TR", "+90");
        m.put("AE", "+971");
        m.put("GB", "+44");
        m.put("US", "+1");
        m.put("VN", "+84");
        return Map.copyOf(m);
    }
}
