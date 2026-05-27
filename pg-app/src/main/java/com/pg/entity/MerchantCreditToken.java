package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** ChillPay CreditToken — 가맹점·고객·PG별 저장 카드 토큰(카드번호 미저장). */
@Entity
@Table(name = "tb_merchant_credit_token")
public class MerchantCreditToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "pg_cd", nullable = false, length = 50)
    private String pgCd;

    @Column(name = "customer_id", nullable = false, length = 200)
    private String customerId;

    @Column(name = "credit_token", nullable = false, length = 500)
    private String creditToken;

    @Column(name = "card_mask", length = 30)
    private String cardMask;

    @Column(name = "card_brand", length = 30)
    private String cardBrand;

    @Column(name = "active_yn", nullable = false, length = 1)
    private String activeYn = "Y";

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (activeYn == null || activeYn.isBlank()) {
            activeYn = "Y";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getPgCd() { return pgCd; }
    public void setPgCd(String pgCd) { this.pgCd = pgCd; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCreditToken() { return creditToken; }
    public void setCreditToken(String creditToken) { this.creditToken = creditToken; }
    public String getCardMask() { return cardMask; }
    public void setCardMask(String cardMask) { this.cardMask = cardMask; }
    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
    public String getActiveYn() { return activeYn; }
    public void setActiveYn(String activeYn) { this.activeYn = activeYn; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
