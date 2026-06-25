package com.pg.service;

import com.pg.util.ChatbotLlmProviderOrderUtil;
import com.pg.util.ChatbotLlmUsage;
import com.pg.util.LlmProviderSlot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutcomeReasonTranslateServiceTest {

    @Test
    void providerTryOrder_usesPlatformSlotsWithModels() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("report_provider_order_platform", List.of(
                Map.of("provider", "gemini", "model", "gemini-2.5-flash-lite"),
                Map.of("provider", "groq", "model", "llama-3.1-8b-instant")
        ));
        cfg.put("report_gemini_api_key", "gem-key");
        cfg.put("report_groq_api_key", "groq-key");

        List<LlmProviderSlot> orderRaw = ChatbotLlmProviderOrderUtil.resolveOrderSlots(cfg, ChatbotLlmUsage.PLATFORM);
        List<LlmProviderSlot> order = ChatbotLlmCompletionService.outcomeReasonSlotTryOrder(orderRaw, cfg);

        assertEquals(2, order.size());
        assertEquals("groq", order.get(0).provider());
        assertEquals("llama-3.1-8b-instant", order.get(0).model());
        assertEquals("gemini", order.get(1).provider());
        assertEquals("gemini-2.5-flash-lite", order.get(1).model());
    }

    @Test
    void providerTryOrder_prefersSecondThenFirst() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("report_provider_order", List.of("gemini", "groq", "anthropic", "openai"));
        cfg.put("report_gemini_api_key", "gem-key");
        cfg.put("report_groq_api_key", "groq-key");

        List<String> order = ChatbotLlmCompletionService.outcomeReasonProviderTryOrder(
                List.of("gemini", "groq", "anthropic", "openai"), cfg);

        assertEquals(List.of("groq", "gemini"), order);
    }

    @Test
    void providerTryOrder_skipsDisabledSecond() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("report_provider_order", List.of("gemini", "groq"));
        cfg.put("report_gemini_api_key", "gem-key");
        cfg.put("report_groq_api_key", "groq-key");
        cfg.put("report_groq_disabled", "Y");

        List<String> order = ChatbotLlmCompletionService.outcomeReasonProviderTryOrder(
                List.of("gemini", "groq"), cfg);

        assertEquals(List.of("gemini"), order);
    }

    @Test
    void parseSlotList_supportsLegacyStringList() {
        List<LlmProviderSlot> slots = ChatbotLlmProviderOrderUtil.parseSlotList(List.of("gemini", "groq"), Map.of());
        assertEquals(2, slots.size());
        assertEquals("gemini", slots.get(0).provider());
        assertEquals("gemini-3.5-flash", slots.get(0).model());
    }

    @Test
    void phraseDictionary_koreanForChineseFailure() {
        String hit = com.pg.util.OutcomeReasonPhraseDictionary.lookup("交易失败 : 余额不足", "KO");
        assertEquals("거래 실패 : 잔액 부족", hit);
    }

    @Test
    void translateBatchFromCacheOnly_doesNotInvokeAiPath() {
        OutcomeReasonTranslateService svc = new OutcomeReasonTranslateService(null, null, null);
        Map<String, String> out = svc.translateBatchFromCacheOnly(List.of("잔액이 부족합니다"), "KO");
        assertEquals("잔액이 부족합니다", out.get("잔액이 부족합니다"));
    }

    @Test
    void likelyAlreadyInLocale_koreanText() {
        assertTrue(OutcomeReasonTranslateService.likelyAlreadyInLocale("잔액이 부족합니다", "KO"));
    }
}
