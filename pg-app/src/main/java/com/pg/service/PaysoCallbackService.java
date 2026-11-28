package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.dto.NotifyReceiveOutcome;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Payso postback — NOTI → {@code …/pg-notify/{token}/PAYSO}.
 * form-urlencoded 또는 JSON. Comp-Id 는 주문 역추적으로 헤더 반환.
 */
@Service
public class PaysoCallbackService {

    private static final Logger log = LoggerFactory.getLogger(PaysoCallbackService.class);

    private final HqNotifyEnvService hqNotifyEnvService;
    private final PaysoSaleRecordService paysoSaleRecordService;
    private final PaysoPaymentService paysoPaymentService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final PaysoCardEnrichService paysoCardEnrichService;
    private final ObjectMapper objectMapper;

    public PaysoCallbackService(HqNotifyEnvService hqNotifyEnvService,
                                PaysoSaleRecordService paysoSaleRecordService,
                                PaysoPaymentService paysoPaymentService,
                                MerchantOutboundNotifyService merchantOutboundNotifyService,
                                PaysoCardEnrichService paysoCardEnrichService,
                                ObjectMapper objectMapper) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.paysoSaleRecordService = paysoSaleRecordService;
        this.paysoPaymentService = paysoPaymentService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.paysoCardEnrichService = paysoCardEnrichService;
        this.objectMapper = objectMapper;
    }

    public Optional<NotifyReceiveOutcome> tryHandleSyncCallback(String pathToken,
                                                                String notifyTargetCode,
                                                                String rawBody,
                                                                String clientIp,
                                                                HttpServletRequest request) {
        if (rawBody == null || rawBody.isBlank()) {
            return Optional.empty();
        }
        String target = notifyTargetCode != null ? notifyTargetCode.trim().toUpperCase(Locale.ROOT) : "";
        boolean looksPath = PgVendor.isPaysoVendorCode(target);
        JsonNode root = parseBody(rawBody);
        if (root == null) {
            return Optional.empty();
        }
        if (!looksPath && !looksLikePaysoPayload(root)) {
            return Optional.empty();
        }
        String expected = hqNotifyEnvService.getOrCreate().getIngressToken();
        if (expected == null || pathToken == null || !expected.equals(pathToken.trim())) {
            return Optional.of(NotifyReceiveOutcome.json("{\"ok\":false,\"error\":\"unauthorized\"}", HttpStatus.UNAUTHORIZED));
        }
        return Optional.of(handleEvent(root, clientIp));
    }

    @Transactional
    protected NotifyReceiveOutcome handleEvent(JsonNode root, String clientIp) {
        /* PG Reference — orderNo/order 를 ref 로 쓰지 않음(가맹 주문번호로 가짜 승인 방지) */
        String refno = text(root, "refno", "refNo", "referenceNo", "ReferenceNo");
        String txnId = text(root, "transactionId", "transactionid", "transaction_id", "TransactionId");
        String merchantOrderHint = text(root, "orderNo", "OrderNo", "order", "note", "Note");
        String paysoKey = !txnId.isBlank() ? txnId : refno;
        ResolvedIds ids = resolveIds(paysoKey, refno, merchantOrderHint);
        boolean failed = looksLikePaysoFailure(root);

        Optional<PgTrnsctn> updated = Optional.empty();
        if (!ids.orderNo.isBlank() && !ids.compId.isBlank()) {
            if (failed) {
                String reason = text(root, "statusDesc", "message", "status", "paymentStatus");
                if (reason.isBlank()) {
                    reason = "PAYSO payment failed";
                }
                updated = paysoSaleRecordService.applyFailedOutcome(
                        ids.orderNo, ids.compId, paysoKey, "PAYSO_WEBHOOK_FAIL", reason);
                log.info("PAYSO webhook FAIL mid={} order={} ref={} txn={} ip={}",
                        ids.compId, ids.orderNo, refno, txnId, clientIp);
            } else {
                boolean usableKey = PaysoSaleRecordService.isUsablePaysoPgKey(ids.orderNo, paysoKey)
                        || PaysoSaleRecordService.isUsablePaysoPgKey(ids.orderNo, refno);
                String orderDt = text(root, "orderDateTime", "order_date_time", "orderDate",
                        "OrderDateTime", "paymentDate", "PaymentDate");
                if (usableKey) {
                    updated = paysoSaleRecordService.applyPaidOutcome(
                            ids.orderNo, ids.compId, paysoKey, refno,
                            text(root, "customeremail", "customerEmail", "payer_name"),
                            false, orderDt);
                } else {
                    /* 키가 주문번호/비어 있음 → Order Detail 실승인 확인 후에만 성공 반영(오성공 방지) */
                    log.info("PAYSO webhook keys unusable → Order Detail confirm mid={} order={} ref={} txn={}",
                            ids.compId, ids.orderNo, refno, txnId);
                    updated = paysoPaymentService.confirmPaidViaOrderDetail(ids.compId, ids.orderNo);
                }
                if (updated.isPresent()) {
                    String cardRaw = text(root, "cardNo", "card_no", "CardNo", "maskedCard",
                            "cardMask", "CardMask", "panMask", "CardNumber");
                    String pgOrd = text(root, "orderNo", "OrderNo", "pgOrderNo");
                    if (!cardRaw.isBlank() || !pgOrd.isBlank() || !orderDt.isBlank()) {
                        String enrichRef = !refno.isBlank() ? refno
                                : (updated.get().getPayNo() != null ? updated.get().getPayNo() : "");
                        paysoSaleRecordService.applyInquiryEnrichment(
                                updated.get(), enrichRef, pgOrd, txnId, cardRaw, null, orderDt);
                    }
                }
            }
        } else {
            log.warn("PAYSO webhook Comp-Id unresolved refno={} txn={} orderHint={} fail={} ip={}",
                    refno, txnId, merchantOrderHint, failed, clientIp);
        }
        updated.ifPresent(t -> {
            try {
                String ch = "99".equals(t.getStatus() != null ? t.getStatus().trim() : "")
                        ? "PAYSO_WEBHOOK_FAIL" : "PAYSO_WEBHOOK";
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, ch);
            } catch (Exception e) {
                log.warn("PAYSO merchant notify: {}", e.getMessage());
            }
            if ("10".equals(t.getStatus() != null ? t.getStatus().trim() : "")) {
                try {
                    paysoCardEnrichService.scheduleAfterCommit(t);
                } catch (Exception e) {
                    log.debug("PAYSO card enrich schedule: {}", e.getMessage());
                }
            }
        });
        return outcomeJson(true, null, paysoKey, ids, HttpStatus.OK);
    }

    /** Payso / NOTI form·JSON — 실패·취소·거절 힌트. */
    static boolean looksLikePaysoFailure(JsonNode root) {
        if (root == null) {
            return false;
        }
        String st = text(root, "status", "Status", "paymentStatus", "PaymentStatus", "result", "Result")
                .trim().toLowerCase(Locale.ROOT);
        if (st.equals("fail") || st.equals("failed") || st.equals("cancel") || st.equals("cancelled")
                || st.equals("canceled") || st.equals("void") || st.equals("decline") || st.equals("declined")
                || st.equals("reject") || st.equals("rejected") || st.equals("error")
                || st.contains("wrong") || st.contains("fail")) {
            return true;
        }
        String rc = text(root, "returncode", "returnCode", "PaymentStatus", "paymentstatus");
        if ("99".equals(rc) || "1".equals(rc) || "FL".equalsIgnoreCase(rc)) {
            return true;
        }
        String desc = text(root, "statusDesc", "message", "Message").toLowerCase(Locale.ROOT);
        return desc.contains("wrong payment") || desc.contains("fail") || desc.contains("declin")
                || desc.contains("reject") || desc.contains("cancel");
    }

    private ResolvedIds resolveIds(String paysoKey, String pgRef, String merchantOrderHint) {
        Optional<PgTrnsctn> matched = paysoSaleRecordService.resolveTxnForWebhook(
                paysoKey, merchantOrderHint, merchantOrderHint, pgRef);
        if (matched.isEmpty() && merchantOrderHint != null && !merchantOrderHint.isBlank()
                && (paysoKey == null || paysoKey.isBlank())) {
            matched = paysoSaleRecordService.resolveTxnForWebhook(
                    "", merchantOrderHint, merchantOrderHint, "");
        }
        String orderNo = "";
        String compId = "";
        if (matched.isPresent()) {
            PgTrnsctn t = matched.get();
            if (t.getOrderNo() != null && !t.getOrderNo().isBlank()) {
                orderNo = t.getOrderNo().trim();
            }
            compId = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        }
        return new ResolvedIds(compId, orderNo);
    }

    private NotifyReceiveOutcome outcomeJson(boolean ok, String error, String pgRef, ResolvedIds ids, HttpStatus status) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (ids != null && ids.compId != null && !ids.compId.isBlank()) {
            headers.put("X-Icopay-Comp-Id", ids.compId.trim());
        }
        if (ids != null && ids.orderNo != null && !ids.orderNo.isBlank()) {
            headers.put("X-Icopay-Order-No", ids.orderNo.trim());
        }
        String bodyJson;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", ok);
            if (error != null && !error.isBlank()) {
                body.put("error", error);
            }
            body.put("pgTxnId", pgRef != null ? pgRef : "");
            body.put("orderNo", ids != null ? ids.orderNo : "");
            body.put("compId", ids != null ? ids.compId : "");
            body.put("compIdResolved", ids != null && !ids.compId.isBlank());
            bodyJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            bodyJson = ok ? "{\"ok\":true}" : "{\"ok\":false}";
        }
        return NotifyReceiveOutcome.json(bodyJson, status, headers);
    }

    private JsonNode parseBody(String rawBody) {
        String raw = rawBody.trim();
        try {
            if (raw.startsWith("{")) {
                return objectMapper.readTree(raw);
            }
        } catch (Exception ignored) {
            /* form fallback */
        }
        ObjectNode n = objectMapper.createObjectNode();
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = urlDecode(part.substring(0, eq));
            String v = urlDecode(part.substring(eq + 1));
            if (!k.isBlank()) {
                n.put(k, v);
            }
        }
        return n.size() > 0 ? n : null;
    }

    private static boolean looksLikePaysoPayload(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        return root.has("refno") || root.has("refNo") || root.has("referenceNo")
                || root.has("transactionId") || root.has("merchantid") || root.has("merchantId");
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

    private static final class ResolvedIds {
        final String compId;
        final String orderNo;

        ResolvedIds(String compId, String orderNo) {
            this.compId = compId != null ? compId : "";
            this.orderNo = orderNo != null ? orderNo : "";
        }
    }
}
