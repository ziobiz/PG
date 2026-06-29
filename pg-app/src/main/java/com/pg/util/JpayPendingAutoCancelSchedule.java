package com.pg.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JPAY 요청(08) 무응답 시 자동 취소(20) 대기 시간(분). 0=미사용 */
public final class JpayPendingAutoCancelSchedule {

    public static final int OFF = 0;

    private static final int[] ALLOWED_MINUTES = {
            OFF, 30, 60, 120, 180, 240, 300, 360, 720
    };

    private JpayPendingAutoCancelSchedule() {
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
            case OFF -> "미사용";
            case 30 -> "30분";
            case 60 -> "1시간";
            case 120 -> "2시간";
            case 180 -> "3시간";
            case 240 -> "4시간";
            case 300 -> "5시간";
            case 360 -> "6시간";
            case 720 -> "12시간";
            default -> String.valueOf(minutes) + "분";
        };
    }
}
