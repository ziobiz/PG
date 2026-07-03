package com.pg.urlpay;

import java.util.Locale;
import java.util.Set;

/** 결제창 상단 경고·안내 문구 모드 (웹결제·URL 분할결제 공통) */
public final class CheckoutHeaderSubtitleModeUtil {

    public static final String DEFAULT = "DEFAULT";
    public static final String DISABLED = "DISABLED";
    /** 가맹 직접 입력 */
    public static final String ACTIVE = "ACTIVE";
    /** 프리셋 — 결제창에서 다국어 i18n으로 표시 */
    public static final String ACTIVE_PREVENT = "ACTIVE_PREVENT";
    public static final String ACTIVE_CONFIRM = "ACTIVE_CONFIRM";
    public static final String ACTIVE_APPROVAL = "ACTIVE_APPROVAL";
    public static final String ACTIVE_FAIL = "ACTIVE_FAIL";

    private static final Set<String> PRESETS = Set.of(
            ACTIVE_PREVENT, ACTIVE_CONFIRM, ACTIVE_APPROVAL, ACTIVE_FAIL);

    private CheckoutHeaderSubtitleModeUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case DISABLED -> DISABLED;
            case ACTIVE -> ACTIVE;
            case ACTIVE_PREVENT, "PREVENT", "ERROR_PREVENT" -> ACTIVE_PREVENT;
            case ACTIVE_CONFIRM, "CONFIRM", "CONFIRM_REMIND" -> ACTIVE_CONFIRM;
            case ACTIVE_APPROVAL, "APPROVAL", "SMOOTH_APPROVAL" -> ACTIVE_APPROVAL;
            case ACTIVE_FAIL, "FAIL", "FAIL_STABLE", "FAIL_WARN" -> ACTIVE_FAIL;
            default -> DEFAULT;
        };
    }

    public static boolean isPreset(String mode) {
        if (mode == null || mode.isBlank()) {
            return false;
        }
        return PRESETS.contains(mode.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isDirectActive(String mode) {
        return ACTIVE.equals(normalize(mode));
    }
}
