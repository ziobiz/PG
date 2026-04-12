package com.pg.service.settlement;

import com.pg.util.BusinessDayCalendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 가맹 정산주기(calc_cycle)에 따른 정산 대상 기간(from~to).
 * {@code W7}, {@code WK1W} 등 주간 규칙과 {@code D0}~{@code D30} 일자 오프셋을 해석한다.
 */
public final class SettlementPeriodResolver {

    private static final Pattern D_CYCLE = Pattern.compile("^D(\\d{1,2})$");

    private SettlementPeriodResolver() {}

    public record PeriodWindow(LocalDate fromDate, LocalDate toDate) {}

    public static String normalizeCalcCycle(String raw) {
        if (raw == null) return "";
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return u.replace("+", "");
    }

    /** RT(당일 정산 배치) 등을 D0과 동일한 일일 창으로 근사한다. */
    private static String expandAliases(String normalized) {
        if (normalized.isEmpty()) {
            return normalized;
        }
        if ("RT".equals(normalized) || "T0".equals(normalized)) {
            return "D0";
        }
        return normalized;
    }

    private static LocalDate nextBusinessDayOrSame(LocalDate d) {
        LocalDate cur = d;
        while (!BusinessDayCalendar.isBusinessDay(cur, Collections.emptySet())) {
            cur = cur.plusDays(1);
        }
        return cur;
    }

    private static boolean isBiWeeklyAnchor(LocalDate monday) {
        LocalDate epochMonday = LocalDate.of(1970, 1, 5);
        long weeks = ChronoUnit.WEEKS.between(epochMonday, monday);
        return weeks % 2 == 0;
    }

    /**
     * 오늘이 해당 주기의 "정산 실행일"일 때만 기간을 반환한다. 해당일이 아니면 {@code null}.
     */
    public static PeriodWindow resolveAutoPeriodWindow(String calcCycle, LocalDate today) {
        String c0 = normalizeCalcCycle(calcCycle);
        if (c0.isEmpty() || "NONE".equals(c0)) {
            return null;
        }
        String c = expandAliases(c0);

        Matcher dm = D_CYCLE.matcher(c);
        if (dm.matches()) {
            int n = Integer.parseInt(dm.group(1), 10);
            if (n < 0 || n > 30) {
                return null;
            }
            LocalDate periodDay = today.minusDays(n);
            return new PeriodWindow(periodDay, periodDay);
        }

        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        if (c.matches("W\\d+")) {
            int d = Integer.parseInt(c.substring(1));
            for (int k = 0; k <= 16; k++) {
                LocalDate start = thisMonday.minusWeeks(k);
                LocalDate end = start.plusDays(6);
                LocalDate base = end.plusDays(d);
                LocalDate settle = nextBusinessDayOrSame(base);
                if (settle.equals(today)) {
                    return new PeriodWindow(start, end);
                }
            }
            return null;
        }

        if ("WK1W".equals(c) || "WK1WT".equals(c)) {
            int deltaToWednesday = "WK1W".equals(c) ? 3 : 10;
            for (int k = 0; k <= 16; k++) {
                LocalDate start = thisMonday.minusWeeks(k);
                LocalDate end = start.plusDays(6);
                LocalDate base = end.plusDays(deltaToWednesday);
                LocalDate settle = nextBusinessDayOrSame(base);
                if (settle.equals(today)) {
                    return new PeriodWindow(start, end);
                }
            }
            return null;
        }

        if ("WK2W".equals(c) || "WK2WT".equals(c)) {
            int deltaToWednesday = "WK2W".equals(c) ? 3 : 10;
            for (int k = 0; k <= 24; k++) {
                LocalDate start = thisMonday.minusWeeks(k);
                if (!isBiWeeklyAnchor(start)) {
                    continue;
                }
                LocalDate end = start.plusDays(13);
                LocalDate base = end.plusDays(deltaToWednesday);
                LocalDate settle = nextBusinessDayOrSame(base);
                if (settle.equals(today)) {
                    return new PeriodWindow(start, end);
                }
            }
            return null;
        }

        return null;
    }
}
