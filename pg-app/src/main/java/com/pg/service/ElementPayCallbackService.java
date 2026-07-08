package com.pg.service;

import com.pg.dto.NotifyReceiveOutcome;
import com.pg.entity.PgAgency;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.elementpay.ElementPayCredentials;
import com.pg.receipt.TransactionReceiptEmailService;
import com.pg.splitpay.SplitPayPaymentHookService;
import com.pg.util.ElementPayHashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElementPay 웹훅(check·pay·payment.*) — 동기 JSON+HMAC 응답이 필요한 콜백을 처리합니다.
 * <p><b>노티미들웨어(외부 NOTI) 경유 — ChillPay와 동일 운영:</b>
 * ElementPay 캐비net Webhook URL 은 <strong>NOTI 서버</strong>에 등록하고,
 * NOTI 가 ICOPAY {@code /api/middleware/notify/v1/pg-notify/{ingressToken}/ELEMENTPAY} 로 전달합니다.
 * check/pay 는 ElementPay 전용 JSON({@code response.status}+{@code hash}) 응답이 필요하므로
 * NOTI 는 ICOPAY 응답 본문을 <strong>변환 없이</strong> ElementPay 로 되돌려야 합니다.
 * <p>가맹 식별: ElementPay 에는 ICOPAY 집계 Merchant Key 만 노출하며,
 * 가맹 업체코드는 웹훅 {@code order}·내부 {@code pg_trnsctn} 으로만 복원합니다.
 */
@Service
public class ElementPayCallbackService {

    private static final Logger log = LoggerFactory.getLogger(ElementPayCallbackService.class);
    private static final Pattern ICOPAY_COMP_ID = Pattern.compile(
            "icopayCompId=([A-Za-z0-9_.-]+)", Pattern.CASE_INSENSITIVE);

    private final HqNotifyEnvService hqNotifyEnvService;
    private final ElementPayPaymentService elementPayPaymentService;
    private final ElementPaySaleRecordService elementPaySaleRecordService;
    private final PgNotifyInboundPersistService inboundPersistService;
    private final SettlementCalcService settlementCalcService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final SplitPayPaymentHookService splitPayPaymentHookService;
    private final TransactionReceiptEmailService transactionReceiptEmailService;

