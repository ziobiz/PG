package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 수수료 정책 (본사 기본 + 가맹점별 오버라이드)
 * 건당수수료·취소·환불(건당 고정), 월간이용료(고정·월 1회), 실패·결제 수수료율, 롤링(담보금) 비율/일수,
 * 기타(비고) 수수료 최대 4건(PCT=승인건별 %, FIX=정산당 고정액)
 */
@Entity
@Table(name = "tb_commission_policy")
public class CommissionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DEFAULT=본사기본, 아니면 가맹점 code */
    @Column(name = "scope", nullable = false, length = 50)
    private String scope = "DEFAULT";

    /** 본사정책 템플릿명 (A/B/C/D 등 이름 변경 가능) */
    @Column(name = "policy_name", length = 100)
    private String policyName;

    /** 본사 템플릿 배포 여부(Y/N). 가맹점 등록 시 자동 부여 대상 표시용 */
    @Column(name = "deploy_yn", length = 1)
    private String deployYn = "N";

    /** 건당 수수료(원) */
    @Column(name = "per_tx_fee", precision = 12, scale = 0)
    private BigDecimal perTxFee = BigDecimal.ZERO;

    /** 취소 건당 수수료({@link #currencyCode} 단위). DB 컬럼명 cancel_rate 유지. */
    @Column(name = "cancel_rate", precision = 12, scale = 0)
    private BigDecimal cancelRate = BigDecimal.ZERO;

    /** 월 1회 부과 이용료(고정 금액, {@link #currencyCode} 단위). DB 컬럼명은 호환을 위해 usage_rate 유지. */
    @Column(name = "usage_rate", precision = 12, scale = 0)
    private BigDecimal usageRate = BigDecimal.ZERO;

    /** 실패 수수료(원/건) */
    @Column(name = "fail_fee", precision = 12, scale = 0)
    private BigDecimal failFee = BigDecimal.ZERO;

    /** 결제 수수료율(%) */
    @Column(name = "pay_rate", precision = 5, scale = 2)
    private BigDecimal payRate = BigDecimal.ZERO;

    /** 환불·강제환불(30·31) 건당 수수료({@link #currencyCode} 단위). DB 컬럼명 refund_rate 유지. */
    @Column(name = "refund_rate", precision = 12, scale = 0)
    private BigDecimal refundRate = BigDecimal.ZERO;

    /** D형: 건당 정산수수료 */
    @Column(name = "fee_settlement_per_tx", precision = 12, scale = 0)
    private BigDecimal feeSettlementPerTx = BigDecimal.ZERO;

    /** USDT 정산/변환 등 — 승인(결제) 금액 대비 수수료율(%) */
    @Column(name = "fee_usdt", precision = 12, scale = 2)
    private BigDecimal feeUsdt = BigDecimal.ZERO;

    /** FX 관련 — 승인(결제) 금액 대비 수수료율(%) */
    @Column(name = "fee_fx", precision = 12, scale = 2)
    private BigDecimal feeFx = BigDecimal.ZERO;

    /** 롤링(담보금) 비율 % - 결제대금에서 보류 */
    @Column(name = "rolling_pct", precision = 5, scale = 2)
    private BigDecimal rollingPct = BigDecimal.ZERO;

    /** 롤링 보류 일수 (120, 180 등) */
    @Column(name = "rolling_days")
    private Integer rollingDays = 0;

    /** 정책 기준 통화 (표시·연동용, 예: KRW) */
    @Column(name = "currency_code", length = 16)
    private String currencyCode = "KRW";

    /** 정책 비고(내부 메모) */
    @Column(name = "policy_remark", columnDefinition = "TEXT")
    private String policyRemark;

    /** 3-D Secure 등 추가 인증 건당/건별 수수료율(%) */
    @Column(name = "fee_3ds_rate", precision = 5, scale = 2)
    private BigDecimal fee3dsRate = BigDecimal.ZERO;

    /** 차지백 건당 수수료(원) */
    @Column(name = "chargeback_fee_per_tx", precision = 12, scale = 0)
    private BigDecimal chargebackFeePerTx = BigDecimal.ZERO;

    /** 선택 시 월간 환불·강제환불(30/31) 건수로 구간별 건당 차지백 단가 적용. null 이면 위 건당 금액만 사용 */
    @Column(name = "chargeback_policy_id")
    private Long chargebackPolicyId;

    @Column(name = "extra_fee_1_name", length = 64)
    private String extraFee1Name;
    @Column(name = "extra_fee_1_mode", length = 8)
    private String extraFee1Mode;
    @Column(name = "extra_fee_1_value", precision = 15, scale = 4)
    private BigDecimal extraFee1Value;

    @Column(name = "extra_fee_2_name", length = 64)
    private String extraFee2Name;
    @Column(name = "extra_fee_2_mode", length = 8)
    private String extraFee2Mode;
    @Column(name = "extra_fee_2_value", precision = 15, scale = 4)
    private BigDecimal extraFee2Value;

    @Column(name = "extra_fee_3_name", length = 64)
    private String extraFee3Name;
    @Column(name = "extra_fee_3_mode", length = 8)
    private String extraFee3Mode;
    @Column(name = "extra_fee_3_value", precision = 15, scale = 4)
    private BigDecimal extraFee3Value;

    @Column(name = "extra_fee_4_name", length = 64)
    private String extraFee4Name;
    @Column(name = "extra_fee_4_mode", length = 8)
    private String extraFee4Mode;
    @Column(name = "extra_fee_4_value", precision = 15, scale = 4)
    private BigDecimal extraFee4Value;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getDeployYn() { return deployYn; }
    public void setDeployYn(String deployYn) { this.deployYn = deployYn; }
    public BigDecimal getPerTxFee() { return perTxFee; }
    public void setPerTxFee(BigDecimal perTxFee) { this.perTxFee = perTxFee; }
    public BigDecimal getCancelRate() { return cancelRate; }
    public void setCancelRate(BigDecimal cancelRate) { this.cancelRate = cancelRate; }
    public BigDecimal getUsageRate() { return usageRate; }
    public void setUsageRate(BigDecimal usageRate) { this.usageRate = usageRate; }
    public BigDecimal getFailFee() { return failFee; }
    public void setFailFee(BigDecimal failFee) { this.failFee = failFee; }
    public BigDecimal getPayRate() { return payRate; }
    public void setPayRate(BigDecimal payRate) { this.payRate = payRate; }
    public BigDecimal getRefundRate() { return refundRate; }
    public void setRefundRate(BigDecimal refundRate) { this.refundRate = refundRate; }
    public BigDecimal getFeeSettlementPerTx() { return feeSettlementPerTx; }
    public void setFeeSettlementPerTx(BigDecimal feeSettlementPerTx) { this.feeSettlementPerTx = feeSettlementPerTx; }
    public BigDecimal getFeeUsdt() { return feeUsdt; }
    public void setFeeUsdt(BigDecimal feeUsdt) { this.feeUsdt = feeUsdt; }
    public BigDecimal getFeeFx() { return feeFx; }
    public void setFeeFx(BigDecimal feeFx) { this.feeFx = feeFx; }
    public BigDecimal getRollingPct() { return rollingPct; }
    public void setRollingPct(BigDecimal rollingPct) { this.rollingPct = rollingPct; }
    public Integer getRollingDays() { return rollingDays; }
    public void setRollingDays(Integer rollingDays) { this.rollingDays = rollingDays; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode != null ? currencyCode : "KRW"; }
    public String getPolicyRemark() { return policyRemark; }
    public void setPolicyRemark(String policyRemark) { this.policyRemark = policyRemark; }
    public BigDecimal getFee3dsRate() { return fee3dsRate; }
    public void setFee3dsRate(BigDecimal fee3dsRate) { this.fee3dsRate = fee3dsRate != null ? fee3dsRate : BigDecimal.ZERO; }
    public BigDecimal getChargebackFeePerTx() { return chargebackFeePerTx; }
    public void setChargebackFeePerTx(BigDecimal chargebackFeePerTx) { this.chargebackFeePerTx = chargebackFeePerTx != null ? chargebackFeePerTx : BigDecimal.ZERO; }
    public Long getChargebackPolicyId() { return chargebackPolicyId; }
    public void setChargebackPolicyId(Long chargebackPolicyId) { this.chargebackPolicyId = chargebackPolicyId; }

    public String getExtraFee1Name() { return extraFee1Name; }
    public void setExtraFee1Name(String extraFee1Name) { this.extraFee1Name = extraFee1Name; }
    public String getExtraFee1Mode() { return extraFee1Mode; }
    public void setExtraFee1Mode(String extraFee1Mode) { this.extraFee1Mode = extraFee1Mode; }
    public BigDecimal getExtraFee1Value() { return extraFee1Value; }
    public void setExtraFee1Value(BigDecimal extraFee1Value) { this.extraFee1Value = extraFee1Value; }

    public String getExtraFee2Name() { return extraFee2Name; }
    public void setExtraFee2Name(String extraFee2Name) { this.extraFee2Name = extraFee2Name; }
    public String getExtraFee2Mode() { return extraFee2Mode; }
    public void setExtraFee2Mode(String extraFee2Mode) { this.extraFee2Mode = extraFee2Mode; }
    public BigDecimal getExtraFee2Value() { return extraFee2Value; }
    public void setExtraFee2Value(BigDecimal extraFee2Value) { this.extraFee2Value = extraFee2Value; }

    public String getExtraFee3Name() { return extraFee3Name; }
    public void setExtraFee3Name(String extraFee3Name) { this.extraFee3Name = extraFee3Name; }
    public String getExtraFee3Mode() { return extraFee3Mode; }
    public void setExtraFee3Mode(String extraFee3Mode) { this.extraFee3Mode = extraFee3Mode; }
    public BigDecimal getExtraFee3Value() { return extraFee3Value; }
    public void setExtraFee3Value(BigDecimal extraFee3Value) { this.extraFee3Value = extraFee3Value; }

    public String getExtraFee4Name() { return extraFee4Name; }
    public void setExtraFee4Name(String extraFee4Name) { this.extraFee4Name = extraFee4Name; }
    public String getExtraFee4Mode() { return extraFee4Mode; }
    public void setExtraFee4Mode(String extraFee4Mode) { this.extraFee4Mode = extraFee4Mode; }
    public BigDecimal getExtraFee4Value() { return extraFee4Value; }
    public void setExtraFee4Value(BigDecimal extraFee4Value) { this.extraFee4Value = extraFee4Value; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
