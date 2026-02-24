package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정산 실행 결과 (정산 주기별 가맹점 지급액)
 */
@Entity
@Table(name = "tb_settlement_run")
public class SettlementRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calc_dt", nullable = false)
    private LocalDate calcDt;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    /** 정산대상 승인합계 */
    @Column(name = "approve_amt", precision = 15, scale = 0)
    private BigDecimal approveAmt = BigDecimal.ZERO;

    /** 취소합계 */
    @Column(name = "cancel_amt", precision = 15, scale = 0)
    private BigDecimal cancelAmt = BigDecimal.ZERO;

    /** 공제 수수료 합계 (건당+결제수수료+취소수수료+실패+환불 등) */
    @Column(name = "total_fee", precision = 15, scale = 0)
    private BigDecimal totalFee = BigDecimal.ZERO;

    /** 롤링(담보금) 보류액 */
    @Column(name = "rolling_reserve_amt", precision = 15, scale = 0)
    private BigDecimal rollingReserveAmt = BigDecimal.ZERO;

    /** 지급액 = 승인 - 취소 - 수수료 - 롤링보류 */
    @Column(name = "pay_amt", precision = 15, scale = 0)
    private BigDecimal payAmt = BigDecimal.ZERO;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getCalcDt() { return calcDt; }
    public void setCalcDt(LocalDate calcDt) { this.calcDt = calcDt; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public BigDecimal getApproveAmt() { return approveAmt; }
    public void setApproveAmt(BigDecimal approveAmt) { this.approveAmt = approveAmt; }
    public BigDecimal getCancelAmt() { return cancelAmt; }
    public void setCancelAmt(BigDecimal cancelAmt) { this.cancelAmt = cancelAmt; }
    public BigDecimal getTotalFee() { return totalFee; }
    public void setTotalFee(BigDecimal totalFee) { this.totalFee = totalFee; }
    public BigDecimal getRollingReserveAmt() { return rollingReserveAmt; }
    public void setRollingReserveAmt(BigDecimal rollingReserveAmt) { this.rollingReserveAmt = rollingReserveAmt; }
    public BigDecimal getPayAmt() { return payAmt; }
    public void setPayAmt(BigDecimal payAmt) { this.payAmt = payAmt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
