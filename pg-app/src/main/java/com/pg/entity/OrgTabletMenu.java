package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 조직 단계(OrgLevel)별 태블릿 모드 메뉴 노출 — 운영모드관리
 */
@Entity
@Table(name = "tb_org_tablet_menu",
        uniqueConstraints = @UniqueConstraint(columnNames = {"org_level", "page_url"}))
public class OrgTabletMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_level", nullable = false, length = 32)
    private String orgLevel;

    @Column(name = "page_url", nullable = false, length = 256)
    private String pageUrl;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "N";

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
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
