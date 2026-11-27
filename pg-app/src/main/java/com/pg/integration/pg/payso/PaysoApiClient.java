package com.pg.integration.pg.payso;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Payso Web API v2 — hosted ui-payments (RE) · Inquiry(기간 목록) · Void(정산 전 취소).
 * {@code POST /payment/gateway/v2/ui-payments}
 * {@code POST /payment/gateway/v2/inquiry/transaction}
 * {@code POST /payment/gateway/v2/payment/voidpayment}
 *
 * <p>가맹 상호·홈페이지·도메인·IP·가맹 orderNo 는 Payso 로 보내지 않는다.
 * return/postback 은 ICOPAY·NOTI(또는 {@code "-"}) 만. customerEmail 은 중립 ICOPAY 메일.
 * 공개 문서에 정산 후 환불 REST 는 없고 Void(정산 전)만 제공된다.
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
                lang, returnUrl, postbackUrl, paymentChannel, null, null, null, false);
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
        return createUiPayment(creds, referenceNo, customerEmail, description, amount, currencyCode,
                lang, returnUrl, postbackUrl, paymentChannel, customerName, firstName, lastName, false);
    }

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
                                               String lastName,
                                               boolean sendRealBuyerEmail) {
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
        /* 이메일증명 활성 시 실제 구매자 이메일, 비활성 시 noreply@icopay.co.kr */
        body.put("customerEmail",
                PgOutboundMerchantPrivacy.safePgCustomerEmail(customerEmail, sendRealBuyerEmail));
        /*
         * Payso HPP: customerName 에 하이픈·숫자·기호가 있으면
         * 「Invalid data, please check the customerName」로 거절(500 UI).
         * 영문 알파벳·공백만 전달. 비우면 필드 생략(카드 명의는 HPP에서 입력).
         * 구매자(카드 명의) 이름만 — 가맹 상호명 자리로 쓰지 말 것.
         */
        String fn = sanitizePaysoPersonName(firstName, 40);
        String ln = sanitizePaysoPersonName(lastName, 40);
        String fullName = sanitizePaysoPersonName(customerName, 80);
        if (fullName.isBlank() && (!fn.isBlank() || !ln.isBlank())) {
            fullName = clamp((fn + " " + ln).trim(), 80);
        }
        if (!fullName.isBlank() && !PgOutboundMerchantPrivacy.containsUrlDomainOrIp(fullName)) {
            body.put("customerName", fullName);
            body.put("cardHolder", fullName);
            body.put("cardholder", fullName);
            body.put("cardHolderName", fullName);
            body.put("payerName", fullName);
            body.put("name", fullName);
        }
        if (!fn.isBlank() && !PgOutboundMerchantPrivacy.containsUrlDomainOrIp(fn)) {
            body.put("firstName", fn);
        }
        if (!ln.isBlank() && !PgOutboundMerchantPrivacy.containsUrlDomainOrIp(ln)) {
            body.put("lastName", ln);
        }
        body.put("referenceNo", ref);
        /*
         * description: 가맹 상품명·주문·URL·도메인·IP 금지 → 항상 중립 Payment.
         */
        String desc = PgOutboundMerchantPrivacy.safePgDescription(description);
        if (looksLikeMerchantOrderLeak(desc)) {
            desc = PgOutboundMerchantPrivacy.SAFE_PG_DESCRIPTION;
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
        String[] allowedUrlBases = PgOutboundUrlPolicy.allowedIcopayAndNotiBases(
                IcopayPublicIdentity.API_BASE_URL,
                PgOutboundUrlPolicy.DEFAULT_NOTI_BASE,
                PgOutboundUrlPolicy.DEFAULT_SITE_BASE,
                PgOutboundUrlPolicy.DEFAULT_NOTI_TEST_BASE);
        String safeReturn = returnUrl != null && !returnUrl.isBlank() ? returnUrl.trim() : "-";
        if (!"-".equals(safeReturn)) {
            safeReturn = PgOutboundUrlPolicy.enforceOwnDomain(
                    safeReturn, IcopayPublicIdentity.API_BASE_URL + "/payso-pay.html", allowedUrlBases);
            if (!PgOutboundUrlPolicy.isOwnDomain(safeReturn, allowedUrlBases)) {
                log.warn("PAYSO returnUrl blocked (non-ICOPAY/NOTI host) — forced ICOPAY fallback");
                safeReturn = IcopayPublicIdentity.API_BASE_URL + "/payso-pay.html";
            }
        }
        String safePostback = PgOutboundUrlPolicy.enforceOwnDomainOrDash(postbackUrl, allowedUrlBases);
        body.put("returnUrl", safeReturn);
        body.put("postbackUrl", safePostback);
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
     * Order Detail — 마스킹 카드 등 Inquiry에 없는 필드 보강.
     * {@code POST /order/orderdetailpost}
     */
    public Map<String, Object> orderDetail(PaysoCredentials creds, String referenceNo, String orderNo) {
        if (creds == null || !creds.isInquiryUsable()) {
            return err("PAYSO_CREDENTIALS", "Payso Order Detail API 키가 없습니다.");
        }
        String mid = creds.merchantId();
        String mid5 = mid.length() > 5 ? mid.substring(mid.length() - 5) : mid;
        ObjectNode body = objectMapper.createObjectNode();
        body.put("merchantId", mid5);
        body.put("orderNo", orderNo != null && !orderNo.isBlank() ? orderNo.trim() : "x");
        body.put("refNo", referenceNo != null && !referenceNo.isBlank() ? referenceNo.trim() : "x");
        body.put("productDetail", "");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("apikey", creds.apiKey());
        headers.set("apiKey", creds.apiKey());
        headers.set("merchantId", mid5);
        headers.set("merchantSecretKey", creds.secretKey());
        Map<String, Object> raw = exchangeWithHeaders(creds.apiBase(), HttpMethod.POST, "/order/orderdetailpost", body, headers);
        if (!Boolean.TRUE.equals(raw.get("ok"))) {
            return raw;
        }
        JsonNode json = raw.get("json") instanceof JsonNode jn ? jn : null;
        JsonNode row = null;
        if (json != null && json.isArray() && json.size() > 0) {
            row = json.get(0);
        } else if (json != null && json.isObject()) {
            if (json.has("data") && json.get("data").isArray() && json.get("data").size() > 0) {
                row = json.get("data").get(0);
            } else {
                row = json;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        if (row != null) {
            out.putAll(parseInquiryRow(row));
            /* Order Detail 응답 키 변형 */
            if (strMap(out, "cardNo").isEmpty()) {
                String c = text(row, "CardNo", "card_no", "CardNumber", "MaskedCardNo", "maskedCardNo",
                        "cardMask", "CardMask", "MaskedPan", "maskedPan", "CardPan", "panMask",
                        "DisplayCardNo", "displayCardNo");
                if (!c.isBlank()) {
                    out.put("cardNo", c);
                }
            }
            if (strMap(out, "feePercent").isEmpty()) {
                String fp = decimalOrText(row, "FeeRate", "feeRate", "fee_rate", "FeePercent");
                if (!fp.isBlank()) {
                    out.put("feePercent", fp);
                }
            }
            if (strMap(out, "fee").isEmpty()) {
                String f = decimalOrText(row, "Fee", "fee", "TotalFee", "ServiceFee");
                if (!f.isBlank()) {
                    out.put("fee", f);
                }
            }
            if (strMap(out, "cardIssuer").isEmpty()) {
                String iss = text(row, "CardIssuer", "cardIssuer");
                if (!iss.isBlank()) {
                    out.put("cardIssuer", iss);
                }
            }
            if (strMap(out, "referenceNo").isEmpty()) {
                String r = text(row, "ReferenceNo", "referenceNo", "refNo", "refno");
                if (!r.isBlank()) {
                    out.put("referenceNo", r);
                }
            }
            if (strMap(out, "orderNo").isEmpty()) {
                String o = text(row, "OrderNo", "orderNo");
                if (!o.isBlank()) {
                    out.put("orderNo", o);
                }
            }
        }
        return out;
    }

    private static String strMap(Map<String, Object> m, String key) {
        if (m == null || key == null) {
            return "";
        }
        Object v = m.get(key);
        return v != null ? String.valueOf(v).trim() : "";
    }

    /**
     * Void API — 정산 전 승인 취소({@code /payment/gateway/v2/payment/voidpayment}).
     * 헤더는 Inquiry 와 동일({@code apiKey}/{@code X-Secret-Key}/{@code X-Merchant-Id}).
     *
     * @param transactionId Payso transactionId(hex 등). ICOPAY 12자리 reference 가 아닐 수 있음.
     * @param amount        원승인 금액
     */
    public Map<String, Object> voidPayment(PaysoCredentials creds, String transactionId, BigDecimal amount) {
        if (creds == null || !creds.isInquiryUsable()) {
            return err("PAYSO_CREDENTIALS", "Payso Void API 키가 없습니다.");
        }
        String tid = transactionId != null ? transactionId.trim() : "";
        if (tid.isBlank()) {
            return err("INVALID_TRANSACTION_ID", "transactionId가 필요합니다.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return err("INVALID_AMOUNT", "취소 금액이 필요합니다.");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("transactionId", tid);
        body.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
        Map<String, Object> raw = exchangeInquiry(creds, "/payment/gateway/v2/payment/voidpayment", body);
        JsonNode json = raw.get("json") instanceof JsonNode jn ? jn : null;
        String code = "";
        String statusMsg = "";
        if (json != null && json.has("status")) {
            if (json.get("status").has("statusCode")) {
                code = json.get("status").get("statusCode").asText("");
            }
            if (json.get("status").has("message")) {
                statusMsg = json.get("status").get("message").asText("");
            }
        }
        if (!Boolean.TRUE.equals(raw.get("ok"))) {
            if (!statusMsg.isBlank()) {
                raw.put("message", statusMsg);
            }
            if (!code.isBlank()) {
                raw.put("statusCode", code);
            }
            return raw;
        }
        if (!SUCCESS_CODE.equals(code)) {
            String msg = !statusMsg.isBlank() ? statusMsg : "Payso Void 실패";
            Map<String, Object> err = err("PAYSO_VOID", msg);
            err.put("statusCode", code);
            if (json != null) {
                err.put("json", json);
            }
            return err;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("json", json);
        out.put("statusCode", code);
        if (json != null) {
            out.put("voidedAt", text(json, "voidedAt", "voided_at"));
            out.put("voidedStatus", text(json, "voidedStatus", "voided_status"));
            JsonNode txn = json.path("transaction");
            if (!txn.isMissingNode() && !txn.isNull()) {
                out.put("referenceNo", text(txn, "referenceNo", "reference_no"));
                if (txn.path("amount").isNumber()) {
                    out.put("amount", txn.path("amount").decimalValue().toPlainString());
                }
            }
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
        JsonNode json = raw.get("json") instanceof JsonNode jn ? jn : null;
        String code = "";
        String statusMsg = "";
        if (json != null && json.has("status")) {
            if (json.get("status").has("statusCode")) {
                code = json.get("status").get("statusCode").asText("");
            }
            if (json.get("status").has("message")) {
                statusMsg = json.get("status").get("message").asText("");
            }
        }
        /* 기간 내 거래 없음(4A008 / HTTP 404) → 빈 목록(오류 아님). 날짜 잘못·무거래 시 흔함 */
        if (isInquiryEmptyNotFound(code, statusMsg, raw.get("httpStatus"))) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("ok", true);
            empty.put("empty", true);
            empty.put("currentPage", page);
            empty.put("recordsPerPage", size);
            empty.put("totalRecords", 0L);
            empty.put("data", List.of());
            return empty;
        }
        if (!Boolean.TRUE.equals(raw.get("ok"))) {
            return raw;
        }
        if (!SUCCESS_CODE.equals(code) && json != null) {
            /* 일부 환경은 2A002 외 성공 코드 — data 배열이 있으면 성공으로 간주 */
            if (!json.has("data") || !json.get("data").isArray()) {
                String msg = !statusMsg.isBlank() ? statusMsg : "Payso Inquiry 실패";
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
        m.put("cardNo", text(n, "cardNo", "card_no", "cardNumber", "card_number", "CardNo",
                "maskedCard", "masked_card", "cardMask", "card_mask", "panMask", "pan", "CardNumber"));
        m.put("cardIssuer", text(n, "cardIssuer", "CardIssuer", "card_issuer", "issuer"));
        String fee = decimalOrText(n, "fee", "Fee", "feeAmount", "fee_amount", "serviceFee",
                "service_fee", "merchantFee", "merchant_fee", "mdr", "MDR");
        m.put("fee", fee);
        String feePct = decimalOrText(n, "feePercent", "fee_percent", "feeRate", "FeeRate",
                "fee_rate", "mdrRate", "FeeRate");
        m.put("feePercent", feePct);
        m.put("statusCode", text(n, "statusCode", "status_code"));
        m.put("statusDesc", text(n, "statusDesc", "status_desc", "status"));
        m.put("orderDateTime", text(n, "orderDateTime", "order_date_time", "orderDate"));
        return m;
    }

    private static String decimalOrText(JsonNode n, String... keys) {
        if (n == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            JsonNode v = n.get(k);
            if (v == null || v.isNull()) {
                continue;
            }
            if (v.isNumber()) {
                return v.decimalValue().stripTrailingZeros().toPlainString();
            }
            if (v.isValueNode()) {
                String s = v.asText("");
                if (s != null && !s.isBlank() && !"-".equals(s.trim())) {
                    return s.trim();
                }
            }
        }
        return "";
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

    /** Inquiry 기간 무거래 — HTTP 404 + 4A008 / transaction not found. */
    private static boolean isInquiryEmptyNotFound(String statusCode, String message, Object httpStatus) {
        String code = statusCode != null ? statusCode.trim() : "";
        if ("4A008".equalsIgnoreCase(code) || "4A007".equalsIgnoreCase(code)) {
            return true;
        }
        String msg = message != null ? message.toLowerCase(Locale.ROOT) : "";
        if (msg.contains("transaction not found") || msg.contains("no transaction")) {
            return true;
        }
        return httpStatus instanceof Number n && n.intValue() == 404;
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
