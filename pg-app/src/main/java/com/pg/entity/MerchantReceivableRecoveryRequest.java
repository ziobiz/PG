package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 미수금 수동 환수 요청. 가맹 정산이 {@code MANUAL}일 때만 다음 정산 마감 시 지급액에서 차감됩니다.
 */
@Entity
@Table(name = "tb_merchant_receivable_recovery_req")
public class MerchantReceivableRecoveryRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_receivable_id", nullable = false)
    private Long merchantReceivableId;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_PENDING;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "applied_settlement_run_id")
    private Long appliedSettlementRunId;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantReceivableId() { return merchantReceivableId; }
    public void setMerchantReceivableId(Long merchantReceivableId) { this.merchantReceivableId = merchantReceivableId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public Long getAppliedSettlementRunId() { return appliedSettlementRunId; }
    public void setAppliedSettlementRunId(Long appliedSettlementRunId) { this.appliedSettlementRunId = appliedSettlementRunId; }
}
