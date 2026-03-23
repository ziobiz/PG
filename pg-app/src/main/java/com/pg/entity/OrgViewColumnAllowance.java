package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 총본사가 지정한 본사(REGIONAL) 단위로, 특정 화면의 VIEW SETTING에서 선택 가능한 그리드 컬럼(키) 상한.
 * 행이 없으면 해당 본사 트리 사용자는 제한 없음. 행이 있으면 allowed_keys_json 에 포함된 키만 개인 설정 가능.
 */
@Entity
@Table(name = "tb_org_view_column_allowance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"regional_org_code", "page_url"}))
public class OrgViewColumnAllowance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regional_org_code", nullable = false, length = 50)
    private String regionalOrgCode;

    @Column(name = "page_url", nullable = false, length = 200)
    private String pageUrl;

    @Column(name = "allowed_keys_json", nullable = false, columnDefinition = "TEXT")
    private String allowedKeysJson;

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
    public String getAllowedKeysJson() { return allowedKeysJson; }
    public void setAllowedKeysJson(String allowedKeysJson) { this.allowedKeysJson = allowedKeysJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
