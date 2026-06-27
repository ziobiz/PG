package com.pg.util;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LedgerZoneDisplayTagTest {

    @Test
    void mapsLedgerPresetCountries() {
        assertEquals("TH", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Asia/Bangkok")));
        assertEquals("JP", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Asia/Tokyo")));
        assertEquals("CH", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Asia/Shanghai")));
        assertEquals("SG", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Asia/Singapore")));
        assertEquals("VT", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Asia/Ho_Chi_Minh")));
        assertEquals("KR", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Asia/Seoul")));
        assertEquals("PP", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Asia/Manila")));
        assertEquals("IN", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Asia/Jakarta")));
        assertEquals("UA", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Asia/Dubai")));
        assertEquals("EU", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("Europe/London")));
        assertEquals("NY", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("America/New_York")));
        assertEquals("LA", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("America/Los_Angeles")));
        assertEquals("UTC", LedgerZoneDisplayTag.zoneIdToShortTag(ZoneId.of("UTC")));
    }
}
