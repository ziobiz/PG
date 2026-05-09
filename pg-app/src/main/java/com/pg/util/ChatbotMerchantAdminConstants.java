package com.pg.util;

import com.pg.entity.AppUser;

/**
 * 가맹 챗봇 상품(관리자 웹 icopay) — 권한그룹·담당자 역할 코드.
 * 공개 챗봇 전용 로그인({@code ChatbotAdminAuthService})은 지정 관리자만 등록 가능하며 별도 토큰으로 동작합니다.
 */
public final class ChatbotMerchantAdminConstants {

    /** {@code tb_user.permission_group_nm} — 사용자관리·세션 표시와 동일 문자열 */
    public static final String PERMISSION_GROUP_NM = "CHATBOT";

    /** 이전 배포에서 저장된 권한그룹명 — 비교 시 허용 */
    public static final String LEGACY_PERMISSION_GROUP_NM = "챗봇관리자";

    /** 보조(ASSISTANT) 계정의 {@code assistant_role_type} */
    public static final String ASSISTANT_ROLE_TYPE = "CHATBOT_ADMIN";

    private ChatbotMerchantAdminConstants() {
    }

    public static boolean isChatbotPermissionGroupNm(String permissionGroupNm) {
        if (permissionGroupNm == null || permissionGroupNm.isBlank()) {
            return false;
        }
        String p = permissionGroupNm.trim();
        return PERMISSION_GROUP_NM.equalsIgnoreCase(p) || LEGACY_PERMISSION_GROUP_NM.equals(p);
    }

    /**
     * 관리자 웹 로그인: 가맹(MERCHANT) 계정이 챗봇 메뉴·상품 API를 쓸 수 있는지.
     * 업체 대표({@code user_type} 가 ASSISTANT 가 아님) 또는 권한그룹 CHATBOT(구 챗봇관리자).
     */
    public static boolean merchantAdminWebMayUseChatbotFeatures(AppUser user) {
        if (user == null) {
            return false;
        }
        String ut = user.getUserType() != null ? user.getUserType().trim() : "";
        if (!"ASSISTANT".equalsIgnoreCase(ut)) {
            return true;
        }
        return isChatbotPermissionGroupNm(user.getPermissionGroupNm());
    }
}
