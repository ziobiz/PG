package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 수수료 정책 (본사 기본 + 가맹점별 오버라이드)
 * 건당수수료, 취소수수료, 이용수수료, 실패수수료, 결제수수료, 환불수수료, 롤링(담보금) 비율/일수
 */
@Entity
@Table(name = "tb_commission_policy")
public class CommissionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DEFAULT=본사기본, 아니면 가맹점 code */
    @Column(name = "scope", nullable = false, length = 50)
    private String scope = "DEFAULT";

    /** 건당 수수료(원) */
    @Column(name = "per_tx_fee", precision = 12, scale = 0)
    private BigDecimal perTxFee = BigDecimal.ZERO;

    /** 취소 수수료율(%) */
    @Column(name = "cancel_rate", precision = 5, scale = 2)
    private BigDecimal cancelRate = BigDecimal.ZERO;

    /** 이용 수수료율(%) */
    @Column(name = "usage_rate", precision = 5, scale = 2)
    private BigDecimal usageRate = BigDecimal.ZERO;

    /** 실패 수수료(원/건) */
    @Column(name = "fail_fee", precision = 12, scale = 0)
    private BigDecimal failFee = BigDecimal.ZERO;

    /** 결제 수수료율(%) */
    @Column(name = "pay_rate", precision = 5, scale = 2)
    private BigDecimal payRate = BigDecimal.ZERO;

    /** 환불 수수료율(%) */
    @Column(name = "refund_rate", precision = 5, scale = 2)
    private BigDecimal refundRate = BigDecimal.ZERO;

    /** 롤링(담보금) 비율 % - 결제대금에서 보류 */
    @Column(name = "rolling_pct", precision = 5, scale = 2)
    private BigDecimal rollingPct = BigDecimal.ZERO;

    /** 롤링 보류 일수 (120, 180 등) */
    @Column(name = "rolling_days")
    private Integer rollingDays = 0;

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
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public BigDecimal getPerTxFee() { return perTxFee; }
    public void setPerTxFee(BigDecimal perTxFee) { this.perTxFee = perTxFee; }
    public BigDecimal getCancelRate() { return cancelRate; }
    public void setCancelRate(BigDecimal cancelRate) { this.cancelRate = cancelRate; }
    public BigDecimal getUsageRate() { return usageRate; }
    public void setUsageRate(BigDecimal usageRate) { this.usageRate = usageRate; }
    public BigDecimal getFailFee() { return failFee; }
    public void setFailFee(BigDecimal failFee) { this.failFee = failFee; }
    public BigDecimal getPayRate() { return payRate; }
    public void setPayRate(BigDecimal payRate) { this.payRate = payRate; }
    public BigDecimal getRefundRate() { return refundRate; }
    public void setRefundRate(BigDecimal refundRate) { this.refundRate = refundRate; }
    public BigDecimal getRollingPct() { return rollingPct; }
    public void setRollingPct(BigDecimal rollingPct) { this.rollingPct = rollingPct; }
    public Integer getRollingDays() { return rollingDays; }
    public void setRollingDays(Integer rollingDays) { this.rollingDays = rollingDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
