package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 결제대행사 등록(근원원가) — ICOPAY↔PG 계약 원가. 결제대행사 설정이 이 행을 참조한다.
 * 동일 벤더라도 청구통화가 다르면 원가 행을 여러 개 둘 수 있다.
 */
@Entity
@Table(name = "tb_pg_agency_cost_policy")
public class PgAgencyCostPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대행사명 표시명 (예: PAYSO JPY) */
    @Column(name = "cost_nm", length = 100)
    private String costNm;

    /** 계약대행사(벤더) 표시명 (예: PAYSO) */
    @Column(name = "vendor_nm", length = 100)
    private String vendorNm;

    /** 원가 결제코드(고유 키). 설정 pg_cd 와 다를 수 있음 */
    @Column(name = "pg_cd", nullable = false, unique = true, length = 20)
    private String pgCd;

    @Column(name = "per_tx_fee", precision = 12, scale = 1)
    private BigDecimal perTxFee = BigDecimal.ZERO;

    @Column(name = "cancel_rate", precision = 12, scale = 1)
    private BigDecimal cancelRate = BigDecimal.ZERO;

    @Column(name = "usage_rate", precision = 12, scale = 1)
    private BigDecimal usageRate = BigDecimal.ZERO;

    @Column(name = "fail_fee", precision = 12, scale = 1)
    private BigDecimal failFee = BigDecimal.ZERO;

    /** 카드수수료(%) — 통합(UNIFIED) 모드 시 사용 */
    @Column(name = "pay_rate", precision = 5, scale = 2)
    private BigDecimal payRate = BigDecimal.ZERO;

    /** UNIFIED=통합 카드수수료 / PER_BRAND=브랜드별(0=해당 브랜드 미사용) */
    @Column(name = "card_fee_mode", nullable = false, length = 16)
    private String cardFeeMode = "UNIFIED";

    @Column(name = "fee_card_visa", precision = 5, scale = 2)
    private BigDecimal feeCardVisa = BigDecimal.ZERO;
    @Column(name = "fee_card_master", precision = 5, scale = 2)
    private BigDecimal feeCardMaster = BigDecimal.ZERO;
    @Column(name = "fee_card_jcb", precision = 5, scale = 2)
    private BigDecimal feeCardJcb = BigDecimal.ZERO;
    @Column(name = "fee_card_amex", precision = 5, scale = 2)
    private BigDecimal feeCardAmex = BigDecimal.ZERO;
    @Column(name = "fee_card_union", precision = 5, scale = 2)
    private BigDecimal feeCardUnion = BigDecimal.ZERO;
    @Column(name = "fee_card_diners", precision = 5, scale = 2)
    private BigDecimal feeCardDiners = BigDecimal.ZERO;

    @Column(name = "refund_rate", precision = 12, scale = 0)
    private BigDecimal refundRate = BigDecimal.ZERO;

    @Column(name = "void_fee_per_tx", precision = 12, scale = 0)
    private BigDecimal voidFeePerTx = BigDecimal.ZERO;

    /** 수동무효(21) 수무효수수료 */
    @Column(name = "hand_void_fee_per_tx", precision = 12, scale = 1)
    private BigDecimal handVoidFeePerTx = BigDecimal.ZERO;

    /** 이메일무효(22·41) 이무효수수료 */
    @Column(name = "manual_void_fee_per_tx", precision = 12, scale = 0)
    private BigDecimal manualVoidFeePerTx = BigDecimal.ZERO;

    /** 수동환불(30) 수환불수수료 */
    @Column(name = "hand_refund_fee_per_tx", precision = 12, scale = 1)
    private BigDecimal handRefundFeePerTx = BigDecimal.ZERO;

    @Column(name = "fee_settlement_per_tx", precision = 12, scale = 0)
    private BigDecimal feeSettlementPerTx = BigDecimal.ZERO;

    @Column(name = "remittance_transfer_fee", precision = 12, scale = 1)
    private BigDecimal remittanceTransferFee = BigDecimal.ZERO;

    @Column(name = "usdt_transfer_fee_usd", precision = 12, scale = 1)
    private BigDecimal usdtTransferFeeUsd = BigDecimal.ZERO;

    @Column(name = "fee_usdt", precision = 12, scale = 2)
    private BigDecimal feeUsdt = BigDecimal.ZERO;

    @Column(name = "fee_fx", precision = 12, scale = 2)
    private BigDecimal feeFx = BigDecimal.ZERO;

    @Column(name = "rolling_pct", precision = 5, scale = 2)
    private BigDecimal rollingPct = BigDecimal.ZERO;

    @Column(name = "rolling_days")
    private Integer rollingDays = 0;

    /** 청구통화(실결제·건당 금액 단위) */
    @Column(name = "currency_code", length = 16)
    private String currencyCode = "KRW";

    /** 표시통화(멀티 Y일 때 청구와 다를 수 있음) */
    @Column(name = "display_currency", length = 16)
    private String displayCurrency;

    /** Y=결제사 멀티(표시≠청구 가능), N=청구통화만 */
    @Column(name = "multi_yn", nullable = false, length = 1)
    private String multiYn = "N";

    /** 계약 부가세율(%) — 수수료는 VAT제외 기준으로 저장, VAT포함은 표시 환산 */
    @Column(name = "vat_rate_pct", precision = 5, scale = 2)
    private BigDecimal vatRatePct = BigDecimal.ZERO;

    /** APM 카드 수수료(%) */
    @Column(name = "fee_apm_crypto", precision = 5, scale = 2)
    private BigDecimal feeApmCrypto = BigDecimal.ZERO;
    @Column(name = "fee_apm_paypay", precision = 5, scale = 2)
    private BigDecimal feeApmPaypay = BigDecimal.ZERO;
    @Column(name = "fee_apm_rakuten", precision = 5, scale = 2)
    private BigDecimal feeApmRakuten = BigDecimal.ZERO;
    @Column(name = "fee_apm_promptpay", precision = 5, scale = 2)
    private BigDecimal feeApmPromptpay = BigDecimal.ZERO;
    @Column(name = "fee_apm_line", precision = 5, scale = 2)
    private BigDecimal feeApmLine = BigDecimal.ZERO;
    @Column(name = "fee_apm_kakao", precision = 5, scale = 2)
    private BigDecimal feeApmKakao = BigDecimal.ZERO;
    @Column(name = "fee_apm_wechat", precision = 5, scale = 2)
    private BigDecimal feeApmWechat = BigDecimal.ZERO;

    @Column(name = "policy_remark", columnDefinition = "TEXT")
    private String policyRemark;

    @Column(name = "fee_3ds_rate", precision = 12, scale = 1)
    private BigDecimal fee3dsRate = BigDecimal.ZERO;

    @Column(name = "chargeback_fee_per_tx", precision = 12, scale = 1)
    private BigDecimal chargebackFeePerTx = BigDecimal.ZERO;

    @Column(name = "chargeback_policy_id")
    private Long chargebackPolicyId;

    @Column(name = "void_settlement_mode", length = 16)
    private String voidSettlementMode;

    @Column(name = "manual_void_settlement_mode", length = 16)
    private String manualVoidSettlementMode;

    @Column(name = "refund_settlement_mode", length = 16)
    private String refundSettlementMode;

    @Column(name = "force_refund_settlement_mode", length = 16)
    private String forceRefundSettlementMode;

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

    /** TRANSACTION 고정 — 결제 시각 기준 정산 */
    @Column(name = "settle_basis", nullable = false, length = 20)
    private String settleBasis = "TRANSACTION";

    /** T=영업일 N일·동일 시각, H=24×N시간, D=달력 N일·일괄 시각 */
    @Column(name = "settle_schedule_type", nullable = false, length = 8)
    private String settleScheduleType = "T";

    @Column(name = "settle_lag_n", nullable = false)
    private Integer settleLagN = 1;

    @Column(name = "settle_batch_time")
    private LocalTime settleBatchTime;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

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
    public String getCostNm() { return costNm; }
    public void setCostNm(String costNm) { this.costNm = costNm; }
    public String getVendorNm() { return vendorNm; }
    public void setVendorNm(String vendorNm) { this.vendorNm = vendorNm; }
    public String getPgCd() { return pgCd; }
    public void setPgCd(String pgCd) { this.pgCd = pgCd; }
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

    public String getCardFeeMode() { return cardFeeMode; }
    public void setCardFeeMode(String cardFeeMode) {
        this.cardFeeMode = cardFeeMode != null && !cardFeeMode.isBlank() ? cardFeeMode.trim().toUpperCase() : "UNIFIED";
    }
    public BigDecimal getFeeCardVisa() { return feeCardVisa; }
    public void setFeeCardVisa(BigDecimal feeCardVisa) { this.feeCardVisa = feeCardVisa != null ? feeCardVisa : BigDecimal.ZERO; }
    public BigDecimal getFeeCardMaster() { return feeCardMaster; }
    public void setFeeCardMaster(BigDecimal feeCardMaster) { this.feeCardMaster = feeCardMaster != null ? feeCardMaster : BigDecimal.ZERO; }
    public BigDecimal getFeeCardJcb() { return feeCardJcb; }
    public void setFeeCardJcb(BigDecimal feeCardJcb) { this.feeCardJcb = feeCardJcb != null ? feeCardJcb : BigDecimal.ZERO; }
    public BigDecimal getFeeCardAmex() { return feeCardAmex; }
    public void setFeeCardAmex(BigDecimal feeCardAmex) { this.feeCardAmex = feeCardAmex != null ? feeCardAmex : BigDecimal.ZERO; }
    public BigDecimal getFeeCardUnion() { return feeCardUnion; }
    public void setFeeCardUnion(BigDecimal feeCardUnion) { this.feeCardUnion = feeCardUnion != null ? feeCardUnion : BigDecimal.ZERO; }
    public BigDecimal getFeeCardDiners() { return feeCardDiners; }
    public void setFeeCardDiners(BigDecimal feeCardDiners) { this.feeCardDiners = feeCardDiners != null ? feeCardDiners : BigDecimal.ZERO; }
    public BigDecimal getRefundRate() { return refundRate; }
    public void setRefundRate(BigDecimal refundRate) { this.refundRate = refundRate; }
    public BigDecimal getVoidFeePerTx() { return voidFeePerTx; }
    public void setVoidFeePerTx(BigDecimal voidFeePerTx) { this.voidFeePerTx = voidFeePerTx != null ? voidFeePerTx : BigDecimal.ZERO; }
    public BigDecimal getHandVoidFeePerTx() { return handVoidFeePerTx; }
    public void setHandVoidFeePerTx(BigDecimal handVoidFeePerTx) { this.handVoidFeePerTx = handVoidFeePerTx != null ? handVoidFeePerTx : BigDecimal.ZERO; }
    public BigDecimal getManualVoidFeePerTx() { return manualVoidFeePerTx; }
    public void setManualVoidFeePerTx(BigDecimal manualVoidFeePerTx) { this.manualVoidFeePerTx = manualVoidFeePerTx != null ? manualVoidFeePerTx : BigDecimal.ZERO; }
    public BigDecimal getHandRefundFeePerTx() { return handRefundFeePerTx; }
    public void setHandRefundFeePerTx(BigDecimal handRefundFeePerTx) { this.handRefundFeePerTx = handRefundFeePerTx != null ? handRefundFeePerTx : BigDecimal.ZERO; }
    public BigDecimal getFeeSettlementPerTx() { return feeSettlementPerTx; }
    public void setFeeSettlementPerTx(BigDecimal feeSettlementPerTx) { this.feeSettlementPerTx = feeSettlementPerTx; }
    public BigDecimal getRemittanceTransferFee() { return remittanceTransferFee; }
    public void setRemittanceTransferFee(BigDecimal remittanceTransferFee) { this.remittanceTransferFee = remittanceTransferFee != null ? remittanceTransferFee : BigDecimal.ZERO; }
    public BigDecimal getUsdtTransferFeeUsd() { return usdtTransferFeeUsd; }
    public void setUsdtTransferFeeUsd(BigDecimal usdtTransferFeeUsd) { this.usdtTransferFeeUsd = usdtTransferFeeUsd != null ? usdtTransferFeeUsd : BigDecimal.ZERO; }
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
    public String getDisplayCurrency() { return displayCurrency; }
    public void setDisplayCurrency(String displayCurrency) { this.displayCurrency = displayCurrency; }
    public String getMultiYn() { return multiYn; }
    public void setMultiYn(String multiYn) { this.multiYn = multiYn != null ? multiYn : "N"; }
    public BigDecimal getVatRatePct() { return vatRatePct; }
    public void setVatRatePct(BigDecimal vatRatePct) { this.vatRatePct = vatRatePct != null ? vatRatePct : BigDecimal.ZERO; }
    public BigDecimal getFeeApmCrypto() { return feeApmCrypto; }
    public void setFeeApmCrypto(BigDecimal feeApmCrypto) { this.feeApmCrypto = feeApmCrypto != null ? feeApmCrypto : BigDecimal.ZERO; }
    public BigDecimal getFeeApmPaypay() { return feeApmPaypay; }
    public void setFeeApmPaypay(BigDecimal feeApmPaypay) { this.feeApmPaypay = feeApmPaypay != null ? feeApmPaypay : BigDecimal.ZERO; }
    public BigDecimal getFeeApmRakuten() { return feeApmRakuten; }
    public void setFeeApmRakuten(BigDecimal feeApmRakuten) { this.feeApmRakuten = feeApmRakuten != null ? feeApmRakuten : BigDecimal.ZERO; }
    public BigDecimal getFeeApmPromptpay() { return feeApmPromptpay; }
    public void setFeeApmPromptpay(BigDecimal feeApmPromptpay) { this.feeApmPromptpay = feeApmPromptpay != null ? feeApmPromptpay : BigDecimal.ZERO; }
    public BigDecimal getFeeApmLine() { return feeApmLine; }
    public void setFeeApmLine(BigDecimal feeApmLine) { this.feeApmLine = feeApmLine != null ? feeApmLine : BigDecimal.ZERO; }
    public BigDecimal getFeeApmKakao() { return feeApmKakao; }
    public void setFeeApmKakao(BigDecimal feeApmKakao) { this.feeApmKakao = feeApmKakao != null ? feeApmKakao : BigDecimal.ZERO; }
    public BigDecimal getFeeApmWechat() { return feeApmWechat; }
    public void setFeeApmWechat(BigDecimal feeApmWechat) { this.feeApmWechat = feeApmWechat != null ? feeApmWechat : BigDecimal.ZERO; }
    public String getPolicyRemark() { return policyRemark; }
    public void setPolicyRemark(String policyRemark) { this.policyRemark = policyRemark; }
    public BigDecimal getFee3dsRate() { return fee3dsRate; }
    public void setFee3dsRate(BigDecimal fee3dsRate) { this.fee3dsRate = fee3dsRate != null ? fee3dsRate : BigDecimal.ZERO; }
    public BigDecimal getChargebackFeePerTx() { return chargebackFeePerTx; }
    public void setChargebackFeePerTx(BigDecimal chargebackFeePerTx) { this.chargebackFeePerTx = chargebackFeePerTx != null ? chargebackFeePerTx : BigDecimal.ZERO; }
    public Long getChargebackPolicyId() { return chargebackPolicyId; }
    public void setChargebackPolicyId(Long chargebackPolicyId) { this.chargebackPolicyId = chargebackPolicyId; }
    public String getVoidSettlementMode() { return voidSettlementMode; }
    public void setVoidSettlementMode(String voidSettlementMode) { this.voidSettlementMode = voidSettlementMode; }
    public String getManualVoidSettlementMode() { return manualVoidSettlementMode; }
    public void setManualVoidSettlementMode(String manualVoidSettlementMode) { this.manualVoidSettlementMode = manualVoidSettlementMode; }
    public String getRefundSettlementMode() { return refundSettlementMode; }
    public void setRefundSettlementMode(String refundSettlementMode) { this.refundSettlementMode = refundSettlementMode; }
    public String getForceRefundSettlementMode() { return forceRefundSettlementMode; }
    public void setForceRefundSettlementMode(String forceRefundSettlementMode) { this.forceRefundSettlementMode = forceRefundSettlementMode; }
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
    public String getSettleBasis() { return settleBasis; }
    public void setSettleBasis(String settleBasis) { this.settleBasis = settleBasis != null ? settleBasis : "TRANSACTION"; }
    public String getSettleScheduleType() { return settleScheduleType; }
    public void setSettleScheduleType(String settleScheduleType) { this.settleScheduleType = settleScheduleType != null ? settleScheduleType : "T"; }
    public Integer getSettleLagN() { return settleLagN; }
    public void setSettleLagN(Integer settleLagN) { this.settleLagN = settleLagN != null ? settleLagN : 1; }
    public LocalTime getSettleBatchTime() { return settleBatchTime; }
    public void setSettleBatchTime(LocalTime settleBatchTime) { this.settleBatchTime = settleBatchTime; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn != null ? useYn : "Y"; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
