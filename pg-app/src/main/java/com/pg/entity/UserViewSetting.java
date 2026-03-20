package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 로그인 사용자별 페이지 VIEW SETTING(그리드 표시 컬럼) 저장
 */
@Entity
@Table(name = "tb_user_view_setting", uniqueConstraints = @UniqueConstraint(columnNames = {"username", "page_url"}))
public class UserViewSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "page_url", nullable = false, length = 200)
    private String pageUrl;

    /** JSON array string, e.g. ["compNm","regNo"] */
    @Column(name = "selected_keys_json", nullable = false, columnDefinition = "TEXT")
    private String selectedKeysJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getSelectedKeysJson() { return selectedKeysJson; }
    public void setSelectedKeysJson(String selectedKeysJson) { this.selectedKeysJson = selectedKeysJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
