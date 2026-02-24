package com.pg.service;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.RollingReserve;
import com.pg.entity.SettlementRun;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.RollingReserveRepository;
import com.pg.repository.SettlementRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 정산 수학 로직: 결제 데이터 → 수수료 차감(건당/취소/이용/실패/결제/환불) → 롤링(담보금 N% N일 보류) → 지급액
 */
@Service
public class SettlementCalcService {

    private final PgTrnsctnRepository trnsctnRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final RollingReserveRepository rollingReserveRepository;

    public SettlementCalcService(PgTrnsctnRepository trnsctnRepository,
                                 CommissionPolicyRepository commissionPolicyRepository,
                                 SettlementRunRepository settlementRunRepository,
                                 RollingReserveRepository rollingReserveRepository) {
        this.trnsctnRepository = trnsctnRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.rollingReserveRepository = rollingReserveRepository;
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
        for (String mid : merchantIds) {
            List<PgTrnsctn> txList = list.stream().filter(t -> mid.equals(t.getMerchantId())).collect(Collectors.toList());
            SettlementRun run = calcOne(mid, fromDate, txList);
            if (run != null) {
                settlementRunRepository.save(run);
                results.add(run);
            }
        }
        return results;
    }

    private SettlementRun calcOne(String merchantId, LocalDate calcDt, List<PgTrnsctn> txList) {
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
        int payCnt = 0;
        int cancelCnt = 0;
        for (PgTrnsctn t : txList) {
            BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
            if ("10".equals(t.getStatus())) {
                approveAmt = approveAmt.add(amt);
                payCnt++;
            } else if ("20".equals(t.getStatus())) {
                cancelAmt = cancelAmt.add(amt);
                cancelCnt++;
            }
        }
        BigDecimal netSales = approveAmt.subtract(cancelAmt);
        if (netSales.compareTo(BigDecimal.ZERO) <= 0 && payCnt == 0 && cancelCnt == 0) {
            return null;
        }
        BigDecimal perTxFee = policy.getPerTxFee() != null ? policy.getPerTxFee() : BigDecimal.ZERO;
        BigDecimal cancelRate = policy.getCancelRate() != null ? policy.getCancelRate() : BigDecimal.ZERO;
        BigDecimal usageRate = policy.getUsageRate() != null ? policy.getUsageRate() : BigDecimal.ZERO;
        BigDecimal payRate = policy.getPayRate() != null ? policy.getPayRate() : BigDecimal.ZERO;
        BigDecimal refundRate = policy.getRefundRate() != null ? policy.getRefundRate() : BigDecimal.ZERO;
        BigDecimal failFee = policy.getFailFee() != null ? policy.getFailFee() : BigDecimal.ZERO;
        BigDecimal rollingPct = policy.getRollingPct() != null ? policy.getRollingPct() : BigDecimal.ZERO;
        int rollingDays = policy.getRollingDays() != null ? policy.getRollingDays() : 0;

        BigDecimal feePerTx = perTxFee.multiply(BigDecimal.valueOf(payCnt)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal feePayRate = approveAmt.multiply(payRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal feeCancelRate = cancelAmt.multiply(cancelRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HAL_UP);
        BigDecimal feeRefundRate = cancelAmt.multiply(refundRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HAL_UP);
        BigDecimal feeUsage = netSales.multiply(usageRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal totalFee = feePerTx.add(feePayRate).add(feeCancelRate).add(feeRefundRate).add(feeUsage).setScale(0, RoundingMode.HAL_UP);

        BigDecimal rollingReserveAmt = BigDecimal.ZERO;
        if (rollingDays > 0 && rollingPct.compareTo(BigDecimal.ZERO) > 0) {
            rollingReserveAmt = netSales.multiply(rollingPct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HAL_UP);
            for (PgTrnsctn t : txList) {
                if (!"10".equals(t.getStatus())) continue;
                BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
                BigDecimal reserve = amt.multiply(rollingPct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HAL_UP);
                if (reserve.compareTo(BigDecimal.ZERO) > 0) {
                    RollingReserve rr = new RollingReserve();
                    rr.setTrnId(t.getTrnId());
                    rr.setMerchantId(merchantId);
                    rr.setReserveAmt(reserve);
                    rr.setRollingPct(rollingPct);
                    rr.setReleaseDate(calcDt.plusDays(rollingDays));
                    rr.setStatus("HOLD");
                    rollingReserveRepository.save(rr);
                }
            }
        }

        BigDecimal payAmt = netSales.subtract(totalFee).subtract(rollingReserveAmt).setScale(0, RoundingMode.HAL_UP);
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
