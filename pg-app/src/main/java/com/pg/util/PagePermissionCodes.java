package com.pg.util;

import java.util.Locale;

/**
 * 본사권한설정 메뉴 권한 코드 — NONE / OBSERVER / OBSERVER_HELLO / MODIFY / MODIFY_HELLO / DELETE.
 * HELLO 변형은 기본 접근(조회·수정)은 동일하고 헬로(안내·VIEW SETTING 토글)만 추가 허용합니다.
 */
public final class PagePermissionCodes {

    public static final String P_NONE = "NONE";
    public static final String P_OBSERVER = "OBSERVER";
    public static final String P_OBSERVER_HELLO = "OBSERVER_HELLO";
    public static final String P_MODIFY = "MODIFY";
    public static final String P_MODIFY_HELLO = "MODIFY_HELLO";
    public static final String P_DELETE = "DELETE";

    private PagePermissionCodes() {
    }

    public static String normalize(String p) {
        if (p == null || p.isBlank()) {
            return P_DELETE;
        }
        String u = p.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case P_NONE, P_OBSERVER, P_OBSERVER_HELLO, P_MODIFY, P_MODIFY_HELLO, P_DELETE -> u;
            default -> P_DELETE;
        };
    }

    /** 쓰기·삭제 판단용 기본 권한(OBSERVER_HELLO→OBSERVER, MODIFY_HELLO→MODIFY). */
    public static String base(String p) {
        return switch (normalize(p)) {
            case P_OBSERVER_HELLO -> P_OBSERVER;
            case P_MODIFY_HELLO -> P_MODIFY;
            default -> normalize(p);
        };
    }

    /** 헬로 버튼·안내(파스텔)·VIEW SETTING 토글 허용 여부. */
    public static boolean helloAllowed(String p) {
        String u = normalize(p);
        return P_DELETE.equals(u) || P_OBSERVER_HELLO.equals(u) || P_MODIFY_HELLO.equals(u);
    }

    public static boolean isObserverLike(String p) {
        return P_OBSERVER.equals(base(p));
    }

    public static boolean isModifyLike(String p) {
        return P_MODIFY.equals(base(p));
    }

    public static boolean canWriteLike(String p) {
        String b = base(p);
        return P_MODIFY.equals(b) || P_DELETE.equals(b);
    }

    public static int baseStrength(String p) {
        return switch (base(p)) {
            case P_DELETE -> 4;
            case P_MODIFY -> 3;
            case P_OBSERVER -> 2;
            case P_NONE -> 1;
            default -> 1;
        };
    }

    public static String fromBaseStrength(int s) {
        if (s <= 1) {
            return P_NONE;
        }
        if (s == 2) {
            return P_OBSERVER;
        }
        if (s == 3) {
            return P_MODIFY;
        }
        return P_DELETE;
    }

    /**
     * 조직 상한과 담당자 권한 교집합 — 기본 접근은 더 제한적인 쪽, 헬로는 양쪽 모두 허용일 때만.
     */
    public static String intersect(String ceiling, String rolePerm) {
        String c = normalize(ceiling);
        String r = normalize(rolePerm);
        int baseS = Math.min(baseStrength(c), baseStrength(r));
        String merged = fromBaseStrength(baseS);
        if (P_DELETE.equals(merged)) {
            return P_DELETE;
        }
        if (helloAllowed(c) && helloAllowed(r)) {
            if (P_OBSERVER.equals(merged)) {
                return P_OBSERVER_HELLO;
            }
            if (P_MODIFY.equals(merged)) {
                return P_MODIFY_HELLO;
            }
        }
        return merged;
    }
}
