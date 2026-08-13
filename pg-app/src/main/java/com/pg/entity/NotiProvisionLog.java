package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_noti_provision_log")
public class NotiProvisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "comp_id", nullable = false, length = 50)
    private String compId;

    @Column(name = "merchant_id", length = 64)
    private String merchantId;

    /** jpay | elementpay */
    @Column(name = "pg_kind", length = 16)
    private String pgKind = "jpay";

    @Column(name = "comp_nm", length = 200)
    private String compNm;

    @Column(name = "internal_target_id", length = 120)
    private String internalTargetId;

    @Column(name = "route_no", length = 32)
    private String routeNo;

    @Column(name = "slot_no")
    private Integer slotNo;

    @Column(name = "base_currency", length = 8)
    private String baseCurrency;

    @Column(name = "jpay_notify_url", length = 2048)
    private String jpayNotifyUrl;

    @Column(name = "jpay_callback_url", length = 2048)
    private String jpayCallbackUrl;

    @Column(name = "dealmai_partner_code", length = 64)
    private String dealmaiPartnerCode;

    /** API | URL */
    @Column(name = "integration_mode", length = 16)
    private String integrationMode = "API";

    /** Y=NOTI 신규 생성, N=기존 동일 */
    @Column(name = "created_flag", length = 1)
    private String createdFlag = "Y";

    @Column(name = "provisioned_by", length = 64)
    private String provisionedBy;

    @Column(name = "provisioned_at")
    private LocalDateTime provisionedAt;

    @PrePersist
    protected void onCreate() {
        if (provisionedAt == null) {
            provisionedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getPgKind() { return pgKind; }
    public void setPgKind(String pgKind) { this.pgKind = pgKind; }
    public String getCompNm() { return compNm; }
    public void setCompNm(String compNm) { this.compNm = compNm; }
    public String getInternalTargetId() { return internalTargetId; }
    public void setInternalTargetId(String internalTargetId) { this.internalTargetId = internalTargetId; }
    public String getRouteNo() { return routeNo; }
    public void setRouteNo(String routeNo) { this.routeNo = routeNo; }
    public Integer getSlotNo() { return slotNo; }
    public void setSlotNo(Integer slotNo) { this.slotNo = slotNo; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public String getJpayNotifyUrl() { return jpayNotifyUrl; }
    public void setJpayNotifyUrl(String jpayNotifyUrl) { this.jpayNotifyUrl = jpayNotifyUrl; }
    public String getJpayCallbackUrl() { return jpayCallbackUrl; }
    public void setJpayCallbackUrl(String jpayCallbackUrl) { this.jpayCallbackUrl = jpayCallbackUrl; }
    public String getDealmaiPartnerCode() { return dealmaiPartnerCode; }
    public void setDealmaiPartnerCode(String dealmaiPartnerCode) { this.dealmaiPartnerCode = dealmaiPartnerCode; }
    public String getIntegrationMode() { return integrationMode; }
    public void setIntegrationMode(String integrationMode) { this.integrationMode = integrationMode; }
    public String getCreatedFlag() { return createdFlag; }
    public void setCreatedFlag(String createdFlag) { this.createdFlag = createdFlag; }
    public String getProvisionedBy() { return provisionedBy; }
    public void setProvisionedBy(String provisionedBy) { this.provisionedBy = provisionedBy; }
    public LocalDateTime getProvisionedAt() { return provisionedAt; }
    public void setProvisionedAt(LocalDateTime provisionedAt) { this.provisionedAt = provisionedAt; }
}
