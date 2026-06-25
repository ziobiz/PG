package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.AppUser;
import com.pg.entity.HqChatbotAiSettings;
import com.pg.repository.HqChatbotAiSettingsRepository;
import com.pg.util.ChatbotLlmProviderOrderUtil;
import com.pg.util.ChatbotLlmUsage;
import com.pg.util.ChatbotProductPricingUtil;
import com.pg.util.LlmProviderSlot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 본사 AI 설정 — 저장 JSON 키는 ziobiz/Stock {@code report_*_api_key}, {@code report_*_model},
 * {@code report_provider_order}, {@code report_{gemini|groq|anthropic|openai}_disabled}(사용중지) 및
 * 챗봇 전용 {@code ai_system_prompt_chatbot}, {@code ai_system_options_chatbot},
 * {@code ai_prompt_chatbot_catalog} 등을 사용합니다.
 */
@Service
public class HqChatbotAiSettingsService {

    public static final List<String> DEFAULT_PROVIDER_ORDER = List.of("gemini", "groq", "anthropic", "openai");

    public static final String KEY_ORDER_CATALOG = ChatbotLlmUsage.CATALOG.configKey();
    public static final String KEY_ORDER_SHORT = ChatbotLlmUsage.SHORT.configKey();
    public static final String KEY_ORDER_GENERAL = ChatbotLlmUsage.GENERAL.configKey();

    public static final String KEY_ORDER_PLATFORM = ChatbotLlmUsage.PLATFORM.configKey();

    private static final List<String> USAGE_ORDER_KEYS = List.of(
            KEY_ORDER_CATALOG, KEY_ORDER_SHORT, KEY_ORDER_GENERAL, KEY_ORDER_PLATFORM);
    private static final List<String> API_KEYS = List.of(
            "report_gemini_api_key",
            "report_groq_api_key",
            "report_anthropic_api_key",
            "report_openai_api_key"
    );

    private final HqChatbotAiSettingsRepository repository;
    private final ObjectMapper objectMapper;

