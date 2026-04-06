package com.pg.util;

import java.util.Locale;

/**
 * 동일 거래에 CALLBACK·RESULT 등 노티가 순차 도착할 때 {@link com.pg.entity.PgTrnsctn#getStatus()} 병합.
 * <ul>
 *   <li>RESULT 로 실패·취소·환불·무효 등 터미널 상태가 오면, 이전 CALLBACK 성공(10)을 덮어씁니다.</li>
 *   <li>CALLBACK 은 일반적으로 더 강한(높은 rank) 상태만 반영해 성공→실패 순서를 허용합니다.</li>
 *   <li>이번 노티에서 상태를 판단할 수 없으면(incoming null) 기존 상태를 유지합니다.</li>
 * </ul>
 */
public final class NotifyToTxnStatusMerge {

    private NotifyToTxnStatusMerge() {
    }

    /**
     * @param previous   기존 DB 상태 (없으면 null)
     * @param incoming   이번 노티에서 해석한 상태 (없으면 null — 유지)
     * @param notifyChannel CALLBACK, RESULT 등 (대소문자 무시)
     */
    public static String merge(String previous, String incoming, String notifyChannel) {
        if (incoming == null || incoming.isBlank()) {
            if (previous != null && !previous.isBlank()) {
                return previous.trim();
            }
            return "08";
        }
        String inc = incoming.trim();
        if (previous == null || previous.isBlank()) {
            return inc;
        }
        String prev = previous.trim();
        /* 승인 완료(10) 건에 이어지는 취소·무효·환불·실패 노티는 즉시 반영(피지·노티미들웨어 후속 통지) */
        if ("10".equals(prev) && isTerminalOutcome(inc)) {
            return inc;
        }
        String ch = notifyChannel == null ? "" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        if ("RESULT".equals(ch) && isTerminalOutcome(inc)) {
            return inc;
        }
        int rp = rank(prev);
        int ri = rank(inc);
        return ri > rp ? inc : prev;
    }

    /** 실패·취소·환불·무효·오류 등 최종 확정에 가까운 상태 */
    public static boolean isTerminalOutcome(String st) {
        if (st == null || st.isBlank()) {
            return false;
        }
        return switch (st.trim()) {
            case "99", "F0", "f0", "20", "30", "31", "21", "22", "40", "41", "42" -> true;
            default -> false;
        };
    }

    private static int rank(String st) {
        if (st == null || st.isBlank()) {
            return 0;
        }
        return switch (st.trim()) {
            case "99", "F0", "f0" -> 100;
            case "30", "31" -> 95;
            case "20" -> 90;
            case "21", "22", "40", "41", "42" -> 88;
            case "10" -> 50;
            case "08" -> 40;
            default -> 25;
        };
    }
}
