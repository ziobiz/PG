package com.pg.util;

/**
 * 루트(Route) 번호 표시 — 미설정·{@code 0} 은 화면에서 {@code -} 로 통일.
 */
public final class RouteNoDisplayUtil {

    private RouteNoDisplayUtil() {
    }

    public static boolean isAbsent(String routeNo) {
        if (routeNo == null) {
            return true;
        }
        String s = routeNo.trim();
        return s.isEmpty() || "0".equals(s);
    }

    public static boolean isAbsent(Integer routeNo) {
        return routeNo == null || routeNo == 0;
    }

    public static String formatForDisplay(String routeNo) {
        if (isAbsent(routeNo)) {
            return "-";
        }
        return routeNo.trim();
    }

    public static String formatForDisplay(Integer routeNo) {
        if (isAbsent(routeNo)) {
            return "-";
        }
        return String.valueOf(routeNo);
    }

    /** {@code pg_trnsctn.route_no} 등 — 없으면 null(0 문자열 저장 방지) */
    public static String normalizeForStorage(int routeNo) {
        return routeNo == 0 ? null : String.valueOf(routeNo);
    }

    public static String normalizeForStorage(String routeNo) {
        if (isAbsent(routeNo)) {
            return null;
        }
        return routeNo.trim();
    }
}
