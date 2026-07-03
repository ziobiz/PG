package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pay_risk_filter_event")
public class PayRiskFilterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "merchant_id", length = 20)
    private String merchantId;

    @Column(name = "order_no", length = 64)
    private String orderNo;

    @Column(name = "trn_id", length = 20)
    private String trnId;

    @Column(name = "pg_vendor", length = 16)
    private String pgVendor;

    @Column(name = "filter_code", nullable = false, length = 64)
    private String filterCode;

    @Column(name = "filter_desc", length = 500)
    private String filterDesc;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getTrnId() { return trnId; }
    public void setTrnId(String trnId) { this.trnId = trnId; }
    public String getPgVendor() { return pgVendor; }
    public void setPgVendor(String pgVendor) { this.pgVendor = pgVendor; }
    public String getFilterCode() { return filterCode; }
    public void setFilterCode(String filterCode) { this.filterCode = filterCode; }
    public String getFilterDesc() { return filterDesc; }
    public void setFilterDesc(String filterDesc) { this.filterDesc = filterDesc; }
    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
