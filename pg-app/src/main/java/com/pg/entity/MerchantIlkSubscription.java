package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_merchant_ilk_subscription",
        uniqueConstraints = @UniqueConstraint(name = "uq_ilk_sub_comp_no",
                columnNames = {"comp_id", "subscription_no"}))
public class MerchantIlkSubscription {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "comp_id", nullable = false, length = 64)
    private String compId;

    @Column(name = "subscription_no", nullable = false, length = 64)
    private String subscriptionNo;

    @Column(name = "plan_json", columnDefinition = "TEXT")
    private String planJson;

    @Column(name = "status", nullable = false, length = 24)
    private String status = STATUS_PENDING;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "amount", precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "first_order_no", length = 64)
    private String firstOrderNo;

    @Column(name = "first_auth_id", length = 64)
    private String firstAuthId;

    @Column(name = "card_brand", length = 16)
    private String cardBrand;

    @Column(name = "card_last4", length = 8)
    private String cardLast4;

    /** Encrypted PAN for MIT (AES via agency seed) — optional; prefer tokenized refill from notify. */
    @Column(name = "card_token_enc", length = 512)
    private String cardTokenEnc;

    @Column(name = "card_exp_month_enc", length = 128)
    private String cardExpMonthEnc;

    @Column(name = "card_exp_year_enc", length = 128)
    private String cardExpYearEnc;

    @Column(name = "next_charge_at")
    private LocalDateTime nextChargeAt;

    @Column(name = "last_charge_at")
    private LocalDateTime lastChargeAt;

    @Column(name = "charge_count", nullable = false)
    private Integer chargeCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (chargeCount == null) {
            chargeCount = 0;
        }
        if (status == null || status.isBlank()) {
            status = STATUS_PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public String getSubscriptionNo() { return subscriptionNo; }
    public void setSubscriptionNo(String subscriptionNo) { this.subscriptionNo = subscriptionNo; }
    public String getPlanJson() { return planJson; }
    public void setPlanJson(String planJson) { this.planJson = planJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getFirstOrderNo() { return firstOrderNo; }
    public void setFirstOrderNo(String firstOrderNo) { this.firstOrderNo = firstOrderNo; }
    public String getFirstAuthId() { return firstAuthId; }
    public void setFirstAuthId(String firstAuthId) { this.firstAuthId = firstAuthId; }
    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
    public String getCardTokenEnc() { return cardTokenEnc; }
    public void setCardTokenEnc(String cardTokenEnc) { this.cardTokenEnc = cardTokenEnc; }
    public String getCardExpMonthEnc() { return cardExpMonthEnc; }
    public void setCardExpMonthEnc(String cardExpMonthEnc) { this.cardExpMonthEnc = cardExpMonthEnc; }
    public String getCardExpYearEnc() { return cardExpYearEnc; }
    public void setCardExpYearEnc(String cardExpYearEnc) { this.cardExpYearEnc = cardExpYearEnc; }
    public LocalDateTime getNextChargeAt() { return nextChargeAt; }
    public void setNextChargeAt(LocalDateTime nextChargeAt) { this.nextChargeAt = nextChargeAt; }
    public LocalDateTime getLastChargeAt() { return lastChargeAt; }
    public void setLastChargeAt(LocalDateTime lastChargeAt) { this.lastChargeAt = lastChargeAt; }
    public Integer getChargeCount() { return chargeCount; }
    public void setChargeCount(Integer chargeCount) { this.chargeCount = chargeCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
