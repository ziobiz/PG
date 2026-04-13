package com.pg.service.settlement;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 자동정산 주기별 실행 판별.
 * <ul>
 *   <li>RT: 승인 노티 직후 해당 건만 집계한 정산 실행 1건(건당 마감, 당일 누적 합산 없음).</li>
 *   <li>T0: 승인 노티 직후 당일 00:00~현재까지 전체 재집계(당일 1행 갱신, 기존과 동일).</li>
 *   <li>M5·M10·M30: N분 격자마다 직전 구간(예: M5면 5분) 거래를 합산해 정산 실행 1건으로 마감·표시.</li>
 *   <li>H1·H2·H4·H6·H8·H12: N시간 격자마다 직전 구간 거래를 합산해 정산 실행 1건.</li>
 *   <li>TM5·TM10·TM30·TH1·…·TH12: M/H와 동일 격자 시각에 배치되나, T0처럼 당일 0시~현재 전체를 재집계해 당일 행 1건.</li>
 * </ul>
 */
public final class SettlementCycleTiming {

    private static final Set<String> MINUTE_CLOSING_CODES = Set.of("M5", "M10", "M30");
    private static final Map<String, Integer> MINUTE_GRID_STEP = Map.of(
            "M5", 5,
            "M10", 10,
            "M30", 30
    );

    private static final Set<String> HOUR_CLOSING_CODES = Set.of("H1", "H2", "H4", "H6", "H8", "H12");
    /** 코드 → 시간 격자(시). H1=매시 정각 마감. */
    private static final Map<String, Integer> HOUR_BLOCK_MOD = Map.of(
            "H1", 1,
            "H2", 2,
            "H4", 4,
            "H6", 6,
            "H8", 8,
            "H12", 12
    );

    private SettlementCycleTiming() {}

    public static String normalize(String raw) {
        return SettlementPeriodResolver.normalizeCalcCycle(raw);
    }

    /**
     * 결제 반영 직후 {@link com.pg.service.SettlementCalcService#triggerRealtimeAutoSettlementIfDue} 대상.
     * (RT·T0 — M5/M10/M30은 분 격자 배치)
     */
    public static boolean isRealtimeCode(String normalized) {
        if (normalized == null) {
            return false;
        }
        String c = normalized.trim().toUpperCase(Locale.ROOT);
        return "RT".equals(c) || "T0".equals(c);
    }

    public static boolean isRtPerTransactionCode(String normalized) {
        return normalized != null && "RT".equals(normalized.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isT0RollingIntradayCode(String normalized) {
        return normalized != null && "T0".equals(normalized.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * TM5·TM10·TM30·TH1·…·TH12: 격자는 M/H와 같으나 집계는 T0처럼 당일 0시~현재 합산 1행 재집계.
     */
    public static boolean isRollingIntradayGridCode(String normalized) {
        if (normalized == null) {
            return false;
        }
        String c = normalize(normalized);
        return c.startsWith("TM") || c.startsWith("TH");
    }

    /** TM5→M5, TH12→H12. 그 외(M5·T0 등)는 그대로. */
    public static String toPlainGridClosingCode(String normalized) {
        if (normalized == null) {
            return "";
        }
        String c = normalize(normalized);
        if (c.startsWith("TM") && c.length() >= 3) {
            return "M" + c.substring(2);
        }
        if (c.startsWith("TH") && c.length() >= 3) {
            return "H" + c.substring(2);
        }
        return c;
    }

    /** N분·N시간 마감(M·H 및 동일 격자의 TM·TH) — 매분 크론에서 격자 정각에만 실행 */
    public static boolean isSubDailyScheduleCode(String normalized) {
        if (normalized == null) {
            return false;
        }
        String g = toPlainGridClosingCode(normalized);
        return MINUTE_GRID_STEP.containsKey(g) || HOUR_BLOCK_MOD.containsKey(g);
    }

    /** M5·M10·M30: 해당 N분 격자의 시작 분(0,5,10… / 0,10… / 0,30) */
    public static boolean isMinuteClosingGridNow(LocalTime now, String normalized) {
        String c = normalize(normalized);
        Integer step = MINUTE_GRID_STEP.get(c);
        if (step == null) {
            return false;
        }
        return now.getMinute() % step == 0;
    }

    /**
     * N시간 마감 격자: 매 시각의 분=0 이고, 시각이 N시간 격자 경계(0, N, 2N …)일 때 true.
     */
    public static boolean isHourBlockClosingGridNow(LocalTime now, String normalized) {
        String c = normalize(normalized);
        Integer mod = HOUR_BLOCK_MOD.get(c);
        if (mod == null) {
            return false;
        }
        if (now.getMinute() != 0) {
            return false;
        }
        return now.getHour() % mod == 0;
    }

    public static boolean shouldRunSubDailyNow(LocalTime now, String normalized) {
        String g = toPlainGridClosingCode(normalized);
        if (MINUTE_GRID_STEP.containsKey(g)) {
            return isMinuteClosingGridNow(now, g);
        }
        if (HOUR_BLOCK_MOD.containsKey(g)) {
            return isHourBlockClosingGridNow(now, g);
        }
        return false;
    }

    /**
     * D+0 일괄 자동정산: 서울 달력일 기준 00:00부터 23:50분 끝(23:50:59.999까지)까지만 배치에서 실행.
     * 23:51 이후에는 당일 D0 자동 실행을 하지 않는다.
     */
    public static boolean isD0AutoBatchAllowedNow(LocalTime nowSeoul) {
        if (nowSeoul == null) {
            return false;
        }
        LocalTime endExclusive = LocalTime.of(23, 51, 0);
        return !nowSeoul.isBefore(LocalTime.MIDNIGHT) && nowSeoul.isBefore(endExclusive);
    }

    /**
     * 격자 정각에 맞춰 방금 끝난 집계 구간 [startInclusive, endExclusive) (서울 시각, 분·초는 0 기준).
     * {@link #shouldRunSubDailyNow} 가 true일 때와 동일 조건에서만 null 이 아님.
     */
    public static SubDailyClosedSlot closedSubDailySlot(LocalDateTime nowSeoulTruncatedToMinute, String normalizedCycle) {
        if (nowSeoulTruncatedToMinute == null) {
            return null;
        }
        LocalDateTime end = nowSeoulTruncatedToMinute.withSecond(0).withNano(0);
        String grid = toPlainGridClosingCode(normalizedCycle);
        Integer stepMin = MINUTE_GRID_STEP.get(grid);
        if (stepMin != null) {
            if (end.getMinute() % stepMin != 0) {
                return null;
            }
            return new SubDailyClosedSlot(end.minusMinutes(stepMin), end);
        }
        Integer hourBlock = HOUR_BLOCK_MOD.get(grid);
        if (hourBlock == null) {
            return null;
        }
        if (end.getMinute() != 0 || end.getHour() % hourBlock != 0) {
            return null;
        }
        return new SubDailyClosedSlot(end.minusHours(hourBlock), end);
    }

    public record SubDailyClosedSlot(LocalDateTime startInclusive, LocalDateTime endExclusive) {}
}
