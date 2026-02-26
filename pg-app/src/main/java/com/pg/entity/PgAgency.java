package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 결제대행사 - 총본사에서 여러 국가 PG사 API로 구축한 결제 모듈.
 * 가맹점이 이 결제대행사를 선택하여 결제 서비스를 이용.
 */
@Entity
@Table(name = "tb_pg_agency")
public class PgAgency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pg_cd", nullable = false, unique = true, length = 20)
    private String pgCd;

    @Column(name = "pg_nm", nullable = false, length = 100)
    private String pgNm;

    @Column(name = "api_endpoint", length = 255)
    private String apiEndpoint;

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
    public String getPgCd() { return pgCd; }
    public void setPgCd(String pgCd) { this.pgCd = pgCd; }
    public String getPgNm() { return pgNm; }
    public void setPgNm(String pgNm) { this.pgNm = pgNm; }
    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
