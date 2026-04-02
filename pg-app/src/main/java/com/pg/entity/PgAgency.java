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

    /** 결제 운영 대행사(Y): 가맹점 PG 선택·연동에 노출. 사용(use_yn)이 Y인 항목 중 운영으로 지정된 것만 */
    @Column(name = "operational_yn", length = 1)
    private String operationalYn = "N";

    /** PG별 MID / Merchant Code */
    @Column(name = "merchant_mid", length = 100)
    private String merchantMid;

    @Column(name = "api_key", length = 512)
    private String apiKey;

    /** CheckSum·서명용 시크릿 (예 ChillPay MD5 Key). 가맹점 연동 IV와 별개 */
    @Column(name = "md5_secret_key", length = 255)
    private String md5SecretKey;

    @Column(name = "route_no")
    private Integer routeNo;

    @Column(name = "sandbox_yn", length = 1)
    private String sandboxYn = "Y";

    /** PG별 추가 파라미터 JSON */
    @Column(name = "credentials_extra_json", columnDefinition = "TEXT")
    private String credentialsExtraJson;

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
    public String getOperationalYn() { return operationalYn; }
    public void setOperationalYn(String operationalYn) { this.operationalYn = operationalYn; }
    public String getMerchantMid() { return merchantMid; }
    public void setMerchantMid(String merchantMid) { this.merchantMid = merchantMid; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getMd5SecretKey() { return md5SecretKey; }
    public void setMd5SecretKey(String md5SecretKey) { this.md5SecretKey = md5SecretKey; }
    public Integer getRouteNo() { return routeNo; }
    public void setRouteNo(Integer routeNo) { this.routeNo = routeNo; }
    public String getSandboxYn() { return sandboxYn; }
    public void setSandboxYn(String sandboxYn) { this.sandboxYn = sandboxYn; }
    public String getCredentialsExtraJson() { return credentialsExtraJson; }
    public void setCredentialsExtraJson(String credentialsExtraJson) { this.credentialsExtraJson = credentialsExtraJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
