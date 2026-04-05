package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 결제 노티 파라미터 → 결제내역 그리드 열 key 매핑을 OpenAI 호환 Chat Completions API로 제안합니다.
 * {@code app.notify-mapping-ai.api-key} 미설정 시 비활성입니다.
 */
@Service
public class NotifyMappingAiService {

    private static final Logger log = LoggerFactory.getLogger(NotifyMappingAiService.class);
    private static final int MAX_SAMPLE_CHARS = 12_000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${app.notify-mapping-ai.api-key:}")
    private String apiKey;

    @Value("${app.notify-mapping-ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.notify-mapping-ai.model:gpt-4o-mini}")
    private String model;

    @Value("${app.notify-mapping-ai.enabled:true}")
    private boolean enabled;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * @return empty if not configured, on HTTP/parse failure, or model returns nothing usable
     */
    public List<Map<String, Object>> suggestWithAi(String vendorCode,
                                                   List<String> paramNames,
                                                   String sampleJson,
                                                   List<Map<String, String>> columnKeyLabels,
                                                   Set<String> allowedInternalKeys) {
        if (!isConfigured() || paramNames == null || paramNames.isEmpty()
                || allowedInternalKeys == null || allowedInternalKeys.isEmpty()) {
            return List.of();
        }
        try {
            String prompt = buildPrompt(vendorCode, paramNames, sampleJson, columnKeyLabels);
            String raw = callChatCompletions(prompt);
            if (raw == null || raw.isBlank()) {
                return List.of();
            }
            return parseAndValidate(raw, paramNames, allowedInternalKeys);
        } catch (Exception e) {
            log.warn("노티매핑 AI 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildPrompt(String vendorCode,
                               List<String> paramNames,
                               String sampleJson,
                               List<Map<String, String>> columnKeyLabels) throws Exception {
        String cols = objectMapper.writeValueAsString(columnKeyLabels);
        String params = objectMapper.writeValueAsString(paramNames);
        String sample = sampleJson == null ? "" : sampleJson;
        if (sample.length() > MAX_SAMPLE_CHARS) {
            sample = sample.substring(0, MAX_SAMPLE_CHARS) + "\n...(truncated)";
        }
        return """
                You map payment gateway notify/callback JSON field names to internal grid column keys for a Korean PG admin system.

                PG vendor code: %s

                Allowed target column keys (use ONLY these as internalKey; each internalKey at most once):
                %s

                Parameter names observed in notify JSON (map only these as pgField, use exact spelling from this list):
                %s

                Sample notify JSON (may be truncated):
                %s

                Task: For each parameter that should store or display merchant pay-list data, pick the single best internalKey.
                Skip fields that are purely security (signature, hash, token) unless they map to a clear display column.

                Respond with ONLY a JSON array (no markdown fences), format:
                [{"pgField":"ExactNameFromParameterList","internalKey":"allowed_key","note":"Korean one-line reason"}]
                If nothing maps, return [].
                """.formatted(
                vendorCode != null ? vendorCode : "",
                cols,
                params,
                sample.replace("```", "'"));
    }

    private String callChatCompletions(String userPrompt) throws Exception {
        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (!url.endsWith("/v1") && !url.contains("/v1/")) {
            url = url + "/v1";
        }
        String endpoint = url + "/chat/completions";

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model != null ? model : "gpt-4o-mini");
        root.put("temperature", 0.15);
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content",
                "You are a precise JSON generator. Output only a JSON array, no markdown, no explanation.");
        messages.addObject().put("role", "user").put("content", userPrompt);

        String body = objectMapper.writeValueAsString(root);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            log.warn("OpenAI HTTP {}: {}", resp.statusCode(), resp.body() != null && resp.body().length() > 200
                    ? resp.body().substring(0, 200) : resp.body());
            return null;
        }
        JsonNode tree = objectMapper.readTree(resp.body());
        JsonNode choices = tree.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).path("message").path("content").asText("");
    }

    private List<Map<String, Object>> parseAndValidate(String content,
                                                       List<String> paramNames,
                                                       Set<String> allowedInternalKeys) throws Exception {
        String t = content.trim();
        if (t.startsWith("```")) {
            int start = t.indexOf('\n');
            if (start > 0) {
                t = t.substring(start + 1).trim();
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3).trim();
            }
        }
        int a = t.indexOf('[');
        int b = t.lastIndexOf(']');
        if (a < 0 || b <= a) {
            return List.of();
        }
        JsonNode arr = objectMapper.readTree(t.substring(a, b + 1));
        if (!arr.isArray()) {
            return List.of();
        }
        Set<String> allowedPg = new LinkedHashSet<>();
        for (String p : paramNames) {
            if (p != null && !p.isBlank()) {
                allowedPg.add(p.trim());
            }
        }
        Set<String> usedKeys = new LinkedHashSet<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode item : arr) {
            if (!item.isObject()) {
                continue;
            }
            String pf = item.path("pgField").asText("").trim();
            String ik = item.path("internalKey").asText("").trim();
            if (pf.isEmpty() || ik.isEmpty()) {
                continue;
            }
            if (!allowedPg.contains(pf)) {
                continue;
            }
            if (!allowedInternalKeys.contains(ik)) {
                continue;
            }
            if (usedKeys.contains(ik)) {
                continue;
            }
            usedKeys.add(ik);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pgField", pf);
            row.put("internalKey", ik);
            String note = item.path("note").asText("").trim();
            row.put("note", note.isEmpty() ? "AI 제안" : note);
            out.add(row);
        }
        return out;
    }
}
