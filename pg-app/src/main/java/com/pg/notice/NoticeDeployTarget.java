package com.pg.notice;

import com.pg.entity.OrgLevel;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 공지사항 배포 대상. {@link #labelKo} 는 화면·i18n 키와 동일합니다.
 */
public enum NoticeDeployTarget {
    HQ_ONLY("HQ_ONLY", "본사"),
    MASTER_ONLY("MASTER_ONLY", "총판"),
    HQ_AND_MASTER("HQ_AND_MASTER", "본사&총판"),
    HQ_SUB("HQ_SUB", "본사이하"),
    MASTER_SUB("MASTER_SUB", "총판이하"),
    MASTER_DIST_ONLY("MASTER_DIST_ONLY", "총판"),
    MARKETING("MARKETING", "마케팅"),
    MERCHANT("MERCHANT", "가맹점"),
    ALL("ALL", "ALL"),
    NOTI("NOTI", "특정지점");

    private final String code;
    private final String labelKo;

    NoticeDeployTarget(String code, String labelKo) {
        this.code = code;
        this.labelKo = labelKo;
    }

    public String getCode() {
        return code;
    }

    public String getLabelKo() {
        return labelKo;
    }

    public static NoticeDeployTarget fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        for (NoticeDeployTarget t : values()) {
            if (t.code.equals(s)) {
                return t;
            }
        }
        return ALL;
    }

    public static List<NoticeDeployTarget> allowedForWriter(OrgLevel writerLevel) {
        if (writerLevel == null) {
            return List.of();
        }
        return switch (writerLevel) {
            case HEADQUARTERS -> List.of(
                    HQ_ONLY, MASTER_ONLY, HQ_AND_MASTER, HQ_SUB, MASTER_SUB,
                    MARKETING, MERCHANT, ALL, NOTI);
            case REGIONAL -> List.of(MASTER_DIST_ONLY, MARKETING, MERCHANT, ALL, NOTI);
            case MASTER_DIST -> List.of(ALL, MARKETING, MERCHANT, NOTI);
            default -> List.of();
        };
    }

    public static OrgLevel notiMinLevelForWriter(OrgLevel writerLevel) {
        if (writerLevel == null) {
            return null;
        }
        return switch (writerLevel) {
            case HEADQUARTERS -> OrgLevel.REGIONAL;
            case REGIONAL -> OrgLevel.MASTER_DIST;
            case MASTER_DIST -> OrgLevel.AGENCY;
            default -> null;
        };
    }

    public static final Set<OrgLevel> MARKETING_LEVELS = EnumSet.of(
            OrgLevel.BRANCH, OrgLevel.AGENCY, OrgLevel.SALES_OFFICE);
}
