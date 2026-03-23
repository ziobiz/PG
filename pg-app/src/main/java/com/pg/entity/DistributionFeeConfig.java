package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_distribution_fee_config", uniqueConstraints = @UniqueConstraint(columnNames = {"comp_id"}))
public class DistributionFeeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comp_id", nullable = false, length = 32)
    private String compId;

    @Column(name = "hq_rate", precision = 5, scale = 2)
    private BigDecimal hqRate = BigDecimal.ZERO;
    @Column(name = "regional_rate", precision = 5, scale = 2)
    private BigDecimal regionalRate = BigDecimal.ZERO;
    @Column(name = "master_rate", precision = 5, scale = 2)
    private BigDecimal masterRate = BigDecimal.ZERO;
    @Column(name = "branch_rate", precision = 5, scale = 2)
    private BigDecimal branchRate = BigDecimal.ZERO;
    @Column(name = "agency_rate", precision = 5, scale = 2)
    private BigDecimal agencyRate = BigDecimal.ZERO;

    @Column(name = "sales_office_rate", precision = 5, scale = 2)
    private BigDecimal salesOfficeRate = BigDecimal.ZERO;

    @Column(name = "hq_per_tx_fee", precision = 12, scale = 2)
    private BigDecimal hqPerTxFee = BigDecimal.ZERO;
    @Column(name = "regional_per_tx_fee", precision = 12, scale = 2)
    private BigDecimal regionalPerTxFee = BigDecimal.ZERO;
    @Column(name = "master_per_tx_fee", precision = 12, scale = 2)
    private BigDecimal masterPerTxFee = BigDecimal.ZERO;
    @Column(name = "branch_per_tx_fee", precision = 12, scale = 2)
    private BigDecimal branchPerTxFee = BigDecimal.ZERO;
    @Column(name = "agency_per_tx_fee", precision = 12, scale = 2)
    private BigDecimal agencyPerTxFee = BigDecimal.ZERO;
    @Column(name = "sales_office_per_tx_fee", precision = 12, scale = 2)
    private BigDecimal salesOfficePerTxFee = BigDecimal.ZERO;

    @Column(name = "apply_start_date")
    private LocalDate applyStartDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    protected void touch() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public BigDecimal getHqRate() { return hqRate; }
    public void setHqRate(BigDecimal hqRate) { this.hqRate = hqRate; }
    public BigDecimal getRegionalRate() { return regionalRate; }
    public void setRegionalRate(BigDecimal regionalRate) { this.regionalRate = regionalRate; }
    public BigDecimal getMasterRate() { return masterRate; }
    public void setMasterRate(BigDecimal masterRate) { this.masterRate = masterRate; }
    public BigDecimal getBranchRate() { return branchRate; }
    public void setBranchRate(BigDecimal branchRate) { this.branchRate = branchRate; }
    public BigDecimal getAgencyRate() { return agencyRate; }
    public void setAgencyRate(BigDecimal agencyRate) { this.agencyRate = agencyRate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public BigDecimal getSalesOfficeRate() { return salesOfficeRate; }
    public void setSalesOfficeRate(BigDecimal salesOfficeRate) { this.salesOfficeRate = salesOfficeRate; }
    public BigDecimal getHqPerTxFee() { return hqPerTxFee; }
    public void setHqPerTxFee(BigDecimal hqPerTxFee) { this.hqPerTxFee = hqPerTxFee; }
    public BigDecimal getRegionalPerTxFee() { return regionalPerTxFee; }
    public void setRegionalPerTxFee(BigDecimal regionalPerTxFee) { this.regionalPerTxFee = regionalPerTxFee; }
    public BigDecimal getMasterPerTxFee() { return masterPerTxFee; }
    public void setMasterPerTxFee(BigDecimal masterPerTxFee) { this.masterPerTxFee = masterPerTxFee; }
    public BigDecimal getBranchPerTxFee() { return branchPerTxFee; }
    public void setBranchPerTxFee(BigDecimal branchPerTxFee) { this.branchPerTxFee = branchPerTxFee; }
    public BigDecimal getAgencyPerTxFee() { return agencyPerTxFee; }
    public void setAgencyPerTxFee(BigDecimal agencyPerTxFee) { this.agencyPerTxFee = agencyPerTxFee; }
    public BigDecimal getSalesOfficePerTxFee() { return salesOfficePerTxFee; }
    public void setSalesOfficePerTxFee(BigDecimal salesOfficePerTxFee) { this.salesOfficePerTxFee = salesOfficePerTxFee; }
    public LocalDate getApplyStartDate() { return applyStartDate; }
    public void setApplyStartDate(LocalDate applyStartDate) { this.applyStartDate = applyStartDate; }
}

