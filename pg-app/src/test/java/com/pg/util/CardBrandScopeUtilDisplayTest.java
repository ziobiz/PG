package com.pg.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
