package com.pg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 본사 송금 수취계좌. ElementPay recipient id 와 표시용 정보만 보관합니다. */
@Entity
@Table(name = "tb_hq_remit_account")
public class HqRemitAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false, length = 64, unique = true)
    private String recipientId;

    @Column(name = "holder_name", length = 200)
    private String holderName;

    @Column(name = "account_last", length = 8)
    private String accountLast;

    @Column(name = "country", length = 8)
    private String country;

    @Column(name = "recipient_type", length = 20)
    private String recipientType;

    @Column(name = "default_yn", nullable = false, length = 1)
    private String defaultYn = "N";

    @Column(name = "source_cd", nullable = false, length = 20)
    private String sourceCd = "EP";

    @Column(name = "active_yn", nullable = false, length = 1)
    private String activeYn = "Y";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (defaultYn == null || defaultYn.isBlank()) {
            defaultYn = "N";
        }
        if (activeYn == null || activeYn.isBlank()) {
            activeYn = "Y";
        }
        if (sourceCd == null || sourceCd.isBlank()) {
            sourceCd = "EP";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public String getAccountLast() { return accountLast; }
    public void setAccountLast(String accountLast) { this.accountLast = accountLast; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }
    public String getDefaultYn() { return defaultYn; }
    public void setDefaultYn(String defaultYn) { this.defaultYn = defaultYn; }
    public String getSourceCd() { return sourceCd; }
    public void setSourceCd(String sourceCd) { this.sourceCd = sourceCd; }
    public String getActiveYn() { return activeYn; }
    public void setActiveYn(String activeYn) { this.activeYn = activeYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