    public HqChatbotAiSettingsService(HqChatbotAiSettingsRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public HqChatbotAiSettings getOrCreate() {
        return repository.findById(1L).orElseGet(() -> {
            HqChatbotAiSettings n = new HqChatbotAiSettings();
            n.setId(1L);
            n.setConfigJson("{}");
            return repository.save(n);
        });
    }

    private Map<String, Object> parseConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> m = objectMapper.readValue(raw, new TypeReference<>() {});
            return m != null ? new LinkedHashMap<>(m) : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private void ensureDefaults(Map<String, Object> m) {
        if (!m.containsKey(ChatbotLlmUsage.LEGACY_ORDER_KEY) || !(m.get(ChatbotLlmUsage.LEGACY_ORDER_KEY) instanceof List<?>)) {
            m.put(ChatbotLlmUsage.LEGACY_ORDER_KEY, new ArrayList<>(DEFAULT_PROVIDER_ORDER));
        }
        List<LlmProviderSlot> legacySlots = readOrderSlots(m.get(ChatbotLlmUsage.LEGACY_ORDER_KEY), m);
        for (String usageKey : USAGE_ORDER_KEYS) {
            if (!m.containsKey(usageKey) || !(m.get(usageKey) instanceof List<?>)) {
                m.put(usageKey, slotsToPersistList(legacySlots.isEmpty()
                        ? defaultLegacySlots(m)
                        : legacySlots));
            } else {
                m.put(usageKey, slotsToPersistList(readOrderSlots(m.get(usageKey), m)));
            }
        }
        ensureChatbotPricingDefaults(m);
    }

    private static List<LlmProviderSlot> defaultLegacySlots(Map<String, Object> m) {
        List<LlmProviderSlot> out = new ArrayList<>();
        for (String prov : DEFAULT_PROVIDER_ORDER) {
            out.add(new LlmProviderSlot(prov, LlmProviderSlot.defaultModelForProvider(prov, m)));
        }
        return out;
    }

    private static void ensureChatbotPricingDefaults(Map<String, Object> m) {
        Map<String, Object> norm = ChatbotProductPricingUtil.normalizeSlotsPricingForJsonPersistence(
                m.get(ChatbotProductPricingUtil.CONFIG_KEY_SLOTS_PRICING));
        m.put(ChatbotProductPricingUtil.CONFIG_KEY_SLOTS_PRICING, norm);
    }

    /**
     * API 응답 — 원문 API 키는 노출하지 않고 *_set 플래그만 포함.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> toMaskedMap(HqChatbotAiSettings row) {
        Map<String, Object> m = parseConfig(row.getConfigJson());
        ensureDefaults(m);
        Map<String, Object> out = new LinkedHashMap<>(m);
        for (String k : API_KEYS) {
            String v = m.get(k) != null ? String.valueOf(m.get(k)).trim() : "";
            out.remove(k);
            String flag = keyToConfiguredFlag(k);
            out.put(flag, !v.isEmpty());
        }
        return out;
    }

    /** JSON 필드명 {@code report_*_api_key} 대응 플래그 — API 응답용 */
    private static String keyToConfiguredFlag(String apiKeyField) {
        return apiKeyField.replace("_api_key", "_api_key_configured");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> rawConfigForServerUse() {
        HqChatbotAiSettings row = getOrCreate();
        Map<String, Object> m = parseConfig(row.getConfigJson());
        ensureDefaults(m);
        return m;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public HqChatbotAiSettings saveFromBody(Map<String, Object> body) {
        HqChatbotAiSettings row = getOrCreate();
        Map<String, Object> cur = parseConfig(row.getConfigJson());
        ensureDefaults(cur);
        if (body == null) {
            body = Map.of();
        }

        List<LlmProviderSlot> orderCatalog = readOrderSlots(body.get(KEY_ORDER_CATALOG), cur);
        if (!orderCatalog.isEmpty()) {
            List<Map<String, Object>> sanitized = sanitizeOrderSlots(orderCatalog);
            cur.put(KEY_ORDER_CATALOG, sanitized);
            cur.put(ChatbotLlmUsage.LEGACY_ORDER_KEY, legacyProvidersFromSlots(orderCatalog));
        }
        List<LlmProviderSlot> orderShort = readOrderSlots(body.get(KEY_ORDER_SHORT), cur);
        if (!orderShort.isEmpty()) {
            cur.put(KEY_ORDER_SHORT, sanitizeOrderSlots(orderShort));
        }
        List<LlmProviderSlot> orderGeneral = readOrderSlots(body.get(KEY_ORDER_GENERAL), cur);
        if (!orderGeneral.isEmpty()) {
            cur.put(KEY_ORDER_GENERAL, sanitizeOrderSlots(orderGeneral));
        }
        List<LlmProviderSlot> orderPlatform = readOrderSlots(body.get(KEY_ORDER_PLATFORM), cur);
        if (!orderPlatform.isEmpty()) {
            cur.put(KEY_ORDER_PLATFORM, sanitizeOrderSlots(orderPlatform));
        }
        List<String> orderLegacy = readStringList(body.get(ChatbotLlmUsage.LEGACY_ORDER_KEY));
        if (!orderLegacy.isEmpty() && orderCatalog.isEmpty()) {
            List<String> sanitized = sanitizeProviderOrder(orderLegacy);
            cur.put(ChatbotLlmUsage.LEGACY_ORDER_KEY, sanitized);
            if (!cur.containsKey(KEY_ORDER_CATALOG)) {
                List<LlmProviderSlot> legacySlots = new ArrayList<>();
                for (String prov : sanitized) {
                    legacySlots.add(new LlmProviderSlot(prov, LlmProviderSlot.defaultModelForProvider(prov, cur)));
                }
                cur.put(KEY_ORDER_CATALOG, sanitizeOrderSlots(legacySlots));
            }
        }

        for (String pk : API_KEYS) {
            if (!body.containsKey(pk)) {
                continue;
            }
            Object pv = body.get(pk);
            if (pv == null) {
                continue;
            }
            String s = String.valueOf(pv).trim();
            if (s.isEmpty()) {
                continue; // 빈 문자열 = 기존 키 유지 (폼 미변경)
            }
            if ("__CLEAR__".equals(s)) {
                cur.remove(pk);
                continue;
            }
            cur.put(pk, s);
        }

        for (String suf : List.of("gemini", "groq", "anthropic", "openai")) {
            String mk = "report_" + suf + "_model";
            if (body.containsKey(mk) && body.get(mk) != null) {
                String mv = String.valueOf(body.get(mk)).trim();
                cur.put(mk, mv.length() <= 200 ? mv : mv.substring(0, 200));
            }
        }

        for (String suf : List.of("gemini", "groq", "anthropic", "openai")) {
            String dk = "report_" + suf + "_disabled";
            if (body.containsKey(dk)) {
                cur.put(dk, normalizeReportProviderDisabled(body.get(dk)));
            }
        }

        putIfPresentString(cur, body, "ai_system_prompt_chatbot");
        putIfPresentString(cur, body, "ai_prompt_chatbot_catalog");

        if (body.get("ai_system_options_chatbot") instanceof Map<?, ?> om) {
            cur.put("ai_system_options_chatbot", objectMapper.convertValue(om, new TypeReference<Map<String, Object>>() {}));
        }

        if (body.containsKey(ChatbotProductPricingUtil.CONFIG_KEY_SLOTS_PRICING)) {
            Object nested = ChatbotProductPricingUtil.normalizeSlotsPricingForJsonPersistence(
                    body.get(ChatbotProductPricingUtil.CONFIG_KEY_SLOTS_PRICING));
            cur.put(ChatbotProductPricingUtil.CONFIG_KEY_SLOTS_PRICING, nested);
        }
        ensureChatbotPricingDefaults(cur);

        row.setConfigJson(writeJson(cur));
        return repository.save(row);
    }

    private static void putIfPresentString(Map<String, Object> cur, Map<String, Object> body, String key) {
        if (!body.containsKey(key)) {
            return;
        }
        Object v = body.get(key);
        if (v == null) {
            return;
        }
        cur.put(key, String.valueOf(v));
    }

    private List<LlmProviderSlot> readOrderSlots(Object raw, Map<String, Object> cfg) {
        return ChatbotLlmProviderOrderUtil.parseSlotList(raw, cfg);
    }

    private static List<Map<String, Object>> slotsToPersistList(List<LlmProviderSlot> slots) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (slots == null) {
            return out;
        }
        for (LlmProviderSlot slot : slots) {
            if (slot == null || !slot.isUsable()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("provider", slot.provider());
            row.put("model", slot.model() != null ? slot.model() : "");
            out.add(row);
        }
        return out;
    }

    private static List<String> legacyProvidersFromSlots(List<LlmProviderSlot> slots) {
        List<String> out = new ArrayList<>();
        if (slots == null) {
            return out;
        }
        for (LlmProviderSlot slot : slots) {
            if (slot != null && slot.isUsable() && !out.contains(slot.provider())) {
                out.add(slot.provider());
            }
        }
        return out;
    }

    private static List<Map<String, Object>> sanitizeOrderSlots(List<LlmProviderSlot> order) {
        return slotsToPersistList(order);
    }

    private List<String> readStringList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> ls) {
            List<String> o = new ArrayList<>();
            for (Object x : ls) {
                if (x != null) {
                    String p = String.valueOf(x).trim().toLowerCase(Locale.ROOT);
                    if ("gemini".equals(p) || "groq".equals(p) || "anthropic".equals(p) || "openai".equals(p)) {
                        o.add(p);
                    }
                }
            }
            return dedupeProviders(o);
        }
        return List.of();
    }

    private static List<String> dedupeProviders(List<String> in) {
        List<String> o = new ArrayList<>();
        for (String p : in) {
            if (!o.contains(p)) {
                o.add(p);
            }
        }
        return o;
    }

    /** JSON 저장용: 사용중지 플래그(true/false). */
    private static boolean normalizeReportProviderDisabled(Object raw) {
        if (raw == null) {
            return false;
        }
        if (raw instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        return "y".equals(s) || "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }

    private static List<String> sanitizeProviderOrder(List<String> order) {
        List<String> o = dedupeProviders(order);
        for (String p : DEFAULT_PROVIDER_ORDER) {
            if (!o.contains(p)) {
                o.add(p);
            }
        }
        return o;
    }

    private String writeJson(Map<String, Object> m) {
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("config_json serialization failed", e);
        }
    }

    /** 총본사·ADMIN 전용 저장 — 컨트롤러에서 호출 전 검사 */
    public static boolean mayEditHqAiSettings(AppUser user, Map<String, Object> org) {
        if (user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        if (org == null) {
            return false;
        }
        String ol = String.valueOf(org.getOrDefault("orgLevel", "")).trim().toUpperCase(Locale.ROOT);
        return "HEADQUARTERS".equals(ol);
    }
}
