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

    /** 로그인 첫화면 로고 URL (로그인 화면 우측 패널 상단) - 최대 1MB, PNG 권장 */
    @Column(name = "first_logo_image_url", length = 500)
    private String firstLogoImageUrl;

    /** 팝콘이미지 URL (프로모션/안내 팝업용) - 최대 1MB, PNG 권장 */
    @Column(name = "popcon_image_url", length = 500)
    private String popconImageUrl;

    /** URL 결제 페이지 상단 이미지 — 미설정 시 {@link #logoImageUrl} 로 폴백 */
    @Column(name = "url_pay_image_url", length = 500)
    private String urlPayImageUrl;

    /** 배경테마: DEFAULT(현재), LIGHT(흰배경/검정글씨), DARK(어두운배경/흰글씨), PASTEL_1~5 */
    @Column(name = "theme", length = 20)
    private String theme = "DEFAULT";

    /** 로그인/브랜딩 안내용 호스트 (예: api.example.com) */
    @Column(name = "brand_host", length = 255)
    private String brandHost;

    /** 브라우저 탭(파비콘 옆) 사이트 이름 */
    @Column(name = "site_name", length = 100)
    private String siteName;

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
    public String getFirstLogoImageUrl() { return firstLogoImageUrl; }
    public void setFirstLogoImageUrl(String firstLogoImageUrl) { this.firstLogoImageUrl = firstLogoImageUrl; }
    public String getPopconImageUrl() { return popconImageUrl; }
    public void setPopconImageUrl(String popconImageUrl) { this.popconImageUrl = popconImageUrl; }
    public String getUrlPayImageUrl() { return urlPayImageUrl; }
    public void setUrlPayImageUrl(String urlPayImageUrl) { this.urlPayImageUrl = urlPayImageUrl; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme != null ? theme : "DEFAULT"; }
    public String getBrandHost() { return brandHost; }
    public void setBrandHost(String brandHost) { this.brandHost = brandHost; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
