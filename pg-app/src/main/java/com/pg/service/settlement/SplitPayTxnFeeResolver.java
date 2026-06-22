package com.pg.service.settlement;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;
import com.pg.repository.SplitPayContractRepository;
import com.pg.repository.SplitPayInstallmentRepository;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PercentDecimalHelper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 분할결제 회차({@link SplitPayInstallment})와 연결된 거래의 분할 %·고정 수수료.
 * 계약 생성 시 스냅샷·회차별 {@code fee_pct_amount}/{@code fee_fixed_amount}를 우선합니다.
 */
@Component
public class SplitPayTxnFeeResolver {

    private final SplitPayInstallmentRepository installmentRepository;
    private final SplitPayContractRepository contractRepository;

    public SplitPayTxnFeeResolver(SplitPayInstallmentRepository installmentRepository,
                                  SplitPayContractRepository contractRepository) {
        this.installmentRepository = installmentRepository;
        this.contractRepository = contractRepository;
    }

    public record InstallmentCache(
            Map<String, SplitPayInstallment> byOrderNo,
            Map<String, SplitPayInstallment> byPgTrnId,
            Map<Long, SplitPayContract> contractById
    ) {
        public static InstallmentCache empty() {
            return new InstallmentCache(Map.of(), Map.of(), Map.of());
        }
    }

    public record SplitPayFeeAmounts(
            double pctFee,
            double fixedFee,
            String pctRatePlain,
            String fixedPerInstPlain
    ) {
        static SplitPayFeeAmounts zero() {
            return new SplitPayFeeAmounts(0d, 0d, "", "");
        }
    }

    public InstallmentCache buildCache(Collection<PgTrnsctn> txns) {
        if (txns == null || txns.isEmpty()) {
            return InstallmentCache.empty();
        }
        Set<String> orderNos = new HashSet<>();
        Set<String> trnIds = new HashSet<>();
        for (PgTrnsctn t : txns) {
            if (t == null) {
                continue;
            }
            if (t.getOrderNo() != null && !t.getOrderNo().isBlank()) {
                orderNos.add(t.getOrderNo().trim());
            }
            if (t.getTrnId() != null && !t.getTrnId().isBlank()) {
                trnIds.add(t.getTrnId().trim());
            }
        }
        Map<String, SplitPayInstallment> byOrderNo = new HashMap<>();
        Map<String, SplitPayInstallment> byPgTrnId = new HashMap<>();
        Set<Long> contractIds = new HashSet<>();
        if (!orderNos.isEmpty()) {
            for (SplitPayInstallment i : installmentRepository.findByOrderNoIn(orderNos)) {
                if (i.getOrderNo() != null && !i.getOrderNo().isBlank()) {
                    byOrderNo.putIfAbsent(i.getOrderNo().trim(), i);
                }
                if (i.getContractId() != null) {
                    contractIds.add(i.getContractId());
                }
            }
        }
        if (!trnIds.isEmpty()) {
            for (SplitPayInstallment i : installmentRepository.findByPgTrnIdIn(trnIds)) {
                if (i.getPgTrnId() != null && !i.getPgTrnId().isBlank()) {
                    byPgTrnId.putIfAbsent(i.getPgTrnId().trim(), i);
                }
                if (i.getContractId() != null) {
                    contractIds.add(i.getContractId());
                }
            }
        }
        Map<Long, SplitPayContract> contractById = new HashMap<>();
        if (!contractIds.isEmpty()) {
            for (SplitPayContract c : contractRepository.findAllById(contractIds)) {
                contractById.put(c.getId(), c);
            }
        }
        return new InstallmentCache(byOrderNo, byPgTrnId, contractById);
    }

    public SplitPayFeeAmounts resolve(PgTrnsctn t,
                                      CommissionPolicy pol,
                                      BigDecimal amountBd,
                                      FeeListRoundingPolicy rp,
                                      InstallmentCache cache) {
        Optional<SplitPayInstallment> instOpt = findInstallment(t, cache);
        if (instOpt.isEmpty()) {
            return SplitPayFeeAmounts.zero();
        }
        SplitPayInstallment inst = instOpt.get();
        SplitPayContract contract = null;
        if (cache != null && inst.getContractId() != null) {
            contract = cache.contractById().get(inst.getContractId());
        }
        if (contract == null && inst.getContractId() != null) {
            contract = contractRepository.findById(inst.getContractId()).orElse(null);
        }

        BigDecimal pctRate = contract != null && contract.getSnapSplitPayFeePct() != null
                ? contract.getSnapSplitPayFeePct()
                : (pol != null && pol.getSplitPayFeePct() != null ? pol.getSplitPayFeePct() : BigDecimal.ZERO);
        BigDecimal fixedPerInst = contract != null && contract.getSnapSplitFixedPerInst() != null
                ? contract.getSnapSplitFixedPerInst()
                : (pol != null && pol.getSplitPayFixedFeePerInst() != null ? pol.getSplitPayFixedFeePerInst() : BigDecimal.ZERO);

        BigDecimal pctAmt = inst.getFeePctAmount();
        if (pctAmt == null) {
            pctAmt = calcPctFee(amountBd, pctRate, rp);
        } else {
            pctAmt = FeeListRoundingPolicy.round(pctAmt, rp);
        }

        BigDecimal fixedAmt = inst.getFeeFixedAmount();
        if (fixedAmt == null) {
            fixedAmt = BigDecimal.ZERO;
        } else {
            fixedAmt = FeeListRoundingPolicy.round(fixedAmt, rp);
        }

        String pctPlain = pctRate.signum() > 0 ? PercentDecimalHelper.toPlainOneDecimal(pctRate) : "";
        String fixedPlain = fixedPerInst.signum() > 0 ? PercentDecimalHelper.toPlainAmountOneDecimal(fixedPerInst) : "";

        return new SplitPayFeeAmounts(
                pctAmt.doubleValue(),
                fixedAmt.doubleValue(),
                pctPlain,
                fixedPlain
        );
    }

    private Optional<SplitPayInstallment> findInstallment(PgTrnsctn t, InstallmentCache cache) {
        if (t == null) {
            return Optional.empty();
        }
        if (cache != null) {
            if (t.getOrderNo() != null && !t.getOrderNo().isBlank()) {
                SplitPayInstallment byOrder = cache.byOrderNo().get(t.getOrderNo().trim());
                if (byOrder != null) {
                    return Optional.of(byOrder);
                }
            }
            if (t.getTrnId() != null && !t.getTrnId().isBlank()) {
                SplitPayInstallment byTrn = cache.byPgTrnId().get(t.getTrnId().trim());
                if (byTrn != null) {
                    return Optional.of(byTrn);
                }
            }
            return Optional.empty();
        }
        if (t.getOrderNo() != null && !t.getOrderNo().isBlank()) {
            Optional<SplitPayInstallment> o = installmentRepository.findByOrderNo(t.getOrderNo().trim());
            if (o.isPresent()) {
                return o;
            }
        }
        if (t.getTrnId() != null && !t.getTrnId().isBlank()) {
            return installmentRepository.findByPgTrnId(t.getTrnId().trim());
        }
        return Optional.empty();
    }

    private static BigDecimal calcPctFee(BigDecimal amount, BigDecimal pct, FeeListRoundingPolicy rp) {
        if (amount == null || pct == null || pct.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        int scale = rp != null ? rp.decimalPlaces() : 4;
        RoundingMode rm = rp != null ? rp.roundMode() : RoundingMode.HALF_UP;
        return FeeListRoundingPolicy.round(
                amount.multiply(pct).divide(BigDecimal.valueOf(100), scale, rm), rp);
    }
}
