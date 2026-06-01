package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_hq_pay_card_blacklist")
public class HqPayCardBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** null 또는 공백 = 전 PG 공통 */
    @Column(name = "pg_vendor", length = 32)
    private String pgVendor;

    @Column(name = "pan_hash", nullable = false, length = 64)
    private String panHash;

    @Column(name = "pan_display", nullable = false, length = 24)
    private String panDisplay;

    @Column(name = "source", nullable = false, length = 16)
    private String source = "MANUAL";

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "active_yn", nullable = false, length = 1)
    private String activeYn = "Y";

    @Column(name = "registered_by", length = 64)
    private String registeredBy;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "released_by", length = 64)
    private String releasedBy;

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
    public String getPanDisplay() { return panDisplay; }
    public void setPanDisplay(String panDisplay) { this.panDisplay = panDisplay; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getActiveYn() { return activeYn; }
    public void setActiveYn(String activeYn) { this.activeYn = activeYn; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }
    public String getReleasedBy() { return releasedBy; }
    public void setReleasedBy(String releasedBy) { this.releasedBy = releasedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
