package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.entity.HqChatbotAiSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 로그인 첫 화면 공지용 — 본사 AI(LLM)로 KO/EN/JP/CH/TH 제목·본문 JSON 생성.
 * LLM 실패 시 원문을 모든 언어 슬롯에 복제한 JSON으로 대체합니다.
 */
@Service
public class NoticeLoginI18nService {

    private static final Logger log = LoggerFactory.getLogger(NoticeLoginI18nService.class);
    private static final List<String> LANGS = List.of("KO", "EN", "JP", "CH", "TH");

    private final ObjectMapper objectMapper;
    private final HqChatbotAiSettingsService hqChatbotAiSettingsService;
    private final ChatbotLlmCompletionService chatbotLlmCompletionService;

    public NoticeLoginI18nService(ObjectMapper objectMapper,
                                  HqChatbotAiSettingsService hqChatbotAiSettingsService,
                                  ChatbotLlmCompletionService chatbotLlmCompletionService) {
        this.objectMapper = objectMapper;
        this.hqChatbotAiSettingsService = hqChatbotAiSettingsService;
        this.chatbotLlmCompletionService = chatbotLlmCompletionService;
    }

    public String buildLoginI18nJson(String title, String bodyPlain) {
        String t = title != null ? title : "";
        String b = bodyPlain != null ? bodyPlain : "";
        try {
            HqChatbotAiSettings hq = hqChatbotAiSettingsService.getOrCreate();
            Map<String, Object> rawConfig = parseJsonAsMap(hq.getConfigJson());
            String system = buildSystemPrompt();
            String userMsg = "TITLE:\n" + t + "\n\nBODY:\n" + b;
            List<Map<String, String>> dialogue = List.of(Map.of("role", "user", "content", userMsg));
            String reply = chatbotLlmCompletionService.completeChat(rawConfig, system, dialogue);
            String json = extractJsonObject(reply);
            if (json != null && validateShape(json)) {
                return json;
            }
        } catch (Exception e) {
            log.warn("Notice login i18n LLM failed: {}", e.getMessage());
        }
        try {
            return fallbackJson(t, b);
        } catch (Exception e2) {
            log.warn("Notice login i18n fallback failed: {}", e2.getMessage());
            try {
                return objectMapper.writeValueAsString(Map.of(
                        "titles", Map.of("KO", t, "EN", t, "JP", t, "CH", t, "TH", t),
                        "bodies", Map.of("KO", b, "EN", b, "JP", b, "CH", b, "TH", b)));
            } catch (Exception e3) {
                return "{}";
            }
        }
    }

    private boolean validateShape(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                return false;
            }
            JsonNode titles = root.get("titles");
            JsonNode bodies = root.get("bodies");
            if (titles == null || !titles.isObject() || bodies == null || !bodies.isObject()) {
                return false;
            }
            for (String lang : LANGS) {
                if (!titles.has(lang) || !titles.get(lang).isTextual()) {
                    return false;
                }
                if (!bodies.has(lang) || !bodies.get(lang).isTextual()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String buildSystemPrompt() {
        return """
                You translate a short admin notice into Korean(KO), English(EN), Japanese(JP), Chinese simplified(CH), Thai(TH).
                Reply with ONLY one JSON object, no markdown fences, no explanation. Exact shape:
                {"titles":{"KO":"","EN":"","JP":"","CH":"","TH":""},"bodies":{"KO":"","EN":"","JP":"","CH":"","TH":""}}
                Rules: Preserve meaning. bodies.* use plain text only; use \\n for line breaks inside JSON strings. Escape double quotes in strings.
                """.strip();
    }

    private static String extractJsonObject(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) {
                s = s.substring(nl + 1).trim();
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3).trim();
            }
        }
        int i = s.indexOf('{');
        int j = s.lastIndexOf('}');
        if (i >= 0 && j > i) {
            return s.substring(i, j + 1);
        }
        return s;
    }

    private String fallbackJson(String title, String body) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode titles = root.putObject("titles");
        ObjectNode bodies = root.putObject("bodies");
        for (String lang : LANGS) {
            titles.put(lang, title);
            bodies.put(lang, body);
        }
        return objectMapper.writeValueAsString(root);
    }

    private Map<String, Object> parseJsonAsMap(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> m = objectMapper.readValue(raw, new TypeReference<>() {});
        return m != null ? new LinkedHashMap<>(m) : new LinkedHashMap<>();
    }
}
