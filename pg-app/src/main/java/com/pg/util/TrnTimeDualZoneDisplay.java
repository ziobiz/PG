package com.pg.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 거래·정산 시각을 <strong>JP(UTC+9)</strong> 한 줄과 <strong>TH(UTC+7)</strong> 한 줄로 표시합니다.
 * {@code naiveInterpretZone}은 naive {@link LocalDateTime}의 벽시계(전산설정 {@code display_timezone} 등)로,
 * 그 순간의 {@link Instant}를 구한 뒤 JP·TH는 <strong>고정 오프셋</strong>으로만 표시합니다(DST 없음).
 */
public final class TrnTimeDualZoneDisplay {

    private static final ZoneOffset DISPLAY_JP = ZoneOffset.ofHours(9);
    private static final ZoneOffset DISPLAY_TH = ZoneOffset.ofHours(7);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TrnTimeDualZoneDisplay() {
    }

    /** 1행: {@code JP HH:mm:ss}, 2행: {@code TH HH:mm:ss} */
    public static String formatDualLineTimeOnly(LocalDateTime naiveWallClock, ZoneId naiveInterpretZone) {
        if (naiveWallClock == null) {
            return "";
        }
        ZoneId z = naiveInterpretZone != null ? naiveInterpretZone : ZoneId.of("Asia/Bangkok");
        Instant instant = naiveWallClock.atZone(z).toInstant();
        LocalTime jpLt = LocalTime.ofInstant(instant, DISPLAY_JP);
        LocalTime thLt = LocalTime.ofInstant(instant, DISPLAY_TH);
        return "JP " + TIME.format(jpLt) + "\n" + "TH " + TIME.format(thLt);
    }

    /** 1행: {@code JP yyyy-MM-dd HH:mm:ss}, 2행: {@code TH ...} */
    public static String formatDualLineDateTime(LocalDateTime naiveWallClock, ZoneId naiveInterpretZone) {
        if (naiveWallClock == null) {
            return "";
        }
        ZoneId z = naiveInterpretZone != null ? naiveInterpretZone : ZoneId.of("Asia/Bangkok");
        Instant instant = naiveWallClock.atZone(z).toInstant();
        ZonedDateTime jp = ZonedDateTime.ofInstant(instant, DISPLAY_JP);
        ZonedDateTime th = ZonedDateTime.ofInstant(instant, DISPLAY_TH);
        return "JP " + jp.format(DT) + "\n" + "TH " + th.format(DT);
    }

    /**
     * 구간 시작·끝(naive, 동일 {@code naiveInterpretZone})을 JP·TH 두 줄로
     * {@code JP 시작 ~ 끝} / {@code TH 시작 ~ 끝} 형태로 반환합니다.
     */
    public static String formatDualLineDateTimeRange(LocalDateTime startNaive, LocalDateTime endNaive, ZoneId naiveInterpretZone) {
        if (startNaive == null || endNaive == null) {
            return "";
        }
        ZoneId z = naiveInterpretZone != null ? naiveInterpretZone : ZoneId.of("Asia/Bangkok");
        Instant si = startNaive.atZone(z).toInstant();
        Instant ei = endNaive.atZone(z).toInstant();
        ZonedDateTime sJp = ZonedDateTime.ofInstant(si, DISPLAY_JP);
        ZonedDateTime eJp = ZonedDateTime.ofInstant(ei, DISPLAY_JP);
        ZonedDateTime sTh = ZonedDateTime.ofInstant(si, DISPLAY_TH);
        ZonedDateTime eTh = ZonedDateTime.ofInstant(ei, DISPLAY_TH);
        return "JP " + sJp.format(DT) + " ~ " + eJp.format(DT) + "\n" + "TH " + sTh.format(DT) + " ~ " + eTh.format(DT);
    }
}
