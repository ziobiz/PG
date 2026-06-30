package com.pg.service.ops;

import com.pg.entity.PgAgencyCostPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AgencyCostPolicyResolverTest {

    @Test
    void resolvesJpayVanViaBindingPgCdAndCurrency() {
        PgAgencyCostPolicy jpy = policy("JPAY_JPY", "JPY", new BigDecimal("2.5"));
        AgencyCostPolicyResolver resolver = AgencyCostPolicyResolver.from(List.of(jpy));
        Map<String, String> names = Map.of("JPAY_JPY", "JPAY API JPY");

        PgAgencyCostPolicy hit = resolver.resolve("JPAY_JPY", "JPAY", "JPY", names);

        assertNotNull(hit);
        assertEquals("JPAY_JPY", hit.getPgCd());
        assertEquals(new BigDecimal("2.5"), hit.getPayRate());
    }

    @Test
    void resolvesJpayFamilyWhenOnlyVanMatches() {
        PgAgencyCostPolicy jpy = policy("JPAY_API_JPY", "JPY", new BigDecimal("1.1"));
        AgencyCostPolicyResolver resolver = AgencyCostPolicyResolver.from(List.of(jpy));

        PgAgencyCostPolicy hit = resolver.resolve("", "JPAY", "JPY", Map.of("JPAY_API_JPY", "JPAY API JPY"));

        assertNotNull(hit);
        assertEquals("JPAY_API_JPY", hit.getPgCd());
    }

    @Test
    void skipsInactivePolicy() {
        PgAgencyCostPolicy off = policy("JPAY_JPY", "JPY", new BigDecimal("9"));
        off.setUseYn("N");
        AgencyCostPolicyResolver resolver = AgencyCostPolicyResolver.from(List.of(off));

        assertNull(resolver.resolve("JPAY_JPY", "JPAY", "JPY", Map.of()));
    }

    private static PgAgencyCostPolicy policy(String pgCd, String cur, BigDecimal payRate) {
        PgAgencyCostPolicy p = new PgAgencyCostPolicy();
        p.setPgCd(pgCd);
        p.setCurrencyCode(cur);
        p.setPayRate(payRate);
        p.setUseYn("Y");
        return p;
    }
}
