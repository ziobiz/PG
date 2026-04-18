package com.pg.service.settlement;

import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantReceivable;
import com.pg.entity.MerchantReceivableRecoveryRequest;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementRecovery;
import com.pg.entity.SettlementSetting;
import com.pg.service.CommissionService;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.MerchantReceivableRecoveryRequestRepository;
import com.pg.repository.MerchantReceivableRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementRecoveryRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.ReceivableRecoveryModeService;
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
 * 환수금(정산 후 환불 등 자동)·미수금(수동·정산 지급부족 자동) 등록 및 정산 지급액에서의 FIFO 차감.
 */
@Service
public class SettlementArrearsService {

    /** 정산 실행 지급액이 음수일 때 자동 등록되는 미수금(다음 정산 지급액에서 FIFO 차감) */
    public static final String REASON_AUTO_SETTLEMENT_DEFICIT = "AUTO_SETTLEMENT_DEFICIT";

    private static final String REASON_POST_SETTLE_REFUND = "POST_SETTLE_REFUND";
    private static final List<String> OPEN_RECOVERY = List.of("PENDING", "PARTIAL");
    private static final List<String> OPEN_RECEIVABLE = List.of("PENDING", "PARTIAL");

    private final SettlementRecoveryRepository settlementRecoveryRepository;
    private final MerchantReceivableRepository merchantReceivableRepository;
    private final MerchantReceivableRecoveryRequestRepository merchantReceivableRecoveryRequestRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator;
    private final CommissionService commissionService;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;
    private final ReceivableRecoveryModeService receivableRecoveryModeService;

    public SettlementArrearsService(SettlementRecoveryRepository settlementRecoveryRepository,
                                  MerchantReceivableRepository merchantReceivableRepository,
                                  MerchantReceivableRecoveryRequestRepository merchantReceivableRecoveryRequestRepository,
                                  SettlementRunRepository settlementRunRepository,
                                  FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator,
                                  CommissionService commissionService,
                                  OrgUnitRepository orgUnitRepository,
                                  SettlementSettingRepository settlementSettingRepository,
                                  HqApiConfigRepository hqApiConfigRepository,
                                  HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository,
                                  ReceivableRecoveryModeService receivableRecoveryModeService) {
        this.settlementRecoveryRepository = settlementRecoveryRepository;
        this.merchantReceivableRepository = merchantReceivableRepository;
        this.merchantReceivableRecoveryRequestRepository = merchantReceivableRecoveryRequestRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.feeListTxnBreakdownCalculator = feeListTxnBreakdownCalculator;
        this.commissionService = commissionService;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
        this.receivableRecoveryModeService = receivableRecoveryModeService;
    }

    /** 유효 미수금 환수 모드가 MANUAL이면 미수금은 「환수처리」 요청이 있을 때만 정산에서 차감합니다. */
    public boolean isManualReceivableRecovery(String merchantId) {
        return receivableRecoveryModeService.isManualForMerchantCode(merchantId);
    }

