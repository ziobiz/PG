package com.pg.util;

import java.time.ZoneId;
import java.util.Locale;

/**
 * IANA {@link ZoneId} → 거래·정산 그리드 2줄 시각 접두 태그 (TH, JP, CH, SG, VT …).
 * <p>본사 전산설정 표준·운영 시간대 및 총판 정산 크론 Zone 표시에 공통 사용.</p>
 */
public final class LedgerZoneDisplayTag {

    private LedgerZoneDisplayTag() {
    }

    public static String zoneIdToShortTag(ZoneId zoneId) {
        if (zoneId == null) {
            return "KR";
        }
        String id = zoneId.getId();
        return switch (id) {
            case "Asia/Seoul" -> "KR";
            case "Asia/Tokyo" -> "JP";
            case "Asia/Bangkok" -> "TH";
            case "Asia/Shanghai" -> "CH";
            case "Asia/Singapore" -> "SG";
            case "Asia/Ho_Chi_Minh", "Asia/Saigon" -> "VT";
            case "Asia/Hong_Kong" -> "HK";
            case "Asia/Manila" -> "PP";
            case "Asia/Jakarta" -> "IN";
            case "Asia/Dubai" -> "UA";
            case "America/New_York" -> "NY";
            case "America/Los_Angeles" -> "LA";
            case "Europe/London" -> "EU";
            case "UTC" -> "UTC";
            default -> tailTag(id);
        };
    }

    private static String tailTag(String zoneId) {
        int slash = zoneId.lastIndexOf('/');
        String tail = slash >= 0 ? zoneId.substring(slash + 1) : zoneId;
        if (tail.length() > 4) {
            tail = tail.substring(0, 4);
        }
        return tail.toUpperCase(Locale.ROOT);
    }
}
