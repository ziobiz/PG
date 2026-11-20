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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payso Web API v2 — hosted ui-payments (RE) · Inquiry(기간 목록).
 * {@code POST /payment/gateway/v2/ui-payments}
 * {@code POST /payment/gateway/v2/inquiry/transaction}
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
        return createUiPayment(creds, referenceNo, customerEmail, description, amount, currencyCode,
                lang, returnUrl, postbackUrl, paymentChannel, null, null, null);
    }

    /**
     * @param customerName 전체 성명(카드 명의 프리필 시도 — 문서 미기재 필드, Payso가 무시할 수 있음)
     * @param firstName    이름
     * @param lastName     성
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
                                               String paymentChannel,
                                               String customerName,
                                               String firstName,
                                               String lastName) {
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
        /*
         * Payso HPP: customerName 에 하이픈·숫자·기호가 있으면
         * 「Invalid data, please check the customerName」로 거절(500 UI).
         * 영문 알파벳·공백만 전달. 비우면 필드 생략(카드 명의는 HPP에서 입력).
         */
        String fn = sanitizePaysoPersonName(firstName, 40);
        String ln = sanitizePaysoPersonName(lastName, 40);
        String fullName = sanitizePaysoPersonName(customerName, 80);
        if (fullName.isBlank() && (!fn.isBlank() || !ln.isBlank())) {
            fullName = clamp((fn + " " + ln).trim(), 80);
        }
        if (!fullName.isBlank()) {
            body.put("customerName", fullName);
            body.put("cardHolder", fullName);
            body.put("cardholder", fullName);
            body.put("cardHolderName", fullName);
            body.put("payerName", fullName);
            body.put("name", fullName);
        }
        if (!fn.isBlank()) {
            body.put("firstName", fn);
        }
        if (!ln.isBlank()) {
            body.put("lastName", ln);
        }
        body.put("referenceNo", ref);
        /*
         * description 에 가맹 상품명·주문번호를 넣지 않음(가맹 식별·패턴 노출 방지).
         * 호출부에서 중립 문구를 넘기며, 비어 있으면 Payment.
         */
        String desc = description != null ? description.trim() : "";
        if (desc.isBlank() || looksLikeMerchantOrderLeak(desc)) {
            desc = "Payment";
        }
        body.put("description", clamp(sanitizePaysoDescription(desc), 100));
        String cur = currencyCode != null && !currencyCode.isBlank() ? currencyCode.trim() : "00";
        /* JPY(02) 등 소수 없는 통화는 정수 금액 — HPP Invalid data 예방 */
        int scale = "02".equals(cur) ? 0 : 2;
        BigDecimal amt = amount != null ? amount.setScale(scale, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        body.put("amount", amt);
        body.put("paymentChannel", paymentChannel != null && !paymentChannel.isBlank() ? paymentChannel : "ALL");
        body.put("paymentGateway", "");
        body.put("installmentTerm", "");
        body.put("currencyCode", cur);
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

    /**
     * Inquiry API — 기간·상태·페이징 거래 목록 (통합내역·통합체크).
     *
     * @param status          예: {@code ALL}
     * @param transactionId   선택(단건 필터). 비우면 기간 전체
     * @param startDateIso    {@code yyyy-MM-dd'T'HH:mm:ss}
     * @param endDateIso      {@code yyyy-MM-dd'T'HH:mm:ss}
     */
    public Map<String, Object> inquiryTransaction(PaysoCredentials creds,
                                                   String status,
                                                   String transactionId,
                                                   String startDateIso,
                                                   String endDateIso,
                                                   int currentPage,
                                                   int recordsPerPage) {
        if (creds == null || !creds.isInquiryUsable()) {
            return err("PAYSO_CREDENTIALS", "Payso Inquiry API 키가 없습니다.");
        }
        int page = Math.max(currentPage, 1);
        int size = Math.min(Math.max(recordsPerPage, 1), 100);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("status", status != null && !status.isBlank() ? status.trim() : "ALL");
        body.put("transactionId", transactionId != null ? transactionId.trim() : "");
        body.put("startDate", startDateIso != null ? startDateIso.trim() : "");
        body.put("endDate", endDateIso != null ? endDateIso.trim() : "");
        body.put("currentPage", page);
        body.put("recordsPerPage", size);

        Map<String, Object> raw = exchangeInquiry(creds, "/payment/gateway/v2/inquiry/transaction", body);
        if (!Boolean.TRUE.equals(raw.get("ok"))) {
            return raw;
        }
        JsonNode json = raw.get("json") instanceof JsonNode jn ? jn : null;
        String code = "";
        if (json != null && json.has("status") && json.get("status").has("statusCode")) {
            code = json.get("status").get("statusCode").asText("");
        }
        if (!SUCCESS_CODE.equals(code) && json != null) {
            /* 일부 환경은 2A002 외 성공 코드 — data 배열이 있으면 성공으로 간주 */
            if (!json.has("data") || !json.get("data").isArray()) {
                String msg = json.has("status") && json.get("status").has("message")
                        ? json.get("status").get("message").asText("Payso Inquiry 실패")
                        : "Payso Inquiry 실패";
                return err("PAYSO_INQUIRY", msg);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("json", json);
        out.put("currentPage", json != null ? json.path("currentPage").asInt(page) : page);
        out.put("recordsPerPage", json != null ? json.path("recordsPerPage").asInt(size) : size);
        out.put("totalRecords", json != null ? json.path("totalRecords").asLong(0L) : 0L);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (json != null && json.path("data").isArray()) {
            for (JsonNode n : json.path("data")) {
                rows.add(parseInquiryRow(n));
            }
        }
        out.put("data", rows);
        return out;
    }

    private static Map<String, Object> parseInquiryRow(JsonNode n) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (n == null || n.isNull()) {
            return m;
        }
        m.put("merchantId", text(n, "merchantId", "merchant_id"));
        m.put("transactionId", text(n, "transactionId", "transaction_id"));
        m.put("orderNo", text(n, "orderNo", "order_no", "order"));
        m.put("referenceNo", text(n, "referenceNo", "reference_no", "refno"));
        m.put("amount", n.path("amount").isNumber()
                ? n.path("amount").decimalValue().toPlainString()
                : text(n, "amount"));
        m.put("currencyCode", text(n, "currencyCode", "currency_code", "currency"));
        m.put("channelCode", text(n, "channelCode", "channel_code"));
        m.put("cardType", text(n, "cardType", "card_type"));
        m.put("statusCode", text(n, "statusCode", "status_code"));
        m.put("statusDesc", text(n, "statusDesc", "status_desc", "status"));
        m.put("orderDateTime", text(n, "orderDateTime", "order_date_time", "orderDate"));
        return m;
    }

    private static String text(JsonNode n, String... keys) {
        if (n == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            JsonNode v = n.get(k);
            if (v != null && !v.isNull() && v.isValueNode()) {
                String s = v.asText("");
                if (s != null && !s.isBlank() && !"-".equals(s.trim())) {
                    return s.trim();
                }
            }
        }
        return "";
    }

    private Map<String, Object> exchange(PaysoCredentials creds, HttpMethod method, String path, ObjectNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        /* Payso docs: exact header casing apiKey / secretKey */
        headers.set("apiKey", creds.apiKey());
        headers.set("secretKey", creds.secretKey());
        return exchangeWithHeaders(creds.apiBase(), method, path, body, headers);
    }

    /** Inquiry 문서: apiKey + X-Secret-Key + X-Merchant-Id (secretKey 병행) */
    private Map<String, Object> exchangeInquiry(PaysoCredentials creds, String path, ObjectNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("apiKey", creds.apiKey());
        headers.set("secretKey", creds.secretKey());
        headers.set("X-Secret-Key", creds.secretKey());
        headers.set("X-Merchant-Id", creds.merchantId());
        return exchangeWithHeaders(creds.apiBase(), HttpMethod.POST, path, body, headers);
    }

    private Map<String, Object> exchangeWithHeaders(String apiBase, HttpMethod method, String path,
                                                    ObjectNode body, HttpHeaders headers) {
        String url = join(apiBase, path);
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

    /**
     * Payso person name: A–Z / a–z / space only.
     * Hyphen·underscore·digits·CJK → space then collapse (HPP customerName 거부 예방).
     */
    static String sanitizePaysoPersonName(String raw, int maxLen) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim()
                .replace('\u00A0', ' ')
                .replaceAll("[‐‑‒–—―−_./\\\\]+", " ")
                .replace('-', ' ');
        s = s.replaceAll("[^A-Za-z ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return clamp(s, maxLen);
    }

    /** description: printable ASCII, no control chars; keep letters/digits/space briefly. */
    static String sanitizePaysoDescription(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Payment";
        }
        String s = raw.trim().replaceAll("[^A-Za-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return s.isBlank() ? "Payment" : s;
    }

    /** 가맹 orderNo·긴 숫자 패턴을 description 으로 넘기지 않기 위한 휴리스틱. */
    static boolean looksLikeMerchantOrderLeak(String desc) {
        if (desc == null) {
            return false;
        }
        String t = desc.trim();
        if (t.matches("(?i)^order[#:\\s-]*[A-Za-z0-9_-]+$")) {
            return true;
        }
        return t.matches("^[0-9]{6,}$");
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
