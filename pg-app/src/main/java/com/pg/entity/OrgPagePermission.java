package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 조직 구분(OrgLevel)별 메뉴(URL) 접근 권한 — 본사설정 조직별 권한 세팅
 */
@Entity
@Table(name = "tb_org_page_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"org_level", "page_url"}))
public class OrgPagePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_level", nullable = false, length = 32)
    private String orgLevel;

    @Column(name = "page_url", nullable = false, length = 256)
    private String pageUrl;

    @Column(name = "menu_id", length = 32)
    private String menuId;

    /** NONE, OBSERVER, MODIFY, DELETE */
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
    public String getOrgLevel() { return orgLevel; }
    public void setOrgLevel(String orgLevel) { this.orgLevel = orgLevel; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getMenuId() { return menuId; }
    public void setMenuId(String menuId) { this.menuId = menuId; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
