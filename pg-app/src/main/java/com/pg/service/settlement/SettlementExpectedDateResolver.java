package com.pg.service.settlement;

import com.pg.util.BusinessDayCalendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 거래일·가맹 정산주기·영업일(총판·본사) 기준 예상 정산일(달력일).
 */
public final class SettlementExpectedDateResolver {

    private static final Pattern D_CYCLE = Pattern.compile("^D(\\d{1,2})$");

    private SettlementExpectedDateResolver() {}

    public static Optional<LocalDate> resolveExpectedSettlementDate(LocalDate txnDate,
                                                                    LocalDateTime txnAt,
                                                                    String calcCycle,
                                                                    Set<LocalDate> holidays) {
        if (txnDate == null) {
            return Optional.empty();
        }
        Set<LocalDate> hol = holidays != null ? holidays : Collections.emptySet();
        String c = SettlementPeriodResolver.normalizeCalcCycle(calcCycle);
        if (c.isEmpty() || "NONE".equals(c)) {
            return Optional.empty();
        }
        LocalDateTime at = txnAt != null ? txnAt : txnDate.atStartOfDay();

        if (SettlementCycleTiming.isRealtimeCode(c) || SettlementCycleTiming.isRollingIntradayGridCode(c)) {
            return Optional.of(txnDate);
        }

        Matcher dm = D_CYCLE.matcher(c);
        if (dm.matches()) {
            int n = Integer.parseInt(dm.group(1), 10);
            if (n < 0 || n > 90) {
                return Optional.empty();
            }
            if (n == 0) {
                return Optional.of(nextBusinessDayOrSame(txnDate, hol));
            }
            if (n <= 30) {
                return Optional.of(nextBusinessDayOrSame(BusinessDayCalendar.addBusinessDays(txnDate, n, hol), hol));
            }
            return Optional.of(txnDate.plusDays(n));
        }

        if (c.matches("W\\d+")) {
            int businessDaysAfterEnd = Integer.parseInt(c.substring(1));
            return Optional.of(expectedWeeklySettleDate(txnDate, businessDaysAfterEnd, hol, 1));
        }

        if ("WK1W".equals(c) || "WK1WT".equals(c) || "WK1WM".equals(c)) {
            int bd = switch (c) {
                case "WK1WT" -> 10;
                case "WK1WM" -> 30;
                default -> 3;
            };
            return Optional.of(expectedWeeklySettleDate(txnDate, bd, hol, 1));
        }

        if ("WK2W".equals(c) || "WK2WT".equals(c) || "WK2WM".equals(c)) {
            int bd = switch (c) {
                case "WK2WT" -> 10;
                case "WK2WM" -> 30;
                default -> 3;
            };
            return Optional.of(expectedBiWeeklySettleDate(txnDate, bd, hol));
        }

        if (SettlementCycleTiming.isSubDailyScheduleCode(c)) {
            return resolveGridExpectedDate(at, c);
        }

        return Optional.empty();
    }

    public static String formatExpectedSettlementDate(LocalDate txnDate,
                                                      LocalDateTime txnAt,
                                                      String calcCycle,
                                                      Set<LocalDate> holidays) {
        return resolveExpectedSettlementDate(txnDate, txnAt, calcCycle, holidays)
                .map(LocalDate::toString)
                .orElse("");
    }

