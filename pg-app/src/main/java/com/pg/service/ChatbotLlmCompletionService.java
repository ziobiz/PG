package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.util.ChatbotLlmProviderOrderUtil;
import com.pg.util.ChatbotLlmUsage;
import com.pg.util.LlmProviderSlot;
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
 * <p>API 키가 둘 이상이면(예: Gemini+Groq) 한쪽이 429·5xx 등으로 막힐 때 같은 요청 안에서
 * 다른 프로바이더를 이어 시도하는 <strong>혼용(페일오버·순환)</strong>을 합니다.</p>
 */
@Service
public class ChatbotLlmCompletionService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotLlmCompletionService.class);

    /**
     * 공개 챗봇 등에 그대로 노출해도 되는 안내 — Groq/OpenAI 원문 JSON(429 rate limit 등)은 절대 넣지 않습니다.
     */
    public static final String PUBLIC_MSG_RATE_LIMIT =
            "요청이 많아 AI 응답이 잠시 제한되었습니다. 잠시 후 다시 시도해 주세요.\n"
                    + "The AI service is temporarily busy. Please try again in a moment.";

    public static final String PUBLIC_MSG_UPSTREAM_UNAVAILABLE =
            "AI 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.\n"
                    + "The AI service is temporarily unavailable. Please try again shortly.";

    public static final String PUBLIC_MSG_UPSTREAM =
            "AI 응답을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.\n"
                    + "We could not get an AI reply. Please try again shortly.";

    private static final Duration TIMEOUT = Duration.ofSeconds(25);

    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ChatbotLlmCompletionService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 결제내역 처리사유 번역 — ICOPAY 그외 서비스 순위의 2순위 → 1순위. 모두 실패 시 {@code null}(호출부가 원문 유지).
     */
    public String translateOutcomeReason(Map<String, Object> rawAiConfig, String text, String targetLanguageName) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (targetLanguageName == null || targetLanguageName.isBlank()) {
            return null;
        }
        List<LlmProviderSlot> slots = ChatbotLlmProviderOrderUtil.resolveOrderSlots(rawAiConfig, ChatbotLlmUsage.PLATFORM);
        List<LlmProviderSlot> trySlots = outcomeReasonSlotTryOrder(slots, rawAiConfig);
        if (trySlots.isEmpty()) {
            return null;
        }
        String system = "You translate payment-gateway failure/cancel/refund messages for admin staff. "
                + "Output ONLY the translated text in " + targetLanguageName
                + ". No quotes, labels, or explanation.";
        String user = text.trim();
        Exception last = null;
        for (LlmProviderSlot slot : trySlots) {
            try {
                String reply = invokeProvider(slot, rawAiConfig, system,
                        List.of(Map.of("role", "user", "content", user)));
                if (reply != null && !reply.isBlank()) {
                    return reply.trim();
                }
                log.warn("outcome reason translate provider={} model={} returned empty",
                        slot.provider(), slot.resolvedModel(rawAiConfig));
            } catch (Exception e) {
                last = e;
                log.warn("outcome reason translate provider={} failed: {}", slot.provider(), abbrev(e.getMessage(), 500));
            }
        }
        if (last != null) {
            log.debug("outcome reason translate exhausted providers: {}", abbrev(last.getMessage(), 200));
        }
        return null;
    }

    /**
     * 처리사유 번역용 시도 순서 — 설정 순위의 2순위 → 1순위(비사용·키 없음은 제외).
     */
    public static List<LlmProviderSlot> outcomeReasonSlotTryOrder(List<LlmProviderSlot> order, Map<String, Object> cfg) {
        List<LlmProviderSlot> available = filterAvailableSlots(order != null ? order : List.of(), cfg);
        List<LlmProviderSlot> tryOrder = new ArrayList<>(2);
        if (available.size() >= 2) {
            tryOrder.add(available.get(1));
            tryOrder.add(available.get(0));
        } else if (available.size() == 1) {
            tryOrder.add(available.get(0));
        }
        return tryOrder;
    }

    /** @deprecated {@link #outcomeReasonSlotTryOrder(List, Map)} */
    @Deprecated
    public static List<String> outcomeReasonProviderTryOrder(List<String> orderRaw, Map<String, Object> cfg) {
        List<LlmProviderSlot> slots = new ArrayList<>();
        if (orderRaw != null) {
            for (String prov : orderRaw) {
                slots.add(new LlmProviderSlot(prov, LlmProviderSlot.defaultModelForProvider(prov, cfg)));
            }
        }
        List<String> out = new ArrayList<>();
        for (LlmProviderSlot slot : outcomeReasonSlotTryOrder(slots, cfg)) {
            out.add(slot.provider());
        }
        return out;
    }

    private static List<LlmProviderSlot> filterAvailableSlots(List<LlmProviderSlot> order, Map<String, Object> cfg) {
        List<LlmProviderSlot> available = new ArrayList<>();
        for (LlmProviderSlot slot : order) {
            if (slot == null || !slot.isUsable()) {
                continue;
            }
            if (isProviderDisabled(slot.provider(), cfg)) {
                continue;
            }
            if (!providerHasApiKey(slot.provider(), cfg)) {
                continue;
            }
            available.add(slot);
        }
        return available;
    }

    public String completeChat(Map<String, Object> rawAiConfig,
                               String systemPrompt,
                               List<Map<String, String>> dialogueMessages) throws Exception {
        return completeChat(rawAiConfig, systemPrompt, dialogueMessages, ChatbotLlmUsage.CATALOG);
    }

    public String completeChat(Map<String, Object> rawAiConfig,
                               String systemPrompt,
                               List<Map<String, String>> dialogueMessages,
                               ChatbotLlmUsage usage) throws Exception {
        if (systemPrompt == null) {
            systemPrompt = "";
        }
        List<Map<String, String>> trimmed = trimMessages(dialogueMessages, 24);
        List<LlmProviderSlot> slots = ChatbotLlmProviderOrderUtil.resolveOrderSlots(rawAiConfig,
                usage != null ? usage : ChatbotLlmUsage.CATALOG);
        ArrayDeque<LlmProviderSlot> queue = new ArrayDeque<>();
        for (LlmProviderSlot slot : slots) {
            if (isProviderDisabled(slot.provider(), rawAiConfig)) {
                log.debug("chatbot llm skip provider={} (disabled in HQ AI settings)", slot.provider());
                continue;
            }
            if (!providerHasApiKey(slot.provider(), rawAiConfig)) {
                log.debug("chatbot llm skip provider={} (no API key configured)", slot.provider());
                continue;
            }
            queue.addLast(slot);
        }
        if (queue.isEmpty()) {
            throw new IllegalStateException(
                    "사용 가능한 LLM 설정이 없습니다. 본사 AI설정에서 API 키를 등록하거나, 해당 제공자의 「사용중지」를 해제하세요.");
        }
        int initialKeyed = queue.size();
        int maxAttempts = Math.max(initialKeyed * 4, 8);
        Exception last = null;
        int invokedWithKey = 0;
        while (!queue.isEmpty() && invokedWithKey < maxAttempts) {
            LlmProviderSlot slot = queue.pollFirst();
            invokedWithKey++;
            try {
                String reply = invokeProvider(slot, rawAiConfig, systemPrompt, trimmed);
                if (reply != null && !reply.isBlank()) {
                    return reply.trim();
                }
                log.warn("chatbot llm provider={} model={} returned empty completion",
                        slot.provider(), slot.resolvedModel(rawAiConfig));
            } catch (Exception e) {
                last = e;
                log.warn("chatbot llm provider={} failed: {}", slot.provider(), abbrev(e.getMessage(), 500));
                if (shouldRotateProviderForHybrid(e) && !queue.isEmpty()) {
                    queue.addLast(slot);
                }
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException(
                "LLM 응답이 비어 있습니다. 잠시 후 다시 시도하거나 본사 AI설정(모델·키)을 확인하세요.");
    }

    /** 429·상류 5xx 등: 같은 요청에서 다른 API 키(예: Gemini)를 먼저 시도하도록 큐 뒤로 돌립니다. */
    private static boolean shouldRotateProviderForHybrid(Throwable e) {
        if (!(e instanceof IllegalStateException ise)) {
            return false;
        }
        String m = ise.getMessage();
        return PUBLIC_MSG_RATE_LIMIT.equals(m) || PUBLIC_MSG_UPSTREAM_UNAVAILABLE.equals(m);
    }

    /** {@code report_{provider}_disabled} 가 Y/true 등이면 해당 프로바이더(및 설정 모델)를 호출하지 않습니다. */
    private static boolean isProviderDisabled(String provider, Map<String, Object> cfg) {
        if (cfg == null || provider == null) {
            return false;
        }
        String p = provider.trim().toLowerCase(Locale.ROOT);
        Object v = cfg.get("report_" + p + "_disabled");
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        return "y".equals(s) || "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }

    /** API 키가 없는 프로바이더는 호출하지 않음 — 선행 프로바이더가 빈 응답일 때 오해의 소지가 있는 \"API key missing\" 연쇄를 막습니다. */
    private static boolean providerHasApiKey(String provider, Map<String, Object> cfg) {
        if (cfg == null || provider == null) {
            return false;
        }
        String p = provider.trim().toLowerCase(Locale.ROOT);
        String key = switch (p) {
            case "groq" -> stringVal(cfg.get("report_groq_api_key"));
            case "openai" -> stringVal(cfg.get("report_openai_api_key"));
            case "anthropic" -> stringVal(cfg.get("report_anthropic_api_key"));
            case "gemini" -> stringVal(cfg.get("report_gemini_api_key"));
            default -> null;
        };
        return key != null && !key.isBlank();
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

    private String invokeProvider(LlmProviderSlot slot,
                                  Map<String, Object> cfg,
                                  String systemPrompt,
                                  List<Map<String, String>> messages) throws Exception {
        return invokeProvider(slot.provider(), cfg, slot.resolvedModel(cfg), systemPrompt, messages);
    }

    private String invokeProvider(String provider,
                                  Map<String, Object> cfg,
                                  String systemPrompt,
                                  List<Map<String, String>> messages) throws Exception {
        return invokeProvider(provider, cfg, null, systemPrompt, messages);
    }

    private String invokeProvider(String provider,
                                  Map<String, Object> cfg,
                                  String modelOverride,
                                  String systemPrompt,
                                  List<Map<String, String>> messages) throws Exception {
        return switch (provider) {
            case "groq" -> openAiCompatible(
                    "groq",
                    "https://api.groq.com/openai/v1/chat/completions",
                    stringVal(cfg.get("report_groq_api_key")),
                    firstNonBlank(modelOverride, "llama-3.1-8b-instant"),
                    systemPrompt,
                    messages);
            case "openai" -> openAiCompatible(
                    "openai",
                    "https://api.openai.com/v1/chat/completions",
                    stringVal(cfg.get("report_openai_api_key")),
                    firstNonBlank(modelOverride, "gpt-4o-mini"),
                    systemPrompt,
                    messages);
            case "anthropic" -> anthropic(
                    stringVal(cfg.get("report_anthropic_api_key")),
                    firstNonBlank(modelOverride, "claude-3-5-sonnet-20241022"),
                    systemPrompt,
                    messages);
            case "gemini" -> gemini(
                    stringVal(cfg.get("report_gemini_api_key")),
                    firstNonBlank(modelOverride, "gemini-3.5-flash"),
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

    /** HTTP 오류 시 응답 본문은 로그에만 남기고, 고객에게는 짧은 공개 메시지로만 예외를 던집니다. */
    private static IllegalStateException upstreamHttp(String providerId, int status, String rawBody) {
        String logBody = abbrev(rawBody, 2000);
        log.warn("chatbot llm upstream provider={} httpStatus={} body={}", providerId, status, logBody);
        if (status == 429) {
            return new IllegalStateException(PUBLIC_MSG_RATE_LIMIT);
        }
        if (status == 503 || status == 502 || status == 504 || status >= 500) {
            return new IllegalStateException(PUBLIC_MSG_UPSTREAM_UNAVAILABLE);
        }
        return new IllegalStateException(PUBLIC_MSG_UPSTREAM);
    }

    private String openAiCompatible(String providerId, String url, String apiKey, String model, String system,
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
            throw upstreamHttp(providerId, res.statusCode(), res.body());
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
            throw upstreamHttp("anthropic", res.statusCode(), res.body());
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
            throw upstreamHttp("gemini", res.statusCode(), res.body());
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
