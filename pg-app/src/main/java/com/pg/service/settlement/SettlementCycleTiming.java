package com.pg.service.settlement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 자동정산 주기별 실행 판별.
 * <p>주기·격자·스케줄 전제 요약: 저장소 {@code docs/정산주기_전체_명세.md}.</p>
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

    /**
     * 자동 정산에서 「정산개시시간」이 의미를 갖는 주기인지.
     * RT·T0·M/H 격자·TM/TH 격자·D0 는 격자·노티·당일 배치 규칙으로 실행되므로 개시시간을 쓰지 않는다(D1+·W·WK 만 적용).
     * 달력 자동 배치는 {@link com.pg.service.settlement.SettlementAutoRunService} 에서 마감시각 다음에,
     * 이 메서드가 true일 때만 개시시각을 추가로 검사한다.
     */
    public static boolean isCalcStartTimeApplicableForAuto(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        String c = normalize(normalized);
        if ("NONE".equals(c)) {
            return false;
        }
        if (isRealtimeCode(c)) {
            return false;
        }
        if ("D0".equals(c)) {
            return false;
        }
        return !isSubDailyScheduleCode(c);
    }

    /**
     * 수동 정산: M5·H1·TM5 등 격자(분·시) 주기는 당일 현재 시각이 직전 격자 구간을 넘긴 뒤에만 실행 허용.
     * (예: H1은 정각 이후, 00:39에는 불가.) 과거 정산일(runTo 가 오늘이 아님)은 통과.
     */
    public static boolean isManualIntradayGridSlotElapsed(LocalTime nowSeoul,
                                                          LocalDate calcDt,
                                                          LocalDate todaySeoul,
                                                          String normalized) {
        if (nowSeoul == null || calcDt == null || todaySeoul == null) {
            return true;
        }
        String c = normalize(normalized);
        if (isRtPerTransactionCode(c)) {
            return true;
        }
        if (!calcDt.equals(todaySeoul)) {
            return true;
        }
        String g = toPlainGridClosingCode(c);
        Integer sm = MINUTE_GRID_STEP.get(g);
        int stepMin;
        if (sm != null) {
            stepMin = sm;
        } else {
            Integer hb = HOUR_BLOCK_MOD.get(g);
            if (hb == null) {
                /* T0·D1+ 등 분·시 격자가 아님 */
                return true;
            }
            stepMin = hb * 60;
        }
        int nowMin = nowSeoul.getHour() * 60 + nowSeoul.getMinute();
        int ceiled = ((nowMin + stepMin - 1) / stepMin) * stepMin;
        if (ceiled == 0) {
            ceiled = stepMin;
        }
        return nowMin >= ceiled;
    }

    /** N분·N시간 마감(M·H 및 동일 격자의 TM·TH) — 매분 크론에서 격자 정각에만 실행 */
    public static boolean isSubDailyScheduleCode(String normalized) {
        if (normalized == null) {
            return false;
        }
        String g = toPlainGridClosingCode(normalized);
        return MINUTE_GRID_STEP.containsKey(g) || HOUR_BLOCK_MOD.containsKey(g);
    }

    /**
     * M5·H12 등 “격자 직전 구간 합산” 주기. (당일 누적 TM/TH·T0 제외)
     */
    public static boolean isPlainSubDailyGridClosingCode(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        String c = normalize(normalized);
        if (isRollingIntradayGridCode(c) || isT0RollingIntradayCode(c)) {
            return false;
        }
        return isSubDailyScheduleCode(c);
    }

    /**
     * 가맹점정산내역·배분·정산실시 리포트 등에 포함할 정산 실행 행인지.
     * <ul>
     *   <li>RT: 건당 마감 행은 항상 포함.</li>
     *   <li>M5·H1 등 격자 마감: 자동 배치가 저장한 행만({@code periodEndAt} ≠ null). 수동 실행으로 생긴 미마감 집계는 제외.</li>
     *   <li>T0·TM·TH: 당일 누적 재집계 행만({@code periodEndAt} 기록된 행).</li>
     *   <li>일·주·달 등 그 외 주기: 기존과 동일하게 포함.</li>
     * </ul>
     * 정산실행(목록) 화면은 이 필터를 쓰지 않고 전체 실행 이력을 보여줍니다.
     *
     * @param periodEndAt        {@link com.pg.entity.SettlementRun#getPeriodEndAt()} (격자·누적 마감 시각)
     * @param normalizedCalcCycle {@link SettlementPeriodResolver#normalizeCalcCycle(String)} 결과
     */
    public static boolean isMerchantStatementSettlementRunVisible(LocalDateTime periodEndAt, String normalizedCalcCycle) {
        String c = normalize(normalizedCalcCycle != null ? normalizedCalcCycle : "");
        if (c.isBlank()) {
            return true;
        }
        if ("NONE".equals(c)) {
            return false;
        }
        if (isRtPerTransactionCode(c)) {
            return true;
        }
        if (isT0RollingIntradayCode(c) || isRollingIntradayGridCode(c)) {
            return periodEndAt != null;
        }
        if (isSubDailyScheduleCode(c)) {
            return periodEndAt != null;
        }
        return true;
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

    /**
     * 저장된 격자 집계 상한({@code endExclusive})과 주기(M5·H1 등, TM/TH는 격자 코드로 환산)로
     * 집계 시작 시각(startInclusive)을 복원합니다. RT·T0·TM/TH 당일 누적 격자는 null.
     */
    /**
     * M/H 격자 마감 시각(배타 상한)인지. TM/TH/T0 등 당일 누적 주기는 true(검사 생략·라벨에서 별도 처리).
     */
    public static boolean isSubDailyGridAlignedPeriodEnd(LocalDateTime endExclusive, String normalizedCycle) {
        if (endExclusive == null || normalizedCycle == null || normalizedCycle.isBlank()) {
            return false;
        }
        String c = normalize(normalizedCycle);
        if (isRollingIntradayGridCode(c) || isT0RollingIntradayCode(c)) {
            return true;
        }
        if (!isSubDailyScheduleCode(c)) {
            return false;
        }
        String g = toPlainGridClosingCode(c);
        LocalDateTime e = endExclusive.withNano(0);
        if (e.getSecond() != 0) {
            return false;
        }
        Integer sm = MINUTE_GRID_STEP.get(g);
        if (sm != null) {
            return e.getMinute() % sm == 0;
        }
        Integer hb = HOUR_BLOCK_MOD.get(g);
        if (hb != null) {
            return e.getMinute() == 0 && e.getHour() % hb == 0;
        }
        return false;
    }

    public static LocalDateTime subDailySlotStartInclusiveFromEndExclusive(LocalDateTime endExclusive, String normalizedCycle) {
        if (endExclusive == null || normalizedCycle == null || normalizedCycle.isBlank()) {
            return null;
        }
        String c = normalize(normalizedCycle);
        if (isRollingIntradayGridCode(c)) {
            return null;
        }
        if (!isSubDailyScheduleCode(c)) {
            return null;
        }
        LocalDateTime end = endExclusive.withSecond(0).withNano(0);
        String grid = toPlainGridClosingCode(c);
        Integer stepMin = MINUTE_GRID_STEP.get(grid);
        if (stepMin != null) {
            if (end.getMinute() % stepMin != 0) {
                return null;
            }
            return end.minusMinutes(stepMin);
        }
        Integer hourBlock = HOUR_BLOCK_MOD.get(grid);
        if (hourBlock == null) {
            return null;
        }
        if (end.getMinute() != 0 || end.getHour() % hourBlock != 0) {
            return null;
        }
        return end.minusHours(hourBlock);
    }

    public record SubDailyClosedSlot(LocalDateTime startInclusive, LocalDateTime endExclusive) {}
}
