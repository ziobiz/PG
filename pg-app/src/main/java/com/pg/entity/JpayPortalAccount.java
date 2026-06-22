package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_jpay_portal_account")
public class JpayPortalAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_org_unit_id", nullable = false)
    private Long masterOrgUnitId;

    @Column(name = "master_comp_code", nullable = false, length = 64)
    private String masterCompCode;

    @Column(length = 200)
    private String label;

    @Column(name = "pg_cd", length = 32)
    private String pgCd;

    @Column(name = "portal_username", nullable = false, length = 255)
    private String portalUsername;

    @Column(name = "portal_password", nullable = false, length = 512)
    private String portalPassword;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = n;
        }
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMasterOrgUnitId() { return masterOrgUnitId; }
    public void setMasterOrgUnitId(Long masterOrgUnitId) { this.masterOrgUnitId = masterOrgUnitId; }
    public String getMasterCompCode() { return masterCompCode; }
    public void setMasterCompCode(String masterCompCode) { this.masterCompCode = masterCompCode; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getPgCd() { return pgCd; }
    public void setPgCd(String pgCd) { this.pgCd = pgCd; }
    public String getPortalUsername() { return portalUsername; }
    public void setPortalUsername(String portalUsername) { this.portalUsername = portalUsername; }
    public String getPortalPassword() { return portalPassword; }
    public void setPortalPassword(String portalPassword) { this.portalPassword = portalPassword; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
