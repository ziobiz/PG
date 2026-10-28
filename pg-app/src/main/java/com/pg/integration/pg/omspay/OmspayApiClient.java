package com.pg.integration.pg.omspay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OMSPAY (Waylar OMS) REST — products / orders / card-payment / status.
 * Paths are under {@code /v1/...}; credentials.apiBase should be {@code https://oms.waylar.net/api}.
 */
@Component
public class OmspayApiClient {

    private static final Logger log = LoggerFactory.getLogger(OmspayApiClient.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OmspayApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(15_000);
        f.setReadTimeout(45_000);
        this.restTemplate = new RestTemplate(f);
    }

    /** Ensure generic SKU exists (ignore 409/duplicate). */
    public Map<String, Object> ensureProduct(OmspayCredentials creds) {
        if (creds == null || !creds.isUsable()) {
            return err("OMSPAY_CREDENTIALS", "OMSPAY API 키가 없습니다.");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("sku", creds.sku());
        body.put("name", "Payment");
        body.put("price", 1);
        body.put("isDigital", true);
        Map<String, Object> created = exchange(creds, HttpMethod.POST, "/v1/products", body);
        if (Boolean.TRUE.equals(created.get("ok"))) {
            return created;
        }
        int st = created.get("httpStatus") instanceof Number n ? n.intValue() : 0;
        if (st == 409 || st == 422 || st == 400) {
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("ok", true);
            ok.put("skipped", true);
            ok.put("httpStatus", st);
            return ok;
        }
        return created;
    }

    public Map<String, Object> createOrder(OmspayCredentials creds, BigDecimal amount, String note) {
        if (creds == null || !creds.canCreateOrder()) {
            return err("OMSPAY_CREDENTIALS", "OMSPAY API 키 또는 customerId가 없습니다.");
        }
        ObjectNode body = objectMapper.createObjectNode();
        try {
            body.put("customer_id", Long.parseLong(creds.customerId()));
        } catch (NumberFormatException e) {
            body.put("customer_id", creds.customerId());
        }
        if (note != null && !note.isBlank()) {
            body.put("note", note.length() > 200 ? note.substring(0, 200) : note);
        }
        ArrayNode items = body.putArray("items");
        ObjectNode item = items.addObject();
        item.put("sku", creds.sku());
        item.put("qty", 1);
        item.put("price", amount != null ? amount : BigDecimal.ZERO);
        return exchange(creds, HttpMethod.POST, "/v1/orders", body);
    }

    public Map<String, Object> createCardPayment(OmspayCredentials creds, String orderCode, String returnUrl) {
        if (creds == null || !creds.isUsable() || orderCode == null || orderCode.isBlank()) {
            return err("OMSPAY_CREDENTIALS", "order code가 필요합니다.");
        }
        ObjectNode body = objectMapper.createObjectNode();
        if (returnUrl != null && !returnUrl.isBlank()) {
            String ru = returnUrl.trim();
            if (ru.length() > 500) {
                ru = ru.substring(0, 500);
            }
            body.put("return_url", ru);
        }
        String path = "/v1/orders/" + orderCode.trim() + "/card-payment";
        return exchange(creds, HttpMethod.POST, path, body);
    }

    public Map<String, Object> getOrderStatus(OmspayCredentials creds, String orderCode) {
        if (creds == null || !creds.isUsable() || orderCode == null || orderCode.isBlank()) {
            return err("OMSPAY_CREDENTIALS", "order code가 필요합니다.");
        }
        return exchange(creds, HttpMethod.GET, "/v1/orders/" + orderCode.trim() + "/status", null);
    }

    private Map<String, Object> exchange(OmspayCredentials creds, HttpMethod method, String path, ObjectNode body) {
        String url = join(creds.apiBase(), path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("X-API-Key", creds.apiKey());
        try {
            HttpEntity<String> entity = new HttpEntity<>(
                    body != null ? objectMapper.writeValueAsString(body) : null, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, method, entity, String.class);
            return okMap(resp.getStatusCode().value(), resp.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("OMSPAY {} {} → {}", method, path, e.getStatusCode().value());
            Map<String, Object> m = okMap(e.getStatusCode().value(), e.getResponseBodyAsString());
            m.put("ok", false);
            return m;
        } catch (Exception e) {
            log.warn("OMSPAY {} {} failed: {}", method, path, e.getMessage());
            return err("OMSPAY_HTTP", e.getMessage() != null ? e.getMessage() : "request failed");
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
                if (n.has("code")) {
                    out.put("code", n.get("code").asText(""));
                }
                if (n.has("id")) {
                    out.put("id", n.get("id").asText(""));
                }
                if (n.has("payment_url")) {
                    out.put("paymentUrl", n.get("payment_url").asText(""));
                }
                if (n.has("order_code")) {
                    out.put("orderCode", n.get("order_code").asText(""));
                }
                if (n.has("is_paid")) {
                    out.put("isPaid", n.get("is_paid").asBoolean(false));
                }
                if (n.has("paid")) {
                    out.put("paid", n.get("paid").asBoolean(false));
                }
                if (n.has("note")) {
                    out.put("note", n.get("note").asText(""));
                }
                if (n.has("message") && !n.get("message").isNull()) {
                    out.put("message", n.get("message").asText(""));
                }
            } catch (Exception ignored) {
                /* raw only */
            }
        }
        return out;
    }

    private static Map<String, Object> err(String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("errorCode", code);
        m.put("message", message);
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
}
