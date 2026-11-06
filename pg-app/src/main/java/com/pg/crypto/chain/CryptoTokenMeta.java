package com.pg.crypto.chain;

/** 자산×네트워크 컨트랙트·소수자리. */
public record CryptoTokenMeta(
        String assetCode,
        String networkCode,
        String contractAddress,
        int decimals,
        boolean evm,
        boolean tron
) {
}
