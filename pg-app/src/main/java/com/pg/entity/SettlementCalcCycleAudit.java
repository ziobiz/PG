package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 가맹 정산주기 변경 이력(즉시 적용 / 다음 정산 실행 후 예약 등).
 */
@Entity
@Table(name = "tb_settlement_calc_cycle_audit")
public class SettlementCalcCycleAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "merchant_code", nullable = false, length = 64)
    private String merchantCode;

    @Column(name = "from_cycle", length = 64)
    private String fromCycle;

    @Column(name = "to_cycle", nullable = false, length = 64)
    private String toCycle;

    /** IMMEDIATE, NEXT_AFTER_RUN, APPLIED_PENDING */
    @Column(name = "transition_mode", nullable = false, length = 32)
    private String transitionMode;

    @Column(name = "actor_username", length = 128)
    private String actorUsername;

    @Column(name = "remark", length = 500)
    private String remark;

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
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }
    public String getFromCycle() { return fromCycle; }
    public void setFromCycle(String fromCycle) { this.fromCycle = fromCycle; }
    public String getToCycle() { return toCycle; }
    public void setToCycle(String toCycle) { this.toCycle = toCycle; }
    public String getTransitionMode() { return transitionMode; }
    public void setTransitionMode(String transitionMode) { this.transitionMode = transitionMode; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
