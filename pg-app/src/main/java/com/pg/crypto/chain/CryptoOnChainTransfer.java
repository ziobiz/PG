package com.pg.crypto.chain;

import java.math.BigDecimal;
import java.time.Instant;

/** 체인에서 관측된 토큰 입금 1건. */
public record CryptoOnChainTransfer(
        String txHash,
        String toAddress,
        String fromAddress,
        String contractAddress,
        BigDecimal amount,
        Instant observedAt,
        long confirmations
) {
}
