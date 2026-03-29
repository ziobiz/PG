package com.pg.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 차지백 정책 구간: 해당 월 누적 건수({@code countMin}~{@code countMax})에 적용할 건당 금액.
 * {@code countMax} 가 null 이면 상한 없음.
 */
@Entity
@Table(name = "tb_chargeback_fee_tier")
public class ChargebackFeeTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private ChargebackFeePolicy policy;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "count_min", nullable = false)
    private int countMin;

    @Column(name = "count_max")
    private Integer countMax;

    @Column(name = "fee_per_case", nullable = false, precision = 12, scale = 0)
    private BigDecimal feePerCase = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ChargebackFeePolicy getPolicy() { return policy; }
    public void setPolicy(ChargebackFeePolicy policy) { this.policy = policy; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public int getCountMin() { return countMin; }
    public void setCountMin(int countMin) { this.countMin = countMin; }
    public Integer getCountMax() { return countMax; }
    public void setCountMax(Integer countMax) { this.countMax = countMax; }
    public BigDecimal getFeePerCase() { return feePerCase; }
    public void setFeePerCase(BigDecimal feePerCase) { this.feePerCase = feePerCase != null ? feePerCase : BigDecimal.ZERO; }
}
