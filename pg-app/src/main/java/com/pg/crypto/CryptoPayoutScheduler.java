package com.pg.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** HOLD 해제 + AUTO 출금 큐 주기 처리. */
@Component
public class CryptoPayoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(CryptoPayoutScheduler.class);

    private final CryptoPayoutService payoutService;

    public CryptoPayoutScheduler(CryptoPayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @Scheduled(fixedDelayString = "${pg.crypto.payout-scan-ms:60000}")
    public void scan() {
        try {
            int released = payoutService.releaseDueHolds();
            int auto = payoutService.processAutoReady(50);
            if (released > 0 || auto > 0) {
                log.info("crypto payout scan: releasedHold={}, autoProcessed={}", released, auto);
            }
        } catch (Exception e) {
            log.warn("crypto payout scan failed: {}", e.getMessage());
        }
    }
}
