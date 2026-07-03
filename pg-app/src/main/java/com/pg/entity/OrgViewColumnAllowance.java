package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 총본사가 지정한 본사(REGIONAL) 트리 단위로, 조직 유형별·화면별 VIEW SETTING 허용 그리드 컬럼(키) 상한.
 * viewer_scope: REGIONAL(본사), MASTER_DIST(총판), BRANCH_GROUP(지사·대리점·영업점), MERCHANT(가맹점).
 * 지사그룹·가맹점에 행이 없으면 동일 본사·화면의 총판(MASTER_DIST) 정책을 따름.
 */
@Entity
@Table(name = "tb_org_view_column_allowance",
        uniqueConstraints = @UniqueConstraint(name = "uk_org_view_col_allow_scope",
                columnNames = {"regional_org_code", "page_url", "viewer_scope"}))
public class OrgViewColumnAllowance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regional_org_code", nullable = false, length = 50)
    private String regionalOrgCode;

    @Column(name = "page_url", nullable = false, length = 200)
    private String pageUrl;

    @Column(name = "viewer_scope", nullable = false, length = 32)
    private String viewerScope;

    @Column(name = "allowed_keys_json", nullable = false, columnDefinition = "TEXT")
    private String allowedKeysJson;

    /** 최초 접속 시 기본 선택(allowed_keys_json 부분집합) */
    @Column(name = "default_selected_keys_json", columnDefinition = "TEXT")
    private String defaultSelectedKeysJson;

    /** 조직항목설정 UI 전체 행 순서 */
    @Column(name = "column_order_keys_json", columnDefinition = "TEXT")
    private String columnOrderKeysJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRegionalOrgCode() { return regionalOrgCode; }
    public void setRegionalOrgCode(String regionalOrgCode) { this.regionalOrgCode = regionalOrgCode; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getViewerScope() { return viewerScope; }
    public void setViewerScope(String viewerScope) { this.viewerScope = viewerScope; }
    public String getAllowedKeysJson() { return allowedKeysJson; }
    public void setAllowedKeysJson(String allowedKeysJson) { this.allowedKeysJson = allowedKeysJson; }
    public String getDefaultSelectedKeysJson() { return defaultSelectedKeysJson; }
    public void setDefaultSelectedKeysJson(String defaultSelectedKeysJson) { this.defaultSelectedKeysJson = defaultSelectedKeysJson; }
    public String getColumnOrderKeysJson() { return columnOrderKeysJson; }
    public void setColumnOrderKeysJson(String columnOrderKeysJson) { this.columnOrderKeysJson = columnOrderKeysJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
