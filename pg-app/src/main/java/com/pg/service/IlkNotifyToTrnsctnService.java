package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.splitpay.SplitPayPaymentHookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * ILK Back Noti 비동기 적재 경로 — {@link IlkCallbackService} 가 동기 ACK 하지 못한 경우 보완.
 */
@Service
public class IlkNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(IlkNotifyToTrnsctnService.class);

    private final IlkSaleRecordService ilkSaleRecordService;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final SplitPayPaymentHookService splitPayPaymentHookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IlkNotifyToTrnsctnService(IlkSaleRecordService ilkSaleRecordService,
                                     NotifyIdempotencyLock notifyIdempotencyLock,
                                     MerchantOutboundNotifyService merchantOutboundNotifyService,
                                     SplitPayPaymentHookService splitPayPaymentHookService) {
        this.ilkSaleRecordService = ilkSaleRecordService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
    }

    @Override
    public int order() {
        return -21;
    }

    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        try {
            return doTry(in, notifyChannel);
        } catch (Exception e) {
            log.warn("ILK 노티 적재 예외: {}", e.getMessage());
            return false;
        }
    }

    private boolean doTry(PgNotifyInbound in, String notifyChannel) {
        if (in == null || !"PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim())) {
            return false;
        }
        String raw = in.getRawBody();
        if (raw == null || raw.isBlank() || !raw.trim().startsWith("{")) {
            return false;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            return false;
        }
        String target = in.getNotifyTargetCode() != null ? in.getNotifyTargetCode().trim().toUpperCase(Locale.ROOT) : "";
        if (!PgVendor.isIlkVendorCode(target)
                && !root.has("merchantInformation.merchantId")
                && !root.has("clientReferenceInformation.code")) {
            return false;
        }
        String orderNo = text(root, "clientReferenceInformation.code");
        if (orderNo.isBlank()) {
            return false;
        }
        Optional<PgTrnsctn> existing = ilkSaleRecordService.findAnyByOrder(orderNo);
        String compCode = existing.map(PgTrnsctn::getMerchantId).orElse("");
        if (compCode.isBlank() && in.getMerchantId() != null) {
            compCode = in.getMerchantId().trim();
        }
        if (compCode.isBlank()) {
            return false;
        }
        notifyIdempotencyLock.lock("ILK", "ORD:" + compCode + "|" + orderNo);
        String status = text(root, "status");
        String id = text(root, "id");
        String transType = text(root, "orderInformation.transType");
        boolean paid = "SUCCESS".equalsIgnoreCase(status)
                && (transType.isBlank() || "PAY".equalsIgnoreCase(transType));
        if ("CANCEL".equalsIgnoreCase(transType) || "REFUND".equalsIgnoreCase(transType)) {
            ilkSaleRecordService.applyCancel(compCode, orderNo, id, status);
            return true;
        }
        Optional<PgTrnsctn> saved = ilkSaleRecordService.applyOutcome(compCode, orderNo, paid, id, status);
        if (saved.isEmpty()) {
            return true;
        }
        PgTrnsctn t = saved.get();
        try {
            splitPayPaymentHookService.onTxnStatusChange(t.getOrderNo(), t.getStatus(), t.getTrnId());
        } catch (Exception ignored) {
        }
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, notifyChannel);
        } catch (Exception e) {
            log.warn("ILK async outbound 실패: {}", e.getMessage());
        }
        return true;
    }

    private static String text(JsonNode root, String key) {
        if (root == null || key == null) {
            return "";
        }
        JsonNode n = root.get(key);
        if (n != null && !n.isNull() && !n.asText("").isBlank()) {
            return n.asText("").trim();
        }
        if (key.contains(".")) {
            JsonNode cur = root;
            for (String part : key.split("\\.")) {
                if (cur == null) {
                    return "";
                }
                cur = cur.get(part);
            }
            if (cur != null && !cur.isNull()) {
                return cur.asText("").trim();
            }
        }
        return "";
    }
}
