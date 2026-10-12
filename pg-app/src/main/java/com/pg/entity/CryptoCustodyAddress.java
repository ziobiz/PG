package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_crypto_custody_address")
public class CryptoCustodyAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "network_code", nullable = false, length = 16)
    private String networkCode;

    @Column(name = "asset_code", length = 16)
    private String assetCode;

    @Column(name = "address", nullable = false, length = 128)
    private String address;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

    /** Y = 해당 자산×네트워크 입금에 쓰는 ICOPAY 대표 수탁 주소 */
    @Column(name = "primary_yn", nullable = false, length = 1)
    private String primaryYn = "N";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNetworkCode() { return networkCode; }
    public void setNetworkCode(String networkCode) { this.networkCode = networkCode; }
    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getPrimaryYn() { return primaryYn; }
    public void setPrimaryYn(String primaryYn) { this.primaryYn = primaryYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
