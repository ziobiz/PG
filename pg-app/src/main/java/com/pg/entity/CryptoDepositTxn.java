package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_crypto_deposit_txn",
        uniqueConstraints = @UniqueConstraint(columnNames = {"org_unit_id", "order_no"}))
public class CryptoDepositTxn {

    public static final String ST_PENDING = "PENDING";
    public static final String ST_AWAITING = "AWAITING_DEPOSIT";
    public static final String ST_CONFIRMED = "CONFIRMED";
    public static final String ST_EXPIRED = "EXPIRED";
    public static final String ST_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "comp_id", nullable = false, length = 32)
    private String compId;

    @Column(name = "order_no", nullable = false, length = 100)
    private String orderNo;

    @Column(name = "amount", nullable = false, precision = 24, scale = 8)
    private BigDecimal amount;

    @Column(name = "asset_code", nullable = false, length = 16)
    private String assetCode;

    @Column(name = "network_code", nullable = false, length = 16)
    private String networkCode;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "buyer_email", length = 200)
    private String buyerEmail;

    @Column(name = "buyer_name", length = 100)
    private String buyerName;

    @Column(name = "custody_address", nullable = false, length = 128)
    private String custodyAddress;

    @Column(name = "session_token", nullable = false, length = 128)
    private String sessionToken;

    @Column(name = "status", nullable = false, length = 24)
    private String status = ST_PENDING;

    @Column(name = "settle_mode", nullable = false, length = 16)
    private String settleMode = MerchantCryptoConfig.SETTLE_SCHEDULED;

    @Column(name = "payout_mode", nullable = false, length = 16)
    private String payoutMode = MerchantCryptoConfig.PAYOUT_MANUAL;

    @Column(name = "deposit_tx_hash", length = 128)
    private String depositTxHash;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = ST_PENDING;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }
    public String getNetworkCode() { return networkCode; }
    public void setNetworkCode(String networkCode) { this.networkCode = networkCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getCustodyAddress() { return custodyAddress; }
    public void setCustodyAddress(String custodyAddress) { this.custodyAddress = custodyAddress; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSettleMode() { return settleMode; }
    public void setSettleMode(String settleMode) { this.settleMode = settleMode; }
    public String getPayoutMode() { return payoutMode; }
    public void setPayoutMode(String payoutMode) { this.payoutMode = payoutMode; }
    public String getDepositTxHash() { return depositTxHash; }
    public void setDepositTxHash(String depositTxHash) { this.depositTxHash = depositTxHash; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
