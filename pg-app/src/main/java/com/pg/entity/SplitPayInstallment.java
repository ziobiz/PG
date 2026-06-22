package com.pg.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_split_pay_installment")
public class SplitPayInstallment {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "installment_no", nullable = false)
    private Integer installmentNo;

    @Column(name = "order_no", nullable = false, length = 64, unique = true)
    private String orderNo;

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "due_date_adjusted", nullable = false)
    private LocalDate dueDateAdjusted;

    @Column(name = "status", nullable = false, length = 16)
    private String status = STATUS_PENDING;

    @Column(name = "pay_token", nullable = false, length = 64, unique = true)
    private String payToken;

    @Column(name = "pg_trn_id", length = 32)
    private String pgTrnId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "fee_pct_amount", precision = 12, scale = 4)
    private BigDecimal feePctAmount;

    @Column(name = "fee_fixed_amount", precision = 12, scale = 4)
    private BigDecimal feeFixedAmount;

    @Column(name = "mail_d_minus1_sent")
    private LocalDateTime mailDMinus1Sent;

    @Column(name = "mail_d0_sent")
    private LocalDateTime mailD0Sent;

    @Column(name = "mail_d1_sent")
    private LocalDateTime mailD1Sent;

    @Column(name = "mail_d2_sent")
    private LocalDateTime mailD2Sent;

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
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public Integer getInstallmentNo() { return installmentNo; }
    public void setInstallmentNo(Integer installmentNo) { this.installmentNo = installmentNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public LocalDate getDueDateAdjusted() { return dueDateAdjusted; }
    public void setDueDateAdjusted(LocalDate dueDateAdjusted) { this.dueDateAdjusted = dueDateAdjusted; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPayToken() { return payToken; }
    public void setPayToken(String payToken) { this.payToken = payToken; }
    public String getPgTrnId() { return pgTrnId; }
    public void setPgTrnId(String pgTrnId) { this.pgTrnId = pgTrnId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public BigDecimal getFeePctAmount() { return feePctAmount; }
    public void setFeePctAmount(BigDecimal feePctAmount) { this.feePctAmount = feePctAmount; }
    public BigDecimal getFeeFixedAmount() { return feeFixedAmount; }
    public void setFeeFixedAmount(BigDecimal feeFixedAmount) { this.feeFixedAmount = feeFixedAmount; }
    public LocalDateTime getMailDMinus1Sent() { return mailDMinus1Sent; }
    public void setMailDMinus1Sent(LocalDateTime mailDMinus1Sent) { this.mailDMinus1Sent = mailDMinus1Sent; }
    public LocalDateTime getMailD0Sent() { return mailD0Sent; }
    public void setMailD0Sent(LocalDateTime mailD0Sent) { this.mailD0Sent = mailD0Sent; }
    public LocalDateTime getMailD1Sent() { return mailD1Sent; }
    public void setMailD1Sent(LocalDateTime mailD1Sent) { this.mailD1Sent = mailD1Sent; }
    public LocalDateTime getMailD2Sent() { return mailD2Sent; }
    public void setMailD2Sent(LocalDateTime mailD2Sent) { this.mailD2Sent = mailD2Sent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
