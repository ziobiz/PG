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
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final ObjectMapper objectMapper;

    public PaysoCallbackService(HqNotifyEnvService hqNotifyEnvService,
                                PaysoSaleRecordService paysoSaleRecordService,
                                MerchantOutboundNotifyService merchantOutboundNotifyService,
                                ObjectMapper objectMapper) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.paysoSaleRecordService = paysoSaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
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
        String refno = text(root, "refno", "refNo", "referenceNo", "orderNo", "order");
        String txnId = text(root, "transactionId", "transactionid", "transaction_id");
        String paysoKey = !txnId.isBlank() ? txnId : refno;
        ResolvedIds ids = resolveIds(paysoKey, refno);

        Optional<PgTrnsctn> updated = Optional.empty();
        if (!ids.orderNo.isBlank() && !ids.compId.isBlank()) {
            updated = paysoSaleRecordService.applyPaidOutcome(
                    ids.orderNo, ids.compId, paysoKey, refno, text(root, "customeremail", "customerEmail", "payer_name"));
        } else {
            log.warn("PAYSO webhook Comp-Id unresolved refno={} txn={} ip={}", refno, txnId, clientIp);
        }
        updated.ifPresent(t -> {
            try {
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "PAYSO_WEBHOOK");
            } catch (Exception e) {
                log.warn("PAYSO merchant notify: {}", e.getMessage());
            }
        });
        return outcomeJson(true, null, paysoKey, ids, HttpStatus.OK);
    }

    private ResolvedIds resolveIds(String paysoKey, String refno) {
        Optional<PgTrnsctn> matched = paysoSaleRecordService.resolveTxnForWebhook(
                paysoKey, refno, refno, paysoKey);
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
