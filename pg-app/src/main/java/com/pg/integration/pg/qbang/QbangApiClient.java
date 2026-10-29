package com.pg.integration.pg.qbang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 큐뱅 POS command API — TK / QR / PL / OKAY / RF / AT. */
@Component
public class QbangApiClient {

    private static final Logger log = LoggerFactory.getLogger(QbangApiClient.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public QbangApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(15_000);
        f.setReadTimeout(45_000);
        this.restTemplate = new RestTemplate(f);
    }

    public Map<String, Object> requestToken(QbangCredentials creds) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("command", "TK");
        body.put("posId", creds.posId());
        if (!creds.agencyCode().isBlank()) {
            body.put("agencyCode", creds.agencyCode());
        }
        return post(creds, body);
    }

    public Map<String, Object> requestQr(QbangCredentials creds, String keyB64, String ivB64,
                                         String transactionNo, String currency, BigDecimal totalAmount,
                                         BigDecimal vat, String productName) {
        ObjectNode plain = objectMapper.createObjectNode();
        plain.put("currency", currency != null ? currency.trim().toUpperCase() : "KRW");
        plain.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        plain.put("vat", vat != null ? vat : BigDecimal.ZERO);
        plain.put("gratuity", 0);
        plain.put("tax", 0);
        plain.put("installmentPeriod", 0);
        ArrayNode items = plain.putArray("items");
        ObjectNode item = items.addObject();
        String name = productName != null && !productName.isBlank() ? productName.trim() : "Payment";
        if (name.length() > 192) {
            name = name.substring(0, 192);
        }
        item.put("name", name);
        item.put("unitPrice", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        item.put("quantity", 1);

        String cipher;
        try {
            cipher = QbangCryptoUtil.encryptAes256CbcBase64(objectMapper.writeValueAsString(plain), keyB64, ivB64);
        } catch (Exception e) {
            return err("QBANG_ENCRYPT", e.getMessage());
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("command", "QR");
        body.put("posId", creds.posId());
        if (!creds.agencyCode().isBlank()) {
            body.put("agencyCode", creds.agencyCode());
        }
        body.put("transactionNo", transactionNo);
        body.put("payload", cipher);
        return post(creds, body);
    }

    public Map<String, Object> pollStatus(QbangCredentials creds, String transactionNo) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("command", "PL");
        body.put("posId", creds.posId());
        if (!creds.agencyCode().isBlank()) {
            body.put("agencyCode", creds.agencyCode());
        }
        body.put("transactionNo", transactionNo);
        return post(creds, body);
    }

    public Map<String, Object> confirmOkay(QbangCredentials creds, String transactionNo) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("command", "OKAY");
        body.put("posId", creds.posId());
        if (!creds.agencyCode().isBlank()) {
            body.put("agencyCode", creds.agencyCode());
        }
        body.put("transactionNo", transactionNo);
        return post(creds, body);
    }

    public Map<String, Object> abortNetwork(QbangCredentials creds, String transactionNo) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("command", "AT");
        body.put("posId", creds.posId());
        if (!creds.agencyCode().isBlank()) {
            body.put("agencyCode", creds.agencyCode());
        }
        body.put("transactionNo", transactionNo);
        return post(creds, body);
    }

    private Map<String, Object> post(QbangCredentials creds, ObjectNode body) {
        if (creds == null || !creds.isUsable()) {
            return err("QBANG_CREDENTIALS", "posId가 없습니다.");
        }
        String url = creds.commandUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            String rawBody = objectMapper.writeValueAsString(body);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(rawBody, headers), String.class);
            return okMap(resp.getStatusCode().value(), resp.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("QBANG {} → {}", body.path("command").asText(), e.getStatusCode().value());
            Map<String, Object> m = okMap(e.getStatusCode().value(), e.getResponseBodyAsString());
            m.put("ok", false);
            return m;
        } catch (Exception e) {
            log.warn("QBANG {} failed: {}", body.path("command").asText(), e.getMessage());
            return err("QBANG_HTTP", e.getMessage() != null ? e.getMessage() : "request failed");
        }
    }

    private Map<String, Object> okMap(int status, String raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("httpStatus", status);
        out.put("raw", raw != null ? raw : "");
        boolean httpOk = status >= 200 && status < 300;
        out.put("ok", httpOk);
        if (raw == null || raw.isBlank()) {
            return out;
        }
        try {
            JsonNode n = objectMapper.readTree(raw);
            out.put("json", n);
            String st = n.path("status").asText("");
            out.put("apiStatus", st);
            if (httpOk && !"success".equalsIgnoreCase(st) && !st.isBlank()) {
                out.put("ok", false);
            }
            out.put("message", n.path("message").asText(""));
            JsonNode data = n.path("data");
            if (data != null && data.isObject()) {
                out.put("data", data);
                if (data.has("key")) {
                    out.put("key", data.path("key").asText(""));
                }
                if (data.has("iv")) {
                    out.put("iv", data.path("iv").asText(""));
                }
                if (data.has("transactionNo")) {
                    out.put("transactionNo", data.path("transactionNo").asText(""));
                }
                if (data.has("qrCode")) {
                    out.put("qrCode", data.path("qrCode").asText(""));
                }
                if (data.has("remainingTime")) {
                    out.put("remainingTime", data.path("remainingTime").asInt(0));
                }
                if (data.has("state")) {
                    out.put("state", data.path("state").asText(""));
                }
                if (data.has("stage")) {
                    out.put("stage", data.path("stage").asText(""));
                }
                if (data.has("actionFlag")) {
                    out.put("actionFlag", data.path("actionFlag").asInt(0));
                }
                if (data.has("approval") && data.get("approval").isObject()) {
                    JsonNode a = data.get("approval");
                    out.put("approvalNo", a.path("approvalNo").asText(""));
                    out.put("approvalTime", a.path("approvalTime").asText(""));
                    out.put("paymentBrand", a.path("paymentBrand").asText(""));
                    out.put("paymentBrandName", a.path("paymentBrandName").asText(""));
                    out.put("subWalletName", a.path("subWalletName").asText(""));
                }
            }
            if (!out.containsKey("transactionNo") && n.has("transactionNo")) {
                out.put("transactionNo", n.path("transactionNo").asText(""));
            }
        } catch (Exception ignored) {
            /* raw only */
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
}