    /**
     * 정산 실행 행 — 집계 구간·주기·영업일 기준 예상 정산일(배치 도래일·W+N·D+N 등).
     * {@code periodTo} 가 있으면 주간·격주 마감일, 없으면 {@code calcDt} 를 기준일로 씁니다.
     */
    public static Optional<LocalDate> resolveExpectedSettlementDateForRun(LocalDate calcDt,
                                                                          LocalDate periodFrom,
                                                                          LocalDate periodTo,
                                                                          String calcCycle,
                                                                          Set<LocalDate> holidays) {
        Set<LocalDate> hol = holidays != null ? holidays : Collections.emptySet();
        String c = SettlementPeriodResolver.normalizeCalcCycle(calcCycle);
        if (c.isEmpty() || "NONE".equals(c)) {
            return calcDt != null ? Optional.of(calcDt) : Optional.empty();
        }
        LocalDate anchor = periodTo != null ? periodTo : calcDt;
        if (anchor == null) {
            return Optional.empty();
        }

        if (SettlementCycleTiming.isRealtimeCode(c) || SettlementCycleTiming.isRollingIntradayGridCode(c)) {
            return Optional.of(anchor);
        }
        if (SettlementCycleTiming.isSubDailyScheduleCode(c)) {
            return Optional.of(anchor);
        }

        Matcher dm = D_CYCLE.matcher(c);
        if (dm.matches()) {
            int n = Integer.parseInt(dm.group(1), 10);
            if (n < 0 || n > 90) {
                return Optional.empty();
            }
            if (n == 0) {
                return Optional.of(nextBusinessDayOrSame(anchor, hol));
            }
            if (n <= 30) {
                LocalDate baseDay = periodFrom != null ? periodFrom : anchor;
                return Optional.of(nextBusinessDayOrSame(BusinessDayCalendar.addBusinessDays(baseDay, n, hol), hol));
            }
            return Optional.of(anchor.plusDays(n));
        }

        if (c.matches("W\\d+")) {
            int businessDaysAfterEnd = Integer.parseInt(c.substring(1));
            LocalDate base = BusinessDayCalendar.addBusinessDays(anchor, businessDaysAfterEnd, hol);
            return Optional.of(nextBusinessDayOrSame(base, hol));
        }

        if ("WK1W".equals(c) || "WK1WT".equals(c) || "WK1WM".equals(c)) {
            int bd = switch (c) {
                case "WK1WT" -> 10;
                case "WK1WM" -> 30;
                default -> 3;
            };
            LocalDate base = BusinessDayCalendar.addBusinessDays(anchor, bd, hol);
            return Optional.of(nextBusinessDayOrSame(base, hol));
        }

        if ("WK2W".equals(c) || "WK2WT".equals(c) || "WK2WM".equals(c)) {
            int bd = switch (c) {
                case "WK2WT" -> 10;
                case "WK2WM" -> 30;
                default -> 3;
            };
            LocalDate base = BusinessDayCalendar.addBusinessDays(anchor, bd, hol);
            return Optional.of(nextBusinessDayOrSame(base, hol));
        }

        return Optional.of(anchor);
    }

    private static LocalDate expectedWeeklySettleDate(LocalDate txnDate, int businessDaysAfterEnd,
                                                      Set<LocalDate> hol, int weekSpan) {
        LocalDate monday = txnDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = monday.plusDays((long) weekSpan * 7L - 1L);
        LocalDate base = BusinessDayCalendar.addBusinessDays(end, businessDaysAfterEnd, hol);
        return nextBusinessDayOrSame(base, hol);
    }

    private static LocalDate expectedBiWeeklySettleDate(LocalDate txnDate, int businessDaysAfterEnd, Set<LocalDate> hol) {
        LocalDate monday = txnDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int k = 0; k <= 104; k++) {
            LocalDate start = monday.minusWeeks(k);
            if (!isBiWeeklyAnchor(start)) {
                continue;
            }
            LocalDate end = start.plusDays(13);
            if (!txnDate.isBefore(start) && !txnDate.isAfter(end)) {
                LocalDate base = BusinessDayCalendar.addBusinessDays(end, businessDaysAfterEnd, hol);
                return nextBusinessDayOrSame(base, hol);
            }
        }
        return expectedWeeklySettleDate(txnDate, businessDaysAfterEnd, hol, 2);
    }

    private static Optional<LocalDate> resolveGridExpectedDate(LocalDateTime txnAt, String normalized) {
        String g = SettlementCycleTiming.toPlainGridClosingCode(normalized);
        if (!SettlementCycleTiming.isSubDailyScheduleCode(normalized)) {
            return Optional.empty();
        }
        LocalDateTime cursor = txnAt.truncatedTo(ChronoUnit.MINUTES);
        for (int i = 0; i < 2880; i++) {
            cursor = cursor.plusMinutes(1);
            if (SettlementCycleTiming.isPlainSubDailyGridEndWallClock(cursor, g)) {
                return Optional.of(cursor.toLocalDate());
            }
        }
        return Optional.empty();
    }

    private static LocalDate nextBusinessDayOrSame(LocalDate d, Set<LocalDate> holidays) {
        LocalDate cur = d;
        while (!BusinessDayCalendar.isBusinessDay(cur, holidays)) {
            cur = cur.plusDays(1);
        }
        return cur;
    }

    private static boolean isBiWeeklyAnchor(LocalDate monday) {
        LocalDate epochMonday = LocalDate.of(1970, 1, 5);
        long weeks = ChronoUnit.WEEKS.between(epochMonday, monday);
        return weeks % 2 == 0;
    }
}
