package com.pg.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(10, all.size());
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
        assertTrue(inactive.contains("비활성"));
        assertFalse(inactive.contains("고위험"));
        assertTrue(tier2.contains("2차"));
    }

    @Test
    void inactiveCardApiMessagesIncludeCheckoutLangKeys() {
        var all = PayCardPolicyI18n.allLang("INACTIVE_CARD");
        assertEquals(all.get("KO"), all.get("KOR"));
        assertEquals(all.get("EN"), all.get("ENG"));
        assertTrue(all.get("KOR").contains("비활성"));
    }

    @Test
    void brandNotAllowedListsAllowedAndPausedInEachLanguage() {
        var allowed = java.util.List.of(com.pg.util.PayCardBrand.VISA, com.pg.util.PayCardBrand.MASTERCARD);
        var paused = java.util.List.of(com.pg.util.PayCardBrand.JCB, com.pg.util.PayCardBrand.UNIONPAY, com.pg.util.PayCardBrand.AMEX);
        var all = PayCardPolicyI18n.allLangBrandNotAllowed(allowed, paused);
        assertEquals("VISA, Master 결제 가능 / JCB & UNION & AMX 사용 일시 중지", all.get("KO"));
        assertEquals("VISA, Master accepted / JCB & UNION & AMX temporarily suspended", all.get("EN"));
        assertTrue(all.get("JP").contains("ご利用可能"));
        assertTrue(all.get("JP").contains("一時停止"));
        assertTrue(all.get("CH").contains("可支付"));
        assertTrue(all.get("CH").contains("暂时停用"));
        assertTrue(all.get("TH").contains("ชำระได้"));
        assertTrue(all.get("TH").contains("ระงับชั่วคราว"));
        assertEquals(all.get("KO"), all.get("KOR"));
    }

    @Test
    void pausedBrandsFollowPgSupportMinusAllowed() {
        var allowed = java.util.EnumSet.of(com.pg.util.PayCardBrand.VISA, com.pg.util.PayCardBrand.MASTERCARD);
        var pg = java.util.EnumSet.of(
                com.pg.util.PayCardBrand.VISA, com.pg.util.PayCardBrand.MASTERCARD,
                com.pg.util.PayCardBrand.JCB, com.pg.util.PayCardBrand.UNIONPAY);
        var paused = PayCardPolicyI18n.pausedBrands(allowed, pg);
        assertEquals(java.util.List.of(com.pg.util.PayCardBrand.JCB, com.pg.util.PayCardBrand.UNIONPAY), paused);
    }
}
