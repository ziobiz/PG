package com.pg.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 정산 기본설정 (가맹점 등록 시 설정, 정산실행에서 참조)
 * 출금제한, 지급한도, 보류율, 영업일, 정산주기, 이체옵션 등
 */
@Entity
@Table(name = "tb_settlement_setting")
public class SettlementSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false, unique = true)
    private Long orgUnitId;

    /** 출금제한일 (일) */
    @Column(name = "withdraw_limit_days")
    private Integer withdrawLimitDays;

    /** 출금제한 시작시간 */
    @Column(name = "withdraw_start_time")
    private LocalTime withdrawStartTime;

    /** 출금제한 종료시간 */
    @Column(name = "withdraw_end_time")
    private LocalTime withdrawEndTime;

    /** 기본 지급한도 (원) */
    @Column(name = "pay_limit_default", precision = 18, scale = 0)
    private BigDecimal payLimitDefault;

    /** 추가 지급한도 (원) */
    @Column(name = "pay_limit_extra", precision = 18, scale = 0)
    private BigDecimal payLimitExtra;

    /** 출금한도 알림 SMS 여부 */
    @Column(name = "pay_limit_alert_sms", length = 1)
    private String payLimitAlertSms = "N";

    /** 보류율 본사정책 따름 Y/N (Y면 본사 수수료정책 롤링 비율/일수 사용) */
    @Column(name = "hold_rate_follow_hq", length = 1)
    private String holdRateFollowHq = "Y";

    /** 보류율 (%) - holdRateFollowHq=N일 때 사용 */
    @Column(name = "hold_rate", precision = 5, scale = 2)
    private BigDecimal holdRate;

    /** 보류기간 (일) - holdRateFollowHq=N일 때 사용 */
    @Column(name = "hold_days")
    private Integer holdDays;

    /** 정산주기 코드: D3,D5,D7,D10,D15,D20,D30 / W5,W7,W10,W14,WEEKLY */
    @Column(name = "calc_cycle", length = 20)
    private String calcCycle;

    /** 정산 마감시간 */
    @Column(name = "calc_close_time")
    private LocalTime calcCloseTime;

    /** 이체구분: MANUAL, AUTO, FUMBANKING */
    @Column(name = "transfer_type", length = 20)
    private String transferType;

    /** 이체주기 (일) */
    @Column(name = "transfer_cycle_days")
    private Integer transferCycleDays;

    /** 자동이체 최소금액 (원) */
    @Column(name = "auto_transfer_min", precision = 18, scale = 0)
    private BigDecimal autoTransferMin;

    /** 지급보류 여부 */
    @Column(name = "pay_hold_yn", length = 1)
    private String payHoldYn = "N";

    /** 정산제외일 (쉼표 구분 문자열) */
    @Column(name = "calc_exclude_dates", length = 200)
    private String calcExcludeDates;

    /** 정산개시시간 (정산제외대상 활성 시 해당일 정산 시작시간) */
    @Column(name = "calc_start_time")
    private LocalTime calcStartTime;

    /** 정산제외대상: NONE, WEB, OFFLINE (주말/공휴일 등 제외일 시 제한할 결제수단) */
    @Column(name = "calc_exclude_target", length = 20)
    private String calcExcludeTarget;

    /** 정산제외 사용여부 (주말·공휴일 제외) */
    @Column(name = "calc_exclude_yn", length = 1)
    private String calcExcludeYn = "N";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public Integer getWithdrawLimitDays() { return withdrawLimitDays; }
    public void setWithdrawLimitDays(Integer withdrawLimitDays) { this.withdrawLimitDays = withdrawLimitDays; }
    public LocalTime getWithdrawStartTime() { return withdrawStartTime; }
    public void setWithdrawStartTime(LocalTime withdrawStartTime) { this.withdrawStartTime = withdrawStartTime; }
    public LocalTime getWithdrawEndTime() { return withdrawEndTime; }
    public void setWithdrawEndTime(LocalTime withdrawEndTime) { this.withdrawEndTime = withdrawEndTime; }
    public BigDecimal getPayLimitDefault() { return payLimitDefault; }
    public void setPayLimitDefault(BigDecimal payLimitDefault) { this.payLimitDefault = payLimitDefault; }
    public BigDecimal getPayLimitExtra() { return payLimitExtra; }
    public void setPayLimitExtra(BigDecimal payLimitExtra) { this.payLimitExtra = payLimitExtra; }
    public String getPayLimitAlertSms() { return payLimitAlertSms; }
    public void setPayLimitAlertSms(String payLimitAlertSms) { this.payLimitAlertSms = payLimitAlertSms; }
    public String getHoldRateFollowHq() { return holdRateFollowHq; }
    public void setHoldRateFollowHq(String holdRateFollowHq) { this.holdRateFollowHq = holdRateFollowHq; }
    public BigDecimal getHoldRate() { return holdRate; }
    public void setHoldRate(BigDecimal holdRate) { this.holdRate = holdRate; }
    public Integer getHoldDays() { return holdDays; }
    public void setHoldDays(Integer holdDays) { this.holdDays = holdDays; }
    public String getCalcCycle() { return calcCycle; }
    public void setCalcCycle(String calcCycle) { this.calcCycle = calcCycle; }
    public LocalTime getCalcCloseTime() { return calcCloseTime; }
    public void setCalcCloseTime(LocalTime calcCloseTime) { this.calcCloseTime = calcCloseTime; }
    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }
    public Integer getTransferCycleDays() { return transferCycleDays; }
    public void setTransferCycleDays(Integer transferCycleDays) { this.transferCycleDays = transferCycleDays; }
    public BigDecimal getAutoTransferMin() { return autoTransferMin; }
    public void setAutoTransferMin(BigDecimal autoTransferMin) { this.autoTransferMin = autoTransferMin; }
    public String getPayHoldYn() { return payHoldYn; }
    public void setPayHoldYn(String payHoldYn) { this.payHoldYn = payHoldYn; }
    public String getCalcExcludeDates() { return calcExcludeDates; }
    public void setCalcExcludeDates(String calcExcludeDates) { this.calcExcludeDates = calcExcludeDates; }
    public LocalTime getCalcStartTime() { return calcStartTime; }
    public void setCalcStartTime(LocalTime calcStartTime) { this.calcStartTime = calcStartTime; }
    public String getCalcExcludeTarget() { return calcExcludeTarget; }
    public void setCalcExcludeTarget(String calcExcludeTarget) { this.calcExcludeTarget = calcExcludeTarget; }
    public String getCalcExcludeYn() { return calcExcludeYn; }
    public void setCalcExcludeYn(String calcExcludeYn) { this.calcExcludeYn = calcExcludeYn; }
}
