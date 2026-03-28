package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 본사/총판 브랜딩 설정 (메인이미지, 로고, 배경테마)
 * - REGIONAL(본사), MASTER_DIST(총판) 레벨만 사용
 */
@Entity
@Table(name = "tb_org_branding", uniqueConstraints = @UniqueConstraint(columnNames = "org_unit_id"))
public class OrgBranding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false, unique = true)
    private Long orgUnitId;

    /** 메인이미지 URL (로그인 화면 왼쪽/중앙 배경) - 최대 2MB, PNG 권장 */
    @Column(name = "main_image_url", length = 500)
    private String mainImageUrl;

    /** 로고이미지 URL (로그인창 상단, 사이드바 상단) - 최대 1MB, PNG 권장 */
    @Column(name = "logo_image_url", length = 500)
    private String logoImageUrl;

    /** 배경테마: DEFAULT(현재), LIGHT(흰배경/검정글씨), DARK(어두운배경/흰글씨), PASTEL_1~5 */
    @Column(name = "theme", length = 20)
    private String theme = "DEFAULT";

    /** 로그인/브랜딩 안내용 호스트 (예: api.example.com) */
    @Column(name = "brand_host", length = 255)
    private String brandHost;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }
    public String getLogoImageUrl() { return logoImageUrl; }
    public void setLogoImageUrl(String logoImageUrl) { this.logoImageUrl = logoImageUrl; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme != null ? theme : "DEFAULT"; }
    public String getBrandHost() { return brandHost; }
    public void setBrandHost(String brandHost) { this.brandHost = brandHost; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
