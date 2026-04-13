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

    /**
     * 출금제한 유형: DAILY(매일), HOLIDAY(공휴일), EVE_HOLIDAY_17, EVE_HOLIDAY_18, NONE(미사용) 등.
     * 공휴일·전영업일 판단은 본사 영업일 설정과 연동해 출금 API·배치에서 해석합니다.
     */
    @Column(name = "withdraw_restrict_type", length = 32)
    private String withdrawRestrictType;

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

    /** 정산주기 코드: RT,T0 / M5,M10,M30 / TM5,TM10,TM30(T0식 당일합산+격자) / H1..H12 / TH1..TH12 / D0~D90 / W+N / WK 등 */
    @Column(name = "calc_cycle", length = 64)
    private String calcCycle;

    /** 정산 마감시간 */
    @Column(name = "calc_close_time")
    private LocalTime calcCloseTime;

    /** 정산구분: MANUAL(수동), AUTO(자동), FUMBANKING(펌뱅킹) */
    @Column(name = "calc_proc_type", length = 20)
    private String calcProcType = "MANUAL";

    /** 이체및송금구분: MANUAL, AUTO, AUTO_NO_MANUAL, ARBITRARY, NONE(사용안함) 등 */
    @Column(name = "transfer_type", length = 32)
    private String transferType;

    /** 이체주기 (일) */
    @Column(name = "transfer_cycle_days")
    private Integer transferCycleDays;

    /** 자동이체 최소금액 (원) — 이체및송금 최소금액(펌뱅킹) */
    @Column(name = "auto_transfer_min", precision = 18, scale = 0)
    private BigDecimal autoTransferMin;

    /** 정산 최소금액(원) — 미만이면 해당 주기 정산 연기 */
    @Column(name = "calc_min_amt", precision = 18, scale = 0)
    private BigDecimal calcMinAmt;

    /** 이체·송금 실행 시각(펌뱅킹 연동) */
    @Column(name = "transfer_exec_time")
    private LocalTime transferExecTime;

    /** 수수료 부가세(VAT) 적용 여부 — Y일 때 fee_vat_rate_pct 로 수수료 합에 부가세 산정 */
    @Column(name = "fee_vat_apply_yn", length = 1, nullable = false)
    private String feeVatApplyYn = "N";

    /** 수수료 부가세율(%) — 10이면 수수료의 10% */
    @Column(name = "fee_vat_rate_pct", precision = 7, scale = 4, nullable = false)
    private BigDecimal feeVatRatePct = BigDecimal.ZERO;

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

    /** 월한도_결제 (원) */
    @Column(name = "pay_limit_month", precision = 18, scale = 0)
    private BigDecimal payLimitMonth;

    /** 연한도_결제 (원) */
    @Column(name = "pay_limit_year", precision = 18, scale = 0)
    private BigDecimal payLimitYear;

    /** 제한시(시)_결제 - 출금제한 시간(시) */
    @Column(name = "withdraw_limit_hour")
    private Integer withdrawLimitHour;

    /** 시간내결제금액 (원) */
    @Column(name = "pay_amount_in_time", precision = 18, scale = 0)
    private BigDecimal payAmountInTime;

    /** 동일카드제한 일(WEB) */
    @Column(name = "same_card_limit_day_web")
    private Integer sameCardLimitDayWeb;

    /** 동일카드제한 회(WEB) */
    @Column(name = "same_card_limit_cnt_web")
    private Integer sameCardLimitCntWeb;

    /** 동일카드제한 원(WEB) */
    @Column(name = "same_card_limit_amt_web", precision = 18, scale = 0)
    private BigDecimal sameCardLimitAmtWeb;

    /** 동일카드제한 일(단말) */
    @Column(name = "same_card_limit_day_terminal")
    private Integer sameCardLimitDayTerminal;

    /** 동일카드제한 회(단말) */
    @Column(name = "same_card_limit_cnt_terminal")
    private Integer sameCardLimitCntTerminal;

    /** 동일카드제한 원(단말) */
    @Column(name = "same_card_limit_amt_terminal", precision = 18, scale = 0)
    private BigDecimal sameCardLimitAmtTerminal;

    /** 일 지급한도 (원) */
    @Column(name = "pay_limit_daily", precision = 18, scale = 0)
    private BigDecimal payLimitDaily;

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
    public String getWithdrawRestrictType() { return withdrawRestrictType; }
    public void setWithdrawRestrictType(String withdrawRestrictType) { this.withdrawRestrictType = withdrawRestrictType; }
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
    public String getCalcProcType() { return calcProcType; }
    public void setCalcProcType(String calcProcType) { this.calcProcType = calcProcType; }
    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }
    public Integer getTransferCycleDays() { return transferCycleDays; }
    public void setTransferCycleDays(Integer transferCycleDays) { this.transferCycleDays = transferCycleDays; }
    public BigDecimal getAutoTransferMin() { return autoTransferMin; }
    public void setAutoTransferMin(BigDecimal autoTransferMin) { this.autoTransferMin = autoTransferMin; }
    public BigDecimal getCalcMinAmt() { return calcMinAmt; }
    public void setCalcMinAmt(BigDecimal calcMinAmt) { this.calcMinAmt = calcMinAmt; }
    public LocalTime getTransferExecTime() { return transferExecTime; }
    public void setTransferExecTime(LocalTime transferExecTime) { this.transferExecTime = transferExecTime; }
    public String getFeeVatApplyYn() { return feeVatApplyYn; }
    public void setFeeVatApplyYn(String feeVatApplyYn) { this.feeVatApplyYn = feeVatApplyYn; }
    public BigDecimal getFeeVatRatePct() { return feeVatRatePct; }
    public void setFeeVatRatePct(BigDecimal feeVatRatePct) { this.feeVatRatePct = feeVatRatePct; }
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
    public BigDecimal getPayLimitMonth() { return payLimitMonth; }
    public void setPayLimitMonth(BigDecimal payLimitMonth) { this.payLimitMonth = payLimitMonth; }
    public BigDecimal getPayLimitYear() { return payLimitYear; }
    public void setPayLimitYear(BigDecimal payLimitYear) { this.payLimitYear = payLimitYear; }
    public Integer getWithdrawLimitHour() { return withdrawLimitHour; }
    public void setWithdrawLimitHour(Integer withdrawLimitHour) { this.withdrawLimitHour = withdrawLimitHour; }
    public BigDecimal getPayAmountInTime() { return payAmountInTime; }
    public void setPayAmountInTime(BigDecimal payAmountInTime) { this.payAmountInTime = payAmountInTime; }
    public Integer getSameCardLimitDayWeb() { return sameCardLimitDayWeb; }
    public void setSameCardLimitDayWeb(Integer sameCardLimitDayWeb) { this.sameCardLimitDayWeb = sameCardLimitDayWeb; }
    public Integer getSameCardLimitCntWeb() { return sameCardLimitCntWeb; }
    public void setSameCardLimitCntWeb(Integer sameCardLimitCntWeb) { this.sameCardLimitCntWeb = sameCardLimitCntWeb; }
    public BigDecimal getSameCardLimitAmtWeb() { return sameCardLimitAmtWeb; }
    public void setSameCardLimitAmtWeb(BigDecimal sameCardLimitAmtWeb) { this.sameCardLimitAmtWeb = sameCardLimitAmtWeb; }
    public Integer getSameCardLimitDayTerminal() { return sameCardLimitDayTerminal; }
    public void setSameCardLimitDayTerminal(Integer sameCardLimitDayTerminal) { this.sameCardLimitDayTerminal = sameCardLimitDayTerminal; }
    public Integer getSameCardLimitCntTerminal() { return sameCardLimitCntTerminal; }
    public void setSameCardLimitCntTerminal(Integer sameCardLimitCntTerminal) { this.sameCardLimitCntTerminal = sameCardLimitCntTerminal; }
    public BigDecimal getSameCardLimitAmtTerminal() { return sameCardLimitAmtTerminal; }
    public void setSameCardLimitAmtTerminal(BigDecimal sameCardLimitAmtTerminal) { this.sameCardLimitAmtTerminal = sameCardLimitAmtTerminal; }
    public BigDecimal getPayLimitDaily() { return payLimitDaily; }
    public void setPayLimitDaily(BigDecimal payLimitDaily) { this.payLimitDaily = payLimitDaily; }
}
