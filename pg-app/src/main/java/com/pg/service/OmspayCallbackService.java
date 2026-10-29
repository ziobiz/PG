package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.dto.NotifyReceiveOutcome;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.omspay.OmspayCredentials;
import com.pg.integration.pg.omspay.OmspaySignatureUtil;
import com.pg.repository.PgAgencyRepository;
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
 * OMSPAY order-paid 웹훅 — NOTI → {@code …/pg-notify/{token}/OMSPAY}.
 * {@code x-waylar-signature} 검증 후 승인 반영, {@code X-Icopay-Comp-Id} 반환.
 */
@Service
public class OmspayCallbackService {

    private static final Logger log = LoggerFactory.getLogger(OmspayCallbackService.class);

    private final HqNotifyEnvService hqNotifyEnvService;
    private final OmspaySaleRecordService omspaySaleRecordService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final PgAgencyRepository pgAgencyRepository;
    private final ObjectMapper objectMapper;

    public OmspayCallbackService(HqNotifyEnvService hqNotifyEnvService,
                                 OmspaySaleRecordService omspaySaleRecordService,
                                 MerchantOutboundNotifyService merchantOutboundNotifyService,
                                 PgAgencyRepository pgAgencyRepository,
                                 ObjectMapper objectMapper) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.omspaySaleRecordService = omspaySaleRecordService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.pgAgencyRepository = pgAgencyRepository;
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
        boolean looksPath = PgVendor.isOmspayVendorCode(target);
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            return Optional.empty();
        }
        if (!looksPath && !looksLikeOmspayPayload(root)) {
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
        String sig = header(request, "x-waylar-signature", "X-Waylar-Signature");
        if (!sig.isBlank()) {
            boolean verified = false;
            for (PgAgency a : pgAgencyRepository.findAll()) {
                if (a.getPgCd() == null || !PgVendor.isOmspayFamily(a.getPgCd())) {
                    continue;
                }
                OmspayCredentials c = OmspayCredentials.fromAgency(a);
                if (c.webhookSecret().isBlank()) {
                    continue;
                }
                if (OmspaySignatureUtil.verify(sig, rawBody, c.webhookSecret())) {
                    verified = true;
                    break;
                }
            }
            if (!verified) {
                log.warn("OMSPAY webhook signature mismatch ip={}", clientIp);
                return NotifyReceiveOutcome.json("{\"ok\":false,\"error\":\"bad_signature\"}", HttpStatus.UNAUTHORIZED);
            }
        }

        String so = text(root, "so");
        String ref1 = text(root, "ref1");
        String note = text(root, "note");
        String orderHint = text(root, "orderNo");
        /* note 없을 때 SO-/pay_no/approval_no/orderNo 로 대기건 역추적 → Comp-Id */
        Optional<PgTrnsctn> matched = omspaySaleRecordService.resolveTxnForWebhook(so, note, orderHint, ref1);
        String orderNo = note;
        String compId = "";
        if (matched.isPresent()) {
            PgTrnsctn t = matched.get();
            if (orderNo.isBlank() && t.getOrderNo() != null) {
                orderNo = t.getOrderNo().trim();
            }
            compId = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        }
        if (orderNo.isBlank()) {
            orderNo = orderHint;
        }

        String payerName = text(root, "payer_name");
        Optional<PgTrnsctn> updated = Optional.empty();
        if (!orderNo.isBlank() && !compId.isBlank()) {
            updated = omspaySaleRecordService.applyPaidOutcome(orderNo, compId, so, ref1, payerName);
        } else {
            log.warn("OMSPAY webhook Comp-Id unresolved so={} note={} orderHint={} ref1={} ip={}",
                    so, note, orderHint, ref1, clientIp);
        }

        updated.ifPresent(t -> {
            try {
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "OMSPAY_WEBHOOK");
            } catch (Exception e) {
                log.warn("OMSPAY merchant notify: {}", e.getMessage());
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
            body.put("so", so);
            body.put("orderNo", orderNo);
            body.put("compIdResolved", !compId.isBlank());
            bodyJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            bodyJson = "{\"ok\":true}";
        }
        return NotifyReceiveOutcome.json(bodyJson, HttpStatus.OK, headers);
    }

    private static boolean looksLikeOmspayPayload(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        String so = text(root, "so");
        return so.startsWith("SO-") || (root.has("paid_at") && root.has("amount") && root.has("ref1"));
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

    private static String text(JsonNode n, String field) {
        if (n == null || field == null || !n.has(field) || n.get(field).isNull()) {
            return "";
        }
        return n.get(field).asText("").trim();
    }
}
