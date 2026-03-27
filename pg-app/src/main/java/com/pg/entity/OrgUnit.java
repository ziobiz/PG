package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 영업 조직 단위 (총본사/지역본사/총판/지사/대리점/가맹점)
 */
@Entity
@Table(name = "tb_org_unit")
public class OrgUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_level", nullable = false, length = 20)
    private OrgLevel orgLevel;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 20)
    private String status = "ACTIVE";

    /**
     * LEVEL_DEFAULT: 조직 단계(orgLevel)별 기본 매트릭스만 사용.
     * CUSTOM: tb_org_unit_page_permission 오버라이드를 병합.
     * <p>운영 DB는 V33 SQL로 NOT NULL+DEFAULT를 줄 수 있다. dev H2는 ddl-auto가 NOT NULL 컬럼만 추가할 때
     * 테이블 복사 INSERT가 실패하므로 JPA 매핑은 nullable로 두고 저장 시 기본값으로 맞춘다.</p>
     */
    @Column(name = "page_permission_mode", length = 20)
    private String pagePermissionMode = "LEVEL_DEFAULT";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    private void normalizePagePermissionMode() {
        if (pagePermissionMode == null || pagePermissionMode.isBlank()) {
            pagePermissionMode = "LEVEL_DEFAULT";
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        normalizePagePermissionMode();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizePagePermissionMode();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrgLevel getOrgLevel() { return orgLevel; }
    public void setOrgLevel(OrgLevel orgLevel) { this.orgLevel = orgLevel; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPagePermissionMode() {
        return (pagePermissionMode != null && !pagePermissionMode.isBlank()) ? pagePermissionMode : "LEVEL_DEFAULT";
    }
    public void setPagePermissionMode(String pagePermissionMode) { this.pagePermissionMode = pagePermissionMode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
