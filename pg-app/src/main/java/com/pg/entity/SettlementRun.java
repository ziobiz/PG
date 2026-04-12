package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    /** 집계 구간 시작일(정산 실행·자동 배치가 사용한 거래 created_at 하한의 날짜) */
    @Column(name = "period_from")
    private LocalDate periodFrom;

    /** 집계 구간 종료일(상한 날짜; 시각은 period_end_at 또는 해당일 23:59:59.999999999) */
    @Column(name = "period_to")
    private LocalDate periodTo;

    /** 당일 누적(RT·분·시) 등: 거래 상한 시각. null이면 period_to 일 끝까지 */
    @Column(name = "period_end_at")
    private LocalDateTime periodEndAt;

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

    /**
     * 지급보류(Y) 가맹점의 정산 실행 건을 가맹점정산내역에서 숨기고 정산보류내역에만 둘 때 Y.
     * 해제 시 N으로 바뀌며 동일 행이 가맹점정산내역에 표시됩니다.
     */
    @Column(name = "payout_hold_yn", nullable = false, length = 1)
    private String payoutHoldYn = "N";

    @Column(name = "payout_hold_remark", length = 800)
    private String payoutHoldRemark;

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
    public LocalDate getPeriodFrom() { return periodFrom; }
    public void setPeriodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public void setPeriodTo(LocalDate periodTo) { this.periodTo = periodTo; }
    public LocalDateTime getPeriodEndAt() { return periodEndAt; }
    public void setPeriodEndAt(LocalDateTime periodEndAt) { this.periodEndAt = periodEndAt; }

    /** 거래 조회용 하한 시각(구버전 행은 정산일 0시) */
    public LocalDateTime resolvePeriodStartAt() {
        LocalDate d = periodFrom != null ? periodFrom : calcDt;
        if (d == null) {
            return LocalDate.now().atStartOfDay();
        }
        return d.atStartOfDay();
    }

    /** 거래 조회용 상한 시각(구버전은 정산일 말일 끝) */
    public LocalDateTime resolvePeriodEndAt() {
        if (periodEndAt != null) {
            return periodEndAt;
        }
        LocalDate d = periodTo != null ? periodTo : calcDt;
        if (d == null) {
            return LocalDate.now().atTime(LocalTime.MAX);
        }
        return d.atTime(LocalTime.MAX);
    }

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
    public String getPayoutHoldYn() { return payoutHoldYn; }
    public void setPayoutHoldYn(String payoutHoldYn) { this.payoutHoldYn = payoutHoldYn; }
    public String getPayoutHoldRemark() { return payoutHoldRemark; }
    public void setPayoutHoldRemark(String payoutHoldRemark) { this.payoutHoldRemark = payoutHoldRemark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
