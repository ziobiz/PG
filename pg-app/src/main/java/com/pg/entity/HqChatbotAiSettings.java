package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 본사설정·AI챗봇설정 — 챗봇 및 상품 안내 등에 사용할 LLM 키/모델/프로바이더 순위·프롬프트 옵션.
 * {@code config_json} 스키마는 ziobiz/Stock {@code php-web/pages/ai.php} 의 리포트 API 키·모델·순위 필드명과 호환되도록 저장합니다.
 */
@Entity
@Table(name = "tb_hq_chatbot_ai_settings")
public class HqChatbotAiSettings {

    @Id
    @Column(nullable = false)
    private Long id;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson = "{}";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = 1L;
        }
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = n;
        }
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
