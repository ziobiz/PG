package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardBrandScopeUtilDisplayTest {

    @Test
    void displayCodesJoined_emptyIsDash() {
        assertEquals("-", CardBrandScopeUtil.displayCodesJoined(null));
        assertEquals("-", CardBrandScopeUtil.displayCodesJoined(List.of()));
    }

    @Test
    void displayCodesJoined_stripsParentheticalByUsingCodesOnly() {
        assertEquals("VM", CardBrandScopeUtil.displayCodesJoined(List.of("VM")));
        assertEquals("ALL", CardBrandScopeUtil.displayCodesJoined(List.of("ALL")));
    }

    @Test
    void displayCodesJoined_dedupesAndKeepsOrder() {
        assertEquals("VM, ALL", CardBrandScopeUtil.displayCodesJoined(List.of("VM", "vm", "ALL")));
    }

    @Test
    void noneDoesNotMatchAnyCard() {
        assertTrue(CardBrandScopeUtil.isNone("NONE"));
        assertFalse(CardBrandScopeUtil.isNone("ALL"));
        assertFalse(CardBrandScopeUtil.isNone(""));
        assertFalse(CardBrandScopeUtil.matchesScope("NONE", "V"));
        assertFalse(CardBrandScopeUtil.matchesScope("NONE", ""));
        assertEquals("NONE", CardBrandScopeUtil.normalize("NONE"));
    }
}
