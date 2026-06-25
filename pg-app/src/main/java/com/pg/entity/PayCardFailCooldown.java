package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pay_card_fail_cooldown")
public class PayCardFailCooldown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pg_vendor", nullable = false, length = 32)
    private String pgVendor;

    @Column(name = "pan_hash", nullable = false, length = 64)
    private String panHash;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "pan_mask_key", length = 24)
    private String panMaskKey;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "last_fail_at")
    private LocalDateTime lastFailAt;

    @Column(name = "last_outcome_code", length = 32)
    private String lastOutcomeCode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = n;
        }
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPgVendor() { return pgVendor; }
    public void setPgVendor(String pgVendor) { this.pgVendor = pgVendor; }
    public String getPanHash() { return panHash; }
    public void setPanHash(String panHash) { this.panHash = panHash; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getPanMaskKey() { return panMaskKey; }
    public void setPanMaskKey(String panMaskKey) { this.panMaskKey = panMaskKey; }
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
    public LocalDateTime getBlockedUntil() { return blockedUntil; }
    public void setBlockedUntil(LocalDateTime blockedUntil) { this.blockedUntil = blockedUntil; }
    public LocalDateTime getLastFailAt() { return lastFailAt; }
    public void setLastFailAt(LocalDateTime lastFailAt) { this.lastFailAt = lastFailAt; }
    public String getLastOutcomeCode() { return lastOutcomeCode; }
    public void setLastOutcomeCode(String lastOutcomeCode) { this.lastOutcomeCode = lastOutcomeCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
