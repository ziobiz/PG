package com.pg.util;

import com.pg.entity.OrgLevel;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 등록된 가맹 결제 통보 URL(BACKGROUND·JPAY_NOTIFY 등) — 총본사·본사·총판(및 ADMIN)만 조회 가능.
 */
public final class MerchantNotifyUrlVisibility {

    public static final String FLAG_REGISTERED_NOTIFY_URLS_VISIBLE = "registeredNotifyUrlsVisible";

    private MerchantNotifyUrlVisibility() {
    }

    public static boolean canViewerSeeRegisteredNotifyUrls(String role, String orgLevel) {
        if (role != null && "ADMIN".equalsIgnoreCase(role.trim())) {
            return true;
        }
        if (orgLevel == null || orgLevel.isBlank()) {
            return false;
        }
        String ol = orgLevel.trim().toUpperCase(Locale.ROOT);
        return OrgLevel.HEADQUARTERS.name().equals(ol)
                || OrgLevel.REGIONAL.name().equals(ol)
                || OrgLevel.MASTER_DIST.name().equals(ol);
    }

    public static void redactMerchantNotifyUrlsFromKit(Map<String, Object> kitOrPortal) {
        if (kitOrPortal == null) {
            return;
        }
        kitOrPortal.put("merchantNotifyUrls", List.of());
        kitOrPortal.put(FLAG_REGISTERED_NOTIFY_URLS_VISIBLE, false);
    }

    public static void markMerchantNotifyUrlsVisible(Map<String, Object> kitOrPortal) {
        if (kitOrPortal != null) {
            kitOrPortal.put(FLAG_REGISTERED_NOTIFY_URLS_VISIBLE, true);
        }
    }

    public static void redactCompDetailNotifyUrlFields(Map<String, Object> detail) {
        if (detail == null) {
            return;
        }
        clearNotifyField(detail, "notifyUrlBackground");
        clearNotifyField(detail, "notifyUrlResult");
        clearNotifyField(detail, "jpayNotifyUrl");
        clearNotifyField(detail, "jpayCallbackUrl");
        detail.put(FLAG_REGISTERED_NOTIFY_URLS_VISIBLE, false);
    }

    public static void markCompDetailNotifyUrlsVisible(Map<String, Object> detail) {
        if (detail != null) {
            detail.put(FLAG_REGISTERED_NOTIFY_URLS_VISIBLE, true);
        }
    }

    /** 가맹점 API 포털 — portal 맵 내부 merchantNotifyUrls 제거 */
    public static void redactSelfPortal(Map<String, Object> selfPortalOut) {
        if (selfPortalOut == null) {
            return;
        }
        Object portalObj = selfPortalOut.get("portal");
        if (portalObj instanceof Map<?, ?> portalRaw) {
            @SuppressWarnings("unchecked")
            Map<String, Object> portal = (Map<String, Object>) portalRaw;
            redactMerchantNotifyUrlsFromKit(portal);
        }
        selfPortalOut.put(FLAG_REGISTERED_NOTIFY_URLS_VISIBLE, false);
    }

    private static void clearNotifyField(Map<String, Object> detail, String key) {
        if (detail.containsKey(key)) {
            detail.put(key, "");
        }
    }
}
