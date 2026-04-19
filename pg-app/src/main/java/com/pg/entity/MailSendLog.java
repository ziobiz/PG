package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_mail_send_log")
public class MailSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mail_kind", nullable = false, length = 32)
    private String mailKind;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "to_address", nullable = false, length = 500)
    private String toAddress;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body_preview", columnDefinition = "TEXT")
    private String bodyPreview;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "pg_trn_id", length = 32)
    private String pgTrnId;

    @Column(name = "actor_username", length = 128)
    private String actorUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMailKind() { return mailKind; }
    public void setMailKind(String mailKind) { this.mailKind = mailKind; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBodyPreview() { return bodyPreview; }
    public void setBodyPreview(String bodyPreview) { this.bodyPreview = bodyPreview; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getPgTrnId() { return pgTrnId; }
    public void setPgTrnId(String pgTrnId) { this.pgTrnId = pgTrnId; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
