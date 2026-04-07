package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 본사 전산노티 수신·결제 후속(자동무효 등) 환경설정 — 단일 행 (NOTI 전산노티대상·환경설정 대응)
 */
@Entity
@Table(name = "tb_hq_notify_env_config")
public class HqNotifyEnvConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL 경로에 포함되는 비밀 토큰 (무단 호출 방지) */
    @Column(name = "ingress_token", nullable = false, unique = true, length = 64)
    private String ingressToken;

    /**
     * 노티 URL 안내용 공개 베이스 (비우면 서버가 요청 Host로 조합).
     * 운영 배포 후 https://실제도메인 형태로 설정 권장.
     */
    @Column(name = "public_base_url", length = 500)
    private String publicBaseUrl;

    @Column(name = "auto_void_yn", length = 1)
    private String autoVoidYn = "N";

    @Column(name = "email_void_yn", length = 1)
    private String emailVoidYn = "N";

    @Column(name = "auto_refund_yn", length = 1)
    private String autoRefundYn = "N";

    @Column(name = "force_refund_yn", length = 1)
    private String forceRefundYn = "N";

    /** 자동무효: 레거시(승인 후 N시간) — 미사용, {@link #autoVoidStartMin}/{@link #autoVoidEndMin} 사용 */
    @Column(name = "auto_void_after_hours")
    private Integer autoVoidAfterHours;

    /** 이메일무효: 승인 후 경과 기준(시간) — 레거시, 미사용 */
    @Column(name = "email_void_after_hours")
    private Integer emailVoidAfterHours;

    /** 자동무효: 승인일(기준 Zone) 당일 시작 시각(분). NULL이면 0:00 */
    @Column(name = "auto_void_start_min")
    private Integer autoVoidStartMin;

    /** 자동무효: 승인일 당일 마감 시각(분). NULL이면 21:00 (일본 동일 시각 +2h → 23:00) */
    @Column(name = "auto_void_end_min")
    private Integer autoVoidEndMin;

    /** 이메일무효: 시작 시각(분). NULL이면 auto_void_end_min+1(자동무효 마감 직후) */
    @Column(name = "email_void_start_min")
    private Integer emailVoidStartMin;

    /** 이메일무효: 마감 시각(분). NULL이면 23:59 */
    @Column(name = "email_void_end_min")
    private Integer emailVoidEndMin;

    /** 자동환불: 결제 익일(태국) 구간 시작 시각(0~1439분). NULL이면 0:00 */
    @Column(name = "auto_refund_window_start_min")
    private Integer autoRefundWindowStartMin;

    /** 후속조치 경과 판단 기준 ZoneId (NULL이면 전산 표준시와 동일) */
    @Column(name = "pay_follow_ref_zone", length = 64)
    private String payFollowRefZone;

    /** 자동환불: 승인일 기준 경과 일수 */
    @Column(name = "auto_refund_after_days")
    private Integer autoRefundAfterDays;

    /** 강제환불: 승인일 기준 경과 일수 */
    @Column(name = "force_refund_after_days")
    private Integer forceRefundAfterDays;

    @Column(name = "auto_void_reflect_settlement_yn", length = 1)
    private String autoVoidReflectSettlementYn = "N";

    @Column(name = "email_void_reflect_settlement_yn", length = 1)
    private String emailVoidReflectSettlementYn = "N";

    @Column(name = "auto_refund_reflect_settlement_yn", length = 1)
    private String autoRefundReflectSettlementYn = "N";

    @Column(name = "force_refund_reflect_settlement_yn", length = 1)
    private String forceRefundReflectSettlementYn = "N";

    @Column(name = "notify_ok_response", length = 500)
    private String notifyOkResponse = "{\"result\":\"OK\"}";

    /** 전 사용자 OTP 필수 여부 (본사설정·ziobiz NOTI 계정정책 대응) */
    @Column(name = "otp_required_yn", length = 1)
    private String otpRequiredYn = "Y";

    /** OTP 정책 모드 (NOTI와 동일 포맷 적용 여부) */
    @Column(name = "otp_policy_mode", length = 20)
    private String otpPolicyMode = "NOTI";

    /** 비밀번호 정책 모드 (NOTI와 동일 포맷 적용 여부) */
    @Column(name = "password_policy_mode", length = 20)
    private String passwordPolicyMode = "NOTI";

    /** 비밀번호 찾기 기능 사용 여부 (보안상 기본 N) */
    @Column(name = "forgot_password_enabled_yn", length = 1)
    private String forgotPasswordEnabledYn = "N";

    /** 사용자관리(추가/삭제/초기화) 권한 기능 활성화 여부 */
    @Column(name = "manager_user_control_enabled_yn", length = 1)
    private String managerUserControlEnabledYn = "N";

    /** 사용자관리 권한자 비밀번호 초기화 허용 여부 */
    @Column(name = "manager_password_reset_enabled_yn", length = 1)
    private String managerPasswordResetEnabledYn = "N";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIngressToken() { return ingressToken; }
    public void setIngressToken(String ingressToken) { this.ingressToken = ingressToken; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public String getAutoVoidYn() { return autoVoidYn; }
    public void setAutoVoidYn(String autoVoidYn) { this.autoVoidYn = autoVoidYn; }
    public String getEmailVoidYn() { return emailVoidYn; }
    public void setEmailVoidYn(String emailVoidYn) { this.emailVoidYn = emailVoidYn; }
    public String getAutoRefundYn() { return autoRefundYn; }
    public void setAutoRefundYn(String autoRefundYn) { this.autoRefundYn = autoRefundYn; }
    public String getForceRefundYn() { return forceRefundYn; }
    public void setForceRefundYn(String forceRefundYn) { this.forceRefundYn = forceRefundYn; }
    public Integer getAutoVoidAfterHours() { return autoVoidAfterHours; }
    public void setAutoVoidAfterHours(Integer autoVoidAfterHours) { this.autoVoidAfterHours = autoVoidAfterHours; }
    public Integer getEmailVoidAfterHours() { return emailVoidAfterHours; }
    public void setEmailVoidAfterHours(Integer emailVoidAfterHours) { this.emailVoidAfterHours = emailVoidAfterHours; }
    public Integer getAutoVoidStartMin() { return autoVoidStartMin; }
    public void setAutoVoidStartMin(Integer autoVoidStartMin) { this.autoVoidStartMin = autoVoidStartMin; }
    public Integer getAutoVoidEndMin() { return autoVoidEndMin; }
    public void setAutoVoidEndMin(Integer autoVoidEndMin) { this.autoVoidEndMin = autoVoidEndMin; }
    public Integer getEmailVoidStartMin() { return emailVoidStartMin; }
    public void setEmailVoidStartMin(Integer emailVoidStartMin) { this.emailVoidStartMin = emailVoidStartMin; }
    public Integer getEmailVoidEndMin() { return emailVoidEndMin; }
    public void setEmailVoidEndMin(Integer emailVoidEndMin) { this.emailVoidEndMin = emailVoidEndMin; }
    public Integer getAutoRefundWindowStartMin() { return autoRefundWindowStartMin; }
    public void setAutoRefundWindowStartMin(Integer autoRefundWindowStartMin) { this.autoRefundWindowStartMin = autoRefundWindowStartMin; }
    public String getPayFollowRefZone() { return payFollowRefZone; }
    public void setPayFollowRefZone(String payFollowRefZone) { this.payFollowRefZone = payFollowRefZone; }
    public Integer getAutoRefundAfterDays() { return autoRefundAfterDays; }
    public void setAutoRefundAfterDays(Integer autoRefundAfterDays) { this.autoRefundAfterDays = autoRefundAfterDays; }
    public Integer getForceRefundAfterDays() { return forceRefundAfterDays; }
    public void setForceRefundAfterDays(Integer forceRefundAfterDays) { this.forceRefundAfterDays = forceRefundAfterDays; }
    public String getAutoVoidReflectSettlementYn() { return autoVoidReflectSettlementYn; }
    public void setAutoVoidReflectSettlementYn(String autoVoidReflectSettlementYn) { this.autoVoidReflectSettlementYn = autoVoidReflectSettlementYn; }
    public String getEmailVoidReflectSettlementYn() { return emailVoidReflectSettlementYn; }
    public void setEmailVoidReflectSettlementYn(String emailVoidReflectSettlementYn) { this.emailVoidReflectSettlementYn = emailVoidReflectSettlementYn; }
    public String getAutoRefundReflectSettlementYn() { return autoRefundReflectSettlementYn; }
    public void setAutoRefundReflectSettlementYn(String autoRefundReflectSettlementYn) { this.autoRefundReflectSettlementYn = autoRefundReflectSettlementYn; }
    public String getForceRefundReflectSettlementYn() { return forceRefundReflectSettlementYn; }
    public void setForceRefundReflectSettlementYn(String forceRefundReflectSettlementYn) { this.forceRefundReflectSettlementYn = forceRefundReflectSettlementYn; }
    public String getNotifyOkResponse() { return notifyOkResponse; }
    public void setNotifyOkResponse(String notifyOkResponse) { this.notifyOkResponse = notifyOkResponse; }
    public String getOtpRequiredYn() { return otpRequiredYn; }
    public void setOtpRequiredYn(String otpRequiredYn) { this.otpRequiredYn = otpRequiredYn; }
    public String getOtpPolicyMode() { return otpPolicyMode; }
    public void setOtpPolicyMode(String otpPolicyMode) { this.otpPolicyMode = otpPolicyMode; }
    public String getPasswordPolicyMode() { return passwordPolicyMode; }
    public void setPasswordPolicyMode(String passwordPolicyMode) { this.passwordPolicyMode = passwordPolicyMode; }
    public String getForgotPasswordEnabledYn() { return forgotPasswordEnabledYn; }
    public void setForgotPasswordEnabledYn(String forgotPasswordEnabledYn) { this.forgotPasswordEnabledYn = forgotPasswordEnabledYn; }
    public String getManagerUserControlEnabledYn() { return managerUserControlEnabledYn; }
    public void setManagerUserControlEnabledYn(String managerUserControlEnabledYn) { this.managerUserControlEnabledYn = managerUserControlEnabledYn; }
    public String getManagerPasswordResetEnabledYn() { return managerPasswordResetEnabledYn; }
    public void setManagerPasswordResetEnabledYn(String managerPasswordResetEnabledYn) { this.managerPasswordResetEnabledYn = managerPasswordResetEnabledYn; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
