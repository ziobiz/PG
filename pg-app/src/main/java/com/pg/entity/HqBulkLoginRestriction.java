package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_hq_bulk_login_restriction")
public class HqBulkLoginRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_org_level", length = 20)
    private String targetOrgLevel;

    @Column(name = "target_org_unit_id")
    private Long targetOrgUnitId;

    @Column(name = "target_org_code", length = 50)
    private String targetOrgCode;

    @Column(name = "target_org_name", length = 200)
    private String targetOrgName;

    /** FORCE_Y | FORCE_N | PAUSED */
    @Column(name = "mode", nullable = false, length = 16)
    private String mode = "FORCE_N";

    @Column(name = "pause_snapshot_json", columnDefinition = "TEXT")
    private String pauseSnapshotJson;

    /** ACTIVE | RELEASED */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTargetOrgLevel() { return targetOrgLevel; }
    public void setTargetOrgLevel(String targetOrgLevel) { this.targetOrgLevel = targetOrgLevel; }
    public Long getTargetOrgUnitId() { return targetOrgUnitId; }
    public void setTargetOrgUnitId(Long targetOrgUnitId) { this.targetOrgUnitId = targetOrgUnitId; }
    public String getTargetOrgCode() { return targetOrgCode; }
    public void setTargetOrgCode(String targetOrgCode) { this.targetOrgCode = targetOrgCode; }
    public String getTargetOrgName() { return targetOrgName; }
    public void setTargetOrgName(String targetOrgName) { this.targetOrgName = targetOrgName; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getPauseSnapshotJson() { return pauseSnapshotJson; }
    public void setPauseSnapshotJson(String pauseSnapshotJson) { this.pauseSnapshotJson = pauseSnapshotJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
