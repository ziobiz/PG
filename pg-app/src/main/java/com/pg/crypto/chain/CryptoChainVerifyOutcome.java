package com.pg.crypto.chain;

/** 온체인 입금 검증 결과. */
public record CryptoChainVerifyOutcome(
        boolean matched,
        String code,
        String message,
        CryptoOnChainTransfer transfer
) {
    public static CryptoChainVerifyOutcome ok(CryptoOnChainTransfer t) {
        return new CryptoChainVerifyOutcome(true, "MATCHED", "On-chain deposit matched", t);
    }

    public static CryptoChainVerifyOutcome fail(String code, String message) {
        return new CryptoChainVerifyOutcome(false, code, message, null);
    }
}
