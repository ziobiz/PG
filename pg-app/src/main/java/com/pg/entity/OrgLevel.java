package com.pg.entity;

/**
 * 영업 조직 단계 (총본사 → 지역본사 → 총판 → 지사 → 대리점 → 가맹점)
 */
public enum OrgLevel {
    HEADQUARTERS(1, "총본사"),
    REGIONAL(2, "지역본사"),
    MASTER_DIST(3, "총판"),
    BRANCH(4, "지사"),
    AGENCY(5, "대리점"),
    MERCHANT(6, "가맹점");

    private final int code;
    private final String nameKo;

    OrgLevel(int code, String nameKo) {
        this.code = code;
        this.nameKo = nameKo;
    }

    public int getCode() { return code; }
    public String getNameKo() { return nameKo; }

    public static OrgLevel fromCode(int code) {
        for (OrgLevel l : values()) {
            if (l.code == code) return l;
        }
        return null;
    }
}
