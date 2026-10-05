package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_merchant_crypto_config")
public class MerchantCryptoConfig {

    public static final String MEANS_CARD = "CARD";
    public static final String MEANS_CRYPTO = "CRYPTO";
    public static final String MEANS_BOTH = "BOTH";
    public static final String SETTLE_IMMEDIATE = "IMMEDIATE";
    public static final String SETTLE_SCHEDULED = "SCHEDULED";
    public static final String PAYOUT_MANUAL = "MANUAL";
    public static final String PAYOUT_AUTO = "AUTO";

    @Id
    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "N";

    @Column(name = "checkout_means", nullable = false, length = 16)
    private String checkoutMeans = MEANS_CARD;

    @Column(name = "settle_mode", nullable = false, length = 16)
    private String settleMode = SETTLE_SCHEDULED;

    @Column(name = "payout_mode", nullable = false, length = 16)
    private String payoutMode = PAYOUT_MANUAL;

    @Column(name = "hold_days", nullable = false)
    private Integer holdDays = 3;

    @Column(name = "assets_csv", nullable = false, length = 128)
    private String assetsCsv = "USDT";

    @Column(name = "networks_csv", nullable = false, length = 128)
    private String networksCsv = "TRC20,ERC20";

    @Column(name = "seller_wallet_json", columnDefinition = "TEXT")
    private String sellerWalletJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getCheckoutMeans() { return checkoutMeans; }
    public void setCheckoutMeans(String checkoutMeans) { this.checkoutMeans = checkoutMeans; }
    public String getSettleMode() { return settleMode; }
    public void setSettleMode(String settleMode) { this.settleMode = settleMode; }
    public String getPayoutMode() { return payoutMode; }
    public void setPayoutMode(String payoutMode) { this.payoutMode = payoutMode; }
    public Integer getHoldDays() { return holdDays; }
    public void setHoldDays(Integer holdDays) { this.holdDays = holdDays; }
    public String getAssetsCsv() { return assetsCsv; }
    public void setAssetsCsv(String assetsCsv) { this.assetsCsv = assetsCsv; }
    public String getNetworksCsv() { return networksCsv; }
    public void setNetworksCsv(String networksCsv) { this.networksCsv = networksCsv; }
    public String getSellerWalletJson() { return sellerWalletJson; }
    public void setSellerWalletJson(String sellerWalletJson) { this.sellerWalletJson = sellerWalletJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
