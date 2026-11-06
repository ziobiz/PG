package com.pg.crypto;

import com.pg.crypto.chain.CryptoChainVerifyOutcome;
import com.pg.crypto.chain.CryptoChainVerifyService;
import com.pg.entity.CryptoDepositTxn;
import com.pg.repository.CryptoDepositTxnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대기 중 크립토 입금 건을 주기적으로 수탁 주소 온체인 스캔 → 매칭 시 confirmDeposit.
 */
@Component
public class CryptoDepositWatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(CryptoDepositWatchScheduler.class);

    private final CryptoDepositTxnRepository depositTxnRepository;
    private final CryptoChainVerifyService chainVerifyService;
    private final CryptoCheckoutService checkoutService;

    @Value("${pg.crypto.chain.enabled:true}")
    private boolean enabled;

    @Value("${pg.crypto.chain.watch-batch-size:40}")
    private int batchSize;

    public CryptoDepositWatchScheduler(CryptoDepositTxnRepository depositTxnRepository,
                                       CryptoChainVerifyService chainVerifyService,
                                       CryptoCheckoutService checkoutService) {
        this.depositTxnRepository = depositTxnRepository;
        this.chainVerifyService = chainVerifyService;
        this.checkoutService = checkoutService;
    }

    @Scheduled(fixedDelayString = "${pg.crypto.chain.watch-scan-ms:30000}", initialDelayString = "20000")
    public void scan() {
        if (!enabled || !chainVerifyService.isEnabled()) {
            return;
        }
        List<CryptoDepositTxn> candidates = depositTxnRepository.findWatchCandidates(
                List.of(CryptoDepositTxn.ST_PENDING, CryptoDepositTxn.ST_AWAITING),
                LocalDateTime.now(),
                PageRequest.of(0, Math.max(1, batchSize)));
        if (candidates.isEmpty()) {
            return;
        }
        int matched = 0;
        for (CryptoDepositTxn txn : candidates) {
            try {
                CryptoChainVerifyOutcome outcome = chainVerifyService.scanCustodyForMatch(txn);
                if (!outcome.matched() || outcome.transfer() == null) {
                    continue;
                }
                String hash = outcome.transfer().txHash();
                if (hash == null || hash.isBlank()) {
                    continue;
                }
                if (depositTxnRepository.findFirstByDepositTxHashIgnoreCase(hash)
                        .filter(other -> !other.getId().equals(txn.getId()))
                        .isPresent()) {
                    log.warn("crypto watch: txHash already used hash={} skip depositId={}", hash, txn.getId());
                    continue;
                }
                checkoutService.confirmDeposit(txn.getId(), hash);
                matched++;
                log.info("crypto watch: confirmed depositId={} orderNo={} hash={}",
                        txn.getId(), txn.getOrderNo(), hash);
            } catch (Exception e) {
                log.warn("crypto watch: depositId={} err={}", txn.getId(), e.getMessage());
            }
        }
        if (matched > 0) {
            log.info("crypto deposit watch: matched={}", matched);
        }
    }
}
