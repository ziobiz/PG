package com.pg.util;

import com.pg.entity.OrgLevel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantNotifyUrlVisibilityTest {

    @Test
    void hqRegionalMasterDistCanSee() {
        assertTrue(MerchantNotifyUrlVisibility.canViewerSeeRegisteredNotifyUrls("USER", OrgLevel.HEADQUARTERS.name()));
        assertTrue(MerchantNotifyUrlVisibility.canViewerSeeRegisteredNotifyUrls("USER", OrgLevel.REGIONAL.name()));
        assertTrue(MerchantNotifyUrlVisibility.canViewerSeeRegisteredNotifyUrls("USER", OrgLevel.MASTER_DIST.name()));
        assertTrue(MerchantNotifyUrlVisibility.canViewerSeeRegisteredNotifyUrls("ADMIN", OrgLevel.MERCHANT.name()));
    }

    @Test
    void lowerOrgCannotSee() {
        assertFalse(MerchantNotifyUrlVisibility.canViewerSeeRegisteredNotifyUrls("USER", OrgLevel.BRANCH.name()));
        assertFalse(MerchantNotifyUrlVisibility.canViewerSeeRegisteredNotifyUrls("USER", OrgLevel.MERCHANT.name()));
    }

    @Test
    void redactKitClearsUrls() {
        Map<String, Object> kit = new LinkedHashMap<>();
        kit.put("merchantNotifyUrls", List.of(Map.of("urlType", "BACKGROUND", "notiUrl", "https://x")));
        MerchantNotifyUrlVisibility.redactMerchantNotifyUrlsFromKit(kit);
        assertTrue(((List<?>) kit.get("merchantNotifyUrls")).isEmpty());
        assertEquals(Boolean.FALSE, kit.get(MerchantNotifyUrlVisibility.FLAG_REGISTERED_NOTIFY_URLS_VISIBLE));
    }

    @Test
    void redactCompDetailClearsFields() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("notifyUrlBackground", "https://a");
        detail.put("jpayNotifyUrl", "https://b");
        MerchantNotifyUrlVisibility.redactCompDetailNotifyUrlFields(detail);
        assertEquals("", detail.get("notifyUrlBackground"));
        assertEquals("", detail.get("jpayNotifyUrl"));
    }
}
