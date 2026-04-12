package com.pg.service.settlement;

import java.time.LocalTime;
import java.util.Locale;

/**
 * 자동정산 중 분·시간 단위 주기의 실행 타이밍: N분·N시간 격자의 시작 정각(경계 시각)에 맞출 때 true.
 */
public final class SettlementCycleTiming {

    private SettlementCycleTiming() {}

    public static String normalize(String raw) {
        return SettlementPeriodResolver.normalizeCalcCycle(raw);
    }

    public static boolean isRealtimeCode(String normalized) {
        if (normalized == null) {
            return false;
        }
        String c = normalized.trim().toUpperCase(Locale.ROOT);
        return "RT".equals(c) || "T0".equals(c);
    }

    /** M5·M10·H1·H2·H4 — 배치 스케줄러에서 당일 누적 재집계에 사용 */
    public static boolean isSubDailyScheduleCode(String normalized) {
        if (normalized == null) {
            return false;
        }
        String c = normalized.trim().toUpperCase(Locale.ROOT);
        return "M5".equals(c) || "M10".equals(c) || "H1".equals(c) || "H2".equals(c) || "H4".equals(c);
    }

    /**
     * 분 단위(M5/M10): 해당 N분 격자의 시작 분(예: M5 → 0,5,10,··· 분에 정각 실행).
     */
    public static boolean isMinuteGridStart(LocalTime now, String normalized) {
        String c = normalize(normalized);
        if ("M5".equals(c)) {
            return now.getMinute() % 5 == 0;
        }
        if ("M10".equals(c)) {
            return now.getMinute() % 10 == 0;
        }
        return false;
    }

    /**
     * 시 단위(H1/H2/H4): 격자 시작 시각의 정각(분=0).
     * H1: 매시, H2: 짝수 시(0,2,4…), H4: 시각을 4로 나눈 나머지 0인 시(0,4,8…).
     */
    public static boolean isHourGridStart(LocalTime now, String normalized) {
        String c = normalize(normalized);
        if (now.getMinute() != 0) {
            return false;
        }
        int h = now.getHour();
        if ("H1".equals(c)) {
            return true;
        }
        if ("H2".equals(c)) {
            return h % 2 == 0;
        }
        if ("H4".equals(c)) {
            return h % 4 == 0;
        }
        return false;
    }

    public static boolean shouldRunSubDailyNow(LocalTime now, String normalized) {
        String c = normalize(normalized);
        if ("M5".equals(c) || "M10".equals(c)) {
            return isMinuteGridStart(now, c);
        }
        if ("H1".equals(c) || "H2".equals(c) || "H4".equals(c)) {
            return isHourGridStart(now, c);
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
}
