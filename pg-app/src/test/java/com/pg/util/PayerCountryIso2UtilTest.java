package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayerCountryIso2UtilTest {

    @Test
    void normalizeLanguageCodesToCountry() {
        assertEquals("JP", PayerCountryIso2Util.normalize("ja"));
        assertEquals("JP", PayerCountryIso2Util.normalize("JA"));
        assertEquals("KR", PayerCountryIso2Util.normalize("ko"));
        assertEquals("KR", PayerCountryIso2Util.normalize("KO"));
    }

    @Test
    void normalizeAlpha3AndStandardCodes() {
        assertEquals("JP", PayerCountryIso2Util.normalize("JPN"));
        assertEquals("KR", PayerCountryIso2Util.normalize("KOR"));
        assertEquals("US", PayerCountryIso2Util.normalize("USA"));
        assertEquals("TH", PayerCountryIso2Util.normalize("TH"));
        assertEquals("SG", PayerCountryIso2Util.normalize("SG"));
        assertEquals("HK", PayerCountryIso2Util.normalize("HK"));
        assertEquals("CH", PayerCountryIso2Util.normalize("CH"));
    }

    @Test
    void normalizeEmptyAndAmbiguous() {
        assertEquals("", PayerCountryIso2Util.normalize(null));
        assertEquals("", PayerCountryIso2Util.normalize("  "));
        assertEquals("", PayerCountryIso2Util.normalize("en"));
    }
}
