package com.pg.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 가맹 결제대행사 행의 QR 지갑 범위.
 * 비어 있으면 QR 없음. 카드 브랜드·라우팅 통화와 같은 행에 둡니다.
 * 승인 API가 연결된 지갑만 {@link #isChargeReady(String)} 가 true 입니다.
 */
public final class QrWalletScopeUtil {

    public static final String PAYPAY = "PAYPAY";

    /** 카탈로그 순서. 승인 규격이 들어오면 여기 지갑의 chargeReady 만 켭니다. */
    private static final List<String> CATALOG = List.of(PAYPAY);

    private static final Set<String> KNOWN = Set.copyOf(CATALOG);

    private QrWalletScopeUtil() {
    }

    /** 알려진 지갑이면 코드, 아니면 빈 문자열. */
    public static String canonicalWallet(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        return KNOWN.contains(t) ? t : "";
    }

    /**
     * 저장값. 빈 문자열·NONE 은 QR 없음.
     * 알 수 없는 토큰은 버립니다. 카탈로그 순서로 중복 없이 합칩니다.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        if ("NONE".equalsIgnoreCase(trimmed)) {
            return "";
        }
        LinkedHashSet<String> found = new LinkedHashSet<>();
        for (String part : trimmed.split("[,\\s|/]+")) {
            String wallet = canonicalWallet(part);
            if (!wallet.isEmpty()) {
                found.add(wallet);
            }
        }
        if (found.isEmpty()) {
            return "";
        }
        List<String> ordered = new ArrayList<>();
        for (String wallet : CATALOG) {
            if (found.contains(wallet)) {
                ordered.add(wallet);
            }
        }
        return String.join(",", ordered);
    }

    public static void validateOrThrow(String raw) {
        if (raw == null || raw.isBlank() || "NONE".equalsIgnoreCase(raw.trim())) {
            return;
        }
        for (String part : raw.trim().split("[,\\s|/]+")) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (canonicalWallet(part).isEmpty()) {
                throw new IllegalArgumentException(
                        "QR 지갑은 비우거나 PAYPAY 여야 합니다. 입력값=" + raw);
            }
        }
    }

    public static boolean matches(String scopeRaw, String walletRaw) {
        String wallet = canonicalWallet(walletRaw);
        if (wallet.isEmpty()) {
            return false;
        }
        String scope = normalize(scopeRaw);
        if (scope.isEmpty()) {
            return false;
        }
        for (String part : scope.split(",")) {
            if (wallet.equals(part)) {
                return true;
            }
        }
        return false;
    }

    /** 구매자 QR 승인에 연결된 지갑. PayPay 는 기존 ILK 승인·노티 경로를 사용합니다. */
    public static boolean isChargeReady(String walletRaw) {
        return PAYPAY.equals(canonicalWallet(walletRaw));
    }

    public static int specificityScore(String scopeRaw) {
        String scope = normalize(scopeRaw);
        if (scope.isEmpty()) {
            return 0;
        }
        int parts = scope.split(",").length;
        return Math.max(1, 8 - parts);
    }

    public static String displayJoined(Collection<String> rawScopes) {
        if (rawScopes == null || rawScopes.isEmpty()) {
            return "-";
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String raw : rawScopes) {
            String n = normalize(raw);
            if (!n.isEmpty()) {
                seen.add(n);
            }
        }
        return seen.isEmpty() ? "-" : String.join(", ", seen);
    }
}
