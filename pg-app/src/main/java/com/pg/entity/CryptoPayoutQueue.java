package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_crypto_payout_queue")
public class CryptoPayoutQueue {

    public static final String ST_QUEUED = "QUEUED";
    public static final String ST_HOLD = "HOLD";
    public static final String ST_READY = "READY";
    public static final String ST_PROCESSING = "PROCESSING";
    public static final String ST_SENT = "SENT";
    public static final String ST_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deposit_txn_id", nullable = false)
    private Long depositTxnId;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "comp_id", nullable = false, length = 32)
    private String compId;

    @Column(name = "order_no", nullable = false, length = 100)
    private String orderNo;

    @Column(name = "asset_code", nullable = false, length = 16)
    private String assetCode;

    @Column(name = "network_code", nullable = false, length = 16)
    private String networkCode;

    @Column(name = "amount_gross", nullable = false, precision = 24, scale = 8)
    private BigDecimal amountGross;

    @Column(name = "amount_fee", nullable = false, precision = 24, scale = 8)
    private BigDecimal amountFee = BigDecimal.ZERO;

    /** 네트워크 가스피(전 가맹 동일 마스터) — 조직% 배분과 별개 */
    @Column(name = "amount_gas", nullable = false, precision = 24, scale = 8)
    private BigDecimal amountGas = BigDecimal.ZERO;

    /** 정산(송금) 부가 수수료 — 조직% 배분과 별개 */
    @Column(name = "amount_settlement_fee", nullable = false, precision = 24, scale = 8)
    private BigDecimal amountSettlementFee = BigDecimal.ZERO;

    @Column(name = "amount_reserve", nullable = false, precision = 24, scale = 8)
    private BigDecimal amountReserve = BigDecimal.ZERO;

    @Column(name = "amount_net", nullable = false, precision = 24, scale = 8)
    private BigDecimal amountNet;

    @Column(name = "settle_mode", nullable = false, length = 16)
    private String settleMode;

    @Column(name = "payout_mode", nullable = false, length = 16)
    private String payoutMode;

    @Column(name = "status", nullable = false, length = 24)
    private String status = ST_QUEUED;

    @Column(name = "seller_address", length = 128)
    private String sellerAddress;

    @Column(name = "payout_tx_hash", length = 128)
    private String payoutTxHash;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = ST_QUEUED;
        }
        if (amountFee == null) {
            amountFee = BigDecimal.ZERO;
        }
        if (amountGas == null) {
            amountGas = BigDecimal.ZERO;
        }
        if (amountSettlementFee == null) {
            amountSettlementFee = BigDecimal.ZERO;
        }
        if (amountReserve == null) {
            amountReserve = BigDecimal.ZERO;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDepositTxnId() { return depositTxnId; }
    public void setDepositTxnId(Long depositTxnId) { this.depositTxnId = depositTxnId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }
    public String getNetworkCode() { return networkCode; }
    public void setNetworkCode(String networkCode) { this.networkCode = networkCode; }
    public BigDecimal getAmountGross() { return amountGross; }
    public void setAmountGross(BigDecimal amountGross) { this.amountGross = amountGross; }
    public BigDecimal getAmountFee() { return amountFee; }
    public void setAmountFee(BigDecimal amountFee) { this.amountFee = amountFee; }
    public BigDecimal getAmountGas() { return amountGas; }
    public void setAmountGas(BigDecimal amountGas) { this.amountGas = amountGas; }
    public BigDecimal getAmountSettlementFee() { return amountSettlementFee; }
    public void setAmountSettlementFee(BigDecimal amountSettlementFee) { this.amountSettlementFee = amountSettlementFee; }
    public BigDecimal getAmountReserve() { return amountReserve; }
    public void setAmountReserve(BigDecimal amountReserve) { this.amountReserve = amountReserve; }
    public BigDecimal getAmountNet() { return amountNet; }
    public void setAmountNet(BigDecimal amountNet) { this.amountNet = amountNet; }
    public String getSettleMode() { return settleMode; }
    public void setSettleMode(String settleMode) { this.settleMode = settleMode; }
    public String getPayoutMode() { return payoutMode; }
    public void setPayoutMode(String payoutMode) { this.payoutMode = payoutMode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSellerAddress() { return sellerAddress; }
    public void setSellerAddress(String sellerAddress) { this.sellerAddress = sellerAddress; }
    public String getPayoutTxHash() { return payoutTxHash; }
    public void setPayoutTxHash(String payoutTxHash) { this.payoutTxHash = payoutTxHash; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
