package com.pg.util;

import java.util.Locale;

/**
 * 가맹 정산설정 {@code receivable_recovery_mode} 및 본사 기본 {@code receivable_recovery_default_mode}.
 * AUTO: 다음 정산에서 미수금 FIFO 차감. MANUAL: 미수금관리에서 환수처리 요청 후 차기 정산에서 차감.
 */
public final class ReceivableRecoveryModeUtil {

    public static final String AUTO = "AUTO";
    public static final String MANUAL = "MANUAL";

    private ReceivableRecoveryModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (MANUAL.equals(u)) {
            return MANUAL;
        }
        return AUTO;
    }

    public static boolean isManual(String mode) {
        return MANUAL.equalsIgnoreCase(normalize(mode));
    }
}
