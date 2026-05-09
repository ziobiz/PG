package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * 본사 {@code tb_hq_chatbot_ai_settings.config_json} 키·프로바이더 순서로 외부 LLM 호출 (챗봇 공개 API용).
 */
@Service
public class ChatbotLlmCompletionService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotLlmCompletionService.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(25);

    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ChatbotLlmCompletionService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String completeChat(Map<String, Object> rawAiConfig,
                               String systemPrompt,
                               List<Map<String, String>> dialogueMessages) throws Exception {
        if (systemPrompt == null) {
            systemPrompt = "";
        }
        List<Map<String, String>> trimmed = trimMessages(dialogueMessages, 24);
        @SuppressWarnings("unchecked")
        List<String> orderRaw = rawAiConfig.get("report_provider_order") instanceof List<?> ls
                ? (List<String>) rawAiConfig.get("report_provider_order")
                : List.of();
        List<String> order = normalizeOrder(orderRaw);
        Exception last = null;
        for (String prov : order) {
            try {
                String reply = invokeProvider(prov, rawAiConfig, systemPrompt, trimmed);
                if (reply != null && !reply.isBlank()) {
                    return reply.trim();
                }
            } catch (Exception e) {
                last = e;
                log.warn("chatbot llm provider={} failed: {}", prov, e.getMessage());
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("사용 가능한 LLM 설정이 없습니다. 본사 AI설정에서 API 키·모델을 등록하세요.");
    }

    private static List<String> normalizeOrder(List<String> raw) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (Object o : raw) {
            if (o == null) {
                continue;
            }
            String p = String.valueOf(o).trim().toLowerCase(Locale.ROOT);
            if ("gemini".equals(p) || "groq".equals(p) || "anthropic".equals(p) || "openai".equals(p)) {
                seen.add(p);
            }
        }
        for (String d : List.of("gemini", "groq", "anthropic", "openai")) {
            seen.add(d);
        }
        return new ArrayList<>(seen);
    }

    private static List<Map<String, String>> trimMessages(List<Map<String, String>> in, int max) {
        if (in == null || in.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> out = new ArrayList<>();
        int start = Math.max(0, in.size() - max);
        for (int i = start; i < in.size(); i++) {
            Map<String, String> m = in.get(i);
            if (m == null) {
                continue;
            }
            String role = m.get("role");
            String content = m.get("content");
            if (role == null || content == null || content.isBlank()) {
                continue;
            }
            String rl = role.trim().toLowerCase(Locale.ROOT);
            if (!"user".equals(rl) && !"assistant".equals(rl)) {
                continue;
            }
            out.add(Map.of("role", rl, "content", content.trim()));
        }
        return out;
    }

    private String invokeProvider(String provider,
                                  Map<String, Object> cfg,
                                  String systemPrompt,
                                  List<Map<String, String>> messages) throws Exception {
        return switch (provider) {
            case "groq" -> openAiCompatible(
                    "https://api.groq.com/openai/v1/chat/completions",
                    stringVal(cfg.get("report_groq_api_key")),
                    firstNonBlank(stringVal(cfg.get("report_groq_model")), "llama-3.1-8b-instant"),
                    systemPrompt,
                    messages);
            case "openai" -> openAiCompatible(
                    "https://api.openai.com/v1/chat/completions",
                    stringVal(cfg.get("report_openai_api_key")),
                    firstNonBlank(stringVal(cfg.get("report_openai_model")), "gpt-4o-mini"),
                    systemPrompt,
                    messages);
            case "anthropic" -> anthropic(
                    stringVal(cfg.get("report_anthropic_api_key")),
                    firstNonBlank(stringVal(cfg.get("report_anthropic_model")), "claude-3-5-sonnet-20241022"),
                    systemPrompt,
                    messages);
            case "gemini" -> gemini(
                    stringVal(cfg.get("report_gemini_api_key")),
                    firstNonBlank(stringVal(cfg.get("report_gemini_model")), "gemini-1.5-flash"),
                    systemPrompt,
                    messages);
            default -> throw new IllegalArgumentException("unknown provider " + provider);
        };
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

    private String openAiCompatible(String url, String apiKey, String model, String system,
                                    List<Map<String, String>> messages) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API key missing");
        }
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        ArrayNode arr = mapper.createArrayNode();
        ObjectNode sys = mapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content", system);
        arr.add(sys);
        for (Map<String, String> m : messages) {
            ObjectNode one = mapper.createObjectNode();
            one.put("role", m.get("role"));
            one.put("content", m.get("content"));
            arr.add(one);
        }
        root.set("messages", arr);
        root.put("temperature", 0.4);
        String body = mapper.writeValueAsString(root);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + res.statusCode() + " " + abbrev(res.body(), 400));
        }
        JsonNode tree = mapper.readTree(res.body());
        JsonNode choice0 = tree.path("choices").path(0).path("message").path("content");
        if (choice0.isTextual()) {
            return choice0.asText();
        }
        return null;
    }

    private String anthropic(String apiKey, String model, String system,
                             List<Map<String, String>> messages) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API key missing");
        }
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 1024);
        if (system != null && !system.isBlank()) {
            root.put("system", system);
        }
        ArrayNode arr = mapper.createArrayNode();
        for (Map<String, String> m : messages) {
            ObjectNode one = mapper.createObjectNode();
            one.put("role", m.get("role"));
            ArrayNode content = mapper.createArrayNode();
            ObjectNode text = mapper.createObjectNode();
            text.put("type", "text");
            text.put("text", m.get("content"));
            content.add(text);
            one.set("content", content);
            arr.add(one);
        }
        root.set("messages", arr);
        String body = mapper.writeValueAsString(root);
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + res.statusCode() + " " + abbrev(res.body(), 400));
        }
        JsonNode tree = mapper.readTree(res.body());
        JsonNode block0 = tree.path("content").path(0).path("text");
        if (block0.isTextual()) {
            return block0.asText();
        }
        return null;
    }

    private String gemini(String apiKey, String model, String system,
                          List<Map<String, String>> messages) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API key missing");
        }
        String encModel = java.net.URLEncoder.encode(model, StandardCharsets.UTF_8);
        String u = "https://generativelanguage.googleapis.com/v1beta/models/" + encModel
                + ":generateContent?key=" + java.net.URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        ObjectNode root = mapper.createObjectNode();
        ArrayNode contents = mapper.createArrayNode();
        if (system != null && !system.isBlank()) {
            ObjectNode sysInst = mapper.createObjectNode();
            ArrayNode sp = mapper.createArrayNode();
            ObjectNode pt = mapper.createObjectNode();
            pt.put("text", system);
            sp.add(pt);
            sysInst.set("parts", sp);
            root.set("systemInstruction", sysInst);
        }
        for (Map<String, String> m : messages) {
            ObjectNode c = mapper.createObjectNode();
            String role = "user".equals(m.get("role")) ? "user" : "model";
            c.put("role", role);
            ObjectNode p = mapper.createObjectNode();
            p.put("text", m.get("content"));
            ArrayNode ps = mapper.createArrayNode();
            ps.add(p);
            c.set("parts", ps);
            contents.add(c);
        }
        root.set("contents", contents);
        String body = mapper.writeValueAsString(root);
        HttpRequest req = HttpRequest.newBuilder(URI.create(u))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + res.statusCode() + " " + abbrev(res.body(), 400));
        }
        JsonNode tree = mapper.readTree(res.body());
        JsonNode text = tree.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text.isTextual()) {
            return text.asText();
        }
        return null;
    }

    private static String abbrev(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ');
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
