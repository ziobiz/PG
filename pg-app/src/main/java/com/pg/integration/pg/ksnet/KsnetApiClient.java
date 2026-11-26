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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    /** 승인 응답에 카드번호·승인 핵심 필드를 요청(구형 rpyParams / JSON 겸용). */
    private static final String APPROVE_RPY_PARAMS =
            "authyn`trno`trddt`trdtm`amt`authno`msg1`msg2`ordno`isscd`aqucd`result`halbu`cardno`cardNo";

    /**
     * 승인 — {@code POST recv_post.jsp} JSON {@code {commConId, amount, actionType:"1", storeid, rpyParams}}.
     *
     * @param amountKsnet KSNET 전송 금액 문자열(USD × 1000, 예 130.00 → "130000")
     * @return {@code ok}(HTTP·파싱), {@code approved}(승인 성공), {@code tid}, {@code authNo}, {@code message},
     *         {@code code}, {@code cardMasked}, {@code amount}, {@code tradeDate}, {@code tradeTime},
     *         {@code tradeDateTime}, {@code fee}, {@code currency}
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
        body.put("rpyParams", APPROVE_RPY_PARAMS);
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
        out.put("cardMasked", extractCardMasked(json));
        putAmountFields(out, json);
        putTradeTimeFields(out, json);
        putFeeField(out, json);
        out.put("currency", firstNonBlank(text(json, "currencyType", "currency", "curType"), "USD"));
        out.put("json", json);
        return out;
    }

    /**
     * 거래 조회(대사) — {@code POST /api/v1/card/trade}, {@code Authorization: pgapi {apiKey}}.
     * TID 또는 ORDER_NUMB 로 단건 조회해 마스킹 카드·승인 원문을 받는다.
     */
    public Map<String, Object> tradeInquire(KsnetCredentials creds, String keyType, String tradeKey) {
        if (creds == null || creds.apiKey().isBlank()) {
            return err("KSNET_API_KEY", "KSNET apiKey(pgapi) 가 없어 거래조회할 수 없습니다.");
        }
        if (tradeKey == null || tradeKey.isBlank()) {
            return err("KSNET_TRADE_KEY", "거래키(TID/주문번호)가 필요합니다.");
        }
        String type = keyType != null && !keyType.isBlank() ? keyType.trim().toUpperCase(Locale.ROOT) : "TID";
        if (!"TID".equals(type) && !"ORDER_NUMB".equals(type) && !"ORDER".equals(type)) {
            type = "TID";
        }
        if ("ORDER".equals(type)) {
            type = "ORDER_NUMB";
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("mid", creds.storeId());
        body.put("orgTradeKeyType", type);
        body.put("orgTradeKey", tradeKey.trim());
        body.put("tradeKeyType", type);
        body.put("tradeKey", tradeKey.trim());
        body.put("currencyType", "USD");
        Map<String, Object> raw = exchange(creds.tradeUrl(), body, "pgapi " + creds.apiKey());
        if (!Boolean.TRUE.equals(raw.get("ok"))) {
            return raw;
        }
        JsonNode json = raw.get("json") instanceof JsonNode jn ? jn : null;
        if (json != null && looksLikeAuthError(json)) {
            Map<String, Object> err = err("KSNET_TRADE_AUTH", text(json, "message", "msg", "resultMsg"));
            err.put("code", text(json, "code", "resultCode"));
            err.put("json", json);
            return err;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("tid", firstNonBlank(
                text(json, "trno", "tid", "transactionId", "transaction_id", "trNo", "orgTradeKey"),
                tradeKey.trim()));
        out.put("authNo", text(json, "authno", "authNo", "approvalNo", "approval_no"));
        out.put("orderNo", text(json, "ordno", "orderNo", "order_no", "orderNumb", "orderNumber"));
        out.put("message", text(json, "msg2", "msg1", "message", "resultMsg", "rmsg"));
        out.put("code", text(json, "authyn", "resultCode", "rcode", "code", "status"));
        out.put("cardMasked", extractCardMasked(json));
        putAmountFields(out, json);
        putTradeTimeFields(out, json);
        putFeeField(out, json);
        out.put("currency", firstNonBlank(text(json, "currencyType", "currency", "curType"), "USD"));
        out.put("approved", isApproved(json) || looksPaidTrade(json));
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

    /** 승인·거래조회 JSON에서 마스킹 카드 추출(포털형 XXXX 포함). */
    static String extractCardMasked(JsonNode json) {
        if (json == null || json.isMissingNode()) {
            return "";
        }
        String direct = text(json, "cardno", "cardNo", "card_no", "cardNumber", "cardNum",
                "cardMasked", "maskedCard", "maskCardNo", "card_mask", "pan", "CardNo");
        String masked = maskCard(direct);
        if (!masked.isBlank()) {
            return masked;
        }
        return maskCard(findCardLikeValue(json, 0));
    }

    private static String findCardLikeValue(JsonNode n, int depth) {
        if (n == null || depth > 4) {
            return "";
        }
        if (n.isValueNode()) {
            String s = n.asText("");
            return looksLikeMaskedOrPan(s) ? s.trim() : "";
        }
        if (n.isObject()) {
            var it = n.fields();
            while (it.hasNext()) {
                var e = it.next();
                String k = e.getKey() != null ? e.getKey().toLowerCase(Locale.ROOT) : "";
                if (k.contains("card") || k.contains("pan") || k.contains("mask")) {
                    String hit = findCardLikeValue(e.getValue(), depth + 1);
                    if (!hit.isBlank()) {
                        return hit;
                    }
                }
            }
            it = n.fields();
            while (it.hasNext()) {
                String hit = findCardLikeValue(it.next().getValue(), depth + 1);
                if (!hit.isBlank()) {
                    return hit;
                }
            }
        } else if (n.isArray()) {
            for (JsonNode c : n) {
                String hit = findCardLikeValue(c, depth + 1);
                if (!hit.isBlank()) {
                    return hit;
                }
            }
        }
        return "";
    }

    private static boolean looksLikeMaskedOrPan(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        String d = s.replaceAll("[\\s-]", "");
        if (d.length() < 8 || d.length() > 24) {
            return false;
        }
        int digits = 0;
        int mask = 0;
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
            } else if (c == '*' || c == 'X' || c == 'x') {
                mask++;
            } else {
                return false;
            }
        }
        return digits >= 4 && (mask >= 2 || digits >= 12);
    }

    /** KSNET 포털형(XXXX)·별표 마스킹 유지, 평문만 앞4+******+뒤4. */
    static String maskCard(String card) {
        if (card == null || card.isBlank()) {
            return "";
        }
        String d = card.replaceAll("\\s+", "");
        if (d.contains("*") || d.toUpperCase(Locale.ROOT).contains("X")) {
            return d.length() > 32 ? d.substring(0, 32) : d;
        }
        String digits = d.replaceAll("[^0-9]", "");
        if (digits.length() < 8) {
            return "";
        }
        return digits.substring(0, 4) + "******" + digits.substring(digits.length() - 4);
    }

    private static boolean looksLikeAuthError(JsonNode json) {
        String code = text(json, "code", "resultCode", "rcode");
        return code.startsWith("A04") || "A0403".equalsIgnoreCase(code);
    }

    private static boolean looksPaidTrade(JsonNode json) {
        String st = text(json, "status", "tradeStatus", "approvalType", "승인구분");
        if (st.isBlank()) {
            return false;
        }
        String u = st.toUpperCase(Locale.ROOT);
        return u.contains("APPROV") || u.contains("성공") || u.contains("승인") || "O".equals(u) || "Y".equals(u);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return b != null ? b.trim() : "";
    }

    private static void putAmountFields(Map<String, Object> out, JsonNode json) {
        String rawAmt = text(json, "amt", "amount", "tradeAmount", "authAmount", "payAmount");
        out.put("amountRaw", rawAmt);
        String display = fromKsnetUsdAmountString(rawAmt);
        out.put("amount", !display.isBlank() ? display : rawAmt);
    }

    private static void putFeeField(Map<String, Object> out, JsonNode json) {
        String fee = text(json, "fee", "feeAmt", "feeAmount", "pgFee", "chargeFee", "commission", "mdr");
        if (fee.isBlank()) {
            out.put("fee", "");
            return;
        }
        String asUsd = fromKsnetUsdAmountString(fee);
        out.put("fee", !asUsd.isBlank() ? asUsd : fee);
    }

    private static void putTradeTimeFields(Map<String, Object> out, JsonNode json) {
        String d = text(json, "trddt", "trdDt", "tradeDate", "authDate", "approvalDate", "trxDate", "payDate");
        String t = text(json, "trdtm", "trdTm", "tradeTime", "authTime", "approvalTime", "trxTime", "payTime");
        String combined = text(json, "tradeDateTime", "trxDateTime", "approvalDateTime", "payDateTime", "orderDateTime");
        out.put("tradeDate", d);
        out.put("tradeTime", t);
        LocalDateTime ldt = parseTradeDateTime(d, t, combined);
        out.put("tradeDateTime", ldt != null
                ? ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "");
    }

    /**
     * KSNET {@code trddt}+{@code trdtm} (yyMMdd/yyyyMMdd + HHmmss) 또는 ISO/공백 구분 일시 파싱.
     */
    public static LocalDateTime parseTradeDateTime(String dateRaw, String timeRaw, String combinedRaw) {
        String combined = combinedRaw != null ? combinedRaw.trim() : "";
        if (!combined.isBlank()) {
            LocalDateTime fromCombined = tryParseDateTime(combined);
            if (fromCombined != null) {
                return fromCombined;
            }
        }
        String d = dateRaw != null ? dateRaw.trim().replaceAll("[^0-9]", "") : "";
        String t = timeRaw != null ? timeRaw.trim().replaceAll("[^0-9]", "") : "";
        if (d.length() == 6) {
            d = "20" + d;
        }
        if (d.length() != 8) {
            return null;
        }
        if (t.isBlank()) {
            t = "000000";
        }
        if (t.length() == 4) {
            t = t + "00";
        }
        if (t.length() > 6) {
            t = t.substring(0, 6);
        }
        while (t.length() < 6) {
            t = t + "0";
        }
        try {
            LocalDate ld = LocalDate.parse(d, DateTimeFormatter.BASIC_ISO_DATE);
            LocalTime lt = LocalTime.parse(t, DateTimeFormatter.ofPattern("HHmmss"));
            return LocalDateTime.of(ld, lt);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime tryParseDateTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim().replace('T', ' ');
        for (DateTimeFormatter f : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
                DateTimeFormatter.ofPattern("yyMMddHHmmss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        }) {
            try {
                if (f == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
                    return LocalDateTime.parse(v.replace(' ', 'T'));
                }
                return LocalDateTime.parse(v, f);
            } catch (Exception ignored) {
                /* next */
            }
        }
        String digits = v.replaceAll("[^0-9]", "");
        if (digits.length() >= 12) {
            return parseTradeDateTime(digits.substring(0, digits.length() >= 14 ? 8 : 6),
                    digits.substring(digits.length() >= 14 ? 8 : 6), "");
        }
        return null;
    }

    /** KSNET 금액 문자열(USD×1000 정수 또는 일반 소수) → 표시용 USD. */
    public static String fromKsnetUsdAmountString(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim().replace(",", "");
        try {
            BigDecimal n = new BigDecimal(s);
            if (s.indexOf('.') < 0 && n.abs().compareTo(new BigDecimal("1000")) >= 0) {
                /* 정수이고 1000 이상이면 ×1000 스케일로 본다 (1.04 → 1040) */
                return n.movePointLeft(3).stripTrailingZeros().toPlainString();
            }
            return n.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return "";
        }
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
            /* 대소문자 무시 키 */
            if (root.isObject()) {
                var it = root.fields();
                while (it.hasNext()) {
                    var e = it.next();
                    if (e.getKey() != null && e.getKey().equalsIgnoreCase(k)
                            && e.getValue() != null && e.getValue().isValueNode()) {
                        String s = e.getValue().asText("");
                        if (s != null && !s.isBlank()) {
                            return s.trim();
                        }
                    }
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

