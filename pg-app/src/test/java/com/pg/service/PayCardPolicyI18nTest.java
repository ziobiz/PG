package com.pg.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayCardPolicyI18nTest {

    @Test
    void tierMessageKeyClampsToFour() {
        assertEquals("CARD_COOLDOWN_TIER_1", PayCardPolicyI18n.tierCooldownMessageKey(0));
        assertEquals("CARD_COOLDOWN_TIER_2", PayCardPolicyI18n.tierCooldownMessageKey(2));
        assertEquals("CARD_COOLDOWN_TIER_4", PayCardPolicyI18n.tierCooldownMessageKey(99));
    }

    @Test
    void tierMessagesSupportAllLanguages() {
        String key = PayCardPolicyI18n.tierCooldownMessageKey(1);
        var all = PayCardPolicyI18n.allLang(key, 5);
        assertEquals(5, all.size());
        assertTrue(all.get("KO").contains("1차"));
        assertTrue(all.get("EN").toLowerCase().contains("1st"));
        assertTrue(all.get("JP").contains("1回目"));
        assertTrue(all.get("CH").contains("第1次"));
        assertTrue(all.get("TH").contains("1"));
    }

    @Test
    void inactiveCardMessageDiffersFromTierTwo() {
        String inactive = PayCardPolicyI18n.format("KO", "INACTIVE_CARD");
        String tier2 = PayCardPolicyI18n.format("KO", "CARD_COOLDOWN_TIER_2", 10);
        assertTrue(inactive.contains("고위험"));
        assertTrue(tier2.contains("2차"));
    }
}
