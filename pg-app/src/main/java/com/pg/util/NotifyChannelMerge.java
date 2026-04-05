package com.pg.util;

import java.util.Locale;

/**
 * 동일 거래에 CALLBACK·RESULT 노티가 순차 도착할 때 {@code pg_trnsctn.notify_channel_type} 병합.
 */
public final class NotifyChannelMerge {

    private NotifyChannelMerge() {
    }

    /**
     * @param previous 기존 저장값 (null·공백 가능)
     * @param incoming 이번 수신 채널 (CALLBACK·CALL·RESULT 등)
     * @return CALLBACK, RESULT, BOTH 또는 incoming 정규화값; 둘 다 비면 null
     */
    public static String mergeStored(String previous, String incoming) {
        String p = normalize(previous);
        String i = normalize(incoming);
        if (i.isEmpty()) {
            return p.isEmpty() ? null : p;
        }
        if (p.isEmpty()) {
            return i;
        }
        if ("BOTH".equals(p) || "BOTH".equals(i)) {
            return "BOTH";
        }
        boolean pCb = isCallbackish(p);
        boolean iCb = isCallbackish(i);
        boolean pRs = "RESULT".equals(p);
        boolean iRs = "RESULT".equals(i);
        if ((pCb && iRs) || (pRs && iCb)) {
            return "BOTH";
        }
        return i;
    }

    private static String normalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isCallbackish(String u) {
        return "CALLBACK".equals(u) || "CALL".equals(u);
    }
}
