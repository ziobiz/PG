package com.pg.util;

import java.util.Locale;

/**
 * 수수료 채널 FIAT(카드) / QR / CRYPTO(크립토 입금).
 * QR 항목은 카드 FIAT와 같고, 가맹이 QR 정책을 고르기 전에는 카드 정책을 그대로 씁니다.
 * FIAT 정책의 USDT 송금·USDT%·3DS 는 정산 부가·카드 3DS 용이며 CRYPTO 결제통화와 무관.
 */
public final class CommissionFeeChannel {

    public static final String FIAT = "FIAT";
    public static final String QR = "QR";
    public static final String CRYPTO = "CRYPTO";

    public static final String DEFAULT_SCOPE_FIAT = "DEFAULT";
    public static final String DEFAULT_SCOPE_QR = "DEFAULT_QR";
    public static final String DEFAULT_SCOPE_CRYPTO = "DEFAULT_CRYPTO";

    /** 가맹 직접입력 CRYPTO 정책 scope 접미 */
    public static final String MERCHANT_CRYPTO_SCOPE_SUFFIX = ":CRYPTO";
    /** 가맹 직접입력 QR 정책 scope 접미 */
    public static final String MERCHANT_QR_SCOPE_SUFFIX = ":QR";

    private CommissionFeeChannel() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return FIAT;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (CRYPTO.equals(u)) {
            return CRYPTO;
        }
        if (QR.equals(u)) {
            return QR;
        }
        return FIAT;
    }

    public static boolean isCrypto(String raw) {
        return CRYPTO.equals(normalize(raw));
    }

    public static boolean isQr(String raw) {
        return QR.equals(normalize(raw));
    }

    public static String defaultScope(String channel) {
        if (isCrypto(channel)) {
            return DEFAULT_SCOPE_CRYPTO;
        }
        if (isQr(channel)) {
            return DEFAULT_SCOPE_QR;
        }
        return DEFAULT_SCOPE_FIAT;
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

    /** 가맹 직접입력 QR 정책 scope */
    public static String merchantQrScope(String merchantCode) {
        if (merchantCode == null || merchantCode.isBlank()) {
            return DEFAULT_SCOPE_QR;
        }
        return merchantCode.trim() + MERCHANT_QR_SCOPE_SUFFIX;
    }

    public static boolean isMerchantQrScope(String scope) {
        return scope != null && scope.endsWith(MERCHANT_QR_SCOPE_SUFFIX);
    }
}
