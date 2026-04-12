package com.pg.service.settlement;

import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantReceivable;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementRecovery;
import com.pg.entity.SettlementSetting;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.MerchantReceivableRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementRecoveryRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.util.FeeListRoundingPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 환수금(정산 후 환불 등 자동)·미수금(수동) 등록 및 정산 지급액에서의 FIFO 차감.
 */
@Service
public class SettlementArrearsService {

    private static final String REASON_POST_SETTLE_REFUND = "POST_SETTLE_REFUND";
    private static final List<String> OPEN_RECOVERY = List.of("PENDING", "PARTIAL");
    private static final List<String> OPEN_RECEIVABLE = List.of("PENDING", "PARTIAL");

    private final SettlementRecoveryRepository settlementRecoveryRepository;
    private final MerchantReceivableRepository merchantReceivableRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;

    public SettlementArrearsService(SettlementRecoveryRepository settlementRecoveryRepository,
                                  MerchantReceivableRepository merchantReceivableRepository,
                                  SettlementRunRepository settlementRunRepository,
                                  FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator,
                                  CommissionPolicyRepository commissionPolicyRepository,
                                  OrgUnitRepository orgUnitRepository,
                                  SettlementSettingRepository settlementSettingRepository,
                                  HqApiConfigRepository hqApiConfigRepository,
                                  HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository) {
        this.settlementRecoveryRepository = settlementRecoveryRepository;
        this.merchantReceivableRepository = merchantReceivableRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.feeListTxnBreakdownCalculator = feeListTxnBreakdownCalculator;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
    }

