package com.pg.util;

import com.pg.api.dto.TxnDualLineSpec;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 거래·정산 시각 2줄 표시.
 * <p>1줄=운영 시간대(전산설정 {@code operational_timezone}), 2줄=표준 시간대(ICOPAY {@code display_timezone}).</p>
 * {@code naiveInterpretZone}은 naive {@link LocalDateTime}의 벽시계(표준 시간대)로 Instant를 구합니다.
 */
public final class TrnTimeDualZoneDisplay {

    private static final ZoneId LEGACY_DEFAULT_OPERATIONAL = ZoneId.of("Asia/Tokyo");
    private static final ZoneId LEGACY_DEFAULT_STANDARD = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TrnTimeDualZoneDisplay() {
    }

    /** 레거시 호환 — 운영=Tokyo, 표준=Bangkok IANA (고정 오프셋 아님). */
    public static String formatDualLineTimeOnly(LocalDateTime naiveWallClock, ZoneId naiveInterpretZone) {
        return formatConfigurableDualLineTimeOnly(naiveWallClock, naiveInterpretZone,
                "JP", LEGACY_DEFAULT_OPERATIONAL, "TH", LEGACY_DEFAULT_STANDARD);
    }

    public static String formatDualLineDateTime(LocalDateTime naiveWallClock, ZoneId naiveInterpretZone) {
        return formatConfigurableDualLineDateTime(naiveWallClock, naiveInterpretZone,
                "JP", LEGACY_DEFAULT_OPERATIONAL, "TH", LEGACY_DEFAULT_STANDARD);
    }

    public static String formatDualLineDateTimeRange(LocalDateTime startNaive, LocalDateTime endNaive, ZoneId naiveInterpretZone) {
        return formatDualLineDateTimeRange(startNaive, endNaive, naiveInterpretZone, null);
    }

    public static String formatDualLineDateTimeRange(LocalDateTime startNaive, LocalDateTime endNaive,
                                                       ZoneId naiveInterpretZone, TxnDualLineSpec spec) {
        if (startNaive == null || endNaive == null) {
            return "";
        }
        if (spec == null) {
            ZoneId z = naiveInterpretZone != null ? naiveInterpretZone : LEGACY_DEFAULT_STANDARD;
            Instant si = startNaive.atZone(z).toInstant();
            Instant ei = endNaive.atZone(z).toInstant();
            ZonedDateTime sJp = ZonedDateTime.ofInstant(si, LEGACY_DEFAULT_OPERATIONAL);
            ZonedDateTime eJp = ZonedDateTime.ofInstant(ei, LEGACY_DEFAULT_OPERATIONAL);
            ZonedDateTime sTh = ZonedDateTime.ofInstant(si, LEGACY_DEFAULT_STANDARD);
            ZonedDateTime eTh = ZonedDateTime.ofInstant(ei, LEGACY_DEFAULT_STANDARD);
            return "JP " + sJp.format(DT) + " ~ " + eJp.format(DT) + "\n" + "TH " + sTh.format(DT) + " ~ " + eTh.format(DT);
        }
        ZoneId z = spec.displayZone2() != null ? spec.displayZone2() : LEGACY_DEFAULT_STANDARD;
        Instant si = startNaive.atZone(z).toInstant();
        Instant ei = endNaive.atZone(z).toInstant();
        ZonedDateTime s1 = ZonedDateTime.ofInstant(si, spec.displayZone1());
        ZonedDateTime e1 = ZonedDateTime.ofInstant(ei, spec.displayZone1());
        ZonedDateTime s2 = ZonedDateTime.ofInstant(si, spec.displayZone2());
        ZonedDateTime e2 = ZonedDateTime.ofInstant(ei, spec.displayZone2());
        if (spec.displayZone1().equals(spec.displayZone2()) && spec.tag1().equalsIgnoreCase(spec.tag2())) {
            return spec.tag1() + " " + s1.format(DT) + " ~ " + e1.format(DT);
        }
        return spec.tag1() + " " + s1.format(DT) + " ~ " + e1.format(DT) + "\n"
                + spec.tag2() + " " + s2.format(DT) + " ~ " + e2.format(DT);
    }

    public static String formatDualLineDateTimeWithSpec(LocalDateTime naiveWallClock, ZoneId naiveInterpretZone,
                                                        TxnDualLineSpec spec) {
        if (spec == null) {
            return formatDualLineDateTime(naiveWallClock, naiveInterpretZone);
        }
        return formatWithSpecDateTime(naiveWallClock, spec);
    }

    /** {@link TxnDualLineSpec} 2줄 — naive 벽시계는 항상 {@code displayZone2}(표준 시간대)로 해석. */
    public static String formatWithSpecTimeOnly(LocalDateTime naiveWallClock, TxnDualLineSpec spec) {
        if (spec == null) {
            return formatDualLineTimeOnly(naiveWallClock, LEGACY_DEFAULT_STANDARD);
        }
        return formatConfigurableDualLineTimeOnly(naiveWallClock, spec.displayZone2(),
                spec.tag1(), spec.displayZone1(), spec.tag2(), spec.displayZone2());
    }

    public static String formatWithSpecDateTime(LocalDateTime naiveWallClock, TxnDualLineSpec spec) {
        if (spec == null) {
            return formatDualLineDateTime(naiveWallClock, LEGACY_DEFAULT_STANDARD);
        }
        return formatConfigurableDualLineDateTime(naiveWallClock, spec.displayZone2(),
                spec.tag1(), spec.displayZone1(), spec.tag2(), spec.displayZone2());
    }

    /** 총판 설정 2줄(시각만). 동일 {@link ZoneId}·동일 태그면 1줄. */
    public static String formatConfigurableDualLineTimeOnly(LocalDateTime naiveWallClock, ZoneId naiveInterpretZone,
                                                            String tag1, ZoneId display1, String tag2, ZoneId display2) {
        if (naiveWallClock == null) {
            return "";
        }
        ZoneId z = naiveInterpretZone != null ? naiveInterpretZone : LEGACY_DEFAULT_STANDARD;
        ZoneId d1 = display1 != null ? display1 : LEGACY_DEFAULT_OPERATIONAL;
        ZoneId d2 = display2 != null ? display2 : LEGACY_DEFAULT_STANDARD;
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
        ZoneId z = naiveInterpretZone != null ? naiveInterpretZone : LEGACY_DEFAULT_STANDARD;
        ZoneId d1 = display1 != null ? display1 : LEGACY_DEFAULT_OPERATIONAL;
        ZoneId d2 = display2 != null ? display2 : LEGACY_DEFAULT_STANDARD;
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
