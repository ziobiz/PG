package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 수수료 변경 이력 (수수료관리 저장 시 적재)
 */
@Entity
@Table(name = "tb_commission_history")
public class CommissionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comp_id", nullable = false, length = 50)
    private String compId;

    @Column(name = "chg_type", length = 20)
    private String chgType = "COMMISSION";

    @Column(name = "chg_desc", length = 500)
    private String chgDesc;

    /** 변경 시점 그리드 스냅샷(JSON) — 수수료관리 화면 재현용 */
    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public String getChgType() { return chgType; }
    public void setChgType(String chgType) { this.chgType = chgType; }
    public String getChgDesc() { return chgDesc; }
    public void setChgDesc(String chgDesc) { this.chgDesc = chgDesc; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
