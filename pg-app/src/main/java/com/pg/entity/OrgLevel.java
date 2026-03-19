package com.pg.entity;

/**
 * 영업 조직 단계 (총본사 → 본사 → 총판 → 지사 → 대리점 → 영업점 → 가맹점)
 * 총본사: 총 슈퍼 관리자, 본사: 총본사가 생성하는 사이트, 수수료는 가맹점 생성 후 부가
 *
 * 계층 배치 규칙: 모든 조직(총판·지사·대리점·영업점·가맹점)은 중간 조직 없이 상위 조직에 직접 배치 가능.
 * 이동 규칙: 상위 조직으로의 이동만 허용, 하위 조직으로의 이동은 불가. 이동 시 하위 조직 전체가 함께 이동.
 * 사용여부: 상위를 미사용으로 변경하면 하위 전체 미사용. 가맹점은 별도 상위 선택으로 개별 사용 활성화 가능.
 */
public enum OrgLevel {
    HEADQUARTERS(1, "총본사"),
    REGIONAL(2, "본사"),
    MASTER_DIST(3, "총판"),
    BRANCH(4, "지사"),
    AGENCY(5, "대리점"),
    SALES_OFFICE(6, "영업점"),
    MERCHANT(7, "가맹점");

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
