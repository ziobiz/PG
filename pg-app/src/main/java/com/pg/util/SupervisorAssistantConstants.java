package com.pg.util;

import com.pg.entity.OrgLevel;

/**
 * 담당자(보조) SUPERVISOR — 총본사·본사·총판 전용 감독 역할.
 * 역할 부여는 본사설정 사용자설정(총본사)에서만 가능합니다.
 * 운영관리 「노티관리」는 조직 단계 권한이 있어도 SUPERVISOR(또는 총본사 ADMIN)만 실제 사용 가능합니다.
 */
public final class SupervisorAssistantConstants {

    public static final String ASSISTANT_ROLE_TYPE = "SUPERVISOR";

    public static final String PERMISSION_GROUP_NM = "감독담당";

    /** 운영관리 — 노티관리 (NOTI Provision). SUPERVISOR 전용 실행 메뉴. */
    public static final String NOTI_PROVISION_PAGE_URL = "/ops/notiProvision";

    private SupervisorAssistantConstants() {
    }

    public static boolean isSupervisorRoleType(String assistantRoleType) {
        return assistantRoleType != null
                && ASSISTANT_ROLE_TYPE.equalsIgnoreCase(assistantRoleType.trim());
    }

    /** 조직 상한 권한과 무관하게 SUPERVISOR만 실행 가능한 메뉴 URL */
    public static boolean isSupervisorOnlyPageUrl(String pageUrl) {
        if (pageUrl == null || pageUrl.isBlank()) {
            return false;
        }
        String path = pageUrl.trim();
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        return NOTI_PROVISION_PAGE_URL.equals(path);
    }

    /** SUPERVISOR 사용자를 둘 수 있는 조직 단계 */
    public static boolean isSupervisorEligibleOrgLevel(OrgLevel level) {
        if (level == null) {
            return false;
        }
        return level == OrgLevel.HEADQUARTERS
                || level == OrgLevel.REGIONAL
                || level == OrgLevel.MASTER_DIST;
    }

    public static boolean isSupervisorEligibleOrgLevelName(String orgLevelName) {
        if (orgLevelName == null || orgLevelName.isBlank()) {
            return false;
        }
        try {
            return isSupervisorEligibleOrgLevel(OrgLevel.valueOf(orgLevelName.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
