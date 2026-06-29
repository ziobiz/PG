package com.pg.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayListItemDtoPayerLocationTest {

    @Test
    void payerLocationLabelNormalizesCountryAndUsesPipeSeparator() {
        assertEquals("KR | SEOUL", PayListItemDto.payerLocationLabel("KO", "Seoul", "EN"));
        assertEquals("JP | TOKYO", PayListItemDto.payerLocationLabel("JA", "Tokyo", "EN"));
        assertEquals("US | NEW YORK", PayListItemDto.payerLocationLabel("US", "New York", "EN"));
    }

    @Test
    void payerLocationLabelKoreanSeparator() {
        assertEquals("KR \u3163 SEOUL", PayListItemDto.payerLocationLabel("KR", "Seoul", "KO"));
    }

    @Test
    void payerLocationLabelPartialValues() {
        assertEquals("SG", PayListItemDto.payerLocationLabel("SG", "", "EN"));
        assertEquals("SEOUL", PayListItemDto.payerLocationLabel("", "Seoul", "EN"));
        assertEquals("-", PayListItemDto.payerLocationLabel("", "", "EN"));
    }
}
