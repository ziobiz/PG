package com.pg.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;

/**
 * J-Pay {@code pay_index} 동기 응답 파싱.
 * <p>공식: {@code status} 0/1/2 + {@code msg} + {@code url}. 실제 운영·샌드박스는 {@code status:"error"} 등 문자열도 반환.</p>
 */
public final class JpayPayIndexResponseParser {

    private static final ObjectMapper OM = new ObjectMapper();

    public record Outcome(int status, String msg, String url3ds, String rawJsonUsed, String transactionId) {}

    private JpayPayIndexResponseParser() {
    }

    public static Outcome parse(String raw) throws JsonProcessingException {
        String body = sanitizeRawBody(raw);
        if (body.isEmpty()) {
            return new Outcome(2, "JPAY pay_index returned empty response (verify pay_index URL)", "", "", "");
        }
        JsonNode n = unwrapJsonPayload(readJsonNode(body));
        int status = resolveStatus(n);
        String msg = resolveMsg(n);
        String url3ds = resolveUrl3ds(n);
        String txnId = n.path("transaction_id").asText("").trim();
        String usedJson = n.isObject() || n.isArray() ? n.toString() : body;
        if (status == 1 && (url3ds == null || url3ds.isBlank())) {
            if (msg.isBlank()) {
                msg = "3DS redirect URL missing";
            }
            return new Outcome(2, msg, "", usedJson, txnId);
        }
        if (status < 0) {
            if (msg.isBlank()) {
                if (!body.startsWith("{") && !body.startsWith("[")) {
                    msg = body.length() > 400 ? body.substring(0, 400) + "…" : body;
                } else {
                    msg = "JPAY response missing status: "
                            + (body.length() > 240 ? body.substring(0, 240) + "…" : body);
                }
            }
            return new Outcome(2, msg, "", usedJson, txnId);
        }
        if (status > 2) {
            if (msg.isBlank()) {
                msg = "JPAY unexpected status " + status;
            }
            return new Outcome(2, msg, "", usedJson, txnId);
        }
        return new Outcome(status, msg, url3ds != null ? url3ds : "", usedJson, txnId);
    }

    static String sanitizeRawBody(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.startsWith("\uFEFF")) {
            t = t.substring(1).trim();
        }
        return t;
    }

    static JsonNode readJsonNode(String body) throws JsonProcessingException {
        try {
            return OM.readTree(body);
        } catch (JsonProcessingException first) {
            int start = body.indexOf('{');
            int end = body.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return OM.readTree(body.substring(start, end + 1));
            }
            throw first;
        }
    }

    /**
     * J-Pay 운영 {@code pay_index} 가 본문 전체를 JSON 문자열로 감싸 반환하는 경우가 있음.
     * 예: {@code "{\"status\":1,\"url\":\"https://…\"}"}
     */
    static JsonNode unwrapJsonPayload(JsonNode n) throws JsonProcessingException {
        if (n == null || n.isMissingNode()) {
            return n;
        }
        if (n.isTextual()) {
            String inner = n.asText("").trim();
            if (inner.startsWith("{") || inner.startsWith("[")) {
                return unwrapJsonPayload(readJsonNode(inner));
            }
        }
        return n;
    }

    static int resolveStatus(JsonNode n) {
        if (n == null || n.isMissingNode()) {
            return -1;
        }
        for (String key : new String[]{"status", "Status"}) {
            int parsed = parseStatusValue(n.path(key));
            if (parsed >= 0) {
                return parsed;
            }
        }
        for (String nest : new String[]{"data", "result"}) {
            JsonNode inner = n.path(nest);
            if (inner.isObject()) {
                int nested = resolveStatus(inner);
                if (nested >= 0) {
                    return nested;
                }
            }
        }
        if (n.path("status").isMissingNode() && !n.path("code").isMissingNode()) {
            String code = n.path("code").asText("").trim();
            if ("1".equals(code)) {
                return 0;
            }
            if ("0".equals(code)) {
                return 2;
            }
        }
        String returnCode = n.path("returncode").asText("").trim();
        if ("00".equals(returnCode)) {
            return 0;
        }
        if ("2".equals(returnCode)) {
            return 2;
        }
        return -1;
    }

    static int parseStatusValue(JsonNode st) {
        if (st == null || st.isMissingNode() || st.isNull()) {
            return -1;
        }
        if (st.isNumber()) {
            return st.intValue();
        }
        if (st.isBoolean()) {
            return st.asBoolean(false) ? 0 : 2;
        }
        if (!st.isTextual()) {
            return -1;
        }
        String t = st.asText("").trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException ignored) {
            return switch (t) {
                case "success", "ok", "succeeded" -> 0;
                case "redirect", "processing", "3ds", "pending" -> 1;
                case "failed", "fail", "failure", "error" -> 2;
                default -> -1;
            };
        }
    }

    static String resolveMsg(JsonNode n) {
        if (n == null || n.isMissingNode()) {
            return "";
        }
        for (String key : new String[]{"msg", "message", "error", "errmsg", "info"}) {
            String v = n.path(key).asText("").trim();
            if (!v.isEmpty()) {
                return v;
            }
        }
        for (String nest : new String[]{"data", "result"}) {
            JsonNode inner = n.path(nest);
            if (inner.isObject()) {
                String nested = resolveMsg(inner);
                if (!nested.isEmpty()) {
                    return nested;
                }
            }
        }
        return "";
    }

    static String resolveUrl3ds(JsonNode n) {
        if (n == null || n.isMissingNode()) {
            return "";
        }
        for (String key : new String[]{"url", "redirectUrl", "redirect_url", "redirect"}) {
            String v = n.path(key).asText("").trim();
            if (!v.isEmpty()) {
                return v;
            }
        }
        for (String nest : new String[]{"data", "result"}) {
            JsonNode inner = n.path(nest);
            if (inner.isObject()) {
                String nested = resolveUrl3ds(inner);
                if (!nested.isEmpty()) {
                    return nested;
                }
            }
        }
        return "";
    }
}
