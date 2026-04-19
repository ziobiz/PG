package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 총판(MASTER_DIST)별 가맹점 등록 시 선택 가능한 정산주기(최대 10) 및 대표(기본) 슬롯(0~9).
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

    @Column(name = "cycle_code_6", length = 64)
    private String cycleCode6;

    @Column(name = "cycle_code_7", length = 64)
    private String cycleCode7;

    @Column(name = "cycle_code_8", length = 64)
    private String cycleCode8;

    @Column(name = "cycle_code_9", length = 64)
    private String cycleCode9;

    @Column(name = "cycle_code_10", length = 64)
    private String cycleCode10;

    /** 대표 정산주기: 지정 슬롯(0~9)에 코드가 있어야 함. */
    @Column(name = "default_slot", nullable = false)
    private int defaultSlot;

    /**
     * 정산 자동 배치 시각 기준(격자 M/H·TM/TH, T0 당일, 마감시각 등) — 영업일 프로필(휴일)과 별도.
     * IANA 예: Asia/Seoul, Asia/Bangkok, Asia/Tokyo, UTC
     */
    @Column(name = "settlement_cron_zone_id", nullable = false, length = 64)
    private String settlementCronZoneId = "Asia/Seoul";

    /**
     * 거래시간 그리드 1줄 프리셋(KR, JP, USA, TH, SG, HK, CH). null이면 JP로 간주.
     * 2줄은 {@link #settlementCronZoneId} 기준.
     */
    @Column(name = "txn_time_display_preset", length = 16)
    private String txnTimeDisplayPreset;

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

    public String getCycleCode6() {
        return cycleCode6;
    }

    public void setCycleCode6(String cycleCode6) {
        this.cycleCode6 = cycleCode6;
    }

    public String getCycleCode7() {
        return cycleCode7;
    }

    public void setCycleCode7(String cycleCode7) {
        this.cycleCode7 = cycleCode7;
    }

    public String getCycleCode8() {
        return cycleCode8;
    }

    public void setCycleCode8(String cycleCode8) {
        this.cycleCode8 = cycleCode8;
    }

    public String getCycleCode9() {
        return cycleCode9;
    }

    public void setCycleCode9(String cycleCode9) {
        this.cycleCode9 = cycleCode9;
    }

    public String getCycleCode10() {
        return cycleCode10;
    }

    public void setCycleCode10(String cycleCode10) {
        this.cycleCode10 = cycleCode10;
    }

    public int getDefaultSlot() {
        return defaultSlot;
    }

    public void setDefaultSlot(int defaultSlot) {
        this.defaultSlot = defaultSlot;
    }

    public String getSettlementCronZoneId() {
        return settlementCronZoneId;
    }

    public void setSettlementCronZoneId(String settlementCronZoneId) {
        this.settlementCronZoneId = settlementCronZoneId;
    }

    public String getTxnTimeDisplayPreset() {
        return txnTimeDisplayPreset;
    }

    public void setTxnTimeDisplayPreset(String txnTimeDisplayPreset) {
        this.txnTimeDisplayPreset = txnTimeDisplayPreset;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
