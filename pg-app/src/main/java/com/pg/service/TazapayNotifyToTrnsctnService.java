package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.integration.pg.tazapay.TazapayApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** NOTI 비동기 보완 — Tazapay webhook 적재. */
@Service
public class TazapayNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(TazapayNotifyToTrnsctnService.class);

    private final TazapaySaleRecordService tazapaySaleRecordService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final ObjectMapper objectMapper;

    public TazapayNotifyToTrnsctnService(TazapaySaleRecordService tazapaySaleRecordService,
                                         MerchantOutboundNotifyService merchantOutboundNotifyService,
                                         NotifyIdempotencyLock notifyIdempotencyLock,
                                         ObjectMapper objectMapper) {
        this.tazapaySaleRecordService = tazapaySaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.objectMapper = objectMapper;
    }

    @Override
    public int order() {
        return -14;
    }

    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        if (in == null || in.getRawBody() == null || in.getRawBody().isBlank()) {
            return false;
        }
        String target = in.getNotifyTargetCode() != null ? in.getNotifyTargetCode().trim() : "";
        JsonNode root;
        try {
            root = objectMapper.readTree(in.getRawBody());
        } catch (Exception e) {
            return false;
        }
        if (!PgVendor.isTazapayVendorCode(target) && !looksLike(root)) {
            return false;
        }
        JsonNode data = root.has("data") ? root.get("data") : root;
        String payinId = text(data, "id", "payin_id");
        String referenceId = text(data, "reference_id", "referenceId");
        String comp = in.getMerchantId() != null ? in.getMerchantId().trim() : "";
        Optional<PgTrnsctn> matched = tazapaySaleRecordService.resolveTxnForWebhook(payinId, referenceId, "");
        if (matched.isPresent()) {
            if (comp.isBlank() && matched.get().getMerchantId() != null) {
                comp = matched.get().getMerchantId().trim();
            }
        }
        String orderNo = matched.map(PgTrnsctn::getOrderNo).orElse("");
        if (orderNo.isBlank() || comp.isBlank()) {
            log.warn("TAZAPAY async Comp-Id unresolved payin={} ref={}", payinId, referenceId);
            return true;
        }
        notifyIdempotencyLock.lock("TAZAPAY", "ORD:" + comp + "|" + orderNo + "|" + payinId);
        boolean paid = TazapayApiClient.isPaidStatus(data);
        boolean failed = TazapayApiClient.isFailedStatus(data);
        Optional<PgTrnsctn> updated = Optional.empty();
        if (failed) {
            updated = tazapaySaleRecordService.applyFailedOutcome(orderNo, comp, payinId,
                    "TAZAPAY_ASYNC_FAIL", "Tazapay async fail");
        } else if (paid) {
            updated = tazapaySaleRecordService.applyPaidOutcome(orderNo, comp, payinId, referenceId);
        }
        updated.ifPresent(t -> {
            try {
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, notifyChannel);
            } catch (Exception e) {
                log.warn("TAZAPAY async merchant notify: {}", e.getMessage());
            }
        });
        return true;
    }

    private static boolean looksLike(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        JsonNode data = root.has("data") ? root.get("data") : root;
        return !text(data, "reference_id", "id", "client_token").isBlank();
    }

    private static String text(JsonNode n, String... keys) {
        if (n == null) {
            return "";
        }
        for (String k : keys) {
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