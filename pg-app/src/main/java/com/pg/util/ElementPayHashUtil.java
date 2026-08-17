package com.pg.util;

import com.fasterxml.jackson.databind.JsonNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ElementPay HMAC-SHA1 서명 — Payment API·웹훅(check/pay) 공통.
 */
public final class ElementPayHashUtil {

    private static final String[] CALLBACK_PARAM_ORDER = {
            "method", "id", "service_id", "amount", "currency", "method_amount", "method_currency",
            "order", "order_id", "timestamp", "data", "payer_name", "status", "status_message",
            "client_amount", "client_currency", "fee", "total_fee", "gross", "recipient",
            "failure_code", "failure_message", "created_at", "paid_at", "canceled_at"
    };

    private ElementPayHashUtil() {
    }

    /**
     * Payment API — {@code <method>?k=v&…} (hash 제외).
     * PHP 샘플·Postman 과 같이 {@code http_build_query(..., PHP_QUERY_RFC3986)} / 삽입 순서(정렬 없음).
     */
    public static String signApiRequest(String secretKey, String method, Map<String, String> params) {
        String payload = method + "?" + joinInsertionOrderRfc3986Params(params);
        return hmacSha1Hex(secretKey, payload);
    }

    /**
     * 서명에 쓴 것과 동일한 RFC3986 query 문자열(hash 제외) — POST body 를 이 값 + {@code &hash=} 로내면
     * RestTemplate 이중 인코딩을 피할 수 있다.
     */
    public static String buildApiQueryString(Map<String, String> params) {
        return joinInsertionOrderRfc3986Params(params);
    }

    /** 웹훅 수신 — ElementPay 문서 예시 파라미터 순서(레거시). */
    public static String signCallbackRequest(String secretKey, String method, Map<String, String> params) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (params != null) {
            merged.putAll(params);
        }
        merged.put("method", method != null ? method : "");
        merged.remove("hash");
        StringBuilder sb = new StringBuilder();
        Set<String> used = new LinkedHashSet<>();
        for (String key : CALLBACK_PARAM_ORDER) {
            if (!merged.containsKey(key)) {
                continue;
            }
            String v = merged.get(key);
            if (v == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(key).append('=').append(v);
            used.add(key);
        }
        List<String> rest = new ArrayList<>();
        for (String key : merged.keySet()) {
            if (!used.contains(key) && merged.get(key) != null) {
                rest.add(key);
            }
        }
        rest.sort(String.CASE_INSENSITIVE_ORDER);
        for (String key : rest) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(key).append('=').append(merged.get(key));
        }
        return hmacSha1Hex(secretKey, sb.toString());
    }

    /**
     * 웹훅 수신 — PHP 샘플과 동일: {@code http_build_query($_POST without hash, PHP_QUERY_RFC3986)}.
     * EP 가 보낸 폼 필드 삽입 순서를 유지하고 값을 RFC3986 인코딩한다.
     */
    public static String signCallbackRequestPhpHttpBuildQuery(String secretKey, Map<String, String> params) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (e.getKey() == null || "hash".equalsIgnoreCase(e.getKey()) || e.getValue() == null) {
                    continue;
                }
                merged.put(e.getKey(), e.getValue());
            }
        }
        return hmacSha1Hex(secretKey, joinInsertionOrderRfc3986Params(merged));
    }

    public static String signCallbackResponse(String secretKey, Map<String, Object> responseFields) {
        return hmacSha1Hex(secretKey, compactJson(responseFields));
    }

    /**
     * Merchant API 응답 hash — compact JSON({@code response} 또는 {@code error} 객체).
     * hash 가 없으면 검증 생략(일부 error 본문). 서명 키는 API Secret, 실패 시 Webhook Signing Secret.
     */
    public static boolean verifyMerchantApiResponse(String apiSecretKey, String webhookSecretKey,
                                                    JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return true;
        }
        JsonNode hashNode = root.get("hash");
        if (hashNode == null || hashNode.isNull() || hashNode.asText("").isBlank()) {
            return true;
        }
        String want = hashNode.asText("").trim().toLowerCase(Locale.ROOT);
        JsonNode payload = root.get("response");
        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            payload = root.get("error");
        }
        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            return true;
        }
        String compact = payload.toString();
        if (secretMatches(apiSecretKey, compact, want)) {
            return true;
        }
        return webhookSecretKey != null && !webhookSecretKey.isBlank()
                && !webhookSecretKey.equals(apiSecretKey)
                && secretMatches(webhookSecretKey, compact, want);
    }

    private static boolean secretMatches(String secret, String compactJson, String wantHex) {
        if (secret == null || secret.isBlank() || compactJson == null) {
            return false;
        }
        String got = hmacSha1Hex(secret, compactJson).toLowerCase(Locale.ROOT);
        return constantTimeEquals(got, wantHex);
    }

    public static boolean verifyCallbackRequest(String secretKey, String method, Map<String, String> params, String hash) {
        if (secretKey == null || secretKey.isBlank() || hash == null || hash.isBlank()) {
            return false;
        }
        String want = hash.trim().toLowerCase(Locale.ROOT);
        /* 1) PHP http_build_query 스타일(실측 EP 콜백) */
        Map<String, String> withMethod = new LinkedHashMap<>();
        if (params != null) {
            withMethod.putAll(params);
        }
        if (method != null && !method.isBlank() && !withMethod.containsKey("method")) {
            Map<String, String> prepend = new LinkedHashMap<>();
            prepend.put("method", method);
            prepend.putAll(withMethod);
            withMethod = prepend;
        } else if (method != null && !method.isBlank()) {
            withMethod.put("method", method);
        }
        String phpStyle = signCallbackRequestPhpHttpBuildQuery(secretKey, withMethod);
        if (constantTimeEquals(phpStyle.toLowerCase(Locale.ROOT), want)) {
            return true;
        }
        /* 2) 레거시 고정 순서(비인코딩) */
        String legacy = signCallbackRequest(secretKey, method, params);
        if (constantTimeEquals(legacy.toLowerCase(Locale.ROOT), want)) {
            return true;
        }
        return false;
    }

    public static String hmacSha1Hex(String secretKey, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("ElementPay HMAC-SHA1 failed", e);
        }
    }

    /** HTTP form body 전송용 RFC3986 인코딩 (서명 문자열과 별개). */
    public static String rfc3986Encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private static String joinInsertionOrderRfc3986Params(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        if (params == null) {
            return "";
        }
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey() == null || "hash".equalsIgnoreCase(e.getKey()) || e.getValue() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            /* PHP http_build_query(PHP_QUERY_RFC3986) · Postman encodeURIComponent(+!'()*) */
            sb.append(rfc3986Encode(e.getKey())).append('=').append(rfc3986Encode(e.getValue()));
        }
        return sb.toString();
    }

    private static String compactJson(Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escapeJson(e.getKey())).append('"').append(':');
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number) {
                sb.append(v);
            } else {
                sb.append('"').append(escapeJson(String.valueOf(v))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
