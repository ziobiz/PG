package com.pg.service;

import com.pg.entity.ChargebackFeePolicy;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.RollingReserve;
import com.pg.entity.SettlementRun;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.RollingReserveRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementSetting;
import com.pg.util.BusinessDayCalendar;
import com.pg.util.ChargebackTierResolver;
import com.pg.util.CommissionExtraFeeUtil;
import com.pg.util.MerchantFeeVatUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 정산 수학 로직: 결제 데이터 → 수수료 차감(건당·정산·차지백·실패·취소·무효·수동무효·환불·이용·결제·USDT·FX %·3DS 건당 고정·롤링) → 롤링(담보금 N% N일 보류) → 지급액
 */
@Service
public class SettlementCalcService {

    private static final List<String> CHARGEBACK_STATUSES = List.of("30", "31");

    private final PgTrnsctnRepository trnsctnRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final RollingReserveRepository rollingReserveRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;

    public SettlementCalcService(PgTrnsctnRepository trnsctnRepository,
                                 CommissionPolicyRepository commissionPolicyRepository,
                                 ChargebackFeePolicyRepository chargebackFeePolicyRepository,
                                 SettlementRunRepository settlementRunRepository,
                                 RollingReserveRepository rollingReserveRepository,
                                 SettlementSettingRepository settlementSettingRepository,
                                 OrgUnitRepository orgUnitRepository,
                                 OrgServiceUseService orgServiceUseService) {
        this.trnsctnRepository = trnsctnRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.rollingReserveRepository = rollingReserveRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
    }

