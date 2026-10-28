package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.dto.NotifyReceiveOutcome;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.qxipay.QxipayCredentials;
import com.pg.integration.pg.qxipay.QxipaySignatureUtil;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
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

/**
 * QXIPAY 웹훅 동기 ACK — NOTI → {@code …/pg-notify/{token}/QXIPAY}.
 * {@code GW-Signature} 검증 후 charge 결과 반영, {@code X-Icopay-Comp-Id} 반환.
 */
@Service
public class QxipayCallbackService {

    private static final Logger log = LoggerFactory.getLogger(QxipayCallbackService.class);

    private final HqNotifyEnvService hqNotifyEnvService;
    private final QxipaySaleRecordService qxipaySaleRecordService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final PgAgencyRepository pgAgencyRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final ObjectMapper objectMapper;

    public QxipayCallbackService(HqNotifyEnvService hqNotifyEnvService,
                                 QxipaySaleRecordService qxipaySaleRecordService,
                                 MerchantOutboundNotifyService merchantOutboundNotifyService,
                                 PgAgencyRepository pgAgencyRepository,
                                 PgTrnsctnRepository pgTrnsctnRepository,
                                 ObjectMapper objectMapper) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.qxipaySaleRecordService = qxipaySaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.pgAgencyRepository = pgAgencyRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
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
        boolean looksPath = PgVendor.isQxipayVendorCode(target);
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            return Optional.empty();
        }
        if (!looksPath && !looksLikeQxipayEvent(root)) {
            return Optional.empty();
        }
        String expected = hqNotifyEnvService.getOrCreate().getIngressToken();
        if (expected == null || pathToken == null || !expected.equals(pathToken.trim())) {
            return Optional.of(NotifyReceiveOutcome.json("{\"ok\":false,\"error\":\"unauthorized\"}", HttpStatus.UNAUTHORIZED));
        }
        return Optional.of(handleEvent(root, rawBody, clientIp, request));
    }

    @Transactional
    protected NotifyReceiveOutcome handleEvent(JsonNode root, String rawBody, String clientIp,
                                               HttpServletRequest request) {
        String sig = header(request, "GW-Signature", "gw-signature");
        if (!sig.isBlank()) {
            boolean verified = false;
            for (PgAgency a : pgAgencyRepository.findAll()) {
                if (a.getPgCd() == null || !PgVendor.isQxipayFamily(a.getPgCd())) {
                    continue;
                }
                QxipayCredentials c = QxipayCredentials.fromAgency(a);
                if (c.webhookSecret().isBlank()) {
                    continue;
                }
                if (QxipaySignatureUtil.verify(sig, rawBody, c.webhookSecret(), 300)) {
                    verified = true;
                    break;
                }
            }
            if (!verified) {
                log.warn("QXIPAY webhook signature mismatch ip={}", clientIp);
                return NotifyReceiveOutcome.json("{\"ok\":false,\"error\":\"bad_signature\"}", HttpStatus.UNAUTHORIZED);
            }
        }

        String eventType = text(root, "type");
        if (eventType.isBlank()) {
            eventType = header(request, "GW-Event-Type", "gw-event-type");
        }
        JsonNode charge = root.path("data").path("object");
        if (charge.isMissingNode() || charge.isNull()) {
            charge = root.path("data");
        }
        String orderNo = text(charge, "reference");
        String chargeId = text(charge, "id");
        String status = text(charge, "status");
        String et = eventType.toLowerCase(Locale.ROOT);
        if (et.contains("succeeded")) {
            status = "succeeded";
        } else if (et.contains("failed") || et.contains("expired")) {
            if (status.isBlank()) {
                status = "failed";
            }
        }

        String compId = "";
        if (!orderNo.isBlank()) {
            Optional<PgTrnsctn> byOrder = pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(orderNo);
            if (byOrder.isPresent() && PgVendor.isQxipayFamily(byOrder.get().getVan())) {
                compId = byOrder.get().getMerchantId() != null ? byOrder.get().getMerchantId() : "";
            }
        }

        Optional<PgTrnsctn> updated = Optional.empty();
        if (!orderNo.isBlank() && !compId.isBlank()) {
            updated = qxipaySaleRecordService.applyChargeOutcome(orderNo, compId, chargeId, status, chargeId);
        }

        updated.ifPresent(t -> {
            try {
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "QXIPAY_WEBHOOK");
            } catch (Exception e) {
                log.warn("QXIPAY merchant notify: {}", e.getMessage());
            }
        });

        Map<String, String> headers = new LinkedHashMap<>();
        if (compId != null && !compId.isBlank()) {
            headers.put("X-Icopay-Comp-Id", compId.trim());
        }
        if (!orderNo.isBlank()) {
            headers.put("X-Icopay-Order-No", orderNo.trim());
        }
        String bodyJson;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("event", eventType);
            body.put("chargeId", chargeId);
            body.put("orderNo", orderNo);
            bodyJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            bodyJson = "{\"ok\":true}";
        }
        return NotifyReceiveOutcome.json(bodyJson, HttpStatus.OK, headers);
    }

    private static boolean looksLikeQxipayEvent(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        String type = text(root, "type");
        if (type.startsWith("charge.")) {
            return true;
        }
        return "event".equalsIgnoreCase(text(root, "object"))
                && root.has("data")
                && "charge".equals(root.path("data").path("object").path("object").asText(""));
    }

    private static String header(HttpServletRequest request, String... names) {
        if (request == null || names == null) {
            return "";
        }
        for (String n : names) {
            String v = request.getHeader(n);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String text(JsonNode n, String path) {
        if (n == null || path == null) {
            return "";
        }
        JsonNode cur = n;
        for (String p : path.split("\\.")) {
            if (cur == null || cur.isMissingNode() || cur.isNull()) {
                return "";
            }
            cur = cur.get(p);
        }
        return cur != null && !cur.isNull() && cur.isValueNode() ? cur.asText("").trim() : "";
    }
}
