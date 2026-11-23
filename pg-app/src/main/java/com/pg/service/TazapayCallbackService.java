package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.dto.NotifyReceiveOutcome;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.tazapay.TazapayApiClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Tazapay webhook — NOTI → {@code …/pg-notify/{token}/TAZAPAY}. */
@Service
public class TazapayCallbackService {

    private static final Logger log = LoggerFactory.getLogger(TazapayCallbackService.class);

    private final HqNotifyEnvService hqNotifyEnvService;
    private final TazapaySaleRecordService tazapaySaleRecordService;
    private final TazapayPaymentService tazapayPaymentService;
    private final ObjectMapper objectMapper;

    public TazapayCallbackService(HqNotifyEnvService hqNotifyEnvService,
                                  TazapaySaleRecordService tazapaySaleRecordService,
                                  TazapayPaymentService tazapayPaymentService,
                                  ObjectMapper objectMapper) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.tazapaySaleRecordService = tazapaySaleRecordService;
        this.tazapayPaymentService = tazapayPaymentService;
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
        JsonNode root = parseBody(rawBody);
        if (root == null) {
            return Optional.empty();
        }
        if (!PgVendor.isTazapayVendorCode(target) && !looksLikeTazapayPayload(root)) {
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
        String payinId = text(root, "id", "payin_id", "payinId", "data.id");
        String referenceId = text(root, "reference_id", "referenceId", "data.reference_id");
        if (referenceId.isBlank() && root.has("data")) {
            referenceId = text(root.get("data"), "reference_id", "referenceId");
        }
        if (payinId.isBlank() && root.has("data")) {
            payinId = text(root.get("data"), "id", "payin_id");
        }
        boolean failed = TazapayApiClient.isFailedStatus(root.has("data") ? root.get("data") : root);
        boolean paid = TazapayApiClient.isPaidStatus(root.has("data") ? root.get("data") : root);

        Optional<PgTrnsctn> matched = tazapaySaleRecordService.resolveTxnForWebhook(payinId, referenceId, "");
        String orderNo = matched.map(PgTrnsctn::getOrderNo).orElse("");
        String compId = matched.map(PgTrnsctn::getMerchantId).orElse("");

        if (!orderNo.isBlank() && !compId.isBlank()) {
            if (failed) {
                tazapaySaleRecordService.applyFailedOutcome(
                        orderNo, compId, payinId, "TAZAPAY_WEBHOOK_FAIL", "Tazapay payment failed");
            } else if (paid) {
                tazapaySaleRecordService.applyPaidOutcome(orderNo, compId, payinId, referenceId);
            } else if (!payinId.isBlank()) {
                tazapayPaymentService.confirmPaidViaGetPayin(compId, orderNo);
            }
        } else {
            log.warn("TAZAPAY webhook Comp-Id unresolved payin={} ref={} ip={}", payinId, referenceId, clientIp);
        }
        /* 가맹 결제통보는 applyPaid/Failed 내부. NOTI 수신통보 매칭용 Comp-Id 헤더 반환. */
        return outcomeJson(true, null, payinId, orderNo, compId, HttpStatus.OK);
    }

    private NotifyReceiveOutcome outcomeJson(boolean ok, String error, String pgRef,
                                             String orderNo, String compId, HttpStatus status) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (compId != null && !compId.isBlank()) {
            headers.put("X-Icopay-Comp-Id", compId.trim());
        }
        if (orderNo != null && !orderNo.isBlank()) {
            headers.put("X-Icopay-Order-No", orderNo.trim());
        }
        String bodyJson;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", ok);
            if (error != null && !error.isBlank()) {
                body.put("error", error);
            }
            body.put("pgTxnId", pgRef != null ? pgRef : "");
            body.put("orderNo", orderNo != null ? orderNo : "");
            body.put("compId", compId != null ? compId : "");
            body.put("compIdResolved", compId != null && !compId.isBlank());
            bodyJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            bodyJson = ok ? "{\"ok\":true}" : "{\"ok\":false}";
        }
        return NotifyReceiveOutcome.json(bodyJson, status, headers);
    }

    private JsonNode parseBody(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean looksLikeTazapayPayload(JsonNode root) {
        if (root == null) {
            return false;
        }
        return !text(root, "reference_id", "referenceId", "client_token", "payin_id").isBlank()
                || (root.has("data") && root.get("data").isObject());
    }

    private static String text(JsonNode n, String... keys) {
        if (n == null) {
            return "";
        }
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            if (k.contains(".")) {
                String[] p = k.split("\\.", 2);
                JsonNode child = n.get(p[0]);
                if (child != null && child.has(p[1])) {
                    String s = child.get(p[1]).asText("").trim();
                    if (!s.isBlank()) {
                        return s;
                    }
                }
                continue;
            }
            JsonNode v = n.get(k);
            if (v != null && !v.isNull() && v.isValueNode()) {
                String s = v.asText("").trim();
                if (!s.isBlank()) {
                    return s;
                }
            }
        }
        return "";
    }
}