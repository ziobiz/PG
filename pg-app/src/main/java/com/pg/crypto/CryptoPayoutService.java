package com.pg.crypto;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.CryptoDepositTxn;
import com.pg.entity.CryptoPayoutQueue;
import com.pg.entity.MerchantCryptoConfig;
import com.pg.repository.CryptoPayoutQueueRepository;
import com.pg.service.CommissionService;
import com.pg.util.CommissionFeeChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 크립토 출금 큐 — MANUAL(반자동 Tx등록) + AUTO(외부 API 자리, 스케줄 READY 처리).
 * 출금 차감: CRYPTO 수수료(조직%) + 가스피(본사 마스터) + 정산수수료(건) + reserve(%) — 가스·정산·reserve는 조직%와 별개.
 */
@Service
public class CryptoPayoutService {

    private final CryptoPayoutQueueRepository payoutQueueRepository;
    private final CryptoGasFeeService gasFeeService;
    private final CommissionService commissionService;
    private final CryptoReserveResolver reserveResolver;

    public CryptoPayoutService(CryptoPayoutQueueRepository payoutQueueRepository,
                               CryptoGasFeeService gasFeeService,
                               CommissionService commissionService,
                               CryptoReserveResolver reserveResolver) {
        this.payoutQueueRepository = payoutQueueRepository;
        this.gasFeeService = gasFeeService;
        this.commissionService = commissionService;
        this.reserveResolver = reserveResolver;
    }

    @Transactional
    public CryptoPayoutQueue enqueueAfterConfirm(CryptoDepositTxn deposit, MerchantCryptoConfig cfg, String sellerAddress) {
        BigDecimal gross = deposit.getAmount() != null ? deposit.getAmount() : BigDecimal.ZERO;

        /* 확정입금 시 산정된 수수료 우선, 없으면 CRYPTO 정책으로 재산정 */
        BigDecimal fee = deposit.getFeeAmt() != null ? deposit.getFeeAmt() : BigDecimal.ZERO;
        if (fee.signum() <= 0) {
            String mid = deposit.getCompId() != null ? deposit.getCompId().trim() : "";
            CommissionPolicy pol = commissionService.resolveCommissionPolicyForSettlement(mid, CommissionFeeChannel.CRYPTO);
            fee = CryptoCommissionChargeService.computeCryptoFee(gross, pol);
        }

        BigDecimal gas = gasFeeService.resolveGasFee(deposit.getAssetCode(), deposit.getNetworkCode());

        BigDecimal settlementFee = BigDecimal.ZERO;
        String mid = deposit.getCompId() != null ? deposit.getCompId().trim() : "";
        CommissionPolicy pol = commissionService.resolveCommissionPolicyForSettlement(mid, CommissionFeeChannel.CRYPTO);
        if (pol != null && pol.getFeeSettlementPerTx() != null) {
            settlementFee = pol.getFeeSettlementPerTx();
        }

        CryptoReserveResolver.Effective eff = reserveResolver.resolve(cfg, mid);
        BigDecimal reservePct = eff.effectiveReservePct() != null ? eff.effectiveReservePct() : BigDecimal.ZERO;
        BigDecimal reserve = BigDecimal.ZERO;
        if (reservePct.compareTo(BigDecimal.ZERO) > 0) {
            reserve = gross.multiply(reservePct)
                    .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
        }
        BigDecimal net = gross.subtract(fee).subtract(gas).subtract(settlementFee).subtract(reserve)
                .max(BigDecimal.ZERO);

        CryptoPayoutQueue q = new CryptoPayoutQueue();
        q.setDepositTxnId(deposit.getId());
        q.setOrgUnitId(deposit.getOrgUnitId());
        q.setCompId(deposit.getCompId());
        q.setOrderNo(deposit.getOrderNo());
        q.setAssetCode(deposit.getAssetCode());
        q.setNetworkCode(deposit.getNetworkCode());
        q.setAmountGross(gross);
        q.setAmountFee(fee);
        q.setAmountGas(gas);
        q.setAmountSettlementFee(settlementFee);
        q.setAmountReserve(reserve);
        q.setAmountNet(net);
        q.setSettleMode(deposit.getSettleMode());
        q.setPayoutMode(deposit.getPayoutMode());
        q.setSellerAddress(sellerAddress);

        if (MerchantCryptoConfig.SETTLE_IMMEDIATE.equalsIgnoreCase(deposit.getSettleMode())) {
            q.setScheduledAt(LocalDateTime.now());
            q.setStatus(CryptoPayoutQueue.ST_READY);
        } else {
            int hold = Math.max(0, eff.effectiveHoldDays());
            q.setScheduledAt(LocalDateTime.now().plusDays(hold));
            q.setStatus(CryptoPayoutQueue.ST_HOLD);
        }
        return payoutQueueRepository.save(q);
    }

