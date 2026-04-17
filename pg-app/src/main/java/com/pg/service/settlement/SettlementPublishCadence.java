package com.pg.service.settlement;

import java.util.Locale;

/**
 * 정산결과 화면용 노출 주기 한 줄 안내(짧은 문구).
 */
public final class SettlementPublishCadence {

    private SettlementPublishCadence() {}

    /**
     * @param cycleRaw {@link com.pg.entity.SettlementRun#getCalcCycleSnapshot()} 또는 정규화 전 주기 문자열
     */
    public static String cadenceGuideKr(String cycleRaw) {
        if (cycleRaw == null || cycleRaw.isBlank()) {
            return "주기 미기록.";
        }
        String c = SettlementPeriodResolver.normalizeCalcCycle(cycleRaw.trim());
        if (c.isEmpty()) {
            return "주기 미기록.";
        }
        String u = c.toUpperCase(Locale.ROOT);
        if ("RT".equals(u)) {
            return "RT: 건별.";
        }
        if ("T0".equals(u)) {
            return "T0: 당일 합산.";
        }
        if (u.startsWith("TM")) {
            return u + ": 당일 누적.";
        }
        if (u.startsWith("TH")) {
            return u + ": 당일 누적.";
        }
        if ("M5".equals(u)) {
            return "M5: 5분마다.";
        }
        if ("M10".equals(u)) {
            return "M10: 10분마다.";
        }
        if ("M30".equals(u)) {
            return "M30: 30분마다.";
        }
        if ("H1".equals(u)) {
            return "H1: 1시간마다, 하루 24회.";
        }
        if ("H2".equals(u)) {
            return "H2: 2시간마다, 하루 12회.";
        }
        if ("H4".equals(u)) {
            return "H4: 4시간마다, 하루 6회.";
        }
        if ("H6".equals(u)) {
            return "H6: 6시간마다, 하루 4회.";
        }
        if ("H8".equals(u)) {
            return "H8: 8시간마다, 하루 3회.";
        }
        if ("H12".equals(u)) {
            return "H12: 12시간마다, 하루 2회.";
        }
        if ("D0".equals(u)) {
            return "D0: 하루 1회.";
        }
        if (u.startsWith("D") && u.length() > 1 && Character.isDigit(u.charAt(1))) {
            return u + ": 하루 1회.";
        }
        if (u.startsWith("W") || u.startsWith("WK")) {
            return u + ": 주기마다 1회.";
        }
        return u + ": 주기마다 1회.";
    }
}
