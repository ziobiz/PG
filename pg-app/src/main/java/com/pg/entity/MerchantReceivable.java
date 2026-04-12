package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 미수금(관리자 등록·차지백 등). 다음 정산 지급액에서 환수금 차감 후 FIFO로 차감.
 */
@Entity
@Table(name = "tb_merchant_receivable")
public class MerchantReceivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "remaining_amount", nullable = false)
    private Long remainingAmount;

    @Column(name = "applied_amount", nullable = false)
    private Long appliedAmount = 0L;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "reason_code", nullable = false, length = 40)
    private String reasonCode = "MANUAL";

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public Long getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Long remainingAmount) { this.remainingAmount = remainingAmount; }
    public Long getAppliedAmount() { return appliedAmount; }
    public void setAppliedAmount(Long appliedAmount) { this.appliedAmount = appliedAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
