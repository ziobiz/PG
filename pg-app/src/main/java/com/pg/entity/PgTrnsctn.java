package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 거래 마스터 (PG_TRNSCTN 스타일) - 목록/조회용 핵심 컬럼
 */
@Entity
@Table(name = "pg_trnsctn")
public class PgTrnsctn {

    @Id
    @Column(name = "trn_id", length = 20)
    private String trnId;

    @Column(name = "merchant_id", nullable = false, length = 20)
    private String merchantId;

    @Column(name = "service_type", length = 20)
    private String serviceType;

    @Column(name = "status", length = 2)
    private String status;

    @Column(name = "cur_type", length = 3)
    private String curType = "KRW";

    @Column(name = "amt_krw", precision = 15, scale = 0)
    private BigDecimal amtKrw;

    @Column(name = "pay_no", length = 50)
    private String payNo;

    @Column(name = "approval_no", length = 20)
    private String approvalNo;

    @Column(name = "van", length = 10)
    private String van;

    /** CHILL(또는 null) API동기화, NOTI 노티적재, URL URL직접결제 */
    @Column(name = "origin", length = 20)
    private String origin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** ChillPay OrderNo */
    @Column(name = "order_no", length = 64)
    private String orderNo;

    /** ChillPay Customer (식별자) */
    @Column(name = "customer_id", length = 100)
    private String customerId;

    /** ChillPay Customer 표시명 */
    @Column(name = "customer_nm", length = 200)
    private String customerNm;

    /** ChillPay Payment Channel (channelCode 등) */
    @Column(name = "payment_channel", length = 80)
    private String paymentChannel;

    /** ChillPay 결제 완료 시각 (거래 적재 시각과 별도일 수 있음) */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** ChillPay/정산 시트 ICOPAY 금액 */
    @Column(name = "icopay_amt", precision = 15, scale = 0)
    private BigDecimal icopayAmt;

    /** ChillPay Fee (PG/대행 수수료 등 원문 금액) */
    @Column(name = "chill_fee_amt", precision = 15, scale = 0)
    private BigDecimal chillFeeAmt;

    /** ChillPay TotalAmount */
    @Column(name = "total_amt", precision = 15, scale = 0)
    private BigDecimal totalAmt;

    /** ChillPay RouteNo */
    @Column(name = "route_no", length = 32)
    private String routeNo;

    /** ChillPay 결제 상태 원문 (Paid, WaitAuthorize 등) */
    @Column(name = "chill_payment_status", length = 50)
    private String chillPaymentStatus;

    /** 정산 완료 여부 Y/N */
    @Column(name = "settled_yn", length = 1)
    private String settledYn = "N";

    /**
     * 칠페이(ChillPay) TransactionId — PG가 부여한 {@link #trnId}와 별도의 고유값.
     * API 응답·노티의 transactionId 등.
     */
    @Column(name = "chill_transaction_id", length = 64)
    private String chillTransactionId;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public String getTrnId() { return trnId; }
    public void setTrnId(String trnId) { this.trnId = trnId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurType() { return curType; }
    public void setCurType(String curType) { this.curType = curType; }
    public BigDecimal getAmtKrw() { return amtKrw; }
    public void setAmtKrw(BigDecimal amtKrw) { this.amtKrw = amtKrw; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public String getApprovalNo() { return approvalNo; }
    public void setApprovalNo(String approvalNo) { this.approvalNo = approvalNo; }
    public String getVan() { return van; }
    public void setVan(String van) { this.van = van; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerNm() { return customerNm; }
    public void setCustomerNm(String customerNm) { this.customerNm = customerNm; }
    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public BigDecimal getIcopayAmt() { return icopayAmt; }
    public void setIcopayAmt(BigDecimal icopayAmt) { this.icopayAmt = icopayAmt; }
    public BigDecimal getChillFeeAmt() { return chillFeeAmt; }
    public void setChillFeeAmt(BigDecimal chillFeeAmt) { this.chillFeeAmt = chillFeeAmt; }
    public BigDecimal getTotalAmt() { return totalAmt; }
    public void setTotalAmt(BigDecimal totalAmt) { this.totalAmt = totalAmt; }
    public String getRouteNo() { return routeNo; }
    public void setRouteNo(String routeNo) { this.routeNo = routeNo; }
    public String getChillPaymentStatus() { return chillPaymentStatus; }
    public void setChillPaymentStatus(String chillPaymentStatus) { this.chillPaymentStatus = chillPaymentStatus; }
    public String getSettledYn() { return settledYn; }
    public void setSettledYn(String settledYn) { this.settledYn = settledYn; }
    public String getChillTransactionId() { return chillTransactionId; }
    public void setChillTransactionId(String chillTransactionId) { this.chillTransactionId = chillTransactionId; }
}
