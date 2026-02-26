package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 가맹점별 수수료 A/B/D/E형 (등록 시 입력, 수수료관리에서 조회/수정)
 * A: 계좌활성화, 연회비, 기술서비스료 / B: 취소,무효,실패,언패이드 / D: 건당정산, USDT, FX / E: 환불, 차지백경고
 */
@Entity
@Table(name = "tb_merchant_commission_extra")
public class MerchantCommissionExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false, unique = true)
    private Long orgUnitId;

    /** A형: 계좌활성화 수수료(원) */
    @Column(name = "fee_account_activation", precision = 12, scale = 0)
    private BigDecimal feeAccountActivation;

    /** A형: 연회비(원) */
    @Column(name = "fee_annual", precision = 12, scale = 0)
    private BigDecimal feeAnnual;

    /** A형: 기술서비스료(원) */
    @Column(name = "fee_tech_service", precision = 12, scale = 0)
    private BigDecimal feeTechService;

    /** B형: 취소 수수료(원/건 또는 %) */
    @Column(name = "fee_cancel", precision = 12, scale = 2)
    private BigDecimal feeCancel;

    /** B형: 무효 수수료 */
    @Column(name = "fee_invalid", precision = 12, scale = 2)
    private BigDecimal feeInvalid;

    /** B형: 실패 수수료 */
    @Column(name = "fee_fail", precision = 12, scale = 2)
    private BigDecimal feeFail;

    /** B형: 언패이드 수수료 */
    @Column(name = "fee_unpaid", precision = 12, scale = 2)
    private BigDecimal feeUnpaid;

    /** D형: 건당 정산수수료(원) */
    @Column(name = "fee_settlement_per_tx", precision = 12, scale = 0)
    private BigDecimal feeSettlementPerTx;

    /** D형: USDT 변환 수수료 */
    @Column(name = "fee_usdt", precision = 12, scale = 2)
    private BigDecimal feeUsdt;

    /** D형: FX 수수료 */
    @Column(name = "fee_fx", precision = 12, scale = 2)
    private BigDecimal feeFx;

    /** E형: 환불 수수료 */
    @Column(name = "fee_refund", precision = 12, scale = 2)
    private BigDecimal feeRefund;

    /** E형: 차지백 경고 수수료 */
    @Column(name = "fee_chargeback_warn", precision = 12, scale = 2)
    private BigDecimal feeChargebackWarn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public BigDecimal getFeeAccountActivation() { return feeAccountActivation; }
    public void setFeeAccountActivation(BigDecimal feeAccountActivation) { this.feeAccountActivation = feeAccountActivation; }
    public BigDecimal getFeeAnnual() { return feeAnnual; }
    public void setFeeAnnual(BigDecimal feeAnnual) { this.feeAnnual = feeAnnual; }
    public BigDecimal getFeeTechService() { return feeTechService; }
    public void setFeeTechService(BigDecimal feeTechService) { this.feeTechService = feeTechService; }
    public BigDecimal getFeeCancel() { return feeCancel; }
    public void setFeeCancel(BigDecimal feeCancel) { this.feeCancel = feeCancel; }
    public BigDecimal getFeeInvalid() { return feeInvalid; }
    public void setFeeInvalid(BigDecimal feeInvalid) { this.feeInvalid = feeInvalid; }
    public BigDecimal getFeeFail() { return feeFail; }
    public void setFeeFail(BigDecimal feeFail) { this.feeFail = feeFail; }
    public BigDecimal getFeeUnpaid() { return feeUnpaid; }
    public void setFeeUnpaid(BigDecimal feeUnpaid) { this.feeUnpaid = feeUnpaid; }
    public BigDecimal getFeeSettlementPerTx() { return feeSettlementPerTx; }
    public void setFeeSettlementPerTx(BigDecimal feeSettlementPerTx) { this.feeSettlementPerTx = feeSettlementPerTx; }
    public BigDecimal getFeeUsdt() { return feeUsdt; }
    public void setFeeUsdt(BigDecimal feeUsdt) { this.feeUsdt = feeUsdt; }
    public BigDecimal getFeeFx() { return feeFx; }
    public void setFeeFx(BigDecimal feeFx) { this.feeFx = feeFx; }
    public BigDecimal getFeeRefund() { return feeRefund; }
    public void setFeeRefund(BigDecimal feeRefund) { this.feeRefund = feeRefund; }
    public BigDecimal getFeeChargebackWarn() { return feeChargebackWarn; }
    public void setFeeChargebackWarn(BigDecimal feeChargebackWarn) { this.feeChargebackWarn = feeChargebackWarn; }
}
