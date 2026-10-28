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

import java.util.Locale;
import java.util.Optional;

/**
 * NOTI {@code /noti/qxipay} → ICOPAY ingress 비동기 보완 적재.
 * 동기 ACK는 {@link QxipayCallbackService}가 담당합니다.
 */
@Service
public class QxipayNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(QxipayNotifyToTrnsctnService.class);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final QxipaySaleRecordService qxipaySaleRecordService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final ObjectMapper objectMapper;

    public QxipayNotifyToTrnsctnService(PgTrnsctnRepository pgTrnsctnRepository,
                                        NotifyIdempotencyLock notifyIdempotencyLock,
                                        QxipaySaleRecordService qxipaySaleRecordService,
                                        MerchantOutboundNotifyService merchantOutboundNotifyService,
                                        ObjectMapper objectMapper) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.qxipaySaleRecordService = qxipaySaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public int order() {
        return -17;
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
        if (!PgVendor.isQxipayVendorCode(target) && !looksLike(root)) {
            return false;
        }
        JsonNode charge = root.path("data").path("object");
        if (charge.isMissingNode() || charge.isNull()) {
            charge = root.path("data");
        }
        String orderNo = text(charge, "reference");
        String chargeId = text(charge, "id");
        String status = text(charge, "status");
        String type = text(root, "type").toLowerCase(Locale.ROOT);
        if (type.contains("succeeded")) {
            status = "succeeded";
        } else if (type.contains("failed") || type.contains("expired")) {
            status = status.isBlank() ? "failed" : status;
        }
        String comp = in.getMerchantId() != null ? in.getMerchantId().trim() : "";
        if (comp.isBlank() && !orderNo.isBlank()) {
            Optional<PgTrnsctn> t = pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(orderNo);
            if (t.isPresent()) {
                comp = t.get().getMerchantId() != null ? t.get().getMerchantId() : "";
            }
        }
        if (orderNo.isBlank() || comp.isBlank()) {
            return true;
        }
        notifyIdempotencyLock.lock("QXIPAY", "ORD:" + comp + "|" + orderNo + "|" + chargeId);
        Optional<PgTrnsctn> updated = qxipaySaleRecordService.applyChargeOutcome(orderNo, comp, chargeId, status, chargeId);
        updated.ifPresent(t -> {
            try {
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, notifyChannel);
            } catch (Exception e) {
                log.warn("QXIPAY async merchant notify: {}", e.getMessage());
            }
        });
        return true;
    }

    private static boolean looksLike(JsonNode root) {
        String type = text(root, "type");
        return type.startsWith("charge.")
                || ("event".equalsIgnoreCase(text(root, "object"))
                && "charge".equals(root.path("data").path("object").path("object").asText("")));
    }

    private static String text(JsonNode n, String field) {
        if (n == null || field == null) {
            return "";
        }
        JsonNode v = n.get(field);
        return v != null && !v.isNull() && v.isValueNode() ? v.asText("").trim() : "";
    }
}
