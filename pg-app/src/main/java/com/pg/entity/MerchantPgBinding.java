package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 가맹점-결제대행사 연결. 가맹점이 사용할 결제대행사별 MID, API KEY, IV 등 연동 정보.
 */
@Entity
@Table(name = "tb_merchant_pg_binding", uniqueConstraints = @UniqueConstraint(columnNames = {"org_unit_id", "pg_cd", "pay_method"}))
public class MerchantPgBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "pg_cd", nullable = false, length = 20)
    private String pgCd;

    /** 착신화 유무 (활성화) */
    @Column(name = "activation_yn", length = 1)
    private String activationYn = "Y";

    /** 운영대상 여부 (가맹점이 여러 PG 중 하나를 선택하여 운영, Y인 것만 실제 운영) */
    @Column(name = "operational_yn", length = 1)
    private String operationalYn = "N";

    /** 결제구분: WEB, OFFLINE, APM */
    @Column(name = "pay_method", nullable = false, length = 20)
    private String payMethod;

    @Column(name = "mid", length = 50)
    private String mid;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(name = "iv_key", length = 100)
    private String ivKey;

    /** 할부사용 여부 */
    @Column(name = "installment_yn", length = 1)
    private String installmentYn = "N";

    /** 최대할부개월수 */
    @Column(name = "max_installment_months")
    private Integer maxInstallmentMonths;

    @Column(name = "sort_order")
    private Integer sortOrder;

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
    public String getPgCd() { return pgCd; }
    public void setPgCd(String pgCd) { this.pgCd = pgCd; }
    public String getActivationYn() { return activationYn; }
    public void setActivationYn(String activationYn) { this.activationYn = activationYn; }
    public String getOperationalYn() { return operationalYn; }
    public void setOperationalYn(String operationalYn) { this.operationalYn = operationalYn; }
    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }
    public String getMid() { return mid; }
    public void setMid(String mid) { this.mid = mid; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getIvKey() { return ivKey; }
    public void setIvKey(String ivKey) { this.ivKey = ivKey; }
    public String getInstallmentYn() { return installmentYn; }
    public void setInstallmentYn(String installmentYn) { this.installmentYn = installmentYn; }
    public Integer getMaxInstallmentMonths() { return maxInstallmentMonths; }
    public void setMaxInstallmentMonths(Integer maxInstallmentMonths) { this.maxInstallmentMonths = maxInstallmentMonths; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
