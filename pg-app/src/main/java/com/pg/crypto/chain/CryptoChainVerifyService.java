package com.pg.crypto.chain;

import com.pg.entity.CryptoDepositTxn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 수탁 주소 온체인 입금 검증.
 * 구매자 지갑 종류와 무관 — ICOPAY가 체인(RPC/인덱서)을 조회한다.
 */
@Service
public class CryptoChainVerifyService {

    private final CryptoTokenMetaService tokenMetaService;
    private final CryptoEvmJsonRpcClient evmClient;
    private final CryptoTronGridClient tronClient;

    @Value("${pg.crypto.chain.amount-tolerance-abs:0.05}")
    private BigDecimal amountToleranceAbs;

    @Value("${pg.crypto.chain.amount-tolerance-pct:0.002}")
    private BigDecimal amountTolerancePct;

    @Value("${pg.crypto.chain.min-confirmations:1}")
    private long minConfirmations;

    @Value("${pg.crypto.chain.enabled:true}")
    private boolean enabled;

    public CryptoChainVerifyService(CryptoTokenMetaService tokenMetaService,
                                    CryptoEvmJsonRpcClient evmClient,
                                    CryptoTronGridClient tronClient) {
        this.tokenMetaService = tokenMetaService;
        this.evmClient = evmClient;
        this.tronClient = tronClient;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Tx 해시로 검증. */
    public CryptoChainVerifyOutcome verifyByTxHash(CryptoDepositTxn txn, String txHash) {
        if (!enabled) {
            return CryptoChainVerifyOutcome.fail("CHAIN_WATCH_DISABLED", "온체인 입금 감시가 비활성입니다.");
        }
        if (txHash == null || txHash.isBlank()) {
            return CryptoChainVerifyOutcome.fail("TX_HASH_REQUIRED", "Tx 해시가 필요합니다.");
        }
        String hash = txHash.trim();
        if (hash.toUpperCase(Locale.ROOT).startsWith("BUYER_ACK")) {
            return CryptoChainVerifyOutcome.fail("INVALID_TX_HASH", "구매자 확인만으로는 확정할 수 없습니다.");
        }
        Optional<CryptoTokenMeta> metaOpt = tokenMetaService.resolve(txn.getAssetCode(), txn.getNetworkCode());
        if (metaOpt.isEmpty()) {
            return CryptoChainVerifyOutcome.fail("NETWORK_UNSUPPORTED",
                    "이 네트워크는 아직 자동 입금확인을 지원하지 않습니다. 관리자 확인이 필요합니다.");
        }
        CryptoTokenMeta meta = metaOpt.get();
        Optional<CryptoOnChainTransfer> found;
        if (meta.tron()) {
            found = tronClient.findTransferByTxHash(hash, txn.getCustodyAddress(), meta.contractAddress(), meta.decimals());
        } else {
            found = evmClient.findTransferByTxHash(
                    meta.networkCode(), hash, txn.getCustodyAddress(), meta.contractAddress(), meta.decimals());
        }
        if (found.isEmpty()) {
            return CryptoChainVerifyOutcome.fail("TX_NOT_FOUND",
                    "체인에서 해당 Tx의 입금을 찾지 못했습니다. 해시·네트워크를 확인해 주세요.");
        }
        return matchAmountAndConfirmations(txn, found.get());
    }

    /** Tx 없이 수탁 주소 최근 입금 스캔 후 금액 매칭. */
    public CryptoChainVerifyOutcome scanCustodyForMatch(CryptoDepositTxn txn) {
        if (!enabled) {
            return CryptoChainVerifyOutcome.fail("CHAIN_WATCH_DISABLED", "온체인 입금 감시가 비활성입니다.");
        }
        Optional<CryptoTokenMeta> metaOpt = tokenMetaService.resolve(txn.getAssetCode(), txn.getNetworkCode());
        if (metaOpt.isEmpty()) {
            return CryptoChainVerifyOutcome.fail("NETWORK_UNSUPPORTED",
                    "이 네트워크는 아직 자동 입금확인을 지원하지 않습니다.");
        }
        CryptoTokenMeta meta = metaOpt.get();
        Instant notBefore = createdInstant(txn).minusSeconds(120);
        List<CryptoOnChainTransfer> list;
        if (meta.tron()) {
            list = tronClient.findIncomingTransfers(
                    txn.getCustodyAddress(), meta.contractAddress(), meta.decimals(), notBefore);
        } else {
            list = evmClient.findIncomingTransfers(
                    meta.networkCode(), txn.getCustodyAddress(), meta.contractAddress(), meta.decimals(), notBefore);
        }
        if (list == null || list.isEmpty()) {
            return CryptoChainVerifyOutcome.fail("DEPOSIT_NOT_SEEN",
                    "아직 수탁 주소로 입금이 확인되지 않았습니다. 송금 후 잠시 뒤 다시 확인해 주세요.");
        }
        BigDecimal expected = expectedAmount(txn);
        return list.stream()
                .sorted(Comparator.comparing(CryptoOnChainTransfer::observedAt).reversed())
                .map(t -> matchAmountAndConfirmations(txn, t))
                .filter(CryptoChainVerifyOutcome::matched)
                .findFirst()
                .orElseGet(() -> CryptoChainVerifyOutcome.fail("AMOUNT_MISMATCH",
                        "입금은 보이나 주문 금액(" + expected.toPlainString() + " " + txn.getAssetCode()
                                + ")과 일치하는 건이 없습니다."));
    }

    private CryptoChainVerifyOutcome matchAmountAndConfirmations(CryptoDepositTxn txn, CryptoOnChainTransfer t) {
        if (t.confirmations() > 0 && t.confirmations() < minConfirmations) {
            return CryptoChainVerifyOutcome.fail("PENDING_CONFIRMATIONS",
                    "블록 확정 대기 중(" + t.confirmations() + "/" + minConfirmations + ")입니다.");
        }
        BigDecimal expected = expectedAmount(txn);
        if (!amountMatches(expected, t.amount())) {
            return CryptoChainVerifyOutcome.fail("AMOUNT_MISMATCH",
                    "입금 금액 " + t.amount().toPlainString() + " 이(가) 주문 "
                            + expected.toPlainString() + " " + txn.getAssetCode() + " 과 다릅니다.");
        }
        return CryptoChainVerifyOutcome.ok(t);
    }

    static BigDecimal expectedAmount(CryptoDepositTxn txn) {
        if (txn.getAssetAmount() != null && txn.getAssetAmount().compareTo(BigDecimal.ZERO) > 0) {
            return txn.getAssetAmount();
        }
        return txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO;
    }

    private boolean amountMatches(BigDecimal expected, BigDecimal actual) {
        if (expected == null || actual == null) {
            return false;
        }
        BigDecimal exp = expected.abs().setScale(8, RoundingMode.HALF_UP);
        BigDecimal act = actual.abs().setScale(8, RoundingMode.HALF_UP);
        BigDecimal tolPct = exp.multiply(amountTolerancePct != null ? amountTolerancePct : BigDecimal.ZERO);
        BigDecimal tolAbs = amountToleranceAbs != null ? amountToleranceAbs : BigDecimal.ZERO;
        BigDecimal tol = tolPct.max(tolAbs).max(new BigDecimal("0.00000001"));
        return exp.subtract(act).abs().compareTo(tol) <= 0;
    }

    private static Instant createdInstant(CryptoDepositTxn txn) {
        LocalDateTime c = txn.getCreatedAt() != null ? txn.getCreatedAt() : LocalDateTime.now();
        return c.atZone(ZoneId.systemDefault()).toInstant();
    }
}
