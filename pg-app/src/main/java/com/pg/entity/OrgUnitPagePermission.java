package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 개별 조직(tb_org_unit) 단위 메뉴(URL) 권한 오버라이드 — page_permission_mode=CUSTOM 일 때만 사용.
 */
@Entity
@Table(name = "tb_org_unit_page_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"org_unit_id", "page_url"}))
public class OrgUnitPagePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

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
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getMenuId() { return menuId; }
    public void setMenuId(String menuId) { this.menuId = menuId; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
