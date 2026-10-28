package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.repository.PgTrnsctnRepository;
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
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final ObjectMapper objectMapper;

    public OmspayNotifyToTrnsctnService(OmspaySaleRecordService omspaySaleRecordService,
                                        MerchantOutboundNotifyService merchantOutboundNotifyService,
                                        NotifyIdempotencyLock notifyIdempotencyLock,
                                        PgTrnsctnRepository pgTrnsctnRepository,
                                        ObjectMapper objectMapper) {
        this.omspaySaleRecordService = omspaySaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
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
        String orderNo = text(root, "note");
        String comp = in.getMerchantId() != null ? in.getMerchantId().trim() : "";
        if (orderNo.isBlank() && !so.isBlank()) {
            Optional<PgTrnsctn> bySo = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(so);
            if (bySo.isPresent() && PgVendor.isOmspayFamily(bySo.get().getVan())) {
                orderNo = bySo.get().getOrderNo() != null ? bySo.get().getOrderNo() : "";
                if (comp.isBlank()) {
                    comp = bySo.get().getMerchantId() != null ? bySo.get().getMerchantId() : "";
                }
            }
        }
        if (comp.isBlank() && !orderNo.isBlank()) {
            Optional<PgTrnsctn> t = pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(orderNo);
            if (t.isPresent() && PgVendor.isOmspayFamily(t.get().getVan())) {
                comp = t.get().getMerchantId() != null ? t.get().getMerchantId() : "";
            }
        }
        if (orderNo.isBlank() || comp.isBlank()) {
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