    /**
     * 수동 가맹만: 미수금 건에 대해 다음 정산 마감에서 차감되도록 요청을 등록합니다.
     */
    @Transactional
    public void requestManualReceivableRecovery(long receivableId, String requestedBy) {
        MerchantReceivable r = merchantReceivableRepository.findById(receivableId)
                .orElseThrow(() -> new IllegalArgumentException("미수금 건을 찾을 수 없습니다."));
        String mid = r.getMerchantId() != null ? r.getMerchantId().trim() : "";
        if (mid.isBlank()) {
            throw new IllegalArgumentException("가맹점 코드가 없습니다.");
        }
        if (!isManualReceivableRecovery(mid)) {
            throw new IllegalStateException("해당 가맹점은 자동 미수금 환수 모드입니다. 환수/미수금설정(총판·가맹) 또는 본사 기본에서 수동으로 적용된 경우에만 이용하세요.");
        }
        BigDecimal rem = r.getRemainingAmount() != null ? r.getRemainingAmount() : BigDecimal.ZERO;
        if (rem.signum() <= 0) {
            throw new IllegalStateException("차감할 잔여 미수금이 없습니다.");
        }
        if (!"PENDING".equals(r.getStatus()) && !"PARTIAL".equals(r.getStatus())) {
            throw new IllegalStateException("대기·부분차감 상태만 환수처리할 수 있습니다.");
        }
        if (merchantReceivableRecoveryRequestRepository.existsByMerchantReceivableIdAndStatus(
                receivableId, MerchantReceivableRecoveryRequest.STATUS_PENDING)) {
            throw new IllegalStateException("이미 환수처리 요청이 등록되어 있습니다.");
        }
        MerchantReceivableRecoveryRequest req = new MerchantReceivableRecoveryRequest();
        req.setMerchantReceivableId(receivableId);
        req.setMerchantId(mid);
        req.setStatus(MerchantReceivableRecoveryRequest.STATUS_PENDING);
        req.setRequestedBy(requestedBy);
        merchantReceivableRecoveryRequestRepository.save(req);
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
            case "20", "21", "22", "30", "31", "40", "41", "42" -> true;
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
        CommissionPolicy pol = commissionService.resolveCommissionPolicyForSettlement(mid);
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
     * 정산 실행 저장 직후: 지급액이 양수일 때만 환수금 FIFO, 이어서 미수금 FIFO 차감 후 지급액을 갱신(차감 후 음수는 0으로 상한).
     * 지급액이 이미 음수(순매출 대비 수수료·담보 초과)면 지급액은 그대로 두고, 절댓값 동액을 미수금({@link #REASON_AUTO_SETTLEMENT_DEFICIT})으로 1회 등록한다.
     * AUTO 환수모드 가맹은 차기 정산에서 미수금을 FIFO로 차감하고, MANUAL은 「환수처리」 후 차기 마감에서만 차감한다.
     */
    @Transactional
    public void applyArrearsToSettledRun(SettlementRun run) {
        if (run == null || run.getId() == null) {
            return;
        }
        FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
        BigDecimal payBd = FeeListRoundingPolicy.round(run.getPayAmt() != null ? run.getPayAmt() : BigDecimal.ZERO, rp);
        String mid = run.getMerchantId();
        if (mid == null || mid.isBlank()) {
            return;
        }
        String midTrim = mid.trim();
        if (payBd.signum() < 0) {
            registerAutoDeficitReceivableIfNeeded(run, payBd.abs(), rp, midTrim);
            run.setReceivableAppliedAmt(BigDecimal.ZERO);
            settlementRunRepository.save(run);
            return;
        }
        if (payBd.signum() == 0) {
            run.setReceivableAppliedAmt(BigDecimal.ZERO);
            settlementRunRepository.save(run);
            return;
        }
        BigDecimal takeR = applyFifoRecoveries(midTrim, run.getId(), payBd);
        BigDecimal payAfterR = payBd.subtract(takeR).max(BigDecimal.ZERO);
        BigDecimal takeM;
        if (isManualReceivableRecovery(midTrim)) {
            takeM = applyManualReceivableRecoveryRequests(midTrim, run.getId(), payAfterR, rp);
        } else {
            takeM = applyFifoReceivables(midTrim, payAfterR, rp);
        }
        run.setReceivableAppliedAmt(takeM != null ? takeM : BigDecimal.ZERO);
        BigDecimal payFinal = FeeListRoundingPolicy.round(payAfterR.subtract(takeM).max(BigDecimal.ZERO), rp);
        run.setPayAmt(payFinal);
        settlementRunRepository.save(run);
    }

    /**
     * 정산 지급액 음수분을 미수금으로 1회 등록. memo에 settlement_run_id를 두어 동일 실행 재처리 시 중복 방지.
     */
    private void registerAutoDeficitReceivableIfNeeded(SettlementRun run, BigDecimal debtPositive, FeeListRoundingPolicy rp, String merchantId) {
        if (debtPositive == null || debtPositive.signum() <= 0) {
            return;
        }
        String memoKey = "AUTO_DEFICIT:" + run.getId();
        if (merchantReceivableRepository.existsByMerchantIdAndReasonCodeAndMemo(merchantId, REASON_AUTO_SETTLEMENT_DEFICIT, memoKey)) {
            return;
        }
        BigDecimal amt = FeeListRoundingPolicy.round(debtPositive, rp);
        MerchantReceivable r = new MerchantReceivable();
        r.setMerchantId(merchantId);
        r.setTitle("정산 지급부족(자동)");
        r.setTotalAmount(amt);
        r.setRemainingAmount(amt);
        r.setAppliedAmount(BigDecimal.ZERO);
        r.setStatus("PENDING");
        r.setReasonCode(REASON_AUTO_SETTLEMENT_DEFICIT);
        r.setMemo(memoKey);
        r.setCreatedBy("SYSTEM");
        merchantReceivableRepository.save(r);
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

    private BigDecimal applyFifoReceivables(String merchantId, BigDecimal cap, FeeListRoundingPolicy rp) {
        if (cap == null || cap.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        List<MerchantReceivable> rows = merchantReceivableRepository.findByMerchantIdAndStatusInOrderByIdAsc(merchantId, OPEN_RECEIVABLE);
        return applyReceivableTakes(rows, cap, rp);
    }

    private BigDecimal applyManualReceivableRecoveryRequests(
            String merchantId, Long settlementRunId, BigDecimal cap, FeeListRoundingPolicy rp) {
        if (cap == null || cap.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        List<MerchantReceivableRecoveryRequest> pending =
                merchantReceivableRecoveryRequestRepository.findByMerchantIdAndStatusOrderByIdAsc(
                        merchantId, MerchantReceivableRecoveryRequest.STATUS_PENDING);
        BigDecimal used = BigDecimal.ZERO;
        List<MerchantReceivable> dirtyRecv = new ArrayList<>();
        List<MerchantReceivableRecoveryRequest> dirtyReq = new ArrayList<>();
        for (MerchantReceivableRecoveryRequest req : pending) {
            BigDecimal room = cap.subtract(used);
            if (room.signum() <= 0) {
                break;
            }
            MerchantReceivable r = merchantReceivableRepository.findById(req.getMerchantReceivableId()).orElse(null);
            if (r == null || !midTrimEq(r.getMerchantId(), merchantId)) {
                req.setStatus(MerchantReceivableRecoveryRequest.STATUS_CANCELLED);
                dirtyReq.add(req);
                continue;
            }
            BigDecimal rem = r.getRemainingAmount() != null ? r.getRemainingAmount() : BigDecimal.ZERO;
            if (rem.signum() <= 0) {
                req.setStatus(MerchantReceivableRecoveryRequest.STATUS_APPLIED);
                req.setAppliedSettlementRunId(settlementRunId);
                dirtyReq.add(req);
                continue;
            }
            BigDecimal take = FeeListRoundingPolicy.round(rem.min(room), rp);
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
            dirtyRecv.add(r);
            used = used.add(take);
            req.setStatus(MerchantReceivableRecoveryRequest.STATUS_APPLIED);
            req.setAppliedSettlementRunId(settlementRunId);
            dirtyReq.add(req);
        }
        if (!dirtyRecv.isEmpty()) {
            merchantReceivableRepository.saveAll(dirtyRecv);
        }
        if (!dirtyReq.isEmpty()) {
            merchantReceivableRecoveryRequestRepository.saveAll(dirtyReq);
        }
        return used;
    }

    private static boolean midTrimEq(String a, String merchantId) {
        return a != null && merchantId != null && a.trim().equalsIgnoreCase(merchantId.trim());
    }

    private BigDecimal applyReceivableTakes(List<MerchantReceivable> rows, BigDecimal cap, FeeListRoundingPolicy rp) {
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
            BigDecimal take = FeeListRoundingPolicy.round(rem.min(room), rp);
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

    /**
     * 수동 잔액 축소: PENDING·PARTIAL 미수금을 등록 순(FIFO)으로 차감합니다.
     * 정산 시 자동 차감과 동일하게 remaining·applied·status를 갱신합니다.
     *
     * @return 적용된 합계(양수)·갱신된 행 수
     */
    @Transactional
    public Map<String, Object> manualReduceReceivableFifo(String merchantId, BigDecimal reduceAmount, String memoNote, String actor) {
        if (merchantId == null || merchantId.isBlank() || reduceAmount == null || reduceAmount.signum() <= 0) {
            throw new IllegalArgumentException("merchantId·amount(양수)");
        }
        FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
        BigDecimal target = FeeListRoundingPolicy.round(reduceAmount, rp);
        if (target.signum() <= 0) {
            throw new IllegalArgumentException("금액이 올바르지 않습니다.");
        }
        String mid = merchantId.trim();
        List<MerchantReceivable> rows = merchantReceivableRepository.findByMerchantIdAndStatusInOrderByIdAsc(mid, OPEN_RECEIVABLE);
        BigDecimal used = BigDecimal.ZERO;
        List<MerchantReceivable> dirty = new ArrayList<>();
        for (MerchantReceivable r : rows) {
            BigDecimal rem = r.getRemainingAmount() != null ? r.getRemainingAmount() : BigDecimal.ZERO;
            if (rem.signum() <= 0) {
                continue;
            }
            BigDecimal room = target.subtract(used);
            if (room.signum() <= 0) {
                break;
            }
            BigDecimal take = FeeListRoundingPolicy.round(rem.min(room), rp);
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
        if (used.signum() <= 0) {
            throw new IllegalArgumentException("차감할 미수금 잔액이 없습니다. (해당 업체에 PENDING·PARTIAL 건 없음)");
        }
        if (used.compareTo(target) < 0) {
            throw new IllegalArgumentException(
                    "차감 가능한 잔액(" + used.toPlainString() + ")이 입력 금액(" + target.toPlainString() + ")보다 작습니다.");
        }
        if (!dirty.isEmpty()) {
            String audit = "[수동차감 합계 " + used.toPlainString() + "]"
                    + (memoNote != null && !memoNote.isBlank() ? " " + memoNote.trim() : "")
                    + (actor != null && !actor.isBlank() ? " (" + actor.trim() + ")" : "");
            MerchantReceivable first = dirty.get(0);
            String m0 = first.getMemo();
            if (m0 == null || m0.isBlank()) {
                first.setMemo(audit);
            } else {
                first.setMemo(m0 + " " + audit);
            }
            merchantReceivableRepository.saveAll(dirty);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("appliedTotal", used);
        out.put("changedRowCount", dirty.size());
        out.put("merchantId", mid);
        return out;
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
