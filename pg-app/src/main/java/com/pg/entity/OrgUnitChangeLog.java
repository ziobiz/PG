package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 업체(조직) 상세 정보 변경 이력 — 필드 단위 (업체변경이력)
 */
@Entity
@Table(name = "tb_org_unit_change_log")
public class OrgUnitChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "comp_id", nullable = false, length = 50)
    private String compId;

    @Column(name = "comp_nm", length = 200)
    private String compNm;

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel;

    @Column(name = "value_before", columnDefinition = "TEXT")
    private String valueBefore;

    @Column(name = "value_after", columnDefinition = "TEXT")
    private String valueAfter;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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

    public String getCompId() {
        return compId;
    }

    public void setCompId(String compId) {
        this.compId = compId;
    }

    public String getCompNm() {
        return compNm;
    }

    public void setCompNm(String compNm) {
        this.compNm = compNm;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getValueBefore() {
        return valueBefore;
    }

    public void setValueBefore(String valueBefore) {
        this.valueBefore = valueBefore;
    }

    public String getValueAfter() {
        return valueAfter;
    }

    public void setValueAfter(String valueAfter) {
        this.valueAfter = valueAfter;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
