package com.pg.urlpay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayerLocationLabelFormatterTest {

    @Test
    void formatOverviewIso2AndTitleCaseCity() {
        assertEquals("JP | Tokyo", PayerLocationLabelFormatter.formatOverview("JP", "Tokyo"));
        assertEquals("KR | Seoul", PayerLocationLabelFormatter.formatOverview("kr", "Seoul"));
        assertEquals("KR | Seoul", PayerLocationLabelFormatter.formatOverview("KR", "SEOUL"));
        assertEquals("JP", PayerLocationLabelFormatter.formatOverview("JP", ""));
    }

    @Test
    void isCompleteOverviewLabel() {
        assertTrue(PayerLocationLabelFormatter.isCompleteOverviewLabel("JP | TOKYO"));
        assertFalse(PayerLocationLabelFormatter.isCompleteOverviewLabel("Japan"));
        assertFalse(PayerLocationLabelFormatter.isCompleteOverviewLabel("JP"));
    }

    @Test
    void normalizeKoreaWithCity() {
        assertEquals("KR | Seoul", PayerLocationLabelFormatter.normalizeForOverviewDisplay(
                "Korea", "KR", "Seoul"));
        assertEquals("KR | Seoul", PayerLocationLabelFormatter.normalizeForOverviewDisplay(
                "한국", "KR", "seoul"));
    }

    @Test
    void normalizeLegacyJapanLabel() {
        assertEquals("JP", PayerLocationLabelFormatter.normalizeForOverviewDisplay("Japan", "JP", ""));
        assertEquals("JP | Tokyo", PayerLocationLabelFormatter.normalizeForOverviewDisplay(
                "Japan-Tokyo", "JP", ""));
        assertEquals("JP | Osaka", PayerLocationLabelFormatter.normalizeForOverviewDisplay(
                null, "JP", "Osaka"));
        assertEquals("JP | Chiba prefecture", PayerLocationLabelFormatter.normalizeForOverviewDisplay(
                "JP | CHIBA PREFECTURE", "JP", ""));
    }
}
