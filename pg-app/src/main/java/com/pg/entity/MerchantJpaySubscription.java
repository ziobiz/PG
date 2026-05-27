package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_merchant_jpay_subscription",
        uniqueConstraints = @UniqueConstraint(name = "uk_merchant_jpay_sub_order",
                columnNames = {"comp_code", "checkout_order_no"}))
public class MerchantJpaySubscription {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "comp_code", nullable = false, length = 32)
    private String compCode;

    @Column(name = "checkout_order_no", nullable = false, length = 64)
    private String checkoutOrderNo;

    @Column(name = "pg_cd", nullable = false, length = 20)
    private String pgCd;

    @Column(name = "subscription_plan_json", nullable = false, columnDefinition = "TEXT")
    private String subscriptionPlanJson;

    @Column(name = "payment_transaction_id", length = 64)
    private String paymentTransactionId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = STATUS_PENDING;

    @Column(name = "period_count", nullable = false)
    private Integer periodCount = 0;

    @Column(name = "last_notify_at")
    private LocalDateTime lastNotifyAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompCode() { return compCode; }
    public void setCompCode(String compCode) { this.compCode = compCode; }
    public String getCheckoutOrderNo() { return checkoutOrderNo; }
    public void setCheckoutOrderNo(String checkoutOrderNo) { this.checkoutOrderNo = checkoutOrderNo; }
    public String getPgCd() { return pgCd; }
    public void setPgCd(String pgCd) { this.pgCd = pgCd; }
    public String getSubscriptionPlanJson() { return subscriptionPlanJson; }
    public void setSubscriptionPlanJson(String subscriptionPlanJson) { this.subscriptionPlanJson = subscriptionPlanJson; }
    public String getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(String paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPeriodCount() { return periodCount; }
    public void setPeriodCount(Integer periodCount) { this.periodCount = periodCount; }
    public LocalDateTime getLastNotifyAt() { return lastNotifyAt; }
    public void setLastNotifyAt(LocalDateTime lastNotifyAt) { this.lastNotifyAt = lastNotifyAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