    public ElementPayCallbackService(HqNotifyEnvService hqNotifyEnvService,
                                     ElementPayPaymentService elementPayPaymentService,
                                     ElementPaySaleRecordService elementPaySaleRecordService,
                                     PgNotifyInboundPersistService inboundPersistService,
                                     SettlementCalcService settlementCalcService,
                                     MerchantOutboundNotifyService merchantOutboundNotifyService,
                                     SplitPayPaymentHookService splitPayPaymentHookService,
                                     TransactionReceiptEmailService transactionReceiptEmailService) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.elementPayPaymentService = elementPayPaymentService;
        this.elementPaySaleRecordService = elementPaySaleRecordService;
        this.inboundPersistService = inboundPersistService;
        this.settlementCalcService = settlementCalcService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
    }

    /**
     * ElementPay 형식 콜백이면 처리하고 JSON 응답 outcome 을 반환. 해당 없으면 empty.
     */
    public Optional<NotifyReceiveOutcome> tryHandleSyncCallback(String pathToken,
                                                                String notifyTargetCode,
                                                                String rawBody,
                                                                String clientIp,
                                                                HttpServletRequest request) {
        Map<String, String> fields = parseForm(rawBody);
        String method = fields.get("method");
        if (method == null || method.isBlank()) {
            return Optional.empty();
        }
        if (!looksLikeElementPay(fields)) {
            return Optional.empty();
        }
        if (!hqNotifyEnvService.getOrCreate().getIngressToken().equals(pathToken)) {
            return Optional.empty();
        }
        try {
            return Optional.of(handle(method.trim().toLowerCase(Locale.ROOT), fields, rawBody, clientIp, notifyTargetCode));
        } catch (Exception e) {
            log.warn("ElementPay callback 실패 method={}: {}", method, e.getMessage());
            return Optional.of(jsonResponse(401, "Internal error", null));
        }
    }

    @Transactional
    protected NotifyReceiveOutcome handle(String method, Map<String, String> fields,
                                          String rawBody, String clientIp, String notifyTargetCode) {
        persistInbound(rawBody, clientIp, notifyTargetCode, fields);

        Optional<PgAgency> agencyOpt = resolveAgency(fields);
        if (agencyOpt.isEmpty()) {
            log.warn("ElementPay callback agency 미해석 order={}", fields.get("order"));
            return jsonResponse(474, "Agency not found", null);
        }
        ElementPayCredentials cred = ElementPayCredentials.from(agencyOpt.get());
        String hash = fields.get("hash");
        if (!ElementPayHashUtil.verifyCallbackRequest(cred.webhookSecretKey(), method, fields, hash)) {
            log.warn("ElementPay callback hash invalid method={} order={}", method, fields.get("order"));
            return jsonResponse(401, "Invalid hash", cred);
        }

        String orderNo = nz(fields.get("order"));
        String compCode = extractCompId(fields);

        return switch (method) {
            case "check" -> handleCheck(orderNo, compCode, fields, cred);
            case "pay" -> handlePay(orderNo, compCode, fields, cred);
            default -> handleAsyncEvent(method, orderNo, compCode, fields, cred);
        };
    }

    private NotifyReceiveOutcome handleCheck(String orderNo, String compCode,
                                             Map<String, String> fields, ElementPayCredentials cred) {
        if (orderNo.isBlank()) {
            return jsonResponse(475, "Order required", cred);
        }
        Optional<PgTrnsctn> txn = compCode.isBlank()
                ? elementPaySaleRecordService.findAnyByOrder(orderNo)
                : elementPaySaleRecordService.findByMerchantAndOrder(compCode, orderNo);
        if (txn.isEmpty()) {
            return jsonResponse(474, "Payment not found", cred);
        }
        BigDecimal expected = txn.get().getAmtKrw();
        BigDecimal received = parseAmount(fields.get("amount"));
        if (expected != null && received != null && expected.compareTo(received) != 0) {
            return jsonResponse(475, "Amount mismatch", cred);
        }
        return jsonResponse(270, "Payment can process", cred);
    }

    private NotifyReceiveOutcome handlePay(String orderNo, String compCode,
                                           Map<String, String> fields, ElementPayCredentials cred) {
        if (orderNo.isBlank()) {
            return jsonResponse(474, "Order required", cred);
        }
        if (compCode.isBlank()) {
            compCode = elementPaySaleRecordService.findAnyByOrder(orderNo)
                    .map(PgTrnsctn::getMerchantId)
                    .orElse("");
        }
        String paymentId = nz(fields.get("id"));
        Optional<PgTrnsctn> updated = elementPaySaleRecordService.applyOutcome(
                compCode, orderNo, true, paymentId, "ElementPay paid");
        if (updated.isEmpty()) {
            return jsonResponse(474, "Payment not found", cred);
        }
        PgTrnsctn t = updated.get();
        try {
            splitPayPaymentHookService.onTxnStatusChange(t.getOrderNo(), t.getStatus(), t.getTrnId());
        } catch (Exception ignored) {
        }
        try {
            transactionReceiptEmailService.scheduleAfterPaid(t);
        } catch (Exception e) {
            log.warn("ElementPay 거래 영수증 메일 연동 실패 trnId={}: {}", t.getTrnId(), e.getMessage());
        }
        try {
            if (t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
                settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
            }
        } catch (Exception e) {
            log.warn("ElementPay 정산 트리거 실패: {}", e.getMessage());
        }
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "CALLBACK");
        } catch (Exception e) {
            log.warn("ElementPay outbound 노티 예약 실패: {}", e.getMessage());
        }
        return jsonResponse(205, "Payment success", cred);
    }

    private NotifyReceiveOutcome handleAsyncEvent(String method, String orderNo, String compCode,
                                                  Map<String, String> fields, ElementPayCredentials cred) {
        if (method.startsWith("payment.") && !orderNo.isBlank()) {
            if (compCode.isBlank()) {
                compCode = elementPaySaleRecordService.findAnyByOrder(orderNo)
                        .map(PgTrnsctn::getMerchantId).orElse("");
            }
            boolean paid = false;
            elementPaySaleRecordService.applyOutcome(compCode, orderNo, paid,
                    nz(fields.get("id")), method);
        }
        return jsonResponse(270, "Notification received", cred);
    }

    private NotifyReceiveOutcome jsonResponse(int status, String message, ElementPayCredentials cred) {
        long ts = Instant.now().toEpochMilli();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("message", message);
        response.put("timestamp", ts);
        String secret = cred != null ? cred.webhookSecretKey() : "";
        String hash = secret.isBlank() ? "" : ElementPayHashUtil.signCallbackResponse(secret, response);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("response", response);
        body.put("hash", hash);
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            return NotifyReceiveOutcome.json(json, HttpStatus.OK);
        } catch (Exception e) {
            return NotifyReceiveOutcome.json("{\"response\":{\"status\":500}}", HttpStatus.OK);
        }
    }

    private void persistInbound(String rawBody, String clientIp, String notifyTargetCode, Map<String, String> fields) {
        try {
            PgNotifyInbound in = new PgNotifyInbound();
            in.setRawBody(rawBody != null && rawBody.length() > 500_000
                    ? rawBody.substring(0, 500_000) + "...(truncated)" : rawBody);
            in.setContentType("application/x-www-form-urlencoded");
            in.setClientIp(clientIp);
            in.setNotifyChannelType("CALLBACK");
            in.setNotifyTargetCode(notifyTargetCode != null ? notifyTargetCode.trim() : "ELEMENTPAY");
            in.setProcessStatus("PARSED");
            in.setMid(fields.get("key"));
            inboundPersistService.saveInbound(in);
        } catch (Exception e) {
            log.debug("ElementPay inbound 저장 생략: {}", e.getMessage());
        }
    }

    private Optional<PgAgency> resolveAgency(Map<String, String> fields) {
        return elementPayPaymentService.resolveAgencyByMerchantKey(fields.get("key"));
    }

    private static boolean looksLikeElementPay(Map<String, String> fields) {
        return fields.containsKey("hash")
                && fields.containsKey("timestamp")
                && (fields.containsKey("order") || fields.containsKey("id"));
    }

    private static String extractCompId(Map<String, String> fields) {
        String data = fields.get("data");
        if (data != null && !data.isBlank()) {
            try {
                String decoded = URLDecoder.decode(data, StandardCharsets.UTF_8);
                Matcher m = ICOPAY_COMP_ID.matcher(decoded);
                if (m.find()) {
                    return m.group(1);
                }
            } catch (Exception ignored) {
            }
            Matcher m = ICOPAY_COMP_ID.matcher(data);
            if (m.find()) {
                return m.group(1);
            }
        }
        String merchantData = fields.get("_merchantData");
        if (merchantData != null) {
            Matcher m = ICOPAY_COMP_ID.matcher(merchantData);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "";
    }

    private static Map<String, String> parseForm(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(k, v);
        }
        return out;
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String nz(String s) {
        return s != null ? s.trim() : "";
    }
}
