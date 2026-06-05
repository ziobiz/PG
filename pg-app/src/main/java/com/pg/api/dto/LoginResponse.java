package com.pg.api.dto;

import java.util.List;
import java.util.Map;

public class LoginResponse {
    private String token;
    private String userId;
    private String userNm;
    private Long orgUnitId;
    private String compId;
    private String orgLevel;
    private String role;
    /** REPRESENTATIVE / ASSISTANT — 가맹 챗봇 메뉴 노출 등 클라이언트 판단용 */
    private String userType;
    /** 권한그룹 표시명(tb_user.permission_group_nm 등). 헤더 다국어용 */
    private String permissionGroupNm;
    /** 본사·총판 관리자 URL 호스트로 로그인한 경우, 사이드바·테마용 브랜딩 조회 compId(포털 루트 조직 코드). 없으면 null */
    private String brandingCompId;
    /** 비밀번호 초기화 등으로 임시 비번 사용 중이면 true — 클라이언트에서 변경 유도 */
    private boolean mustChangePassword;
    /** ADMIN·총본사·본사·총판 등 Google OTP 미등록 시 true — 로그인 후 등록 유도 */
    private boolean mustSetupOtp;
    /** OTP 등록 여부 Y/N */
    private String otpRegisteredYn = "N";
    /** 조직별 메뉴 권한(URL→NONE/OBSERVER/MODIFY/DELETE). ADMIN이면 null(무제한) */
    private Map<String, String> pagePermissions;
    /** 가맹점(MERCHANT) 로그인 시 챗봇결제 사용 Y/N — 그 외 조직·역할은 null */
    private String chatbotPaymentUseYn;
    /** 가맹점(MERCHANT) 로그인 시 URL(웹)결제 사용 Y/N — 그 외 조직·역할은 null */
    private String webPaymentUseYn;
    /** 가맹점(MERCHANT) 로그인 시 본사 API 배포(브로커 시크릿 발급) 완료 Y/N — 가맹점API 메뉴 노출용 */
    private String merchantApiDeployedYn;
    /** 공지사항 등록 가능(총본사·본사·총판 + 화면 권한 MODIFY 이상) */
    private boolean canWriteNotice;
    /** 태블릿 모드 사이드바에 노출 가능한 URL 목록(조직 단계 설정 ∩ 페이지 권한) */
    private List<String> tabletMenuUrls;
    /** 소속 조직(tb_org_unit) 태블릿 UI 기능 Y/N — ADMIN·조직 없음은 null */
    private String tabletFeatureUseYn;

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
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getPermissionGroupNm() { return permissionGroupNm; }
    public void setPermissionGroupNm(String permissionGroupNm) { this.permissionGroupNm = permissionGroupNm; }
    public String getBrandingCompId() { return brandingCompId; }
    public void setBrandingCompId(String brandingCompId) { this.brandingCompId = brandingCompId; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public boolean isMustSetupOtp() { return mustSetupOtp; }
    public void setMustSetupOtp(boolean mustSetupOtp) { this.mustSetupOtp = mustSetupOtp; }
    public String getOtpRegisteredYn() { return otpRegisteredYn; }
    public void setOtpRegisteredYn(String otpRegisteredYn) { this.otpRegisteredYn = otpRegisteredYn; }
    public Map<String, String> getPagePermissions() { return pagePermissions; }
    public void setPagePermissions(Map<String, String> pagePermissions) { this.pagePermissions = pagePermissions; }
    public String getChatbotPaymentUseYn() { return chatbotPaymentUseYn; }
    public void setChatbotPaymentUseYn(String chatbotPaymentUseYn) { this.chatbotPaymentUseYn = chatbotPaymentUseYn; }
    public String getWebPaymentUseYn() { return webPaymentUseYn; }
    public void setWebPaymentUseYn(String webPaymentUseYn) { this.webPaymentUseYn = webPaymentUseYn; }
    public String getMerchantApiDeployedYn() { return merchantApiDeployedYn; }
    public void setMerchantApiDeployedYn(String merchantApiDeployedYn) { this.merchantApiDeployedYn = merchantApiDeployedYn; }
    public boolean isCanWriteNotice() { return canWriteNotice; }
    public void setCanWriteNotice(boolean canWriteNotice) { this.canWriteNotice = canWriteNotice; }
    public List<String> getTabletMenuUrls() { return tabletMenuUrls; }
    public void setTabletMenuUrls(List<String> tabletMenuUrls) { this.tabletMenuUrls = tabletMenuUrls; }
    public String getTabletFeatureUseYn() { return tabletFeatureUseYn; }
    public void setTabletFeatureUseYn(String tabletFeatureUseYn) { this.tabletFeatureUseYn = tabletFeatureUseYn; }
}