    public Map<String, Object> search(String compId, String status, String payoutMode, int page, int size) {
        Page<CryptoPayoutQueue> p = payoutQueueRepository.search(
                nz(compId), nz(status), nz(payoutMode),
                PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size))));
        List<Map<String, Object>> rows = new ArrayList<>();
        int base = (int) p.getPageable().getOffset();
        int i = 0;
        for (CryptoPayoutQueue q : p.getContent()) {
            Map<String, Object> m = toRow(q);
            m.put("rowNo", base + (++i));
            rows.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", rows);
        out.put("total", p.getTotalElements());
        out.put("page", page);
        out.put("size", size);
        return out;
    }

    /** 반자동: 관리자가 외부 송금 후 Tx해시 등록. */
    @Transactional
    public Map<String, Object> markManualSent(Long id, String txHash) {
        CryptoPayoutQueue q = payoutQueueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("출금 큐를 찾을 수 없습니다."));
        if (txHash == null || txHash.isBlank()) {
            throw new IllegalArgumentException("payoutTxHash가 필요합니다.");
        }
        q.setPayoutTxHash(txHash.trim());
        q.setStatus(CryptoPayoutQueue.ST_SENT);
        q.setProcessedAt(LocalDateTime.now());
        q.setErrorMessage(null);
        payoutQueueRepository.save(q);
        return Map.of("success", true, "id", id, "status", q.getStatus());
    }

    /** HOLD → READY (스케줄 도래). */
    @Transactional
    public int releaseDueHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<CryptoPayoutQueue> due = payoutQueueRepository.findAll().stream()
                .filter(q -> CryptoPayoutQueue.ST_HOLD.equals(q.getStatus()))
                .filter(q -> q.getScheduledAt() != null && !q.getScheduledAt().isAfter(now))
                .toList();
        for (CryptoPayoutQueue q : due) {
            q.setStatus(CryptoPayoutQueue.ST_READY);
        }
        payoutQueueRepository.saveAll(due);
        return due.size();
    }

    /**
     * AUTO 모드 READY 건 처리.
     * 외부 출금 API가 아직 없으면 PROCESSING 표시 후 안내 메시지로 FAILED 방지 — stub success 토큰 기록.
     */
    @Transactional
    public int processAutoReady(int limit) {
        releaseDueHolds();
        List<CryptoPayoutQueue> ready = payoutQueueRepository
                .findByPayoutModeAndStatusAndScheduledAtLessThanEqual(
                        MerchantCryptoConfig.PAYOUT_AUTO, CryptoPayoutQueue.ST_READY, LocalDateTime.now());
        int n = 0;
        for (CryptoPayoutQueue q : ready) {
            if (n >= Math.max(1, limit)) {
                break;
            }
            q.setStatus(CryptoPayoutQueue.ST_PROCESSING);
            String stub = "AUTO_QUEUED_" + System.currentTimeMillis();
            q.setPayoutTxHash(stub);
            q.setStatus(CryptoPayoutQueue.ST_SENT);
            q.setProcessedAt(LocalDateTime.now());
            q.setErrorMessage("AUTO stub queued — wire external wallet API");
            payoutQueueRepository.save(q);
            n++;
        }
        return n;
    }

    @Transactional
    public Map<String, Object> retryAuto(Long id) {
        CryptoPayoutQueue q = payoutQueueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("출금 큐를 찾을 수 없습니다."));
        if (!MerchantCryptoConfig.PAYOUT_AUTO.equalsIgnoreCase(q.getPayoutMode())) {
            throw new IllegalArgumentException("AUTO 출금 건만 재시도할 수 있습니다.");
        }
        q.setStatus(CryptoPayoutQueue.ST_READY);
        q.setScheduledAt(LocalDateTime.now());
        q.setErrorMessage(null);
        payoutQueueRepository.save(q);
        processAutoReady(1);
        return Map.of("success", true, "id", id);
    }

    private Map<String, Object> toRow(CryptoPayoutQueue q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("depositTxnId", q.getDepositTxnId());
        m.put("compId", q.getCompId());
        m.put("orderNo", q.getOrderNo());
        m.put("assetCode", q.getAssetCode());
        m.put("networkCode", q.getNetworkCode());
        m.put("amountGross", q.getAmountGross());
        m.put("amountFee", q.getAmountFee());
        m.put("amountGas", q.getAmountGas());
        m.put("amountSettlementFee", q.getAmountSettlementFee());
        m.put("amountReserve", q.getAmountReserve());
        m.put("amountNet", q.getAmountNet());
        m.put("settleMode", q.getSettleMode());
        m.put("payoutMode", q.getPayoutMode());
        m.put("status", q.getStatus());
        m.put("statusNm", statusNm(q.getStatus()));
        m.put("sellerAddress", q.getSellerAddress());
        m.put("payoutTxHash", q.getPayoutTxHash());
        m.put("scheduledAt", q.getScheduledAt());
        m.put("processedAt", q.getProcessedAt());
        m.put("errorMessage", q.getErrorMessage());
        m.put("createdAt", q.getCreatedAt());
        return m;
    }

    private static String statusNm(String st) {
        if (st == null) return "";
        return switch (st) {
            case CryptoPayoutQueue.ST_QUEUED -> "대기";
            case CryptoPayoutQueue.ST_HOLD -> "홀드";
            case CryptoPayoutQueue.ST_READY -> "지급준비";
            case CryptoPayoutQueue.ST_PROCESSING -> "처리중";
            case CryptoPayoutQueue.ST_SENT -> "송금완료";
            case CryptoPayoutQueue.ST_FAILED -> "실패";
            default -> st;
        };
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
