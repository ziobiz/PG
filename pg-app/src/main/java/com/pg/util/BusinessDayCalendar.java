package com.pg.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 영업일 계산 (주말 제외 + 휴일 집합 제외). Weekly/Weekly2(달력·금요일)는 별도 정책에서 처리.
 */
public final class BusinessDayCalendar {

    private BusinessDayCalendar() {}

    public static boolean isWeekend(LocalDate d) {
        DayOfWeek w = d.getDayOfWeek();
        return w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY;
    }

    /** 주말이 아니고 holidays에 없으면 영업일 */
    public static boolean isBusinessDay(LocalDate d, Set<LocalDate> holidays) {
        if (isWeekend(d)) return false;
        return holidays == null || !holidays.contains(d);
    }

    /**
     * 시작일 다음날부터 순서대로 세어, n영업일째 되는 날(시작일은 포함하지 않음)을 반환.
     * n=1이면 시작일 기준 다음 첫 영업일.
     */
    public static LocalDate addBusinessDays(LocalDate start, int businessDaysToAdd, Set<LocalDate> holidays) {
        if (businessDaysToAdd <= 0) return start;
        LocalDate d = start;
        int left = businessDaysToAdd;
        while (left > 0) {
            d = d.plusDays(1);
            if (isBusinessDay(d, holidays)) left--;
        }
        return d;
    }
}
