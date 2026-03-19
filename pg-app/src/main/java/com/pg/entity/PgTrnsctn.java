package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 거래 마스터 (PG_TRNSCTN 스타일) - 목록/조회용 핵심 컬럼
 */
@Entity
@Table(name = "pg_trnsctn")
public class PgTrnsctn {

    @Id
    @Column(name = "trn_id", length = 20)
    private String trnId;

    @Column(name = "merchant_id", nullable = false, length = 20)
    private String merchantId;

    @Column(name = "service_type", length = 20)
    private String serviceType;

    @Column(name = "status", length = 2)
    private String status;

    @Column(name = "cur_type", length = 3)
    private String curType = "KRW";

    @Column(name = "amt_krw", precision = 15, scale = 0)
    private BigDecimal amtKrw;

    @Column(name = "pay_no", length = 50)
    private String payNo;

    @Column(name = "approval_no", length = 20)
    private String approvalNo;

    @Column(name = "van", length = 10)
    private String van;

    /** CHILL(또는 null) API동기화, NOTI 노티적재, URL URL직접결제 */
    @Column(name = "origin", length = 20)
    private String origin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public String getTrnId() { return trnId; }
    public void setTrnId(String trnId) { this.trnId = trnId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurType() { return curType; }
    public void setCurType(String curType) { this.curType = curType; }
    public BigDecimal getAmtKrw() { return amtKrw; }
    public void setAmtKrw(BigDecimal amtKrw) { this.amtKrw = amtKrw; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public String getApprovalNo() { return approvalNo; }
    public void setApprovalNo(String approvalNo) { this.approvalNo = approvalNo; }
    public String getVan() { return van; }
    public void setVan(String van) { this.van = van; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
