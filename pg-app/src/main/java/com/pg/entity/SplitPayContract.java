package com.pg.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_split_pay_contract")
public class SplitPayContract {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_STOPPED = "STOPPED";

    public static final String INTERVAL_MONTH = "MONTH";
    public static final String INTERVAL_DAY = "DAY";
    public static final String INTERVAL_MULTI = "MULTI";

    public static final String FIRST_IMMEDIATE = "IMMEDIATE";
    public static final String FIRST_LINK = "LINK";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_no", nullable = false, length = 64, unique = true)
    private String contractNo;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "merchant_code", nullable = false, length = 64)
    private String merchantCode;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_locale", length = 8)
    private String customerLocale = "KOR";

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "JPY";

    @Column(name = "installment_count", nullable = false)
    private Integer installmentCount;

    @Column(name = "interval_type", nullable = false, length = 16)
    private String intervalType;

    @Column(name = "interval_value", nullable = false)
    private Integer intervalValue = 1;

    @Column(name = "status", nullable = false, length = 16)
    private String status = STATUS_ACTIVE;

    @Column(name = "first_pay_mode", nullable = false, length = 16)
    private String firstPayMode = FIRST_IMMEDIATE;

    @Column(name = "snap_split_pay_fee_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal snapSplitPayFeePct = BigDecimal.ZERO;

    @Column(name = "snap_split_fixed_per_inst", nullable = false, precision = 12, scale = 1)
    private BigDecimal snapSplitFixedPerInst = BigDecimal.ZERO;

    @Column(name = "snap_split_fixed_total", nullable = false, precision = 12, scale = 1)
    private BigDecimal snapSplitFixedTotal = BigDecimal.ZERO;

    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel = "URL";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

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
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerLocale() { return customerLocale; }
    public void setCustomerLocale(String customerLocale) { this.customerLocale = customerLocale; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    public String getIntervalType() { return intervalType; }
    public void setIntervalType(String intervalType) { this.intervalType = intervalType; }
    public Integer getIntervalValue() { return intervalValue; }
    public void setIntervalValue(Integer intervalValue) { this.intervalValue = intervalValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFirstPayMode() { return firstPayMode; }
    public void setFirstPayMode(String firstPayMode) { this.firstPayMode = firstPayMode; }
    public BigDecimal getSnapSplitPayFeePct() { return snapSplitPayFeePct; }
    public void setSnapSplitPayFeePct(BigDecimal snapSplitPayFeePct) { this.snapSplitPayFeePct = snapSplitPayFeePct; }
    public BigDecimal getSnapSplitFixedPerInst() { return snapSplitFixedPerInst; }
    public void setSnapSplitFixedPerInst(BigDecimal snapSplitFixedPerInst) { this.snapSplitFixedPerInst = snapSplitFixedPerInst; }
    public BigDecimal getSnapSplitFixedTotal() { return snapSplitFixedTotal; }
    public void setSnapSplitFixedTotal(BigDecimal snapSplitFixedTotal) { this.snapSplitFixedTotal = snapSplitFixedTotal; }
    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
}
