package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_hq_notify_target", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"target_code"})
})
public class HqNotifyTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_code", nullable = false, length = 50)
    private String targetCode;

    @Column(name = "target_name", nullable = false, length = 100)
    private String targetName;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    /** CALLBACK: 서버→서버 노티, RESULT: 브라우저 리다이렉트 등 */
    @Column(name = "channel_type", nullable = false, length = 16)
    private String channelType = "CALLBACK";

    @Column(name = "use_yn", length = 1)
    private String useYn = "Y";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTargetCode() { return targetCode; }
    public void setTargetCode(String targetCode) { this.targetCode = targetCode; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

