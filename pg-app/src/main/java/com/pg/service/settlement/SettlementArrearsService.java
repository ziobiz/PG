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
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayDisplayCurrency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    /** 정산관리·수수료내역과 동일: 전산설정 기준통화 + 통화별 소수 규칙 */
    private FeeListRoundingPolicy settlementLedgerRoundPolicy() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(s -> FeeCurrencyRoundResolver.from(s).forCurrency(PayDisplayCurrency.alphaFromSettings(s)))
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
        FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
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
        BigDecimal recall = FeeListRoundingPolicy.round(recallBd.max(BigDecimal.ZERO), rp);
        if (recall.signum() <= 0) {
            return;
        }
        SettlementRecovery r = new SettlementRecovery();
        r.setMerchantId(mid);
        r.setTrnId(tid.length() > 20 ? tid.substring(0, 20) : tid);
        r.setRecallAmount(recall);
        r.setRemainingAmount(recall);
        r.setAppliedAmount(BigDecimal.ZERO);
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
        FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
        BigDecimal payBd = FeeListRoundingPolicy.round(run.getPayAmt() != null ? run.getPayAmt() : BigDecimal.ZERO, rp);
        if (payBd.signum() <= 0) {
            return;
        }
        String mid = run.getMerchantId();
        if (mid == null || mid.isBlank()) {
            return;
        }
        BigDecimal takeR = applyFifoRecoveries(mid.trim(), run.getId(), payBd);
        BigDecimal payAfterR = payBd.subtract(takeR).max(BigDecimal.ZERO);
        BigDecimal takeM = applyFifoReceivables(mid.trim(), run.getId(), payAfterR);
        BigDecimal payFinal = FeeListRoundingPolicy.round(payAfterR.subtract(takeM).max(BigDecimal.ZERO), rp);
        run.setPayAmt(payFinal);
        settlementRunRepository.save(run);
    }

    private BigDecimal applyFifoRecoveries(String merchantId, Long settlementRunId, BigDecimal cap) {
        if (cap == null || cap.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        List<SettlementRecovery> rows = settlementRecoveryRepository.findByMerchantIdAndStatusInOrderByIdAsc(merchantId, OPEN_RECOVERY);
        BigDecimal used = BigDecimal.ZERO;
        List<SettlementRecovery> dirty = new ArrayList<>();
        for (SettlementRecovery r : rows) {
            BigDecimal rem = r.getRemainingAmount() != null ? r.getRemainingAmount() : BigDecimal.ZERO;
            if (rem.signum() <= 0) {
                continue;
            }
            BigDecimal room = cap.subtract(used);
            if (room.signum() <= 0) {
                break;
            }
            BigDecimal take = rem.min(room);
            if (take.signum() <= 0) {
                continue;
            }
            r.setRemainingAmount(rem.subtract(take));
            r.setAppliedAmount(nzBd(r.getAppliedAmount()).add(take));
            r.setLastAppliedSettlementRunId(settlementRunId);
            if (r.getRemainingAmount().signum() == 0) {
                r.setStatus("CLOSED");
            } else {
                r.setStatus("PARTIAL");
            }
            dirty.add(r);
            used = used.add(take);
        }
        if (!dirty.isEmpty()) {
            settlementRecoveryRepository.saveAll(dirty);
        }
        return used;
    }

    private BigDecimal applyFifoReceivables(String merchantId, Long settlementRunId, BigDecimal cap) {
        if (cap == null || cap.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        List<MerchantReceivable> rows = merchantReceivableRepository.findByMerchantIdAndStatusInOrderByIdAsc(merchantId, OPEN_RECEIVABLE);
        BigDecimal used = BigDecimal.ZERO;
        List<MerchantReceivable> dirty = new ArrayList<>();
        for (MerchantReceivable r : rows) {
            BigDecimal rem = r.getRemainingAmount() != null ? r.getRemainingAmount() : BigDecimal.ZERO;
            if (rem.signum() <= 0) {
                continue;
            }
            BigDecimal room = cap.subtract(used);
            if (room.signum() <= 0) {
                break;
            }
            BigDecimal take = rem.min(room);
            if (take.signum() <= 0) {
                continue;
            }
            r.setRemainingAmount(rem.subtract(take));
            r.setAppliedAmount(nzBd(r.getAppliedAmount()).add(take));
            if (r.getRemainingAmount().signum() == 0) {
                r.setStatus("CLOSED");
            } else {
                r.setStatus("PARTIAL");
            }
            dirty.add(r);
            used = used.add(take);
        }
        if (!dirty.isEmpty()) {
            merchantReceivableRepository.saveAll(dirty);
        }
        return used;
    }

    private static BigDecimal nzBd(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    @Transactional
    public MerchantReceivable createReceivable(String merchantId, BigDecimal amount, String title, String reasonCode, String memo, String createdBy) {
        if (merchantId == null || merchantId.isBlank() || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("merchantId·amount");
        }
        FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
        BigDecimal amt = FeeListRoundingPolicy.round(amount, rp);
        MerchantReceivable r = new MerchantReceivable();
        r.setMerchantId(merchantId.trim());
        r.setTitle(title != null && !title.isBlank() ? title.trim() : "미수금");
        r.setTotalAmount(amt);
        r.setRemainingAmount(amt);
        r.setAppliedAmount(BigDecimal.ZERO);
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
        r.setRemainingAmount(BigDecimal.ZERO);
        merchantReceivableRepository.save(r);
    }

    @Transactional
    public void cancelReceivable(long id) {
        MerchantReceivable r = merchantReceivableRepository.findById(id).orElseThrow();
        if (!"PENDING".equals(r.getStatus())) {
            throw new IllegalStateException("대기 상태만 취소할 수 있습니다.");
        }
        r.setStatus("CANCELLED");
        r.setRemainingAmount(BigDecimal.ZERO);
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
