package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * NOTI {@code /noti/omspay} → ICOPAY ingress 비동기 보완 적재.
 * 동기 ACK는 {@link OmspayCallbackService}가 담당합니다.
 */
@Service
public class OmspayNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(OmspayNotifyToTrnsctnService.class);

    private final OmspaySaleRecordService omspaySaleRecordService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final ObjectMapper objectMapper;

    public OmspayNotifyToTrnsctnService(OmspaySaleRecordService omspaySaleRecordService,
                                        MerchantOutboundNotifyService merchantOutboundNotifyService,
                                        NotifyIdempotencyLock notifyIdempotencyLock,
                                        ObjectMapper objectMapper) {
        this.omspaySaleRecordService = omspaySaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.objectMapper = objectMapper;
    }

    @Override
    public int order() {
        return -16;
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
        if (!PgVendor.isOmspayVendorCode(target) && !looksLike(root)) {
            return false;
        }
        String so = text(root, "so");
        String ref1 = text(root, "ref1");
        String note = text(root, "note");
        String orderHint = text(root, "orderNo");
        String orderNo = note;
        String comp = in.getMerchantId() != null ? in.getMerchantId().trim() : "";
        Optional<PgTrnsctn> matched = omspaySaleRecordService.resolveTxnForWebhook(so, note, orderHint, ref1);
        if (matched.isPresent()) {
            PgTrnsctn t = matched.get();
            if (orderNo.isBlank() && t.getOrderNo() != null) {
                orderNo = t.getOrderNo().trim();
            }
            if (comp.isBlank() && t.getMerchantId() != null) {
                comp = t.getMerchantId().trim();
            }
        }
        if (orderNo.isBlank()) {
            orderNo = orderHint;
        }
        if (orderNo.isBlank() || comp.isBlank()) {
            log.warn("OMSPAY async Comp-Id unresolved so={} note={} orderHint={}", so, note, orderHint);
            return true;
        }
        String chargeId = so.isBlank() ? ref1 : so;
        notifyIdempotencyLock.lock("OMSPAY", "ORD:" + comp + "|" + orderNo + "|" + chargeId);
        String payerName = text(root, "payer_name");
        Optional<PgTrnsctn> updated = omspaySaleRecordService.applyPaidOutcome(orderNo, comp, so, ref1, payerName);
        updated.ifPresent(t -> {
            try {
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, notifyChannel);
            } catch (Exception e) {
                log.warn("OMSPAY async merchant notify: {}", e.getMessage());
            }
        });
        return true;
    }

    private static boolean looksLike(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        String so = text(root, "so");
        return so.startsWith("SO-") || (root.has("paid_at") && root.has("amount"));
    }

    private static String text(JsonNode n, String field) {
        if (n == null || field == null || !n.has(field) || n.get(field).isNull()) {
            return "";
        }
        return n.get(field).asText("").trim();
    }
}
