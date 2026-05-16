package com.pg.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;

/**
 * 결제·정산 목록에서 검색구분 「루트」는 부분 일치가 아닌 루트 번호 완전 일치만 허용합니다.
 * (예: 5 → 5만, 15는 제외)
 */
public final class RouteSearchUtil {

    private RouteSearchUtil() {
    }

    public static Predicate buildRouteNoExactPredicate(CriteriaBuilder cb, Path<String> routePath, String keywordRaw) {
        if (keywordRaw == null || keywordRaw.isBlank()) {
            return cb.conjunction();
        }
        String kw = keywordRaw.trim().toUpperCase(Locale.ROOT);
        return cb.and(
                cb.isNotNull(routePath),
                cb.equal(cb.upper(cb.trim(routePath)), kw));
    }

    /** 정산 실행·가맹정산 목록 등 메모리 필터용 */
    public static boolean routeNoRowMatches(Object routeCellValue, String keywordRaw) {
        if (keywordRaw == null || keywordRaw.isBlank()) {
            return true;
        }
        String kw = keywordRaw.trim();
        if (routeCellValue == null) {
            return false;
        }
        String cell = String.valueOf(routeCellValue).trim();
        if (cell.isEmpty() || "-".equals(cell)) {
            return false;
        }
        return kw.equalsIgnoreCase(cell);
    }
}
