package com.pg.integration.pg.ksnet;

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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * KSNET(KSPAY) 서버 호출 — 결제창 인증 후 {@code payKey(commConId)} 승인({@code recv_post.jsp})과 REST 취소.
 * <p>인증(requestPay 성공)만으로는 결제 완료가 아니며, {@link #approve} 성공 시에만 승인이다.
 * KSNET 으로는 가맹 MID 를 보내지 않는다(ICOPAY 집계 storeid·order 만).
 */
@Component
public class KsnetApiClient {

    private static final Logger log = LoggerFactory.getLogger(KsnetApiClient.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public KsnetApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(15_000);
        f.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(f);
    }

    /**
     * 승인 — {@code POST recv_post.jsp} JSON {@code {commConId, amount, actionType:"1", storeid, rpyParams:""}}.
     *
     * @param amountKsnet KSNET 전송 금액 문자열(USD × 1000, 예 130.00 → "130000")
     * @return {@code ok}(HTTP·파싱), {@code approved}(승인 성공), {@code tid}, {@code authNo}, {@code message},
     *         {@code code}, {@code cardMasked}, {@code amount}
     */
    public Map<String, Object> approve(KsnetCredentials creds, boolean mobile, String payKey, String amountKsnet) {
        if (creds == null || !creds.isUsable()) {
            return err("KSNET_CREDENTIALS", "KSNET storeid 가 없습니다.");
        }
        if (payKey == null || payKey.isBlank()) {
            return err("KSNET_PAYKEY", "payKey 가 필요합니다.");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("commConId", payKey.trim());
        body.put("amount", amountKsnet != null ? amountKsnet.trim() : "");
        body.put("actionType", "1");
        body.put("storeid", creds.storeId());
        body.put("rpyParams", "");
        String url = creds.approveUrl(mobile);
        Map<String, Object> raw = exchange(url, body, null);
        if (!Boolean.TRUE.equals(raw.get("ok"))) {
            return raw;
        }
        JsonNode json = raw.get("json") instanceof JsonNode jn ? jn : null;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("approved", isApproved(json));
        out.put("tid", text(json, "trno", "tid", "transactionId", "transaction_id", "trNo"));
        out.put("authNo", text(json, "authno", "authNo", "approvalNo", "approval_no"));
        out.put("message", text(json, "msg2", "msg1", "message", "resultMsg", "rmsg"));
        out.put("code", text(json, "authyn", "resultCode", "rcode", "code", "status"));
        out.put("cardMasked", maskCard(text(json, "cardno", "cardNo", "card_no", "cardNumber")));
        out.put("amount", text(json, "amt", "amount"));
        out.put("json", json);
        return out;
    }

    /**
     * 승인 취소 — {@code POST /api/v1/card/cancel}, {@code Authorization: pgapi {apiKey}}.
     */
    public Map<String, Object> cancel(KsnetCredentials creds, String tid, String orderNo,
                                      String amountKsnet, String reason) {
        if (creds == null || creds.apiKey().isBlank()) {
            return err("KSNET_API_KEY", "KSNET apiKey 가 없어 취소할 수 없습니다.");
        }
        /* WebFEP OpenAPI CardCancelRequest */
        ObjectNode body = objectMapper.createObjectNode();
        body.put("mid", creds.storeId());
        String payload = orderNo != null ? orderNo.trim() : "";
        if (payload.isBlank() && reason != null && !reason.isBlank()) {
            payload = reason.trim().length() > 200 ? reason.trim().substring(0, 200) : reason.trim();
        }
        body.put("payload", payload);
        body.put("cancelType", "FULL");
        if (tid != null && !tid.isBlank()) {
            body.put("orgTradeKeyType", "TID");
            body.put("orgTradeKey", tid.trim());
        } else {
            body.put("orgTradeKeyType", "ORDER_NUMB");
            body.put("orgTradeKey", orderNo != null ? orderNo.trim() : "");
        }
        body.put("currencyType", "USD");
        Map<String, Object> raw = exchange(creds.cancelUrl(), body, "pgapi " + creds.apiKey());
        if (!Boolean.TRUE.equals(raw.get("ok"))) {
            return raw;
        }
        JsonNode json = raw.get("json") instanceof JsonNode jn ? jn : null;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("approved", isApproved(json));
        out.put("message", text(json, "msg2", "msg1", "message", "resultMsg", "rmsg"));
        out.put("code", text(json, "authyn", "resultCode", "rcode", "code", "status"));
        out.put("json", json);
        return out;
    }

    /** KSPAY 승인 응답 성공 판정 — authyn=O 또는 결과코드 0000/00/0/SUCCESS 또는 success=true. */
    static boolean isApproved(JsonNode json) {
        if (json == null || json.isMissingNode()) {
            return false;
        }
        String authyn = text(json, "authyn");
        if (!authyn.isEmpty()) {
            return "O".equalsIgnoreCase(authyn);
        }
        JsonNode succ = json.get("success");
        if (succ != null && succ.isBoolean()) {
            return succ.asBoolean();
        }
        String code = text(json, "resultCode", "rcode", "code", "status", "result");
        if (code.isEmpty()) {
            return false;
        }
        String c = code.toUpperCase(Locale.ROOT);
        return "0000".equals(c) || "00".equals(c) || "0".equals(c) || "SUCCESS".equals(c) || "OK".equals(c);
    }

    private Map<String, Object> exchange(String url, ObjectNode body, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));
        if (authorization != null && !authorization.isBlank()) {
            headers.set("Authorization", authorization);
        }
        try {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("httpStatus", resp.getStatusCode().value());
            String respBody = resp.getBody();
            JsonNode parsed = parseBody(respBody);
            if (parsed == null) {
                return err("KSNET_PARSE", "KSNET 응답을 해석할 수 없습니다.");
            }
            out.put("json", parsed);
            return out;
        } catch (HttpStatusCodeException e) {
            log.warn("KSNET HTTP {} {} body={}", e.getStatusCode().value(), url,
                    truncate(e.getResponseBodyAsString(), 400));
            Map<String, Object> err = err("KSNET_HTTP", "KSNET HTTP " + e.getStatusCode().value());
            err.put("httpStatus", e.getStatusCode().value());
            return err;
        } catch (Exception e) {
            log.warn("KSNET call failed {}: {}", url, e.toString());
            return err("KSNET_ERROR", e.getMessage() != null ? e.getMessage() : "KSNET 호출 실패");
        }
    }

    /** JSON 우선, 아니면 {@code k=v&k2=v2} / {@code k=v\n} 텍스트로 파싱. */
    private JsonNode parseBody(String respBody) {
        if (respBody == null || respBody.isBlank()) {
            return null;
        }
        String t = respBody.trim();
        try {
            if (t.startsWith("{")) {
                return objectMapper.readTree(t);
            }
        } catch (Exception ignored) {
            /* text fallback */
        }
        ObjectNode n = objectMapper.createObjectNode();
        for (String part : t.split("[&\\r\\n]+")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = urlDecode(part.substring(0, eq).trim());
            String v = urlDecode(part.substring(eq + 1).trim());
            if (!k.isEmpty()) {
                n.put(k, v);
            }
        }
        return n.size() > 0 ? n : null;
    }

    private static String maskCard(String card) {
        if (card == null || card.isBlank()) {
            return "";
        }
        String d = card.replaceAll("\\s+", "");
        if (d.contains("*")) {
            return d.length() > 32 ? d.substring(0, 32) : d;
        }
        if (d.length() < 8) {
            return "";
        }
        return d.substring(0, 4) + "******" + d.substring(d.length() - 4);
    }

    private static String text(JsonNode root, String... keys) {
        if (root == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            JsonNode v = root.get(k);
            if (v != null && !v.isNull() && v.isValueNode()) {
                String s = v.asText("");
                if (s != null && !s.isBlank()) {
                    return s.trim();
                }
            }
        }
        return "";
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s != null ? s : "";
        }
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
        m.put("approved", false);
        m.put("errorCode", code);
        m.put("message", message);
        return m;
    }

    /** USD × 1000 → 정수 문자열 (예: 130.00 → "130000"). 소수 3자리 초과는 HALF_UP. */
    public static String toKsnetUsdAmount(BigDecimal usd) {
        if (usd == null) {
            return "0";
        }
        return usd.movePointRight(3).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
