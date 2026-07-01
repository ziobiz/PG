package com.pg.urlpay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JpayLocationHintEnglishMapperTest {

    @Test
    void mapsJapanChiba() {
        assertEquals("JP | CHIBA PREFECTURE",
                JpayLocationHintEnglishMapper.toOverviewLabel("日本-千葉縣"));
    }

    @Test
    void mapsJapanTokyo() {
        assertEquals("JP | TOKYO",
                JpayLocationHintEnglishMapper.toOverviewLabel("日本-东京都"));
    }
}
