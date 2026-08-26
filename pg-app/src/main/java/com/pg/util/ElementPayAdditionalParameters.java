package com.pg.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ElementPay initPayment 구매자 속성.
 * URL 결제에서 배송주소(도시·우편번호)를 숨겨도 결제망 필수 검사가 있으면
 * 서버가 자리값을 채운다. 구매자에게 도시·우편번호 입력을 요구하지 않는다.
 */
public final class ElementPayAdditionalParameters {

    private ElementPayAdditionalParameters() {
    }

    public static Map<String, String> build(Map<String, Object> body, String clientIp, String phoneE164) {
        Map<String, String> extra = new LinkedHashMap<>();
        String email = firstNonBlank(str(body, "payEmailAddress"), str(body, "email"));
        String first = str(body, "payFirstname");
        String last = str(body, "payLastname");
        String name = str(body, "customerNm");
        if (name.isBlank()) {
            name = (first + " " + last).trim();
        }
        String ip = firstNonBlank(clientIp, str(body, "_payerClientIp"));
        if (ip.contains(":") || ip.startsWith("127.") || ip.startsWith("0.") || ip.isBlank()) {
            /* 루프백·IPv6 는 라이브 필수 IP 검사에서 빈 값으로 버려질 수 있다 */
            ip = "8.8.8.8";
        }
        String country = str(body, "payCountryIsoCode2").toUpperCase(Locale.ROOT);
        if (country.length() != 2) {
            country = str(body, "country").toUpperCase(Locale.ROOT);
        }
        if (country.length() != 2) {
            country = "TH";
        }
        String addr = firstNonBlank(str(body, "payStreetAddress1"), defaultStreet(country));
        String city = firstNonBlank(str(body, "payCity"), defaultCity(country));
        String zip = firstNonBlank(str(body, "payPostcode"), defaultZip(country));
        String phone = firstNonBlank(phoneE164, "66000000000");
        put(extra, "email", email.isBlank() ? "noreply@icopay.co.kr" : email);
        put(extra, "phone", phone);
        put(extra, "ip", ip.isBlank() ? "8.8.8.8" : ip);
        put(extra, "first_name", first.isBlank() ? "NA" : first);
        put(extra, "last_name", last.isBlank() ? "NA" : last);
        put(extra, "name", name.isBlank() ? "NA" : name);
        put(extra, "address", addr);
        put(extra, "city", city);
        put(extra, "zip", zip);
        put(extra, "country", country);
        return extra;
    }

    public static Map<String, String> onlyKeys(Map<String, String> canonical, Iterable<String> keys) {
        Map<String, String> out = new LinkedHashMap<>();
        if (canonical == null || keys == null) {
            return out;
        }
        for (String k : keys) {
            if (k == null || k.isBlank() || isReservedInitKey(k)) {
                continue;
            }
            String v = valueForAttributeKey(k, canonical);
            if (v != null && !v.isBlank()) {
                out.put(k, v);
            }
        }
        return out;
    }

    /** getMethods 가 비어 있는데 EP 가 속성을 요구할 때(라이브 thCardsCheckout). */
    public static final List<String> FALLBACK_ATTR_KEYS = List.of(
            "email", "phone", "ip", "country", "first_name", "last_name", "name", "address", "city", "zip",
            "PayerEmail", "PayerPhone", "PayerName", "lang");

    /** Light·구버전 EPS 가 쓰는 별칭을 같은 값으로 복제한다. */
    public static Map<String, String> withAliases(Map<String, String> canonical) {
        Map<String, String> out = new LinkedHashMap<>();
        if (canonical != null) {
            out.putAll(canonical);
        }
        alias(out, "email", "PayerEmail", "payer_email", "customer_email", "payerEmail", "Email");
        alias(out, "phone", "PayerPhone", "payer_phone", "telephone", "mobile", "tel", "Phone");
        alias(out, "ip", "ip_address", "customer_ip", "payer_ip", "customerIp", "payerIp");
        alias(out, "name", "PayerName", "payer_name", "customer_name", "cardholder", "full_name");
        alias(out, "first_name", "firstname", "firstName", "fname");
        alias(out, "last_name", "lastname", "lastName", "lname");
        alias(out, "address", "street", "address1", "street_address");
        alias(out, "city", "town");
        alias(out, "zip", "postcode", "postal_code", "zipcode");
        alias(out, "country", "country_code", "countryIso2", "iso2");
        if (!out.containsKey("lang") || out.get("lang") == null || out.get("lang").isBlank()) {
            out.put("lang", "en");
        }
        return out;
    }

