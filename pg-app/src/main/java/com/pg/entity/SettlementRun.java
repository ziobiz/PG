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

    /**
     * 실행 시점 가맹 정산주기 스냅샷(정규화 코드). 가맹 설정 변경 후에도 과거 실행 행 표시에 사용.
     * null이면 구버전 데이터(당시 주기 불명)로, 화면에서는 현재 설정으로 덮어쓰지 않음.
     */
    @Column(name = "calc_cycle_snapshot", length = 64)
    private String calcCycleSnapshot;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    /** 정산대상 승인합계 */
    @Column(name = "approve_amt", precision = 21, scale = 8)
    private BigDecimal approveAmt = BigDecimal.ZERO;

    /** 취소합계 */
    @Column(name = "cancel_amt", precision = 21, scale = 8)
    private BigDecimal cancelAmt = BigDecimal.ZERO;

    /**
     * 거래 집계 기반 공제 수수료 합계(건당·결제%·취소·무효·환불·실패·차지백·USDT/FX·3DS·기타 등).
     * 정산 실행당 1회 정산수수료·송금수수료는 {@link #settlementBatchFeeAmt}, {@link #remittanceFeeAmt} 에 별도 저장합니다.
     */
    @Column(name = "total_fee", precision = 21, scale = 8)
    private BigDecimal totalFee = BigDecimal.ZERO;

    /**
     * 이번 정산 실행에 집계에 포함된 거래({@code pg_trnsctn}) 건수.
     * 구버전 행은 null일 수 있습니다.
     */
    @Column(name = "included_txn_cnt")
    private Integer includedTxnCnt;

    /** 정산 실행당 1회 정산수수료(정책 {@code fee_settlement_per_tx} 금액). 스키마 이전 행은 null. */
    @Column(name = "settlement_batch_fee_amt", precision = 21, scale = 8)
    private BigDecimal settlementBatchFeeAmt;

    /** 정산 실행당 1회 송금 이체 수수료. 스키마 이전 행은 null. */
    @Column(name = "remittance_fee_amt", precision = 21, scale = 8)
    private BigDecimal remittanceFeeAmt;

    /** 롤링(담보금) 보류액 */
    @Column(name = "rolling_reserve_amt", precision = 21, scale = 8)
    private BigDecimal rollingReserveAmt = BigDecimal.ZERO;

    /**
     * 지급액(정산 직후·환수/미수금 차감 전 원칙): 순매출 − 거래수수료({@link #totalFee}) − 정산수수료(1회)
     * − 수수료부가세(위 합에 대한 부가세) − 담보금(롤링보류 신규) + 만기 담보 환급.
     * 송금 이체 수수료는 지급액·부가세 계산에 포함하지 않으며 {@link #remittanceFeeAmt} 는 0으로 둡니다.
     * 수수료·담보가 순매출을 초과하면 음수가 될 수 있으며 0으로 끌어올리지 않습니다.
     * 그 경우 {@link com.pg.service.settlement.SettlementArrearsService} 가 동액을 미수금으로 등록하고,
     * 이후 정산에서는 환수금 FIFO 후 미수금 FIFO로 차감합니다(가맹 환수모드 MANUAL이면 「환수처리」 후 차기 마감에서 차감).
     */
    @Column(name = "pay_amt", precision = 21, scale = 8)
    private BigDecimal payAmt = BigDecimal.ZERO;

    /**
     * 유통 단계별 분배 스냅샷(최종 지급액 × {@code tb_distribution_fee_config} 요율 %).
     * {@link com.pg.service.settlement.SettlementArrearsService#applyArrearsToSettledRun} 확정 시 기록되며,
     * 유통망정산 집계 시 재계산 대신 이 값을 우선 사용합니다(구데이터는 null → 조회 시 계산).
     */
    @Column(name = "dist_hq_fee_amt", precision = 21, scale = 8)
    private BigDecimal distHqFeeAmt;
    @Column(name = "dist_regional_fee_amt", precision = 21, scale = 8)
    private BigDecimal distRegionalFeeAmt;
    @Column(name = "dist_master_fee_amt", precision = 21, scale = 8)
    private BigDecimal distMasterFeeAmt;
    @Column(name = "dist_branch_fee_amt", precision = 21, scale = 8)
    private BigDecimal distBranchFeeAmt;
    @Column(name = "dist_agency_fee_amt", precision = 21, scale = 8)
    private BigDecimal distAgencyFeeAmt;
    @Column(name = "dist_sales_office_fee_amt", precision = 21, scale = 8)
    private BigDecimal distSalesOfficeFeeAmt;

    /** 해당 정산 실행에서 미수금 FIFO로 차감된 합계(가맹점정산내역 표시) */
    @Column(name = "receivable_applied_amt", precision = 21, scale = 8)
    private BigDecimal receivableAppliedAmt;

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

    /**
     * 정산결과(배포 게이트): PENDING=가맹점정산내역 미반영, DISTRIBUTED=반영(가맹·유통·확정리포트 대상),
     * HOLD=정산대기(가맹 내역 제외).
     */
    @Column(name = "settlement_publish_sts", nullable = false, length = 20)
    private String settlementPublishSts = "PENDING";

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

    public String getCalcCycleSnapshot() { return calcCycleSnapshot; }
    public void setCalcCycleSnapshot(String calcCycleSnapshot) { this.calcCycleSnapshot = calcCycleSnapshot; }

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
    public Integer getIncludedTxnCnt() { return includedTxnCnt; }
    public void setIncludedTxnCnt(Integer includedTxnCnt) { this.includedTxnCnt = includedTxnCnt; }
    public BigDecimal getSettlementBatchFeeAmt() { return settlementBatchFeeAmt; }
    public void setSettlementBatchFeeAmt(BigDecimal settlementBatchFeeAmt) { this.settlementBatchFeeAmt = settlementBatchFeeAmt; }
    public BigDecimal getRemittanceFeeAmt() { return remittanceFeeAmt; }
    public void setRemittanceFeeAmt(BigDecimal remittanceFeeAmt) { this.remittanceFeeAmt = remittanceFeeAmt; }
    public BigDecimal getRollingReserveAmt() { return rollingReserveAmt; }
    public void setRollingReserveAmt(BigDecimal rollingReserveAmt) { this.rollingReserveAmt = rollingReserveAmt; }
    public BigDecimal getPayAmt() { return payAmt; }
    public void setPayAmt(BigDecimal payAmt) { this.payAmt = payAmt; }
    public BigDecimal getDistHqFeeAmt() { return distHqFeeAmt; }
    public void setDistHqFeeAmt(BigDecimal distHqFeeAmt) { this.distHqFeeAmt = distHqFeeAmt; }
    public BigDecimal getDistRegionalFeeAmt() { return distRegionalFeeAmt; }
    public void setDistRegionalFeeAmt(BigDecimal distRegionalFeeAmt) { this.distRegionalFeeAmt = distRegionalFeeAmt; }
    public BigDecimal getDistMasterFeeAmt() { return distMasterFeeAmt; }
    public void setDistMasterFeeAmt(BigDecimal distMasterFeeAmt) { this.distMasterFeeAmt = distMasterFeeAmt; }
    public BigDecimal getDistBranchFeeAmt() { return distBranchFeeAmt; }
    public void setDistBranchFeeAmt(BigDecimal distBranchFeeAmt) { this.distBranchFeeAmt = distBranchFeeAmt; }
    public BigDecimal getDistAgencyFeeAmt() { return distAgencyFeeAmt; }
    public void setDistAgencyFeeAmt(BigDecimal distAgencyFeeAmt) { this.distAgencyFeeAmt = distAgencyFeeAmt; }
    public BigDecimal getDistSalesOfficeFeeAmt() { return distSalesOfficeFeeAmt; }
    public void setDistSalesOfficeFeeAmt(BigDecimal distSalesOfficeFeeAmt) { this.distSalesOfficeFeeAmt = distSalesOfficeFeeAmt; }
    public BigDecimal getReceivableAppliedAmt() { return receivableAppliedAmt; }
    public void setReceivableAppliedAmt(BigDecimal receivableAppliedAmt) { this.receivableAppliedAmt = receivableAppliedAmt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPayoutHoldYn() { return payoutHoldYn; }
    public void setPayoutHoldYn(String payoutHoldYn) { this.payoutHoldYn = payoutHoldYn; }
    public String getPayoutHoldRemark() { return payoutHoldRemark; }
    public void setPayoutHoldRemark(String payoutHoldRemark) { this.payoutHoldRemark = payoutHoldRemark; }
    public String getSettlementPublishSts() { return settlementPublishSts; }
    public void setSettlementPublishSts(String settlementPublishSts) {
        this.settlementPublishSts = settlementPublishSts != null ? settlementPublishSts : "PENDING";
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
