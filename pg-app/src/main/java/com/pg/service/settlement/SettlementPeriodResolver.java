package com.pg.service.settlement;

import com.pg.util.BusinessDayCalendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 가맹 정산주기(calc_cycle)에 따른 정산 대상 기간(from~to).
 * <p>자동·실시간·수동 정산 실행은 본사 {@code tb_hq_settlement_cycle_def} 병합 목록에서 해당 코드가 <strong>사용(Y)</strong>일 때만
 * 진행한다({@link com.pg.service.HqSettlementCycleAdminService#isActiveSettlementCycle}).</p>
 * <p>주기 전체(정규화·RT/T0·격자·달력) 요약: 저장소 {@code docs/정산주기_전체_명세.md}.</p>
 * {@code W7}, {@code WK1W} 등 주간 규칙과 {@code D0}~{@code D90} 일자 오프셋을 해석한다.
 * <p>{@code WK+1W}/{@code WK+1WT}: 1주(월~일) 마감 후 각각 영업일 3·10일째 정산.
 * {@code WK+2W}/{@code WK+2WT}: 격주 2주 마감 후 각각 영업일 3·10일째.
 * {@code WK+1WM}/{@code WK+2WM}: 1주·격주 2주 마감 후 영업일 30일째.
 * “마감 다음날”(일요일 다음날)부터 N영업일째가 정산 도래일이며, 도래일이 비영업일이면 다음 영업일로 맞춘다.
 * 영업일은 주말 제외 + 호출 측이 넘긴 비영업일 집합(가맹 소속 총판·본사 영업일설정, {@link SettlementBusinessHolidayService})을 반영한다.</p>
 * <p>{@code D0} 자동 배치는 서울 기준 당일 00:00~23:50 구간에서만 실행된다.</p>
 * <p>{@code D+N}: 정산 실행·정산일은 달력 당일을 기준으로 하고(통상 마감시간 이후·새벽 배치),
 * 집계 기준일 하루는 그 정산일에서 N을 역산해 정한다. {@code D1}~{@code D30}의 N은 영업일(주말·비영업일 집합 제외),
 * {@code D31} 이상은 달력일 역산이다. ‘전일 하루’가 아니라 집계 기준일과 정산일(당일)의 관계로 본다.</p>
 */
public final class SettlementPeriodResolver {

    private static final Pattern D_CYCLE = Pattern.compile("^D(\\d{1,2})$");

    private SettlementPeriodResolver() {}

    public record PeriodWindow(LocalDate fromDate, LocalDate toDate) {}

    public static String normalizeCalcCycle(String raw) {
        if (raw == null) return "";
        String u = raw.trim().toUpperCase(Locale.ROOT).replace("+", "");
        if ("TM05".equals(u)) {
            u = "TM5";
        }
        /* 구 1D~12D 시간 마감 코드 → H1·H2·H4·H6·H8·H12 로 통일(저장값 호환) */
        return switch (u) {
            case "1D" -> "H1";
            case "2D" -> "H2";
            case "4D" -> "H4";
            case "6D" -> "H6";
            case "8D" -> "H8";
            case "12D" -> "H12";
            case "REALTIME" -> "RT";
            default -> u;
        };
    }

    private static LocalDate nextBusinessDayOrSame(LocalDate d, Set<LocalDate> holidays) {
        Set<LocalDate> hol = holidays != null ? holidays : Collections.emptySet();
        LocalDate cur = d;
        while (!BusinessDayCalendar.isBusinessDay(cur, hol)) {
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
     * 휴일 집합 없이 호출 시 주말만 제외(본사·총판 프로필 미반영).
     */
    public static PeriodWindow resolveAutoPeriodWindow(String calcCycle, LocalDate today) {
        return resolveAutoPeriodWindow(calcCycle, today, Collections.emptySet());
    }

    /**
     * @param holidays 가맹 소속 총판 영업일·휴일(본사 영업일설정). 주말은 {@link BusinessDayCalendar} 가 별도 처리.
     */
    public static PeriodWindow resolveAutoPeriodWindow(String calcCycle, LocalDate today, Set<LocalDate> holidays) {
        Set<LocalDate> hol = holidays != null ? holidays : Collections.emptySet();
        String c = normalizeCalcCycle(calcCycle);
        if (c.isEmpty() || "NONE".equals(c)) {
            return null;
        }
        if ("RT".equals(c) || "T0".equals(c)) {
            return null;
        }

        Matcher dm = D_CYCLE.matcher(c);
        if (dm.matches()) {
            int n = Integer.parseInt(dm.group(1), 10);
            if (n < 0 || n > 90) {
                return null;
            }
            LocalDate periodDay;
            if (n >= 1 && n <= 30) {
                periodDay = BusinessDayCalendar.subtractBusinessDays(today, n, hol);
            } else {
                periodDay = today.minusDays(n);
            }
            return new PeriodWindow(periodDay, periodDay);
        }

        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        if (c.matches("W\\d+")) {
            int businessDaysAfterEnd = Integer.parseInt(c.substring(1));
            for (int k = 0; k <= 16; k++) {
                LocalDate start = thisMonday.minusWeeks(k);
                LocalDate end = start.plusDays(6);
                /* W+N: 일요일(마감) 다음날부터 N영업일째 — WK* 와 동일 카운트 방식 */
                LocalDate base = BusinessDayCalendar.addBusinessDays(end, businessDaysAfterEnd, hol);
                LocalDate settle = nextBusinessDayOrSame(base, hol);
                if (settle.equals(today)) {
                    return new PeriodWindow(start, end);
                }
            }
            return null;
        }

        if ("WK1W".equals(c) || "WK1WT".equals(c) || "WK1WM".equals(c)) {
            int businessDaysAfterEnd = switch (c) {
                case "WK1W" -> 3;
                case "WK1WT" -> 10;
                case "WK1WM" -> 30;
                default -> 3;
            };
            for (int k = 0; k <= 16; k++) {
                LocalDate start = thisMonday.minusWeeks(k);
                LocalDate end = start.plusDays(6);
                LocalDate base = BusinessDayCalendar.addBusinessDays(end, businessDaysAfterEnd, hol);
                LocalDate settle = nextBusinessDayOrSame(base, hol);
                if (settle.equals(today)) {
                    return new PeriodWindow(start, end);
                }
            }
            return null;
        }

        if ("WK2W".equals(c) || "WK2WT".equals(c) || "WK2WM".equals(c)) {
            int businessDaysAfterEnd = switch (c) {
                case "WK2W" -> 3;
                case "WK2WT" -> 10;
                case "WK2WM" -> 30;
                default -> 3;
            };
            for (int k = 0; k <= 24; k++) {
                LocalDate start = thisMonday.minusWeeks(k);
                if (!isBiWeeklyAnchor(start)) {
                    continue;
                }
                LocalDate end = start.plusDays(13);
                LocalDate base = BusinessDayCalendar.addBusinessDays(end, businessDaysAfterEnd, hol);
                LocalDate settle = nextBusinessDayOrSame(base, hol);
                if (settle.equals(today)) {
                    return new PeriodWindow(start, end);
                }
            }
            return null;
        }

        return null;
    }
}
