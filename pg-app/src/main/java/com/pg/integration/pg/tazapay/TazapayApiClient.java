package com.pg.integration.pg.tazapay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.util.IcopayPublicIdentity;
import com.pg.util.PgOutboundMerchantPrivacy;
import com.pg.util.PgOutboundUrlPolicy;
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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tazapay REST — {@code POST /v3/payin}, {@code GET /v3/payin/{id}}, {@code POST /v3/checkout}.
 * 가맹 상호·도메인·주문 설명 등은 전달하지 않는다.
 */
@Component
public class TazapayApiClient {

    private static final Logger log = LoggerFactory.getLogger(TazapayApiClient.class);
    private static final String STATEMENT_DESCRIPTOR = "ICOPAY";
    private static final Set<String> ZERO_DECIMAL = Set.of("JPY", "KRW", "VND", "CLP", "BIF");

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public TazapayApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(15_000);
        f.setReadTimeout(45_000);
        this.restTemplate = new RestTemplate(f);
    }

    public Map<String, Object> createPayin(TazapayCredentials creds,
                                           String referenceId,
                                           BigDecimal amount,
                                           String currency,
                                           String customerEmail,
                                           String customerName,
                                           String customerCountry,
                                           String successUrl,
                                           String cancelUrl,
                                           String webhookUrl) {
        if (creds == null || !creds.isApiUsable()) {
            return err("TAZAPAY_CREDENTIALS", "Tazapay API 키가 없습니다.");
        }
        String ref = referenceId != null ? referenceId.trim() : "";
        if (ref.isBlank()) {
            return err("INVALID_REFERENCE", "reference_id가 필요합니다.");
        }
        String cur = currency != null ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
        long minor = toMinorUnits(amount, cur);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("invoice_currency", cur);
        body.put("amount", minor);
        body.put("reference_id", ref);
        body.put("transaction_description", PgOutboundMerchantPrivacy.SAFE_PG_DESCRIPTION);
        body.put("statement_descriptor", STATEMENT_DESCRIPTOR);

        ObjectNode customer = objectMapper.createObjectNode();
        customer.put("email", PgOutboundMerchantPrivacy.safePgCustomerEmail(customerEmail));
        String safeName = sanitizePersonName(customerName);
        if (safeName.isBlank() || PgOutboundMerchantPrivacy.containsUrlDomainOrIp(safeName)) {
            safeName = "Customer";
        }
        customer.put("name", safeName);
        /* ISO 3166-1 alpha-2 — 가맹 도메인 유출 없이 구매자 국가만(없으면 US) */
        String cc = customerCountry != null ? customerCountry.trim().toUpperCase(Locale.ROOT) : "";
        if (cc.length() != 2 || !cc.chars().allMatch(Character::isLetter)) {
            cc = "US";
        }
        customer.put("country", cc);
        body.set("customer_details", customer);

        String[] allowed = PgOutboundUrlPolicy.allowedIcopayAndNotiBases(IcopayPublicIdentity.API_BASE_URL, webhookUrl);
        String safe = IcopayPublicIdentity.API_BASE_URL + "/tazapay-pay.html";
        body.put("success_url", PgOutboundUrlPolicy.enforceOwnDomain(successUrl, safe, allowed));
        body.put("cancel_url", PgOutboundUrlPolicy.enforceOwnDomain(cancelUrl, safe, allowed));
        body.put("webhook_url", PgOutboundUrlPolicy.enforceOwnDomain(webhookUrl,
                PgOutboundUrlPolicy.DEFAULT_NOTI_BASE + "/noti/tazapay", allowed));

        return postJson(creds, "/v3/payin", body);
    }

    public Map<String, Object> getPayin(TazapayCredentials creds, String payinId) {
        if (creds == null || !creds.isApiUsable()) {
            return err("TAZAPAY_CREDENTIALS", "Tazapay API 키가 없습니다.");
        }
        String id = payinId != null ? payinId.trim() : "";
        if (id.isBlank()) {
            return err("INVALID_PAYIN_ID", "payin id가 필요합니다.");
        }
        return getJson(creds, "/v3/payin/" + encPath(id));
    }

    public Map<String, Object> createCheckout(TazapayCredentials creds,
                                              String referenceId,
                                              BigDecimal amount,
                                              String currency,
                                              String customerEmail,
                                              String successUrl,
                                              String cancelUrl,
                                              String webhookUrl) {
        if (creds == null || !creds.isApiUsable()) {
            return err("TAZAPAY_CREDENTIALS", "Tazapay API 키가 없습니다.");
        }
        String ref = referenceId != null ? referenceId.trim() : "";
        if (ref.isBlank()) {
            return err("INVALID_REFERENCE", "reference_id가 필요합니다.");
        }
        String cur = currency != null ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
        long minor = toMinorUnits(amount, cur);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("invoice_currency", cur);
        body.put("amount", minor);
        body.put("reference_id", ref);
        body.put("transaction_description", PgOutboundMerchantPrivacy.SAFE_PG_DESCRIPTION);
        body.put("statement_descriptor", STATEMENT_DESCRIPTOR);

        ObjectNode customer = objectMapper.createObjectNode();
        customer.put("email", PgOutboundMerchantPrivacy.safePgCustomerEmail(customerEmail));
        customer.put("name", "Customer");
        customer.put("country", "US");
        body.set("customer_details", customer);

        String[] allowed = PgOutboundUrlPolicy.allowedIcopayAndNotiBases(IcopayPublicIdentity.API_BASE_URL, webhookUrl);
        String safe = IcopayPublicIdentity.API_BASE_URL + "/tazapay-pay.html";
        body.put("success_url", PgOutboundUrlPolicy.enforceOwnDomain(successUrl, safe, allowed));
        body.put("cancel_url", PgOutboundUrlPolicy.enforceOwnDomain(cancelUrl, safe, allowed));
        body.put("webhook_url", PgOutboundUrlPolicy.enforceOwnDomain(webhookUrl,
                PgOutboundUrlPolicy.DEFAULT_NOTI_BASE + "/noti/tazapay", allowed));

        return postJson(creds, "/v3/checkout", body);
    }

    public static boolean isPaidStatus(JsonNode root) {
        if (root == null) {
            return false;
        }
        String st = textDeep(root, "status", "payment_status", "state").toLowerCase(Locale.ROOT);
        return st.contains("succeed") || st.contains("success") || st.contains("paid")
                || st.contains("complete") || st.equals("captured");
    }

    public static boolean isFailedStatus(JsonNode root) {
        if (root == null) {
            return false;
        }
        String st = textDeep(root, "status", "payment_status", "state").toLowerCase(Locale.ROOT);
        return st.contains("fail") || st.contains("cancel") || st.contains("expire")
                || st.contains("void") || st.contains("decline");
    }

    private Map<String, Object> postJson(TazapayCredentials creds, String path, ObjectNode body) {
        try {
            HttpHeaders headers = authHeaders(creds);
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> res = restTemplate.exchange(
                    creds.apiBase() + path,
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                    String.class);
            return parseSuccess(res.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("Tazapay POST {} HTTP {}: {}", path, e.getStatusCode().value(), clip(e.getResponseBodyAsString()));
            return err("TAZAPAY_HTTP", clip(e.getResponseBodyAsString()));
        } catch (Exception e) {
            return err("TAZAPAY_ERROR", e.getMessage() != null ? e.getMessage() : "Tazapay 호출 실패");
        }
    }

    private Map<String, Object> getJson(TazapayCredentials creds, String path) {
        try {
            HttpHeaders headers = authHeaders(creds);
            ResponseEntity<String> res = restTemplate.exchange(
                    creds.apiBase() + path,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            return parseSuccess(res.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("Tazapay GET {} HTTP {}: {}", path, e.getStatusCode().value(), clip(e.getResponseBodyAsString()));
            return err("TAZAPAY_HTTP", clip(e.getResponseBodyAsString()));
        } catch (Exception e) {
            return err("TAZAPAY_ERROR", e.getMessage() != null ? e.getMessage() : "Tazapay 조회 실패");
        }
    }

    private Map<String, Object> parseSuccess(String raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return err("TAZAPAY_EMPTY", "빈 응답");
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.has("data") ? root.get("data") : root;
            String clientToken = textDeep(data, "client_token", "clientToken");
            String id = textDeep(data, "id", "payin_id", "payinId");
            String status = textDeep(data, "status", "payment_status");
            String redirect = textDeep(data, "url", "redirect_url", "checkout_url", "payment_url");

            out.put("ok", true);
            out.put("clientToken", clientToken);
            out.put("payinId", id);
            out.put("status", status);
            out.put("redirectUrl", redirect);
            out.put("raw", root);
            return out;
        } catch (Exception e) {
            return err("TAZAPAY_PARSE", "응답 파싱 실패");
        }
    }

    private static HttpHeaders authHeaders(TazapayCredentials creds) {
        HttpHeaders h = new HttpHeaders();
        String token = creds.apiKey() + ":" + creds.apiSecret();
        String basic = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        h.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        return h;
    }

    public static long toMinorUnits(BigDecimal amount, String currency) {
        if (amount == null) {
            return 0L;
        }
        String cur = currency != null ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
        int scale = ZERO_DECIMAL.contains(cur) ? 0 : 2;
        BigDecimal scaled = amount.setScale(scale, RoundingMode.HALF_UP);
        if (scale == 0) {
            return scaled.longValue();
        }
        return scaled.movePointRight(2).longValue();
    }

    private static String sanitizePersonName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.trim().replaceAll("[^A-Za-z \\-'.]", "").trim();
    }

    private static String encPath(String id) {
        return id.replace(" ", "%20");
    }

    private static String textDeep(JsonNode n, String... keys) {
        if (n == null) {
            return "";
        }
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            JsonNode v = n.get(k);
            if (v == null && n.has("attributes")) {
                v = n.get("attributes").get(k);
            }
            if (v != null && !v.isNull() && v.isValueNode()) {
                String s = v.asText("").trim();
                if (!s.isBlank()) {
                    return s;
                }
            }
        }
        return "";
    }

    private static Map<String, Object> err(String code, String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", false);
        out.put("errorCode", code);
        out.put("message", message);
        return out;
    }

    private static String clip(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() > 300 ? t.substring(0, 300) + "…" : t;
    }
}