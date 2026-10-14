package com.pg.util;

import java.util.Locale;

/**
 * 수수료 채널 FIAT(카드·법정) / CRYPTO(크립토 입금).
 * FIAT 정책의 USDT 송금·USDT%·3DS 는 정산 부가·카드 3DS 용이며 CRYPTO 결제통화와 무관.
 */
public final class CommissionFeeChannel {

    public static final String FIAT = "FIAT";
    public static final String CRYPTO = "CRYPTO";

    public static final String DEFAULT_SCOPE_FIAT = "DEFAULT";
    public static final String DEFAULT_SCOPE_CRYPTO = "DEFAULT_CRYPTO";

    /** 가맹 직접입력 CRYPTO 정책 scope 접미 */
    public static final String MERCHANT_CRYPTO_SCOPE_SUFFIX = ":CRYPTO";

    private CommissionFeeChannel() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return FIAT;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return CRYPTO.equals(u) ? CRYPTO : FIAT;
    }

    public static boolean isCrypto(String raw) {
        return CRYPTO.equals(normalize(raw));
    }

    public static String defaultScope(String channel) {
        return isCrypto(channel) ? DEFAULT_SCOPE_CRYPTO : DEFAULT_SCOPE_FIAT;
    }

    /** 가맹 직접입력 CRYPTO 정책 scope */
    public static String merchantCryptoScope(String merchantCode) {
        if (merchantCode == null || merchantCode.isBlank()) {
            return DEFAULT_SCOPE_CRYPTO;
        }
        return merchantCode.trim() + MERCHANT_CRYPTO_SCOPE_SUFFIX;
    }

    public static boolean isMerchantCryptoScope(String scope) {
        return scope != null && scope.endsWith(MERCHANT_CRYPTO_SCOPE_SUFFIX);
    }
}
