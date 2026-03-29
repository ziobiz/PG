package com.pg.api.dto;

import java.util.Map;

public class LoginResponse {
    private String token;
    private String userId;
    private String userNm;
    private Long orgUnitId;
    private String compId;
    private String orgLevel;
    private String role;
    /** 본사·총판 관리자 URL 호스트로 로그인한 경우, 사이드바·테마용 브랜딩 조회 compId(포털 루트 조직 코드). 없으면 null */
    private String brandingCompId;
    /** 비밀번호 초기화 등으로 임시 비번 사용 중이면 true — 클라이언트에서 변경 유도 */
    private boolean mustChangePassword;
    /** 조직별 메뉴 권한(URL→NONE/OBSERVER/MODIFY/DELETE). ADMIN이면 null(무제한) */
    private Map<String, String> pagePermissions;
    /** 공지사항 등록 가능(총본사·본사·총판 + 화면 권한 MODIFY 이상) */
    private boolean canWriteNotice;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserNm() { return userNm; }
    public void setUserNm(String userNm) { this.userNm = userNm; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public String getOrgLevel() { return orgLevel; }
    public void setOrgLevel(String orgLevel) { this.orgLevel = orgLevel; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getBrandingCompId() { return brandingCompId; }
    public void setBrandingCompId(String brandingCompId) { this.brandingCompId = brandingCompId; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public Map<String, String> getPagePermissions() { return pagePermissions; }
    public void setPagePermissions(Map<String, String> pagePermissions) { this.pagePermissions = pagePermissions; }
    public boolean isCanWriteNotice() { return canWriteNotice; }
    public void setCanWriteNotice(boolean canWriteNotice) { this.canWriteNotice = canWriteNotice; }
}
