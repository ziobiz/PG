package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 차지백(정산 후 환불·분쟁 등) 구간 정책 — 월간 건수(환불/강제환불)에 따른 건당 부과 단가.
 */
@Entity
@Table(name = "tb_chargeback_fee_policy")
public class ChargebackFeePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String remark;

    /** 건당 구간 금액의 기준 통화(표시·안내용, 정책 통화와 동일 개념) */
    @Column(name = "currency_code", nullable = false, length = 8)
    private String currencyCode = "KRW";

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ChargebackFeeTier> tiers = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = (currencyCode != null && !currencyCode.isBlank()) ? currencyCode.trim().toUpperCase() : "KRW";
    }
    public List<ChargebackFeeTier> getTiers() { return tiers; }
    public void setTiers(List<ChargebackFeeTier> tiers) { this.tiers = tiers != null ? tiers : new ArrayList<>(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
