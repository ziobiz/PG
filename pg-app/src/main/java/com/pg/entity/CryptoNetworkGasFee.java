package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 본사 CRYPTO 네트워크별 가스피(고정). 전 가맹 동일 적용. */
@Entity
@Table(name = "tb_crypto_network_gas_fee",
        uniqueConstraints = @UniqueConstraint(columnNames = {"asset_code", "network_code"}))
public class CryptoNetworkGasFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_code", nullable = false, length = 16)
    private String assetCode;

    @Column(name = "network_code", nullable = false, length = 32)
    private String networkCode;

    @Column(name = "gas_fee_amt", nullable = false, precision = 24, scale = 8)
    private BigDecimal gasFeeAmt = BigDecimal.ZERO;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode != null ? assetCode.trim().toUpperCase() : null;
    }
    public String getNetworkCode() { return networkCode; }
    public void setNetworkCode(String networkCode) {
        this.networkCode = networkCode != null ? networkCode.trim().toUpperCase() : null;
    }
    public BigDecimal getGasFeeAmt() { return gasFeeAmt; }
    public void setGasFeeAmt(BigDecimal gasFeeAmt) {
        this.gasFeeAmt = gasFeeAmt != null ? gasFeeAmt : BigDecimal.ZERO;
    }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) {
        this.useYn = "N".equalsIgnoreCase(useYn) ? "N" : "Y";
    }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
