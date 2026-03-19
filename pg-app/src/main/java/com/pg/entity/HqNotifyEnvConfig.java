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

    /** 자동무효: 거래 후 경과 시간(시간) — 추후 배치 연동용 */
    @Column(name = "auto_void_after_hours")
    private Integer autoVoidAfterHours;

    @Column(name = "notify_ok_response", length = 500)
    private String notifyOkResponse = "{\"result\":\"OK\"}";

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
    public String getNotifyOkResponse() { return notifyOkResponse; }
    public void setNotifyOkResponse(String notifyOkResponse) { this.notifyOkResponse = notifyOkResponse; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
