package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 가맹점이 ICOPAY PG 브로커({@code /api/middleware/v1/pg/...})를 호출할 때 사용하는 시크릿.
 * {@code vendor_scope}: {@code ALL} 또는 {@link com.pg.integration.pg.PgVendor} 계열.
 */
@Entity
@Table(name = "tb_merchant_icopay_broker_credential",
        uniqueConstraints = @UniqueConstraint(columnNames = {"org_unit_id", "vendor_scope"}))
public class MerchantIcopayBrokerCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "vendor_scope", nullable = false, length = 20)
    private String vendorScope = "ALL";

    @Column(name = "broker_secret", nullable = false, length = 128)
    private String brokerSecret;

    @Column(name = "secret_prefix", length = 12)
    private String secretPrefix;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

    @Column(name = "enforce_yn", nullable = false, length = 1)
    private String enforceYn = "Y";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    @Column(length = 500)
    private String remark;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (useYn == null || useYn.isBlank()) {
            useYn = "Y";
        }
        if (enforceYn == null || enforceYn.isBlank()) {
            enforceYn = "Y";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getVendorScope() { return vendorScope; }
    public void setVendorScope(String vendorScope) { this.vendorScope = vendorScope; }
    public String getBrokerSecret() { return brokerSecret; }
    public void setBrokerSecret(String brokerSecret) { this.brokerSecret = brokerSecret; }
    public String getSecretPrefix() { return secretPrefix; }
    public void setSecretPrefix(String secretPrefix) { this.secretPrefix = secretPrefix; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getEnforceYn() { return enforceYn; }
    public void setEnforceYn(String enforceYn) { this.enforceYn = enforceYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getRotatedAt() { return rotatedAt; }
    public void setRotatedAt(LocalDateTime rotatedAt) { this.rotatedAt = rotatedAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
