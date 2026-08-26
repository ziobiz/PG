package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ElementPayAdditionalParametersTest {

    @Test
    void hiddenAddress_isFilledFromCountry_notBlank() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payEmailAddress", "buyer@example.com");
        body.put("payFirstname", "Hong");
        body.put("payLastname", "Gildong");
        body.put("payCountryIsoCode2", "KR");
        Map<String, String> extra = ElementPayAdditionalParameters.build(body, "2001:db8::1", "82101111");
        assertEquals("Seoul", extra.get("city"));
        assertEquals("03187", extra.get("zip"));
        assertFalse(extra.get("address").isBlank());
        assertEquals("buyer@example.com", extra.get("email"));
        assertEquals("KR", extra.get("country"));
        assertEquals("8.8.8.8", extra.get("ip"));
        Map<String, String> aliased = ElementPayAdditionalParameters.withAliases(extra);
        assertEquals("buyer@example.com", aliased.get("PayerEmail"));
        assertEquals("en", aliased.get("lang"));
    }

    @Test
    void valueForAttributeKey_matchesAliases() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payEmailAddress", "a@b.c");
        body.put("payCountryIsoCode2", "TH");
        Map<String, String> extra = ElementPayAdditionalParameters.build(body, "8.8.8.8", "66812345678");
        assertEquals("a@b.c", ElementPayAdditionalParameters.valueForAttributeKey("PayerEmail", extra));
        assertEquals("Bangkok", ElementPayAdditionalParameters.valueForAttributeKey("city", extra));
        assertEquals("10110", ElementPayAdditionalParameters.valueForAttributeKey("postal_code", extra));
    }

    @Test
    void onlyKeys_includesName_andNameIsNotReserved() {
        assertFalse(ElementPayAdditionalParameters.isReservedInitKey("name"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payEmailAddress", "a@b.c");
        body.put("payCountryIsoCode2", "KR");
        Map<String, String> canonical = ElementPayAdditionalParameters.build(body, "1.2.3.4", "82101111");
        Map<String, String> none = ElementPayAdditionalParameters.onlyKeys(canonical, java.util.List.of());
        assertEquals(0, none.size());
        Map<String, String> emailOnly = ElementPayAdditionalParameters.onlyKeys(canonical, java.util.List.of("email", "name"));
        assertEquals("a@b.c", emailOnly.get("email"));
        assertEquals("NA", emailOnly.get("name"));
    }
}
