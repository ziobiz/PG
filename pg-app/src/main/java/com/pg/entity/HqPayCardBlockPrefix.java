package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_hq_pay_card_block_prefix")
public class HqPayCardBlockPrefix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pg_vendor", nullable = false, length = 32)
    private String pgVendor;

    @Column(name = "prefix_digits", nullable = false, length = 8)
    private String prefixDigits;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "active_yn", nullable = false, length = 1)
    private String activeYn = "Y";

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
    public String getPrefixDigits() { return prefixDigits; }
    public void setPrefixDigits(String prefixDigits) { this.prefixDigits = prefixDigits; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getActiveYn() { return activeYn; }
    public void setActiveYn(String activeYn) { this.activeYn = activeYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
