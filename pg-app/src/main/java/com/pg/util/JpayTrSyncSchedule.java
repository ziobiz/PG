package com.pg.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JPAY 통합조회 포털 Export 자동 동기화 주기(분) */
public final class JpayTrSyncSchedule {

    public static final int OFF = 0;

    private static final int[] ALLOWED_MINUTES = {
            OFF, 10, 30,
            60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 660, 720
    };

    private JpayTrSyncSchedule() {
    }

    public static int clampMinutes(Integer raw) {
        int v = raw != null ? raw : OFF;
        for (int allowed : ALLOWED_MINUTES) {
            if (allowed == v) {
                return v;
            }
        }
        return OFF;
    }

    public static boolean isEnabled(int minutes) {
        return minutes > 0;
    }

    public static List<Map<String, Object>> optionRows() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int m : ALLOWED_MINUTES) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("value", m);
            row.put("labelKey", labelKey(m));
            out.add(row);
        }
        return out;
    }

    public static String labelKey(int minutes) {
        return switch (minutes) {
            case OFF -> "사용 안 함";
            case 10 -> "10분";
            case 30 -> "30분";
            case 60 -> "1시간";
            case 120 -> "2시간";
            case 180 -> "3시간";
            case 240 -> "4시간";
            case 300 -> "5시간";
            case 360 -> "6시간";
            case 420 -> "7시간";
            case 480 -> "8시간";
            case 540 -> "9시간";
            case 600 -> "10시간";
            case 660 -> "11시간";
            case 720 -> "12시간";
            default -> String.valueOf(minutes) + "분";
        };
    }
}
