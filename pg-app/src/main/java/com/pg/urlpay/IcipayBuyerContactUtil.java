package com.pg.urlpay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ICOPAY 통합 가맹 prepare — {@code buyer} 연락처(이메일·전화·국가 ISO2) 정규화·검증.
 * PG별 전송 여부는 {@link UrlPayVendorContactPolicy} 에서 결정합니다.
 */
public final class IcipayBuyerContactUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public enum BuyerFailKind {
        OBJECT, JSON, EMAIL, PHONE, COUNTRY
    }

    public static final class BuyerRequiredException extends IllegalArgumentException {
        private final BuyerFailKind kind;

        public BuyerRequiredException(BuyerFailKind kind, String message) {
            super(message);
            this.kind = kind != null ? kind : BuyerFailKind.OBJECT;
        }

        public BuyerFailKind kind() {
            return kind;
        }

        public Map<String, Object> toFailMap() {
            return switch (kind) {
                case JSON -> CheckoutFailI18n.buyerJsonInvalid();
                case EMAIL -> CheckoutFailI18n.buyerEmailRequired();
                case PHONE -> CheckoutFailI18n.buyerPhoneRequired();
                case COUNTRY -> CheckoutFailI18n.buyerCountryRequired();
                default -> CheckoutFailI18n.buyerObjectRequired();
            };
        }
    }

    private IcipayBuyerContactUtil() {
    }

    /**
     * 통합 prepare 필수: {@code buyer.email}, {@code buyer.phone}, {@code buyer.countryIso2}.
     *
     * @throws BuyerRequiredException 검증 실패(5개국어 fail map: {@link BuyerRequiredException#toFailMap()})
     */
    public static Map<String, String> extractAndValidateRequired(Map<String, Object> body) {
        Map<String, Object> raw = extractRawMap(body);
        if (raw.isEmpty()) {
            throw new BuyerRequiredException(BuyerFailKind.OBJECT,
                    "buyer 객체(email·phone·countryIso2)가 필요합니다.");
        }
        Map<String, String> normalized = normalize(raw);
        validateRequired(normalized);
        return normalized;
    }

    /**
     * URL sale 본문({@code payEmailAddress} 등)에 구매자 연락처가 없으면 ICOPAY fail map.
     * 통과하면 {@code null}.
     */
    public static Map<String, Object> failMapIfSaleContactMissing(Map<String, Object> saleBody) {
        try {
            extractAndValidateRequired(wrapSaleFieldsAsBuyerBody(saleBody));
            return null;
        } catch (BuyerRequiredException ex) {
            return ex.toFailMap();
        }
    }

    static Map<String, Object> wrapSaleFieldsAsBuyerBody(Map<String, Object> saleBody) {
        Map<String, Object> raw = new LinkedHashMap<>(extractRawMap(saleBody));
        putIfBlank(raw, "email", saleBody, "payEmailAddress", "email");
        putIfBlank(raw, "phone", saleBody, "payTelephone", "phone", "telephone");
        putIfBlank(raw, "countryIso2", saleBody, "payCountryIsoCode2", "countryIso2", "country");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("buyer", raw);
        return body;
    }

    private static void putIfBlank(Map<String, Object> raw, String canonical,
                                   Map<String, Object> saleBody, String... keys) {
        Object cur = raw.get(canonical);
        if (cur != null && !cur.toString().isBlank()) {
            return;
        }
        if (saleBody == null) {
            return;
        }
        for (String k : keys) {
            Object v = saleBody.get(k);
            if (v != null && !v.toString().isBlank()) {
                raw.put(canonical, v.toString().trim());
                return;
            }
        }
    }

    /**
     * body 에 buyer / buyerPrefill 이 있으면 JSON 문자열로 반환. 없으면 null.
     *
     * @throws IllegalArgumentException 형식 오류
     */
    public static String resolvePrefillJsonFromBodyOptional(Map<String, Object> body) {
        Map<String, Object> raw = extractRawMap(body);
        if (raw.isEmpty()) {
            return null;
        }
        Map<String, String> normalized = normalize(raw);
        return toPrefillJson(normalized);
    }

    public static Map<String, Object> toPublicMap(Map<String, String> normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        normalized.forEach(out::put);
        return out;
    }

    public static String toPrefillJson(Map<String, String> normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("buyerPrefill 저장 형식 오류");
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> extractRawMap(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return Map.of();
        }
        Object raw = body.get("buyer");
        if (raw == null) {
            raw = body.get("buyerPrefill");
        }
        if (raw == null) {
            raw = body.get("buyer_prefill");
        }
        if (raw instanceof Map<?, ?> m) {
            Map<String, Object> map = new LinkedHashMap<>();
            m.forEach((k, v) -> {
                if (k != null && v != null) {
                    map.put(String.valueOf(k), v);
                }
            });
            return map;
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return MAPPER.readValue(s.trim(), Map.class);
            } catch (JsonProcessingException e) {
                throw new BuyerRequiredException(BuyerFailKind.JSON, "buyer JSON 형식이 올바르지 않습니다.");
            }
        }
        return Map.of();
    }

    static Map<String, String> normalize(Map<String, Object> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        putAlias(out, raw, "email", "payEmailAddress");
        putAlias(out, raw, "phone", "payTelephone", "telephone");
        putAlias(out, raw, "countryIso2", "payCountryIsoCode2", "country");
        putAlias(out, raw, "firstName", "payFirstname", "firstname", "givenName");
        putAlias(out, raw, "lastName", "payLastname", "lastname", "surname", "familyName");
        putAlias(out, raw, "address", "payStreetAddress1", "streetAddress1", "billingAddress");
        putAlias(out, raw, "address2", "payStreetAddress2", "streetAddress2");
        putAlias(out, raw, "city", "payCity");
        putAlias(out, raw, "state", "payState");
        putAlias(out, raw, "postcode", "payPostcode", "zip");
        putAlias(out, raw, "shippingAddress", "shippingStreetAddress1");
        putAlias(out, raw, "shippingAddress2", "shippingStreetAddress2");
        putAlias(out, raw, "shippingCity", "shippingCity");
        putAlias(out, raw, "shippingState", "shippingState");
        putAlias(out, raw, "shippingPostcode", "shippingPostcode");
        putAlias(out, raw, "shippingCountryIso2", "shippingCountryIsoCode2");
        putAlias(out, raw, "shippingPhone", "shippingTelephone");
        if (out.containsKey("phone")) {
            out.put("phone", stripDialPrefix(out.get("phone")));
        }
        if (out.containsKey("countryIso2")) {
            out.put("countryIso2", JpayBuyerPrefillUtil.canonicalCountryIso2(out.get("countryIso2")));
        }
        return out;
    }

    private static void validateRequired(Map<String, String> n) {
        if (n.get("email") == null || n.get("email").isBlank()) {
            throw new BuyerRequiredException(BuyerFailKind.EMAIL, "buyer.email 이 필요합니다.");
        }
        if (n.get("phone") == null || n.get("phone").isBlank()) {
            throw new BuyerRequiredException(BuyerFailKind.PHONE, "buyer.phone 이 필요합니다.");
        }
        String country = n.get("countryIso2");
        if (country == null || country.length() != 2) {
            throw new BuyerRequiredException(BuyerFailKind.COUNTRY,
                    "buyer.countryIso2(ISO2 국가코드)가 필요합니다.");
        }
    }

    private static String stripDialPrefix(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\+\\d{1,4}[\\s\\-]*(.*)$").matcher(t);
        if (m.matches()) {
            return m.group(1).trim();
        }
        return t;
    }

    private static void putAlias(Map<String, String> out, Map<String, Object> raw, String canonical, String... aliases) {
        Object cv = raw.get(canonical);
        if (cv != null && !cv.toString().isBlank()) {
            out.put(canonical, cv.toString().trim());
            return;
        }
        for (String k : aliases) {
            Object v = raw.get(k);
            if (v != null && !v.toString().isBlank()) {
                out.put(canonical, v.toString().trim());
                return;
            }
        }
    }
}
