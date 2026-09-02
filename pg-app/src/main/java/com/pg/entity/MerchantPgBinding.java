package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @Column(name = "pg_cd", nullable = false, length = 40)
    private String pgCd;

    /** 착신화 유무 (활성화) */
    @Column(name = "activation_yn", length = 1)
    private String activationYn = "Y";

    /** 운영대상 여부 (URL·챗봇·API 결제는 복수 Y 가능. 노티 전용 PG는 ALL 고정 등 별도 규칙) */
    @Column(name = "operational_yn", length = 1)
    private String operationalYn = "N";

    /** 결제구분: WEB, OFFLINE, APM */
    @Column(name = "pay_method", nullable = false, length = 20)
    private String payMethod;

    @Column(name = "mid", length = 50)
    private String mid;

    /** ChillPay Route / NOTI 루트번호 — 노티 MID+루트로 가맹점 매칭 시 사용 (없으면 MID만으로 매칭) */
    @Column(name = "root_no", length = 40)
    private String rootNo;

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

    /**
     * URL 공개 결제 금액 모드.
     * CHECKOUT_CURRENCY: 가맹점 기준 통화로 직접 결제(기존).
     * DISPLAY_FX_THB: 화면은 JPY/USD 등 표시, 실제 ChillPay 청구는 태국 바트(THB)로 환산.
     */
    @Column(name = "url_pay_pricing_mode", nullable = false, length = 32)
    private String urlPayPricingMode = "CHECKOUT_CURRENCY";

    /**
     * 이 행으로 받을 카드 브랜드 범위(약어).
     * ALL, VMJU, VMJ, VM, VJ, MJ, V, M, J, U, A(AMX), D(DINERS) — 화면 셀렉트 값과 동일.
     */
    @Column(name = "card_brand_scope", nullable = false, length = 16)
    private String cardBrandScope = "ALL";

    /**
     * 멀티 PG 라우팅 통화 범위 — ALL 또는 JPY/USD/KRW/THB 등({@link com.pg.util.CurrencyScopeUtil}).
     */
    @Column(name = "currency_scope", nullable = false, length = 8)
    private String currencyScope = "ALL";

    /** {@code null}: {@code tb_pg_agency} 기본 따름. {@code OFF}/{@code T}/{@code D}: 가맹 MID별 덮어쓰기 */
    @Column(name = "ext_settle_mode", length = 8)
    private String extSettleMode;

    @Column(name = "ext_settle_lag")
    private Integer extSettleLag;

    @Column(name = "ext_settle_batch_time")
    private LocalTime extSettleBatchTime;

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
    public String getRootNo() { return rootNo; }
    public void setRootNo(String rootNo) { this.rootNo = rootNo; }
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
    public String getUrlPayPricingMode() { return urlPayPricingMode; }
    public void setUrlPayPricingMode(String urlPayPricingMode) { this.urlPayPricingMode = urlPayPricingMode; }
    public String getCardBrandScope() { return cardBrandScope; }
    public void setCardBrandScope(String cardBrandScope) { this.cardBrandScope = cardBrandScope; }
    public String getCurrencyScope() { return currencyScope; }
    public void setCurrencyScope(String currencyScope) { this.currencyScope = currencyScope; }
    public String getExtSettleMode() { return extSettleMode; }
    public void setExtSettleMode(String extSettleMode) { this.extSettleMode = extSettleMode; }
    public Integer getExtSettleLag() { return extSettleLag; }
    public void setExtSettleLag(Integer extSettleLag) { this.extSettleLag = extSettleLag; }
    public LocalTime getExtSettleBatchTime() { return extSettleBatchTime; }
    public void setExtSettleBatchTime(LocalTime extSettleBatchTime) { this.extSettleBatchTime = extSettleBatchTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
