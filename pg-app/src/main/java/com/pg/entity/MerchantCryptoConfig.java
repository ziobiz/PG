package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
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

    /** 보증금(담보) 비율 %. USD 입금금액 기준. 직접입력(N)일 때만 출금 산정에 사용. */
    @Column(name = "reserve_pct", nullable = false, precision = 10, scale = 4)
    private BigDecimal reservePct = BigDecimal.ZERO;

    /**
     * Y=본사 CRYPTO 수수료정책 롤링(담보)%·보류일수 따름.
     * N=reserve_pct·hold_days 직접입력.
     */
    @Column(name = "reserve_follow_hq", nullable = false, length = 1)
    private String reserveFollowHq = "Y";

    /**
     * 구매자 표시통화(JPY·USD 등). 청구·수수료·한도 앵커는 USD({@code CryptoCheckoutService.QUOTE_CURRENCY}).
     */
    @Column(name = "display_currency", nullable = false, length = 8)
    private String displayCurrency = "USD";

    /** 기본 미사용 — 허용자산 비활성(빈 CSV). */
    @Column(name = "assets_csv", nullable = false, length = 128)
    private String assetsCsv = "";

    /** 기본 미사용 — 네트워크 미선택. */
    @Column(name = "networks_csv", nullable = false, length = 128)
    private String networksCsv = "";

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
    public BigDecimal getReservePct() { return reservePct; }
    public void setReservePct(BigDecimal reservePct) { this.reservePct = reservePct; }
    public String getReserveFollowHq() { return reserveFollowHq; }
    public void setReserveFollowHq(String reserveFollowHq) { this.reserveFollowHq = reserveFollowHq; }
    public String getDisplayCurrency() { return displayCurrency; }
    public void setDisplayCurrency(String displayCurrency) { this.displayCurrency = displayCurrency; }
    public String getAssetsCsv() { return assetsCsv; }
    public void setAssetsCsv(String assetsCsv) { this.assetsCsv = assetsCsv; }
    public String getNetworksCsv() { return networksCsv; }
    public void setNetworksCsv(String networksCsv) { this.networksCsv = networksCsv; }
    public String getSellerWalletJson() { return sellerWalletJson; }
    public void setSellerWalletJson(String sellerWalletJson) { this.sellerWalletJson = sellerWalletJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
