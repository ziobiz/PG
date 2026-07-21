package com.pg.urlpay;

import java.util.Locale;
import java.util.Set;

/** 결제창 상단 경고·안내 문구 모드 (웹결제·URL 분할결제 공통) */
public final class CheckoutHeaderSubtitleModeUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";
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

    /** 결제창 실효 모드(FOLLOW_HQ 제외) */
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
            case FOLLOW_HQ, "HQ" -> DEFAULT;
            default -> DEFAULT;
        };
    }

    /** 가맹 DB 저장 — FOLLOW_HQ 유지 */
    public static String normalizeMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        return normalize(u);
    }

    public static String resolveEffective(String merchantStored, String hqDefault) {
        String stored = normalizeMerchantStored(merchantStored);
        if (!FOLLOW_HQ.equals(stored)) {
            return stored;
        }
        return normalize(hqDefault != null ? hqDefault : DEFAULT);
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
