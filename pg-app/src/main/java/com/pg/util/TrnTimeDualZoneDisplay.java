package com.pg.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 거래 시각을 JP(Asia/Tokyo)·TH(Asia/Bangkok) 두 줄로 표시할 때 공통 포맷.
 * 원 시각은 {@code curType}에 따라 칠페이 집계와 동일하게 “기준 존”에 둔 뒤 두 타임존으로 변환한다.
 */
public final class TrnTimeDualZoneDisplay {

    private static final ZoneId ZONE_JP = ZoneId.of("Asia/Tokyo");
    private static final ZoneId ZONE_TH = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TrnTimeDualZoneDisplay() {
    }

    /** JPY 계열만 도쿄, 그 외는 방콕(칠페이 {@code primaryZoneForChillCurrency} 와 동일). */
    public static ZoneId interpretAsZoneForCurrency(String curRaw) {
        if (curRaw == null || curRaw.isBlank()) {
            return ZONE_TH;
        }
        String u = PayListStatusBarBuckets.normalizeCurrency(curRaw.trim());
        if ("JPY".equals(u)) {
            return ZONE_JP;
        }
        return ZONE_TH;
    }

    /** 1행: {@code JP HH:mm:ss}, 2행: {@code TH HH:mm:ss} */
    public static String formatDualLineTimeOnly(LocalDateTime naiveWallClock, ZoneId interpretAsZone) {
        if (naiveWallClock == null) {
            return "";
        }
        ZonedDateTime z = naiveWallClock.atZone(interpretAsZone);
        ZonedDateTime jp = z.withZoneSameInstant(ZONE_JP);
        ZonedDateTime th = z.withZoneSameInstant(ZONE_TH);
        return "JP " + jp.toLocalTime().format(TIME) + "\n" + "TH " + th.toLocalTime().format(TIME);
    }

    /** 1행: {@code JP yyyy-MM-dd HH:mm:ss}, 2행: {@code TH ...} */
    public static String formatDualLineDateTime(LocalDateTime naiveWallClock, ZoneId interpretAsZone) {
        if (naiveWallClock == null) {
            return "";
        }
        ZonedDateTime z = naiveWallClock.atZone(interpretAsZone);
        ZonedDateTime jp = z.withZoneSameInstant(ZONE_JP);
        ZonedDateTime th = z.withZoneSameInstant(ZONE_TH);
        return "JP " + jp.format(DT) + "\n" + "TH " + th.format(DT);
    }
}
