package com.pg.util;

import java.util.Locale;

/** 일괄운영관리 모드·액션 코드 */
public final class HqBulkOpsModes {

    public static final String POLICY_ORG_USE = "ORG_USE";
    public static final String POLICY_URL_PAY = "URL_PAY";

    public static final String MODE_NONE = "NONE";
    public static final String MODE_FORCE_Y = "FORCE_Y";
    public static final String MODE_FORCE_N = "FORCE_N";
    public static final String MODE_PAUSED = "PAUSED";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RELEASED = "RELEASED";

    /** Y | N | PAUSE | RELEASE */
    public static final String ACTION_Y = "Y";
    public static final String ACTION_N = "N";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_RELEASE = "RELEASE";

    private HqBulkOpsModes() {
    }

    public static String normalizeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return MODE_NONE;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        if ("Y".equals(t) || "사용".equals(raw.trim())) {
            return MODE_FORCE_Y;
        }
        if ("N".equals(t) || "미사용".equals(raw.trim())) {
            return MODE_FORCE_N;
        }
        if (MODE_PAUSED.equals(t) || "PAUSE".equals(t) || "일시중지".equals(raw.trim())) {
            return MODE_PAUSED;
        }
        if (MODE_NONE.equals(t) || "RELEASE".equals(t) || "중지해제".equals(raw.trim())) {
            return MODE_NONE;
        }
        return MODE_NONE;
    }

    public static String normalizeAction(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        if (ACTION_Y.equals(t) || "사용".equals(raw.trim())) {
            return ACTION_Y;
        }
        if (ACTION_N.equals(t) || "미사용".equals(raw.trim())) {
            return ACTION_N;
        }
        if (ACTION_PAUSE.equals(t) || "PAUSED".equals(t) || "일시중지".equals(raw.trim())) {
            return ACTION_PAUSE;
        }
        if (ACTION_RELEASE.equals(t) || "중지해제".equals(raw.trim())) {
            return ACTION_RELEASE;
        }
        return t;
    }

    public static String modeToActionLabelKey(String mode) {
        return switch (normalizeMode(mode)) {
            case MODE_FORCE_Y -> "사용";
            case MODE_FORCE_N -> "미사용";
            case MODE_PAUSED -> "일시중지";
            default -> "중지해제";
        };
    }

    public static String actionToMode(String action) {
        return switch (normalizeAction(action)) {
            case ACTION_Y -> MODE_FORCE_Y;
            case ACTION_N -> MODE_FORCE_N;
            case ACTION_PAUSE -> MODE_PAUSED;
            case ACTION_RELEASE -> MODE_NONE;
            default -> MODE_NONE;
        };
    }
}
