package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 정산 후 취소·환불 등으로 발생한 자동 환수금(다음 정산 지급액에서 FIFO 차감).
 */
@Entity
@Table(name = "tb_settlement_recovery")
public class SettlementRecovery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(name = "trn_id", nullable = false, length = 20)
    private String trnId;

    @Column(name = "recall_amount", nullable = false, precision = 21, scale = 8)
    private BigDecimal recallAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", nullable = false, precision = 21, scale = 8)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Column(name = "applied_amount", nullable = false, precision = 21, scale = 8)
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "reason_code", nullable = false, length = 40)
    private String reasonCode;

    @Column(name = "fee_included_yn", length = 1)
    private String feeIncludedYn;

    @Column(name = "vat_applied_yn", length = 1)
    private String vatAppliedYn;

    @Column(name = "last_applied_settlement_run_id")
    private Long lastAppliedSettlementRunId;

    @Column(name = "memo", length = 500)
    private String memo;

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
    public String getTrnId() { return trnId; }
    public void setTrnId(String trnId) { this.trnId = trnId; }
    public BigDecimal getRecallAmount() { return recallAmount; }
    public void setRecallAmount(BigDecimal recallAmount) { this.recallAmount = recallAmount; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    public BigDecimal getAppliedAmount() { return appliedAmount; }
    public void setAppliedAmount(BigDecimal appliedAmount) { this.appliedAmount = appliedAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getFeeIncludedYn() { return feeIncludedYn; }
    public void setFeeIncludedYn(String feeIncludedYn) { this.feeIncludedYn = feeIncludedYn; }
    public String getVatAppliedYn() { return vatAppliedYn; }
    public void setVatAppliedYn(String vatAppliedYn) { this.vatAppliedYn = vatAppliedYn; }
    public Long getLastAppliedSettlementRunId() { return lastAppliedSettlementRunId; }
    public void setLastAppliedSettlementRunId(Long lastAppliedSettlementRunId) { this.lastAppliedSettlementRunId = lastAppliedSettlementRunId; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
