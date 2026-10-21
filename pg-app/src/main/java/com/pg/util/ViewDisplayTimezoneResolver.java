package com.pg.util;

import com.pg.api.dto.TxnDualLineSpec;
import com.pg.service.HqLedgerSysSettingsService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 목록 화면 일시적 표시 시간대 — 본사 전산설정({@code display_timezone})과 무관하게
 * 현재 조회 결과의 거래일·거래시간 2줄 표시 중 표준(2줄)만 바꿉니다. DB·설정은 변경하지 않습니다.
 */
public final class ViewDisplayTimezoneResolver {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ThreadLocal<Optional<ZoneId>> REQUEST_OVERRIDE = new ThreadLocal<>();
    /** 통합체크 비교정렬 시간. 비어 있으면 전산 표준 시간대. */
    private static final ThreadLocal<Optional<ZoneId>> COMPARE_ALIGN = new ThreadLocal<>();
    /** 통합체크 1줄(대행사) 추가 시간. */
    private static final ThreadLocal<Optional<ZoneId>> LINE1_EXTRA = new ThreadLocal<>();
    /** 통합체크 상세 거래시간 — 기준 1줄 + 추가 1줄. */
    private static final ThreadLocal<Boolean> INTEGRATED_CHECK_TIMES = new ThreadLocal<>();

    private ViewDisplayTimezoneResolver() {
    }

    public static void bindRequestOverride(String rawParam) {
        REQUEST_OVERRIDE.set(parseParam(rawParam));
    }

    public static void bindCompareAlign(String rawParam) {
        COMPARE_ALIGN.set(parseParam(rawParam));
    }

    public static void bindLine1Extra(String rawParam) {
        LINE1_EXTRA.set(parseParam(rawParam));
    }

    public static void bindIntegratedCheckTimes(boolean on) {
        INTEGRATED_CHECK_TIMES.set(on);
    }

    public static void clearRequestOverride() {
        REQUEST_OVERRIDE.remove();
        COMPARE_ALIGN.remove();
        LINE1_EXTRA.remove();
        INTEGRATED_CHECK_TIMES.remove();
    }

    public static Optional<ZoneId> currentRequestOverride() {
        Optional<ZoneId> v = REQUEST_OVERRIDE.get();
        return v != null ? v : Optional.empty();
    }

    public static Optional<ZoneId> currentCompareAlign() {
        Optional<ZoneId> v = COMPARE_ALIGN.get();
        return v != null ? v : Optional.empty();
    }

    public static Optional<ZoneId> currentLine1Extra() {
        Optional<ZoneId> v = LINE1_EXTRA.get();
        return v != null ? v : Optional.empty();
    }

    public static boolean integratedCheckTimes() {
        Boolean v = INTEGRATED_CHECK_TIMES.get();
        return v != null && v;
    }

    public static Optional<ZoneId> parseParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZoneId.of(raw.trim()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** 표준(2줄) Zone 교체. 1줄(운영)은 유지. */
    public static Optional<TxnDualLineSpec> effectiveDualSpec(TxnDualLineSpec dual,
                                                              ZoneId systemStandard,
                                                              ZoneId systemOperational,
                                                              Optional<ZoneId> viewOverride) {
        if (viewOverride == null || viewOverride.isEmpty()) {
            return Optional.ofNullable(dual);
        }
        ZoneId view = viewOverride.get();
        String viewTag = LedgerZoneDisplayTag.zoneIdToShortTag(view);
        if (dual != null) {
            return Optional.of(new TxnDualLineSpec(dual.tag1(), dual.displayZone1(), viewTag, view));
        }
        ZoneId op = systemOperational != null ? systemOperational : ZoneId.of("Asia/Tokyo");
        String opTag = LedgerZoneDisplayTag.zoneIdToShortTag(op);
        return Optional.of(new TxnDualLineSpec(opTag, op, viewTag, view));
    }

    public static LocalDate trnDateInZone(LocalDateTime naiveWallClock,
                                          ZoneId naiveInterpretZone,
                                          Optional<ZoneId> viewOverride) {
        if (naiveWallClock == null) {
            return null;
        }
        ZoneId interpret = naiveInterpretZone != null ? naiveInterpretZone : ZoneId.of("Asia/Bangkok");
        if (viewOverride == null || viewOverride.isEmpty()) {
            return naiveWallClock.toLocalDate();
        }
        Instant instant = naiveWallClock.atZone(interpret).toInstant();
        return ZonedDateTime.ofInstant(instant, viewOverride.get()).toLocalDate();
    }

    public static String formatIsoDate(LocalDate date) {
        return date != null ? date.format(ISO_DATE) : "";
    }

    /** naive 시각을 표준 Zone으로 해석한 뒤 view Override가 있으면 해당 Zone 벽시계 문자열. */
    public static String formatNaiveAsWallDateTime(LocalDateTime naive,
                                                   ZoneId naiveInterpretZone,
                                                   Optional<ZoneId> viewOverride) {
        if (naive == null) {
            return "";
        }
        ZoneId interpret = naiveInterpretZone != null ? naiveInterpretZone : ZoneId.of("Asia/Bangkok");
        if (viewOverride == null || viewOverride.isEmpty()) {
            return naive.format(DT);
        }
        Instant instant = naive.atZone(interpret).toInstant();
        return ZonedDateTime.ofInstant(instant, viewOverride.get()).format(DT);
    }

    public static ZoneId resolveStandardFromService(HqLedgerSysSettingsService svc) {
        return svc != null ? svc.resolveLedgerDisplayZoneId() : ZoneId.of("Asia/Bangkok");
    }

    public static ZoneId resolveOperationalFromService(HqLedgerSysSettingsService svc) {
        return svc != null ? svc.resolveOperationalDisplayZoneId() : ZoneId.of("Asia/Tokyo");
    }
}