    public List<SettlementRun> listRuns(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) fromDate = LocalDate.now().minusYears(1);
        if (toDate == null) toDate = LocalDate.now();
        return settlementRunRepository.findByCalcDtBetweenOrderByMerchantId(fromDate, toDate);
    }

    public CommissionPolicy getPolicy(String merchantId) {
        if (merchantId != null && !merchantId.isEmpty()) {
            return commissionPolicyRepository.findByScope(merchantId)
                    .orElseGet(() -> commissionPolicyRepository.findByScope("DEFAULT").orElse(null));
        }
        return commissionPolicyRepository.findByScope("DEFAULT").orElse(null);
    }

    /**
     * 정산 실행: 기간 내 거래 합산 → 수수료 차감 → 롤링 보류 → 지급액 계산 및 저장
     */
    @Transactional
    public List<SettlementRun> execute(LocalDate fromDate, LocalDate toDate, String merchantId) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);
        List<PgTrnsctn> list = trnsctnRepository.findForSettlement(merchantId, from, to);
        List<String> merchantIds = list.stream().map(PgTrnsctn::getMerchantId).distinct().collect(Collectors.toList());
        List<SettlementRun> results = new ArrayList<>();
        /* 정산 기준일: 기간 종료일(해지일 비교·롤링 해지일 계산에 사용) */
        LocalDate calcDt = toDate;
        for (String mid : merchantIds) {
            List<PgTrnsctn> txList = list.stream().filter(t -> mid.equals(t.getMerchantId())).collect(Collectors.toList());
            SettlementRun run = calcOne(mid, calcDt, txList);
            if (run != null) {
                settlementRunRepository.save(run);
                results.add(run);
            }
        }
        appendReleaseOnlyMerchants(calcDt, merchantId, results);
        return results;
    }

    /**
     * 기간 내 거래는 없으나, 종료일 기준 해지 대상 담보가 있는 가맹은 지급액에 반환만 하는 정산 행 생성.
     */
    private void appendReleaseOnlyMerchants(LocalDate calcDt, String merchantIdFilter, List<SettlementRun> results) {
        Set<String> done = results.stream().map(SettlementRun::getMerchantId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<RollingReserve> due = rollingReserveRepository.findByStatusAndReleaseDateLessThanEqual("HOLD", calcDt);
        Set<String> candidates = due.stream().map(RollingReserve::getMerchantId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (merchantIdFilter != null && !merchantIdFilter.isBlank()) {
            candidates.removeIf(m -> !merchantIdFilter.trim().equals(m));
        }
        candidates.removeAll(done);
        for (String mid : candidates) {
            SettlementRun run = calcOne(mid, calcDt, Collections.emptyList());
            if (run != null) {
                settlementRunRepository.save(run);
                results.add(run);
            }
        }
    }

    private SettlementRun calcOne(String merchantId, LocalDate calcDt, List<PgTrnsctn> txList) {
        if (merchantId != null && !orgServiceUseService.isOrgServiceActiveByCompCode(merchantId)) {
            return null;
        }
        if (merchantId != null && !merchantId.isBlank()) {
            Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantId.trim());
            if (ou.isPresent()) {
                Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ou.get().getId());
                if (ssOpt.isPresent() && "NONE".equalsIgnoreCase(ssOpt.get().getCalcCycle())) {
                    return null;
                }
            }
        }
        CommissionPolicy policy = getPolicy(merchantId);
        if (policy == null) {
            policy = new CommissionPolicy();
            policy.setScope("DEFAULT");
            policy.setPayRate(new BigDecimal("2.5"));
            policy.setRollingPct(BigDecimal.ZERO);
            policy.setRollingDays(0);
        }
        BigDecimal approveAmt = BigDecimal.ZERO;
        BigDecimal cancelAmt = BigDecimal.ZERO;
        BigDecimal voidAmt = BigDecimal.ZERO;
        BigDecimal manualVoidAmt = BigDecimal.ZERO;
        BigDecimal refundAmt = BigDecimal.ZERO;
        int payCnt = 0;
        int cancelCnt = 0;
        int voidCnt = 0;
        int manualVoidCnt = 0;
        int refundCnt = 0;
        int failCnt = 0;
        int txCount = txList.size();
        for (PgTrnsctn t : txList) {
            BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            if ("10".equals(st)) {
                approveAmt = approveAmt.add(amt);
                payCnt++;
            } else if ("20".equals(st)) {
                cancelAmt = cancelAmt.add(amt);
                cancelCnt++;
            } else if ("21".equals(st)) {
                voidAmt = voidAmt.add(amt);
                voidCnt++;
            } else if ("22".equals(st)) {
                manualVoidAmt = manualVoidAmt.add(amt);
                manualVoidCnt++;
            } else if ("30".equals(st) || "31".equals(st)) {
                refundAmt = refundAmt.add(amt);
                refundCnt++;
            } else if ("F0".equals(st) || "99".equals(st)) {
                failCnt++;
            }
        }
        BigDecimal netSales = approveAmt.subtract(cancelAmt).subtract(voidAmt).subtract(manualVoidAmt);

        /* 해지일이 도래한 담보금(롤링) → 이번 정산 지급액에 합산 후 RELEASED 처리 */
        List<RollingReserve> maturing = rollingReserveRepository.findByMerchantIdAndStatusAndReleaseDateLessThanEqual(
                merchantId, "HOLD", calcDt);
        BigDecimal releasedFromReserve = BigDecimal.ZERO;
        LocalDateTime releaseStamp = LocalDateTime.now();
        if (!maturing.isEmpty()) {
            for (RollingReserve rr : maturing) {
                if (rr.getReserveAmt() != null) {
                    releasedFromReserve = releasedFromReserve.add(rr.getReserveAmt());
                }
                rr.setStatus("RELEASED");
                rr.setReleasedAt(releaseStamp);
            }
            rollingReserveRepository.saveAll(maturing);
        }

        if (txList.isEmpty() && releasedFromReserve.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal perTxFee = policy.getPerTxFee() != null ? policy.getPerTxFee() : BigDecimal.ZERO;
        BigDecimal cancelRate = policy.getCancelRate() != null ? policy.getCancelRate() : BigDecimal.ZERO;
        BigDecimal voidFeePerTx = policy.getVoidFeePerTx() != null ? policy.getVoidFeePerTx() : BigDecimal.ZERO;
        BigDecimal manualVoidFeePerTx = policy.getManualVoidFeePerTx() != null ? policy.getManualVoidFeePerTx() : BigDecimal.ZERO;
        BigDecimal usageRate = policy.getUsageRate() != null ? policy.getUsageRate() : BigDecimal.ZERO;
        BigDecimal payRate = policy.getPayRate() != null ? policy.getPayRate() : BigDecimal.ZERO;
        BigDecimal refundRate = policy.getRefundRate() != null ? policy.getRefundRate() : BigDecimal.ZERO;
        BigDecimal failFee = policy.getFailFee() != null ? policy.getFailFee() : BigDecimal.ZERO;
        BigDecimal[] rollingPctRef = new BigDecimal[]{ policy.getRollingPct() != null ? policy.getRollingPct() : BigDecimal.ZERO };
        int[] rollingDaysRef = new int[]{ policy.getRollingDays() != null ? policy.getRollingDays() : 0 };
        orgUnitRepository.findByCode(merchantId).ifPresent(ou ->
                settlementSettingRepository.findByOrgUnitId(ou.getId()).ifPresent(ss -> {
                    if ("N".equalsIgnoreCase(ss.getHoldRateFollowHq() != null ? ss.getHoldRateFollowHq().trim() : "")) {
                        if (ss.getHoldRate() != null && ss.getHoldRate().compareTo(BigDecimal.ZERO) > 0) rollingPctRef[0] = ss.getHoldRate();
                        if (ss.getHoldDays() != null && ss.getHoldDays() > 0) rollingDaysRef[0] = ss.getHoldDays();
                    }
                }));
        BigDecimal rollingPct = rollingPctRef[0];
        int rollingDays = rollingDaysRef[0];

        BigDecimal feePerTx = perTxFee.multiply(BigDecimal.valueOf(txCount)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal feePayRate = approveAmt.multiply(payRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        /* 취소·무효·수동무효·환불: 건당 고정액 × 해당 건수 */
        BigDecimal feeCancelRate = cancelRate.multiply(BigDecimal.valueOf(cancelCnt)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal feeVoidPerTx = voidFeePerTx.multiply(BigDecimal.valueOf(voidCnt)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal feeManualVoidPerTx = manualVoidFeePerTx.multiply(BigDecimal.valueOf(manualVoidCnt)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal feeRefundRate = refundRate.multiply(BigDecimal.valueOf(refundCnt)).setScale(0, RoundingMode.HALF_UP);
        YearMonth ym = YearMonth.from(calcDt);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        long runsAlreadyThisMonth = settlementRunRepository.countByMerchantIdAndCalcDtBetween(merchantId, monthStart, monthEnd);
        boolean chargeMonthlyUsage = usageRate.compareTo(BigDecimal.ZERO) > 0 && runsAlreadyThisMonth == 0;
        BigDecimal feeUsage = chargeMonthlyUsage ? usageRate.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal feeFailTotal = failFee.multiply(BigDecimal.valueOf(failCnt)).setScale(0, RoundingMode.HALF_UP);

        BigDecimal feeSettlementPerTxBd = policy.getFeeSettlementPerTx() != null ? policy.getFeeSettlementPerTx() : BigDecimal.ZERO;
        BigDecimal feeSettlementTotal = feeSettlementPerTxBd.multiply(BigDecimal.valueOf(txCount)).setScale(0, RoundingMode.HALF_UP);

        long chargebackBatchCnt = txList.stream()
                .map(PgTrnsctn::getStatus)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(st -> "30".equals(st) || "31".equals(st))
                .count();
        BigDecimal feeChargebackTotal = BigDecimal.ZERO;
        if (chargebackBatchCnt > 0) {
            BigDecimal perCase;
            Long cbPolId = policy.getChargebackPolicyId();
            if (cbPolId != null) {
                Optional<ChargebackFeePolicy> cbOpt = chargebackFeePolicyRepository.findByIdWithTiers(cbPolId);
                if (cbOpt.isPresent() && cbOpt.get().getTiers() != null && !cbOpt.get().getTiers().isEmpty()) {
                    LocalDateTime monthStartDt = ym.atDay(1).atStartOfDay();
                    LocalDateTime nextMonthStartDt = ym.plusMonths(1).atDay(1).atStartOfDay();
                    long monthCbCount = trnsctnRepository.countByMerchantIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            merchantId, CHARGEBACK_STATUSES, monthStartDt, nextMonthStartDt);
                    int mc = (int) Math.min(monthCbCount, Integer.MAX_VALUE);
                    perCase = ChargebackTierResolver.feePerCaseForMonthlyCount(mc, cbOpt.get().getTiers());
                } else {
                    perCase = policy.getChargebackFeePerTx() != null ? policy.getChargebackFeePerTx() : BigDecimal.ZERO;
                }
            } else {
                perCase = policy.getChargebackFeePerTx() != null ? policy.getChargebackFeePerTx() : BigDecimal.ZERO;
            }
            feeChargebackTotal = perCase.multiply(BigDecimal.valueOf(chargebackBatchCnt)).setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal feeUsdtBd = policy.getFeeUsdt() != null ? policy.getFeeUsdt() : BigDecimal.ZERO;
        BigDecimal feeFxBd = policy.getFeeFx() != null ? policy.getFeeFx() : BigDecimal.ZERO;
        BigDecimal fee3dsFixedPerTx = policy.getFee3dsRate() != null ? policy.getFee3dsRate() : BigDecimal.ZERO;
        BigDecimal extraRateOnApprove = feeUsdtBd.add(feeFxBd);
        BigDecimal feeUsdtFxPctSum = BigDecimal.ZERO;
        BigDecimal fee3dsFixedSum = BigDecimal.ZERO;
        BigDecimal feeExtraPctSum = BigDecimal.ZERO;
        for (PgTrnsctn t : txList) {
            String st = t.getStatus() != null ? t.getStatus().trim() : "";
            if (!"10".equals(st)) continue;
            BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
            if (extraRateOnApprove.signum() > 0 && amt.signum() > 0) {
                feeUsdtFxPctSum = feeUsdtFxPctSum.add(
                        amt.multiply(extraRateOnApprove).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP));
            }
            if (fee3dsFixedPerTx.signum() > 0 && amt.signum() > 0) {
                fee3dsFixedSum = fee3dsFixedSum.add(fee3dsFixedPerTx);
            }
            feeExtraPctSum = feeExtraPctSum.add(CommissionExtraFeeUtil.sumPctOnApprovedAmount(policy, amt));
        }
        fee3dsFixedSum = fee3dsFixedSum.setScale(0, RoundingMode.HALF_UP);

        BigDecimal feeExtraFix = CommissionExtraFeeUtil.sumFixedForSettlement(policy);

        BigDecimal totalFee = feePerTx.add(feePayRate).add(feeCancelRate).add(feeVoidPerTx).add(feeManualVoidPerTx).add(feeRefundRate).add(feeUsage)
                .add(feeFailTotal).add(feeSettlementTotal).add(feeChargebackTotal).add(feeUsdtFxPctSum).add(fee3dsFixedSum)
                .add(feeExtraPctSum).add(feeExtraFix)
                .setScale(0, RoundingMode.HALF_UP);

        SettlementSetting feeVatSs = null;
        if (merchantId != null && !merchantId.isBlank()) {
            feeVatSs = orgUnitRepository.findByCode(merchantId.trim())
                    .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                    .orElse(null);
        }
        BigDecimal feeVatAmt = MerchantFeeVatUtil.vatOnFeeAmount(totalFee, feeVatSs, 0);

        BigDecimal rollingReserveAmt = BigDecimal.ZERO;
        if (rollingDays > 0 && rollingPct.compareTo(BigDecimal.ZERO) > 0) {
            rollingReserveAmt = netSales.multiply(rollingPct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            for (PgTrnsctn t : txList) {
                if (!"10".equals(t.getStatus())) continue;
                BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
                BigDecimal reserve = amt.multiply(rollingPct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (reserve.compareTo(BigDecimal.ZERO) > 0) {
                    RollingReserve rr = new RollingReserve();
                    rr.setTrnId(t.getTrnId());
                    rr.setMerchantId(merchantId);
                    rr.setReserveAmt(reserve);
                    rr.setRollingPct(rollingPct);
                    rr.setHoldStartDate(calcDt);
                    rr.setHoldBusinessDays(rollingDays);
                    rr.setReleaseDate(BusinessDayCalendar.addBusinessDays(calcDt, rollingDays, Collections.emptySet()));
                    rr.setStatus("HOLD");
                    rollingReserveRepository.save(rr);
                }
            }
        }

        BigDecimal payAmt = netSales.subtract(totalFee).subtract(feeVatAmt).subtract(rollingReserveAmt).add(releasedFromReserve).setScale(0, RoundingMode.HALF_UP);
        if (payAmt.compareTo(BigDecimal.ZERO) < 0) payAmt = BigDecimal.ZERO;

        SettlementRun run = new SettlementRun();
        run.setCalcDt(calcDt);
        run.setMerchantId(merchantId);
        run.setApproveAmt(approveAmt);
        run.setCancelAmt(cancelAmt);
        run.setTotalFee(totalFee);
        run.setRollingReserveAmt(rollingReserveAmt);
        run.setPayAmt(payAmt);
        run.setStatus("CALCULATED");
        return run;
    }
}
