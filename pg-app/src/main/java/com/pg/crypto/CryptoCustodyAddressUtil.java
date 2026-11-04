package com.pg.crypto;

import java.util.Locale;

/**
 * 본사 대표수탁 주소 유효성 — V265 시드 {@code ReplaceMe} 플레이스홀더는 구매자 입금에 쓰지 않는다.
 */
public final class CryptoCustodyAddressUtil {

    private CryptoCustodyAddressUtil() {
    }

    /** 시드·임시 주소면 true (입금·카탈로그 제외). */
    public static boolean isPlaceholder(String address) {
        if (address == null || address.isBlank()) {
            return true;
        }
        String a = address.trim().toUpperCase(Locale.ROOT);
        return a.contains("REPLACEME")
                || a.contains("ICOPAYCUSTODYREPLACE")
                || a.startsWith("0XICOPAY")
                || a.startsWith("TICOPAYCUSTODY");
    }

    /** 구매자 입금에 쓸 수 있는 실주소. */
    public static boolean isUsableDepositAddress(String address) {
        return address != null && !address.isBlank() && !isPlaceholder(address);
    }
}
