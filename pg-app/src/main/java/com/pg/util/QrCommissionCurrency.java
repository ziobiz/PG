package com.pg.util;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * QR(PayPay·QBANG·향후 Alipay/WeChat 등) 수수료 고정금액 통화.
 * KRW 고정이 아니며, 가맹 QR 카드·본사 QR 템플릿에서 선택한다.
 */
public final class QrCommissionCurrency {

    /** 가맹점등록 QR 카드·기본 선택지 (6종). */
    public static final Set<String> ALLOWED = Set.of(
            "KRW", "JPY", "THB", "USD", "CNY", "SGD");

    public static final String DEFAULT = "KRW";

    private QrCommissionCurrency() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return ALLOWED.contains(u) ? u : DEFAULT;
    }

    public static boolean isAllowed(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return ALLOWED.contains(raw.trim().toUpperCase(Locale.ROOT));
    }

    /** UI select options order. */
    public static LinkedHashSet<String> ordered() {
        LinkedHashSet<String> s = new LinkedHashSet<>();
        s.add("KRW");
        s.add("JPY");
        s.add("THB");
        s.add("USD");
        s.add("CNY");
        s.add("SGD");
        return s;
    }
}