    private FeeListRoundingPolicy feeListRoundingPolicy() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(FeeListRoundingPolicy::fromSettings)
                .orElseGet(FeeListRoundingPolicy::defaults);
    }

    private static boolean isRecallTriggerStatus(String st) {
        if (st == null) {
            return false;
        }
        return switch (st.trim()) {
            case "20", "21", "22", "30", "31" -> true;
            default -> false;
        };
    }

    /**
     * 승인(10)이 정산 반영(settled Y)된 뒤 취소·환불 등으로 바뀐 경우 자동 환수금 행을 만듭니다.
     */
    @Transactional
    public void registerPostSettlementRecoveryIfDue(String prevStatus, String prevSettledYn, PgTrnsctn saved) {
        if (!"10".equals(prevStatus != null ? prevStatus.trim() : "")) {
            return;
        }
        if (!"Y".equalsIgnoreCase(String.valueOf(prevSettledYn != null ? prevSettledYn : "").trim())) {
            return;
        }
        if (saved == null || saved.getTrnId() == null || saved.getTrnId().isBlank()
                || saved.getMerchantId() == null || saved.getMerchantId().isBlank()) {
            return;
        }
        String newSt = saved.getStatus() != null ? saved.getStatus().trim() : "";
        if (!isRecallTriggerStatus(newSt)) {
            return;
        }
        String tid = saved.getTrnId().trim();
        if (settlementRecoveryRepository.existsByTrnIdAndReasonCode(tid, REASON_POST_SETTLE_REFUND)) {
            return;
        }
        String mid = saved.getMerchantId().trim();
        CommissionPolicy pol = commissionPolicyRepository.findByScope(mid)
                .orElseGet(() -> commissionPolicyRepository.findByScope("DEFAULT").orElseGet(CommissionPolicy::new));
        OrgUnit ou = orgUnitRepository.findByCode(mid).orElse(null);
        SettlementSetting feeVatSs = ou != null ? settlementSettingRepository.findByOrgUnitId(ou.getId()).orElse(null) : null;
        FeeListRoundingPolicy rp = feeListRoundingPolicy();
        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                saved, mid, pol, monthCbCountCache, tiersByPolicyId, feeVatSs, rp);
        BigDecimal feeAmtBd = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), rp);
        BigDecimal feeVatBd = br.feeVatBd();
        BigDecimal txnAmtBd = saved.getAmtKrw() != null ? saved.getAmtKrw() : BigDecimal.ZERO;
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        boolean recallInc = c != null && "Y".equalsIgnoreCase(c.getRecallIncludeFeeYn());
        BigDecimal recallBd = recallInc
                ? txnAmtBd.add(feeAmtBd).add(feeVatBd).max(BigDecimal.ZERO)
                : txnAmtBd.max(BigDecimal.ZERO);
        long recall = Math.max(0, recallBd.setScale(0, RoundingMode.HALF_UP).longValue());
        if (recall <= 0) {
            return;
        }
        SettlementRecovery r = new SettlementRecovery();
        r.setMerchantId(mid);
        r.setTrnId(tid.length() > 20 ? tid.substring(0, 20) : tid);
        r.setRecallAmount(recall);
        r.setRemainingAmount(recall);
        r.setAppliedAmount(0L);
        r.setStatus("PENDING");
        r.setReasonCode(REASON_POST_SETTLE_REFUND);
        r.setFeeIncludedYn(recallInc ? "Y" : "N");
        r.setVatAppliedYn(br.feeVatBd().signum() > 0 ? "Y" : "N");
        r.setMemo("정산 반영 후 후속 상태(" + newSt + ")");
        settlementRecoveryRepository.save(r);
    }

    /**
     * 정산 실행 저장 직후: 지급액에서 환수금 FIFO, 이어서 미수금 FIFO 차감.
     */
    @Transactional
    public void applyArrearsToSettledRun(SettlementRun run) {
        if (run == null || run.getId() == null) {
            return;
        }
        BigDecimal payBd = run.getPayAmt() != null ? run.getPayAmt() : BigDecimal.ZERO;
        long payLong = payBd.longValue();
        if (payLong <= 0) {
            return;
        }
        String mid = run.getMerchantId();
        if (mid == null || mid.isBlank()) {
            return;
        }
        long takeR = applyFifoRecoveries(mid.trim(), run.getId(), payLong);
        payLong = Math.max(0, payLong - takeR);
        long takeM = applyFifoReceivables(mid.trim(), run.getId(), payLong);
        payLong = Math.max(0, payLong - takeM);
        run.setPayAmt(BigDecimal.valueOf(payLong));
        settlementRunRepository.save(run);
    }

    private long applyFifoRecoveries(String merchantId, Long settlementRunId, long cap) {
        if (cap <= 0) {
            return 0;
        }
        List<SettlementRecovery> rows = settlementRecoveryRepository.findByMerchantIdAndStatusInOrderByIdAsc(merchantId, OPEN_RECOVERY);
        long used = 0;
        List<SettlementRecovery> dirty = new ArrayList<>();
        for (SettlementRecovery r : rows) {
            long rem = r.getRemainingAmount() != null ? r.getRemainingAmount() : 0L;
            if (rem <= 0) {
                continue;
            }
            long room = cap - used;
            if (room <= 0) {
                break;
            }
            long take = Math.min(rem, room);
            if (take <= 0) {
                continue;
            }
            r.setRemainingAmount(rem - take);
            r.setAppliedAmount((r.getAppliedAmount() != null ? r.getAppliedAmount() : 0L) + take);
            r.setLastAppliedSettlementRunId(settlementRunId);
            if (r.getRemainingAmount() == 0) {
                r.setStatus("CLOSED");
            } else {
                r.setStatus("PARTIAL");
            }
            dirty.add(r);
            used += take;
        }
        if (!dirty.isEmpty()) {
            settlementRecoveryRepository.saveAll(dirty);
        }
        return used;
    }

    private long applyFifoReceivables(String merchantId, Long settlementRunId, long cap) {
        if (cap <= 0) {
            return 0;
        }
        List<MerchantReceivable> rows = merchantReceivableRepository.findByMerchantIdAndStatusInOrderByIdAsc(merchantId, OPEN_RECEIVABLE);
        long used = 0;
        List<MerchantReceivable> dirty = new ArrayList<>();
        for (MerchantReceivable r : rows) {
            long rem = r.getRemainingAmount() != null ? r.getRemainingAmount() : 0L;
            if (rem <= 0) {
                continue;
            }
            long room = cap - used;
            if (room <= 0) {
                break;
            }
            long take = Math.min(rem, room);
            if (take <= 0) {
                continue;
            }
            r.setRemainingAmount(rem - take);
            r.setAppliedAmount((r.getAppliedAmount() != null ? r.getAppliedAmount() : 0L) + take);
            if (r.getRemainingAmount() == 0) {
                r.setStatus("CLOSED");
            } else {
                r.setStatus("PARTIAL");
            }
            dirty.add(r);
            used += take;
        }
        if (!dirty.isEmpty()) {
            merchantReceivableRepository.saveAll(dirty);
        }
        return used;
    }

    @Transactional
    public MerchantReceivable createReceivable(String merchantId, long amount, String title, String reasonCode, String memo, String createdBy) {
        if (merchantId == null || merchantId.isBlank() || amount <= 0) {
            throw new IllegalArgumentException("merchantId·amount");
        }
        MerchantReceivable r = new MerchantReceivable();
        r.setMerchantId(merchantId.trim());
        r.setTitle(title != null && !title.isBlank() ? title.trim() : "미수금");
        r.setTotalAmount(amount);
        r.setRemainingAmount(amount);
        r.setAppliedAmount(0L);
        r.setStatus("PENDING");
        r.setReasonCode(reasonCode != null && !reasonCode.isBlank() ? reasonCode.trim() : "MANUAL");
        r.setMemo(memo);
        r.setCreatedBy(createdBy);
        return merchantReceivableRepository.save(r);
    }

    @Transactional
    public void writeOffReceivable(long id) {
        MerchantReceivable r = merchantReceivableRepository.findById(id).orElseThrow();
        if ("CLOSED".equals(r.getStatus()) || "CANCELLED".equals(r.getStatus())) {
            return;
        }
        r.setStatus("WRITE_OFF");
        r.setRemainingAmount(0L);
        merchantReceivableRepository.save(r);
    }

    @Transactional
    public void cancelReceivable(long id) {
        MerchantReceivable r = merchantReceivableRepository.findById(id).orElseThrow();
        if (!"PENDING".equals(r.getStatus())) {
            throw new IllegalStateException("대기 상태만 취소할 수 있습니다.");
        }
        r.setStatus("CANCELLED");
        r.setRemainingAmount(0L);
        merchantReceivableRepository.save(r);
    }

    public List<SettlementRecovery> listRecoveriesForMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return List.of();
        }
        return settlementRecoveryRepository.findByMerchantIdOrderByIdDesc(merchantId.trim());
    }

    public List<MerchantReceivable> listReceivablesForMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return List.of();
        }
        return merchantReceivableRepository.findByMerchantIdOrderByIdDesc(merchantId.trim());
    }
}
