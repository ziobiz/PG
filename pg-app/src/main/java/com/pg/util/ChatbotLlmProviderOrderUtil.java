package com.pg.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 용도별 LLM 프로바이더·모델 순위 해석 — 미설정 시 {@link ChatbotLlmUsage#LEGACY_ORDER_KEY} 폴백.
 * <p>항목 형식: {@code {"provider":"gemini","model":"gemini-3.5-flash"}} 또는 레거시 문자열 {@code "gemini"}.</p>
 */
public final class ChatbotLlmProviderOrderUtil {

    private static final Pattern PAYMENT_GUIDANCE_HINT = Pattern.compile(
            "(?i)(결제|지불|환불|취소|무효|분할|할부|카드|페이|payment|pay\\s|checkout|refund|"
                    + "払い|支払|決済|退款|付款|分期|ชำระ|แบ่งงวด)");

    private ChatbotLlmProviderOrderUtil() {
    }

    public static List<LlmProviderSlot> resolveOrderSlots(Map<String, Object> cfg, ChatbotLlmUsage usage) {
        if (cfg == null || usage == null) {
            return List.of();
        }
        Object specific = cfg.get(usage.configKey());
        List<LlmProviderSlot> fromSpecific = parseSlotList(specific, cfg);
        if (!fromSpecific.isEmpty()) {
            return fromSpecific;
        }
        Object legacy = cfg.get(ChatbotLlmUsage.LEGACY_ORDER_KEY);
        return parseSlotList(legacy, cfg);
    }

    /** API·UI 응답용 — 문자열 레거시 목록을 슬롯 객체 배열로 정규화. */
    public static List<Map<String, String>> slotsToApiList(List<LlmProviderSlot> slots) {
        List<Map<String, String>> out = new ArrayList<>();
        if (slots == null) {
            return out;
        }
        for (LlmProviderSlot slot : slots) {
            if (slot == null || !slot.isUsable()) {
                continue;
            }
            out.add(new LinkedHashMap<>(slot.toJsonMap()));
        }
        return out;
    }

    public static List<LlmProviderSlot> parseSlotList(Object raw, Map<String, Object> cfg) {
        List<LlmProviderSlot> out = new ArrayList<>();
        if (!(raw instanceof List<?> ls) || ls.isEmpty()) {
            return out;
        }
        for (Object item : ls) {
            LlmProviderSlot slot = parseOneSlot(item, cfg);
            if (slot != null && slot.isUsable()) {
                out.add(slot);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static LlmProviderSlot parseOneSlot(Object item, Map<String, Object> cfg) {
        if (item == null) {
            return null;
        }
        if (item instanceof LlmProviderSlot slot) {
            return slot;
        }
        if (item instanceof Map<?, ?> map) {
            String prov = LlmProviderSlot.normalizeProvider(stringVal(map.get("provider")));
            if (prov.isEmpty()) {
                prov = LlmProviderSlot.normalizeProvider(stringVal(map.get("prov")));
            }
            if (prov.isEmpty()) {
                return null;
            }
            String model = firstNonBlank(stringVal(map.get("model")), "");
            return new LlmProviderSlot(prov, model);
        }
        String legacy = LlmProviderSlot.normalizeProvider(String.valueOf(item));
        if (legacy.isEmpty()) {
            return null;
        }
        return new LlmProviderSlot(legacy, LlmProviderSlot.defaultModelForProvider(legacy, cfg));
    }

    /**
     * @deprecated {@link #resolveOrderSlots(Map, ChatbotLlmUsage)} 사용
     */
    @Deprecated
    public static List<String> resolveOrderRaw(Map<String, Object> cfg, ChatbotLlmUsage usage) {
        List<String> out = new ArrayList<>();
        for (LlmProviderSlot slot : resolveOrderSlots(cfg, usage)) {
            if (!out.contains(slot.provider())) {
                out.add(slot.provider());
            }
        }
        return out;
    }

    /**
     * 공개 챗봇 — 최신 사용자 메시지가 결제·환불·분할 등이면 {@link ChatbotLlmUsage#GENERAL}, 아니면 {@link ChatbotLlmUsage#CATALOG}.
     */
    public static ChatbotLlmUsage resolvePublicChatUsage(List<Map<String, String>> messages) {
        String latestUser = latestUserText(messages);
        if (latestUser != null && PAYMENT_GUIDANCE_HINT.matcher(latestUser).find()) {
            return ChatbotLlmUsage.GENERAL;
        }
        return ChatbotLlmUsage.CATALOG;
    }

    private static String latestUserText(List<Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, String> m = messages.get(i);
            if (m == null) {
                continue;
            }
            String role = m.get("role");
            String content = m.get("content");
            if (role != null && "user".equalsIgnoreCase(role.trim()) && content != null && !content.isBlank()) {
                return content.trim();
            }
        }
        return null;
    }

    private static String stringVal(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return b;
    }
}
