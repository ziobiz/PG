package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantPayNotifyUrlRulesTest {

    @Test
    void detectsNotiIngressUrl() {
        assertTrue(MerchantPayNotifyUrlRules.isNotiMiddlewareIngressUrl(
                "https://noti.icopay.net/noti/result/j6"));
    }

    @Test
    void detectsWordpressWebhook() {
        assertTrue(MerchantPayNotifyUrlRules.isWordpressIcopayWebhookUrl(
                "https://modelab.store/wp-json/icopay/v1/webhook"));
    }

    @Test
    void hidesWordpressWebhookWhenChannelOff() {
        assertEquals("",
                MerchantPayNotifyUrlRules.sanitizeBackgroundForMerchant(
                        "https://modelab.store/wp-json/icopay/v1/webhook", "N"));
    }

    @Test
    void keepsWordpressWebhookWhenChannelOn() {
        String url = "https://modelab.store/wp-json/icopay/v1/webhook";
        assertEquals(url, MerchantPayNotifyUrlRules.sanitizeBackgroundForMerchant(url, "Y"));
    }

    @Test
    void stripsNotiIngressFromResult() {
        assertEquals("",
                MerchantPayNotifyUrlRules.sanitizeResultForMerchant(
                        "https://noti.icopay.net/noti/result/j6"));
    }

    @Test
    void keepsCustomMerchantResult() {
        String url = "https://merchant.example.com/pay/result";
        assertEquals(url, MerchantPayNotifyUrlRules.sanitizeResultForMerchant(url));
    }
}
