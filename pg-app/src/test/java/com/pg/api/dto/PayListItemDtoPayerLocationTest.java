package com.pg.api.dto;

import com.pg.entity.PgTrnsctn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayListItemDtoPayerLocationTest {

    @Test
    void displayPayerRegionUsesIso2OverviewFormat() {
        PgTrnsctn t = new PgTrnsctn();
        t.setPayerLocationLabel("JP | CHIBA PREFECTURE");
        assertEquals("JP | Chiba prefecture", PayListItemDto.displayPayerRegion(t));
    }

    @Test
    void displayPayerRegionNormalizesLegacyJapanOnly() {
        PgTrnsctn t = new PgTrnsctn();
        t.setPayerLocationLabel("Japan");
        t.setPayerCountryIso2("JP");
        assertEquals("JP", PayListItemDto.displayPayerRegion(t));
    }

    @Test
    void displayPayerRegionFromIsoAndCity() {
        PgTrnsctn t = new PgTrnsctn();
        t.setPayerCountryIso2("JP");
        t.setPayerCity("Tokyo");
        assertEquals("JP | Tokyo", PayListItemDto.displayPayerRegion(t));
    }

    @Test
    void payerLocationEnglishLegacyUsesPipeFormat() {
        assertEquals("KR | Seoul", PayListItemDto.payerLocationEnglishLegacy("KR", "Seoul"));
        assertEquals("-", PayListItemDto.payerLocationEnglishLegacy("", ""));
    }
}
