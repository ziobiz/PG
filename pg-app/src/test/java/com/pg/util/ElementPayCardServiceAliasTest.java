package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementPayCardServiceAliasTest {

    @Test
    void liveDoesNotRewriteExplicitKCards() {
        assertEquals("kCards", ElementPayCardServiceAlias.resolveConfigured("kCards", false));
        assertEquals("thCardsCheckout", ElementPayCardServiceAlias.resolveConfigured("thCardsCheckout", false));
        assertEquals("thCardsCheckout", ElementPayCardServiceAlias.resolveConfigured("card", false));
        assertEquals("kCards", ElementPayCardServiceAlias.resolveConfigured("", true));
    }

    @Test
    void liveMissingHostedCheckoutFallsBackToKCards() {
        List<Map<String, Object>> live = List.of(
                row("56", "thaiQr", "Thai QR"),
                row("77", "kCards", "Visa/MasterCard/JCB/UnionPay")
        );
        assertFalse(ElementPayCardServiceAlias.catalogContains(live, "thCardsCheckout"));
        assertTrue(ElementPayCardServiceAlias.catalogContains(live, "kCards"));
        assertEquals("kCards",
                ElementPayCardServiceAlias.resolveAgainstCatalog("thCardsCheckout", live));
        assertEquals("kCards",
                ElementPayCardServiceAlias.resolveAgainstCatalog("kCards", live));
    }

    @Test
    void keepsHostedWhenPresent() {
        List<Map<String, Object>> both = List.of(
                row("77", "kCards", "Visa"),
                row("90", "thCardsCheckout", "TH Cards Checkout")
        );
        assertEquals("thCardsCheckout",
                ElementPayCardServiceAlias.resolveAgainstCatalog("thCardsCheckout", both));
        assertEquals("kCards",
                ElementPayCardServiceAlias.resolveAgainstCatalog("kCards", both));
        assertEquals("thCardsCheckout", ElementPayCardServiceAlias.suggestCard(both));
    }

    private static Map<String, Object> row(String id, String alias, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("alias", alias);
        m.put("name", name);
        return m;
    }
}
