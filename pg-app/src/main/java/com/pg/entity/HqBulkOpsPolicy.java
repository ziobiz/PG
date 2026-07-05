package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_hq_bulk_ops_policy")
public class HqBulkOpsPolicy {

    @Id
    private Long id;

    @Column(name = "policy_type", nullable = false, length = 32)
    private String policyType;

    /** NONE | FORCE_Y | FORCE_N | PAUSED */
    @Column(name = "mode", nullable = false, length = 16)
    private String mode = "NONE";

    @Column(name = "pause_snapshot_json", columnDefinition = "TEXT")
    private String pauseSnapshotJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPolicyType() { return policyType; }
    public void setPolicyType(String policyType) { this.policyType = policyType; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getPauseSnapshotJson() { return pauseSnapshotJson; }
    public void setPauseSnapshotJson(String pauseSnapshotJson) { this.pauseSnapshotJson = pauseSnapshotJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
