package com.pg.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 거래·정산 시각 2줄 표시.
 * <p>레거시: {@link #formatDualLineTimeOnly} 등은 JP(UTC+9)·TH(UTC+7) 고정 오프셋.</p>
 * <p>총판 설정: {@link #formatConfigurableDualLineTimeOnly} 등은 태그·{@link ZoneId} 두 줄(동일 Zone이면 1줄).</p>
 * {@code naiveInterpretZone}은 naive {@link LocalDateTime}의 벽시계(전산설정 {@code display_timezone} 등)로 Instant를 구합니다.
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

    /** 총판 설정 2줄(시각만). 동일 {@link ZoneId}·동일 태그면 1줄. */
    public static String formatConfigurableDualLineTimeOnly(LocalDateTime naiveWallClock, ZoneId naiveInterpretZone,
                                                            String tag1, ZoneId display1, String tag2, ZoneId display2) {
        if (naiveWallClock == null) {
            return "";
        }
        ZoneId z = naiveInterpretZone != null ? naiveInterpretZone : ZoneId.of("Asia/Bangkok");
        ZoneId d1 = display1 != null ? display1 : ZoneId.of("Asia/Tokyo");
        ZoneId d2 = display2 != null ? display2 : ZoneId.of("Asia/Bangkok");
        String t1 = tag1 != null && !tag1.isBlank() ? tag1.trim() : "JP";
        String t2 = tag2 != null && !tag2.isBlank() ? tag2.trim() : "TH";
        Instant instant = naiveWallClock.atZone(z).toInstant();
        if (d1.equals(d2) && t1.equalsIgnoreCase(t2)) {
            return t1 + " " + TIME.format(LocalTime.ofInstant(instant, d1));
        }
        LocalTime lt1 = LocalTime.ofInstant(instant, d1);
        LocalTime lt2 = LocalTime.ofInstant(instant, d2);
        return t1 + " " + TIME.format(lt1) + "\n" + t2 + " " + TIME.format(lt2);
    }

    public static String formatConfigurableDualLineDateTime(LocalDateTime naiveWallClock, ZoneId naiveInterpretZone,
                                                              String tag1, ZoneId display1, String tag2, ZoneId display2) {
        if (naiveWallClock == null) {
            return "";
        }
        ZoneId z = naiveInterpretZone != null ? naiveInterpretZone : ZoneId.of("Asia/Bangkok");
        ZoneId d1 = display1 != null ? display1 : ZoneId.of("Asia/Tokyo");
        ZoneId d2 = display2 != null ? display2 : ZoneId.of("Asia/Bangkok");
        String t1 = tag1 != null && !tag1.isBlank() ? tag1.trim() : "JP";
        String t2 = tag2 != null && !tag2.isBlank() ? tag2.trim() : "TH";
        Instant instant = naiveWallClock.atZone(z).toInstant();
        if (d1.equals(d2) && t1.equalsIgnoreCase(t2)) {
            ZonedDateTime z1 = ZonedDateTime.ofInstant(instant, d1);
            return t1 + " " + z1.format(DT);
        }
        ZonedDateTime z1 = ZonedDateTime.ofInstant(instant, d1);
        ZonedDateTime z2 = ZonedDateTime.ofInstant(instant, d2);
        return t1 + " " + z1.format(DT) + "\n" + t2 + " " + z2.format(DT);
    }
}
