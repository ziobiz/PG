package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 조직 단위 + 담당자 권한그룹(관리/운영/정산/기술)별 메뉴(URL) 권한.
 * 조직 최종 권한(effectiveMapForOrgUnit)을 상한으로 하며, 저장·적용 시 초과 불가.
 */
@Entity
@Table(name = "tb_org_unit_assistant_page_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"org_unit_id", "assistant_role_type", "page_url"}))
public class OrgUnitAssistantPagePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "assistant_role_type", nullable = false, length = 32)
    private String assistantRoleType;

    @Column(name = "page_url", nullable = false, length = 256)
    private String pageUrl;

    @Column(name = "menu_id", length = 32)
    private String menuId;

    @Column(name = "permission", nullable = false, length = 16)
    private String permission;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getAssistantRoleType() { return assistantRoleType; }
    public void setAssistantRoleType(String assistantRoleType) { this.assistantRoleType = assistantRoleType; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getMenuId() { return menuId; }
    public void setMenuId(String menuId) { this.menuId = menuId; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
