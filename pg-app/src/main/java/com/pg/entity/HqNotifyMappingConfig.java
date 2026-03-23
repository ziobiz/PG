package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 본사설정 — 노티매핑 (PG사별 CALLBACK/RESULT 등 수신 데이터 → 전산 화면·필드 매핑 정의, JSON 단일 행)
 */
@Entity
@Table(name = "tb_hq_notify_mapping")
public class HqNotifyMappingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "mapping_json")
    private String mappingJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMappingJson() { return mappingJson; }
    public void setMappingJson(String mappingJson) { this.mappingJson = mappingJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
