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

    /** 청구·수수료 앵커 금액(USD). */
    @Column(name = "amount", nullable = false, precision = 24, scale = 8)
    private BigDecimal amount;

    /** 구매자 표시 금액(표시통화). */
    @Column(name = "display_amount", precision = 24, scale = 8)
    private BigDecimal displayAmount;

    @Column(name = "display_currency", length = 8)
    private String displayCurrency;

    /** 선택 자산 송금 금액(USDC/USDT/EURC). */
    @Column(name = "asset_amount", precision = 24, scale = 8)
    private BigDecimal assetAmount;

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

    /** 국가번호(ISO2)와 분리된 로컬 전화번호 */
    @Column(name = "buyer_phone", length = 40)
    private String buyerPhone;

    @Column(name = "buyer_country_iso2", length = 2)
    private String buyerCountryIso2;

    /** 배송주소 JSON(선택) — zip/city/address 등 */
    @Column(name = "buyer_shipping_json", length = 1000)
    private String buyerShippingJson;

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

    /** 확정 입금 시 산정·과금된 CRYPTO 수수료 합계(견적 통화·자산 액면) */
    @Column(name = "fee_amt", precision = 24, scale = 8)
    private BigDecimal feeAmt;

    /** Y=확정 입금 시 수수료 과금 완료 */
    @Column(name = "fee_charged_yn", length = 1)
    private String feeChargedYn = "N";

    /** 결제내역 연동 pg_trnsctn.trn_id */
    @Column(name = "pay_trn_id", length = 20)
    private String payTrnId;

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
    public BigDecimal getDisplayAmount() { return displayAmount; }
    public void setDisplayAmount(BigDecimal displayAmount) { this.displayAmount = displayAmount; }
    public String getDisplayCurrency() { return displayCurrency; }
    public void setDisplayCurrency(String displayCurrency) { this.displayCurrency = displayCurrency; }
    public BigDecimal getAssetAmount() { return assetAmount; }
    public void setAssetAmount(BigDecimal assetAmount) { this.assetAmount = assetAmount; }
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
    public String getBuyerPhone() { return buyerPhone; }
    public void setBuyerPhone(String buyerPhone) { this.buyerPhone = buyerPhone; }
    public String getBuyerCountryIso2() { return buyerCountryIso2; }
    public void setBuyerCountryIso2(String buyerCountryIso2) { this.buyerCountryIso2 = buyerCountryIso2; }
    public String getBuyerShippingJson() { return buyerShippingJson; }
    public void setBuyerShippingJson(String buyerShippingJson) { this.buyerShippingJson = buyerShippingJson; }
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
    public BigDecimal getFeeAmt() { return feeAmt; }
    public void setFeeAmt(BigDecimal feeAmt) { this.feeAmt = feeAmt; }
    public String getFeeChargedYn() { return feeChargedYn; }
    public void setFeeChargedYn(String feeChargedYn) { this.feeChargedYn = feeChargedYn; }
    public String getPayTrnId() { return payTrnId; }
    public void setPayTrnId(String payTrnId) { this.payTrnId = payTrnId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

