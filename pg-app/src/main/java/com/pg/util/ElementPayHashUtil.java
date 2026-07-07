package com.pg.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
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
            "order", "timestamp", "data", "payer_name", "status", "status_message", "client_amount",
            "client_currency", "fee", "created_at", "paid_at", "canceled_at"
    };

    private ElementPayHashUtil() {
    }

    /** Payment API — {@code <method>?key=…&timestamp=…} (hash 제외, 키 알파벳 순, raw 값). */
    public static String signApiRequest(String secretKey, String method, Map<String, String> params) {
        String payload = method + "?" + joinSortedRawParams(params);
        return hmacSha1Hex(secretKey, payload);
    }

    /** 웹훅 수신 — ElementPay 문서 예시 파라미터 순서. */
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

    public static String signCallbackResponse(String secretKey, Map<String, Object> responseFields) {
        return hmacSha1Hex(secretKey, compactJson(responseFields));
    }

    public static boolean verifyCallbackRequest(String secretKey, String method, Map<String, String> params, String hash) {
        if (secretKey == null || secretKey.isBlank() || hash == null || hash.isBlank()) {
            return false;
        }
        String expected = signCallbackRequest(secretKey, method, params);
        return constantTimeEquals(expected.toLowerCase(Locale.ROOT), hash.trim().toLowerCase(Locale.ROOT));
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

    private static String joinSortedRawParams(Map<String, String> params) {
        List<Map.Entry<String, String>> entries = new ArrayList<>();
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (e.getKey() == null || "hash".equalsIgnoreCase(e.getKey()) || e.getValue() == null) {
                    continue;
                }
                entries.add(e);
            }
        }
        entries.sort(Comparator.comparing(e -> e.getKey().toLowerCase(Locale.ROOT)));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : entries) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
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
