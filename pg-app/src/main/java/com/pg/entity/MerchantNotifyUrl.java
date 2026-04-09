package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 가맹점 결제통보 URL. 결제 응답을 가맹점에게 송부할 노티 주소.
 * urlType: BACKGROUND(백그라운드), RESULT(결과), MIDDLEWARE(PG중계→가맹점 JSON 콜백)
 */
@Entity
@Table(name = "tb_merchant_notify_url", uniqueConstraints = @UniqueConstraint(columnNames = {"org_unit_id", "url_type"}))
public class MerchantNotifyUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    /** URL구분: BACKGROUND, RESULT, MIDDLEWARE */
    @Column(name = "url_type", nullable = false, length = 20)
    private String urlType;

    /** 전산노티·칠페이 등 전체 URL(쿼리 포함 가능). DB는 V48 마이그레이션으로 2048 정렬. */
    @Column(name = "noti_url", length = 2048)
    private String notiUrl;

    /** {@code url_type}=MIDDLEWARE 일 때 아웃바운드 JSON 서명용 (V83). */
    @Column(name = "sign_secret", length = 256)
    private String signSecret;

    @Column(name = "use_yn", length = 1)
    private String useYn = "Y";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getUrlType() { return urlType; }
    public void setUrlType(String urlType) { this.urlType = urlType; }
    public String getNotiUrl() { return notiUrl; }
    public void setNotiUrl(String notiUrl) { this.notiUrl = notiUrl; }
    public String getSignSecret() { return signSecret; }
    public void setSignSecret(String signSecret) { this.signSecret = signSecret; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
