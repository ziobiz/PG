package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 총판(MASTER_DIST)별 가맹점 등록 시 선택 가능한 정산주기(최대 5) 및 대표(기본) 슬롯.
 */
@Entity
@Table(name = "tb_master_dist_settlement_cycle_config")
public class MasterDistSettlementCycleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false, unique = true)
    private Long orgUnitId;

    @Column(name = "cycle_code_1", length = 64)
    private String cycleCode1;

    @Column(name = "cycle_code_2", length = 64)
    private String cycleCode2;

    @Column(name = "cycle_code_3", length = 64)
    private String cycleCode3;

    @Column(name = "cycle_code_4", length = 64)
    private String cycleCode4;

    @Column(name = "cycle_code_5", length = 64)
    private String cycleCode5;

    /** 대표 정산주기: cycle_code_(1+default_slot) 가 비어 있지 않아야 함 (0~4). */
    @Column(name = "default_slot", nullable = false)
    private int defaultSlot;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime n = LocalDateTime.now();
        createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getCycleCode1() {
        return cycleCode1;
    }

    public void setCycleCode1(String cycleCode1) {
        this.cycleCode1 = cycleCode1;
    }

    public String getCycleCode2() {
        return cycleCode2;
    }

    public void setCycleCode2(String cycleCode2) {
        this.cycleCode2 = cycleCode2;
    }

    public String getCycleCode3() {
        return cycleCode3;
    }

    public void setCycleCode3(String cycleCode3) {
        this.cycleCode3 = cycleCode3;
    }

    public String getCycleCode4() {
        return cycleCode4;
    }

    public void setCycleCode4(String cycleCode4) {
        this.cycleCode4 = cycleCode4;
    }

    public String getCycleCode5() {
        return cycleCode5;
    }

    public void setCycleCode5(String cycleCode5) {
        this.cycleCode5 = cycleCode5;
    }

    public int getDefaultSlot() {
        return defaultSlot;
    }

    public void setDefaultSlot(int defaultSlot) {
        this.defaultSlot = defaultSlot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
