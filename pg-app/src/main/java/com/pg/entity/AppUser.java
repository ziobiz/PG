package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 관리자/웹 로그인 사용자 (TB_USER 스타일)
 */
@Entity
@Table(name = "tb_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    @Column(nullable = false)
    private boolean enabled = true;

    /** 소속 업체코드(OrgUnit.code) — 사용자관리·접근권한 연동 */
    @Column(name = "org_unit_code", length = 32)
    private String orgUnitCode;

    /** 권한그룹명(본사설정 본사별 권한 세팅과 동일 정책 트리 표시용) */
    @Column(name = "permission_group_nm", length = 100)
    private String permissionGroupNm;

    /** OTP 등록 여부 Y/N */
    @Column(name = "otp_registered_yn", length = 1)
    private String otpRegisteredYn = "N";

    /** Y: 다음 로그인 후 비밀번호 변경 필수(초기화 시 loginId+1! 등) */
    @Column(name = "password_must_change_yn", nullable = false, length = 1)
    private String passwordMustChangeYn = "N";

    /** 사용자 유형: REPRESENTATIVE / ASSISTANT */
    @Column(name = "user_type", length = 20)
    private String userType = "REPRESENTATIVE";

    /** 보조아이디 역할: MANAGER / OPERATOR / SETTLEMENT / TECH */
    @Column(name = "assistant_role_type", length = 20)
    private String assistantRoleType;

    /** 대표아이디 username (보조아이디인 경우) */
    @Column(name = "parent_username", length = 50)
    private String parentUsername;

    /** 총판이 배포한 접근페이지 정책 코드 */
    @Column(name = "menu_policy_code", length = 50)
    private String menuPolicyCode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getOrgUnitCode() { return orgUnitCode; }
    public void setOrgUnitCode(String orgUnitCode) { this.orgUnitCode = orgUnitCode; }
    public String getPermissionGroupNm() { return permissionGroupNm; }
    public void setPermissionGroupNm(String permissionGroupNm) { this.permissionGroupNm = permissionGroupNm; }
    public String getOtpRegisteredYn() { return otpRegisteredYn; }
    public void setOtpRegisteredYn(String otpRegisteredYn) { this.otpRegisteredYn = otpRegisteredYn; }
    public String getPasswordMustChangeYn() { return passwordMustChangeYn; }
    public void setPasswordMustChangeYn(String passwordMustChangeYn) { this.passwordMustChangeYn = passwordMustChangeYn; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getAssistantRoleType() { return assistantRoleType; }
    public void setAssistantRoleType(String assistantRoleType) { this.assistantRoleType = assistantRoleType; }
    public String getParentUsername() { return parentUsername; }
    public void setParentUsername(String parentUsername) { this.parentUsername = parentUsername; }
    public String getMenuPolicyCode() { return menuPolicyCode; }
    public void setMenuPolicyCode(String menuPolicyCode) { this.menuPolicyCode = menuPolicyCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