    private static void alias(Map<String, String> out, String src, String... names) {
        String v = out.get(src);
        if (v == null || v.isBlank()) {
            return;
        }
        for (String n : names) {
            if (n != null && !n.isBlank() && !out.containsKey(n)) {
                out.put(n, v);
            }
        }
    }

    public static boolean isReservedInitKey(String key) {
        if (key == null) {
            return true;
        }
        String k = key.trim().toLowerCase(Locale.ROOT);
        return k.equals("service_id") || k.equals("amount") || k.equals("order") || k.equals("currency")
                || k.equals("key") || k.equals("timestamp") || k.equals("hash") || k.equals("light")
                || k.equals("additional_parameters") || k.startsWith("additional_parameters[")
                || k.equals("user_parameter") || k.startsWith("user_parameter[")
                || k.equals("_successurl") || k.equals("_rejecturl") || k.equals("_waitingurl")
                || k.equals("_merchantdata") || k.equals("method");
    }
    public static String valueForAttributeKey(String key, Map<String, String> extra) {
        if (key == null || key.isBlank() || extra == null) {
            return "";
        }
        String direct = extra.get(key);
        if (direct != null && !direct.isBlank()) {
            return direct;
        }
        String norm = key.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        for (Map.Entry<String, String> e : extra.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key) && e.getValue() != null && !e.getValue().isBlank()) {
                return e.getValue();
            }
        }
        if (norm.contains("email")) {
            return extra.getOrDefault("email", "");
        }
        if (norm.contains("phone") || norm.contains("tel") || norm.contains("mobile")) {
            return extra.getOrDefault("phone", "");
        }
        if (norm.equals("ip") || norm.contains("ip_address") || norm.contains("payer_ip")) {
            return extra.getOrDefault("ip", "");
        }
        if (norm.contains("first")) {
            return extra.getOrDefault("first_name", "");
        }
        if (norm.contains("last")) {
            return extra.getOrDefault("last_name", "");
        }
        if (norm.equals("name") || norm.contains("payer_name") || norm.contains("full_name")) {
            return extra.getOrDefault("name", "");
        }
        if (norm.contains("address") || norm.contains("street")) {
            return extra.getOrDefault("address", "");
        }
        if (norm.contains("city") || norm.contains("town")) {
            return extra.getOrDefault("city", "");
        }
        if (norm.contains("zip") || norm.contains("post") || norm.contains("postal")) {
            return extra.getOrDefault("zip", "");
        }
        if (norm.contains("country") || norm.equals("iso2")) {
            return extra.getOrDefault("country", "");
        }
        return firstNonBlank(extra.get("name"), extra.get("city"), "NA");
    }

    public static String defaultCity(String iso2) {
        return switch (iso2 == null ? "" : iso2.trim().toUpperCase(Locale.ROOT)) {
            case "KR" -> "Seoul";
            case "JP" -> "Tokyo";
            case "CN" -> "Beijing";
            case "US", "CA" -> "New York";
            case "GB" -> "London";
            case "SG" -> "Singapore";
            case "HK" -> "Hong Kong";
            case "TW" -> "Taipei";
            default -> "Bangkok";
        };
    }

    public static String defaultZip(String iso2) {
        return switch (iso2 == null ? "" : iso2.trim().toUpperCase(Locale.ROOT)) {
            case "KR" -> "03187";
            case "JP" -> "1000001";
            case "CN" -> "100000";
            case "US", "CA" -> "10001";
            case "GB" -> "SW1A1AA";
            case "SG" -> "018956";
            case "HK" -> "999077";
            case "TW" -> "100";
            default -> "10110";
        };
    }

    public static String defaultStreet(String iso2) {
        return "123 " + defaultCity(iso2) + " Road";
    }

    private static void put(Map<String, String> map, String key, String value) {
        map.put(key, value != null && !value.isBlank() ? value : "NA");
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || key == null) {
            return "";
        }
        Object v = body.get(key);
        return v != null ? v.toString().trim() : "";
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
