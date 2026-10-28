package com.pg.integration.pg.qxipay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * QXIPAY REST — tokens / charges / refunds / expire.
 * Base path already includes {@code /api/sandbox} or live prefix from credentials.
 */
@Component
public class QxipayApiClient {

    private static final Logger log = LoggerFactory.getLogger(QxipayApiClient.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public QxipayApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(15_000);
        f.setReadTimeout(45_000);
        this.restTemplate = new RestTemplate(f);
    }

    public Map<String, Object> createCardToken(QxipayCredentials creds, Map<String, Object> card) {
        if (creds == null || !creds.canTokenize()) {
            return err("QXIPAY_CREDENTIALS", "QXIPAY publishable key가 없습니다.");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("number", str(card.get("number")).replaceAll("\\s+", ""));
        body.put("name", str(card.get("name")));
        body.put("expMonth", intVal(card.get("expMonth"), card.get("exp_month")));
        body.put("expYear", intVal(card.get("expYear"), card.get("exp_year")));
        body.put("cvv", str(card.get("cvv"), card.get("cvc")));
        return exchange(creds, true, HttpMethod.POST, "/client/v1/tokens", body, null, null);
    }

    public Map<String, Object> createCharge(QxipayCredentials creds, Map<String, Object> chargeReq,
                                            String idempotencyKey, String sandboxOutcome) {
        if (creds == null || !creds.isUsable()) {
            return err("QXIPAY_CREDENTIALS", "QXIPAY secret key가 없습니다.");
        }
        ObjectNode body = objectMapper.valueToTree(chargeReq);
        return exchange(creds, false, HttpMethod.POST, "/v1/charges", body, idempotencyKey, sandboxOutcome);
    }

    public Map<String, Object> getCharge(QxipayCredentials creds, String chargeId) {
        if (creds == null || !creds.isUsable() || chargeId == null || chargeId.isBlank()) {
            return err("QXIPAY_CREDENTIALS", "chargeId가 필요합니다.");
        }
        return exchange(creds, false, HttpMethod.GET, "/v1/charges/" + chargeId.trim(), null, null, null);
    }

    public Map<String, Object> expireCharge(QxipayCredentials creds, String chargeId) {
        if (creds == null || !creds.isUsable() || chargeId == null || chargeId.isBlank()) {
            return err("QXIPAY_CREDENTIALS", "chargeId가 필요합니다.");
        }
        return exchange(creds, false, HttpMethod.POST, "/v1/charges/" + chargeId.trim() + "/expire",
                objectMapper.createObjectNode(), UUID.randomUUID().toString(), null);
    }

    public Map<String, Object> createRefund(QxipayCredentials creds, String chargeId, Map<String, Object> refundReq,
                                            String idempotencyKey) {
        if (creds == null || !creds.isUsable() || chargeId == null || chargeId.isBlank()) {
            return err("QXIPAY_CREDENTIALS", "chargeId가 필요합니다.");
        }
        ObjectNode body = refundReq != null
                ? objectMapper.valueToTree(refundReq)
                : objectMapper.createObjectNode();
        String key = idempotencyKey != null && !idempotencyKey.isBlank()
                ? idempotencyKey
                : UUID.randomUUID().toString();
        return exchange(creds, false, HttpMethod.POST,
                "/v1/charges/" + chargeId.trim() + "/refunds", body, key, null);
    }

    private Map<String, Object> exchange(QxipayCredentials creds, boolean publishable, HttpMethod method,
                                         String path, ObjectNode body, String idempotencyKey, String sandboxOutcome) {
        String url = join(creds.apiBase(), path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        String password = publishable ? creds.publishableKey() : creds.secretKey();
        String basic = Base64.getEncoder().encodeToString(
                (creds.accountId() + ":" + password).getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            headers.set("Idempotency-Key", idempotencyKey.trim());
        }
        if (sandboxOutcome != null && !sandboxOutcome.isBlank() && creds.sandbox()) {
            headers.set("GW-Sandbox-Outcome", sandboxOutcome.trim());
        }
        try {
            HttpEntity<String> entity = new HttpEntity<>(
                    body != null ? objectMapper.writeValueAsString(body) : null, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, method, entity, String.class);
            return okMap(resp.getStatusCode().value(), resp.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("QXIPAY {} {} → {}", method, path, e.getStatusCode().value());
            Map<String, Object> m = okMap(e.getStatusCode().value(), e.getResponseBodyAsString());
            m.put("ok", false);
            return m;
        } catch (Exception e) {
            log.warn("QXIPAY {} {} failed: {}", method, path, e.getMessage());
            return err("QXIPAY_HTTP", e.getMessage() != null ? e.getMessage() : "request failed");
        }
    }

    private Map<String, Object> okMap(int status, String raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("httpStatus", status);
        out.put("raw", raw != null ? raw : "");
        boolean ok = status >= 200 && status < 300;
        out.put("ok", ok);
        if (raw != null && !raw.isBlank()) {
            try {
                JsonNode n = objectMapper.readTree(raw);
                out.put("json", n);
                if (n.has("id")) {
                    out.put("id", n.get("id").asText(""));
                }
                if (n.has("status")) {
                    out.put("status", n.get("status").asText(""));
                }
                if (n.has("error") || n.has("code") || n.has("message")) {
                    out.put("errorCode", text(n, "code", "error"));
                    out.put("message", text(n, "message", "error"));
                }
            } catch (Exception ignored) {
                /* keep raw */
            }
        }
        return out;
    }

    private static Map<String, Object> err(String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("errorCode", code);
        m.put("message", message);
        m.put("httpStatus", 0);
        return m;
    }

    private static String join(String base, String path) {
        String b = base != null ? base.replaceAll("/+$", "") : "";
        String p = path != null ? path : "";
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return b + p;
    }

    private static String str(Object... vals) {
        for (Object v : vals) {
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isBlank() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return "";
    }

    private static int intVal(Object... vals) {
        for (Object v : vals) {
            if (v == null) {
                continue;
            }
            if (v instanceof Number n) {
                return n.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(v).trim());
            } catch (Exception ignored) {
                /* next */
            }
        }
        return 0;
    }

    private static String text(JsonNode n, String... keys) {
        for (String k : keys) {
            if (n.has(k) && !n.get(k).isNull()) {
                if (n.get(k).isTextual() || n.get(k).isNumber() || n.get(k).isBoolean()) {
                    return n.get(k).asText("");
                }
                if (n.get(k).isObject() && n.get(k).has("message")) {
                    return n.get(k).get("message").asText("");
                }
            }
        }
        return "";
    }
}
