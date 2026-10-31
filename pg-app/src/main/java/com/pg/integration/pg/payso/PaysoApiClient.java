package com.pg.integration.pg.payso;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Payso Web API v2 — hosted ui-payments (RE).
 * {@code POST /payment/gateway/v2/ui-payments}
 */
@Component
public class PaysoApiClient {

    private static final Logger log = LoggerFactory.getLogger(PaysoApiClient.class);
    private static final String SUCCESS_CODE = "2A002";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public PaysoApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(15_000);
        f.setReadTimeout(45_000);
        this.restTemplate = new RestTemplate(f);
    }

    /**
     * @param referenceNo max 12 chars (Payso 제약)
     * @param currencyCode numeric Payso code (00=THB …)
     */
    public Map<String, Object> createUiPayment(PaysoCredentials creds,
                                               String referenceNo,
                                               String customerEmail,
                                               String description,
                                               BigDecimal amount,
                                               String currencyCode,
                                               String lang,
                                               String returnUrl,
                                               String postbackUrl,
                                               String paymentChannel) {
        if (creds == null || !creds.isUsable()) {
            return err("PAYSO_CREDENTIALS", "Payso API 키가 없습니다.");
        }
        String ref = referenceNo != null ? referenceNo.trim() : "";
        if (ref.length() > 12) {
            ref = ref.substring(0, 12);
        }
        if (ref.isBlank()) {
            return err("INVALID_ORDER_NO", "referenceNo가 필요합니다.");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("merchantId", creds.merchantId());
        body.put("customerEmail", customerEmail != null && !customerEmail.isBlank()
                ? customerEmail.trim() : "noreply@icopay.co.kr");
        body.put("referenceNo", ref);
        body.put("description", clamp(description != null ? description : "Payment", 100));
        BigDecimal amt = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        body.put("amount", amt);
        body.put("paymentChannel", paymentChannel != null && !paymentChannel.isBlank() ? paymentChannel : "ALL");
        body.put("paymentGateway", "");
        body.put("installmentTerm", "");
        body.put("currencyCode", currencyCode != null && !currencyCode.isBlank() ? currencyCode : "00");
        body.put("lang", "JA".equalsIgnoreCase(lang) || "JP".equalsIgnoreCase(lang) || "JPN".equalsIgnoreCase(lang)
                ? "EN" : ("TH".equalsIgnoreCase(lang) || "THA".equalsIgnoreCase(lang) ? "TH" : "EN"));
        body.put("returnUrl", returnUrl != null && !returnUrl.isBlank() ? returnUrl.trim() : "-");
        body.put("postbackUrl", postbackUrl != null && !postbackUrl.isBlank() ? postbackUrl.trim() : "-");
        body.put("terminalId", creds.terminalId());
        body.put("keyVersion", creds.keyVersion());

        Map<String, Object> raw = exchange(creds, HttpMethod.POST, "/payment/gateway/v2/ui-payments", body);
        if (!Boolean.TRUE.equals(raw.get("ok"))) {
            return raw;
        }
        JsonNode json = (JsonNode) raw.get("json");
        String code = "";
        if (json != null && json.has("status") && json.get("status").has("statusCode")) {
            code = json.get("status").get("statusCode").asText("");
        }
        if (!SUCCESS_CODE.equals(code)) {
            String msg = json != null && json.has("status") && json.get("status").has("message")
                    ? json.get("status").get("message").asText("Payso 결제 생성 실패")
                    : "Payso 결제 생성 실패";
            return err("PAYSO_CREATE", msg);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("json", json);
        if (json.has("transactionId")) {
            out.put("transactionId", json.get("transactionId").asText(""));
        }
        if (json.has("redirectUrl")) {
            out.put("redirectUrl", json.get("redirectUrl").asText(""));
            out.put("paymentUrl", json.get("redirectUrl").asText(""));
        }
        return out;
    }

    private Map<String, Object> exchange(PaysoCredentials creds, HttpMethod method, String path, ObjectNode body) {
        String url = join(creds.apiBase(), path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        /* Payso docs: exact header casing apiKey / secretKey */
        headers.set("apiKey", creds.apiKey());
        headers.set("secretKey", creds.secretKey());
        try {
            HttpEntity<String> entity = new HttpEntity<>(
                    body != null ? objectMapper.writeValueAsString(body) : null, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, method, entity, String.class);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("httpStatus", resp.getStatusCode().value());
            String respBody = resp.getBody();
            if (respBody != null && !respBody.isBlank()) {
                try {
                    out.put("json", objectMapper.readTree(respBody));
                } catch (Exception e) {
                    out.put("raw", respBody);
                }
            }
            return out;
        } catch (HttpStatusCodeException e) {
            log.warn("Payso HTTP {} {} body={}", e.getStatusCode().value(), url,
                    truncate(e.getResponseBodyAsString(), 400));
            Map<String, Object> err = err("PAYSO_HTTP", "Payso HTTP " + e.getStatusCode().value());
            err.put("httpStatus", e.getStatusCode().value());
            try {
                err.put("json", objectMapper.readTree(e.getResponseBodyAsString()));
            } catch (Exception ignored) {
                err.put("raw", e.getResponseBodyAsString());
            }
            return err;
        } catch (Exception e) {
            log.warn("Payso call failed {}: {}", url, e.toString());
            return err("PAYSO_ERROR", e.getMessage() != null ? e.getMessage() : "Payso 호출 실패");
        }
    }

    private static String join(String base, String path) {
        String b = base != null ? base.replaceAll("/+$", "") : "";
        String p = path != null ? path : "";
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return b + p;
    }

    private static String clamp(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static Map<String, Object> err(String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("errorCode", code);
        m.put("message", message);
        return m;
    }
}
