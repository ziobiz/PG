package com.pg.util;

import java.math.BigDecimal;
import java.util.List;

/**
 * 대시보드용 JPA 다중 컬럼 집계 결과 보정.
 * Hibernate/Spring Data 조합에서 단일 튜플이 {@code Object[]} 한 겹 더 감싸지거나,
 * 셀 값이 예상과 다른 타입으로 올 때 {@link ClassCastException} 을 막는다.
 */
public final class DashboardTupleRows {

    private DashboardTupleRows() {
    }

    /** 리포지토리가 돌려준 원시 결과를 컬럼 배열로 정규화한다. */
    public static Object[] normalizeRow(Object raw) {
        if (raw == null) {
            return null;
        }
        Object[] row = null;
        if (raw instanceof Object[] arr) {
            row = arr;
        } else if (raw instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Object[] a2) {
                row = a2;
            }
        }
        if (row == null) {
            return null;
        }
        if (row.length == 1 && row[0] instanceof Object[] inner) {
            return inner;
        }
        return row;
    }

    public static long readLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        if (o instanceof Object[] a) {
            return a.length > 0 ? readLong(a[0]) : 0L;
        }
        if (o instanceof List<?> list && !list.isEmpty()) {
            return readLong(list.get(0));
        }
        try {
            return new BigDecimal(o.toString()).longValue();
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    public static BigDecimal readDecimal(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (o instanceof Object[] a) {
            return a.length > 0 ? readDecimal(a[0]) : BigDecimal.ZERO;
        }
        if (o instanceof List<?> list && !list.isEmpty()) {
            return readDecimal(list.get(0));
        }
        try {
            return new BigDecimal(o.toString());
        } catch (RuntimeException e) {
            return BigDecimal.ZERO;
        }
    }
}
