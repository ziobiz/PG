package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 롤링 담보금 (결제대금의 N%를 N일간 보류)
 */
@Entity
@Table(name = "tb_rolling_reserve")
public class RollingReserve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trn_id", length = 20)
    private String trnId;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(name = "reserve_amt", precision = 15, scale = 0)
    private BigDecimal reserveAmt = BigDecimal.ZERO;

    @Column(name = "rolling_pct", precision = 5, scale = 2)
    private BigDecimal rollingPct;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "status", length = 20)
    private String status = "HOLD";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrnId() { return trnId; }
    public void setTrnId(String trnId) { this.trnId = trnId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public BigDecimal getReserveAmt() { return reserveAmt; }
    public void setReserveAmt(BigDecimal reserveAmt) { this.reserveAmt = reserveAmt; }
    public BigDecimal getRollingPct() { return rollingPct; }
    public void setRollingPct(BigDecimal rollingPct) { this.rollingPct = rollingPct; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
