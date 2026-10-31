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
 * 서명 검증 후 승인 반영. 가맹 매칭용 {@code X-Icopay-Comp-Id} 는 서명 실패 시에도
 * 주문 역추적이 되면 헤더·본문에 내려 NOTI 노티전송 이력에 가맹명이 찍히게 한다.
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
        String so = text(root, "so");
        String ref1 = text(root, "ref1");
        String note = text(root, "note");
        String orderHint = text(root, "orderNo");
        /* note 없을 때 SO-/pay_no/approval_no/orderNo 로 대기건 역추적 → Comp-Id */
        ResolvedIds ids = resolveIds(so, note, orderHint, ref1);

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
                log.warn("OMSPAY webhook signature mismatch ip={} so={} compId={}",
                        clientIp, so, ids.compId.isBlank() ? "-" : ids.compId);
                /* 승인 반영은 거부하되 Comp-Id 는 NOTI 가맹명 표시용으로 반환 */
                return outcomeJson(false, "bad_signature", so, ids, HttpStatus.UNAUTHORIZED);
            }
        }

        String payerName = text(root, "payer_name");
        Optional<PgTrnsctn> updated = Optional.empty();
        if (!ids.orderNo.isBlank() && !ids.compId.isBlank()) {
            updated = omspaySaleRecordService.applyPaidOutcome(ids.orderNo, ids.compId, so, ref1, payerName);
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

        return outcomeJson(true, null, so, ids, HttpStatus.OK);
    }

    private ResolvedIds resolveIds(String so, String note, String orderHint, String ref1) {
        Optional<PgTrnsctn> matched = omspaySaleRecordService.resolveTxnForWebhook(so, note, orderHint, ref1);
        String orderNo = note != null ? note.trim() : "";
        String compId = "";
        if (matched.isPresent()) {
            PgTrnsctn t = matched.get();
            /* 매칭된 대기건의 가맹 orderNo 우선 — OMS note/SO 가 orderNo 자리에 와도 덮지 않음 */
            if (t.getOrderNo() != null && !t.getOrderNo().isBlank()) {
                orderNo = t.getOrderNo().trim();
            }
            compId = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        }
        if (orderNo.isBlank() && orderHint != null && !looksLikeOmspaySo(orderHint)) {
            orderNo = orderHint.trim();
        }
        if (looksLikeOmspaySo(orderNo)) {
            orderNo = "";
        }
        return new ResolvedIds(compId, orderNo);
    }

    private static boolean looksLikeOmspaySo(String v) {
        if (v == null || v.isBlank()) {
            return false;
        }
        String s = v.trim();
        return s.length() >= 3 && s.regionMatches(true, 0, "SO-", 0, 3);
    }

    private NotifyReceiveOutcome outcomeJson(boolean ok, String error, String so, ResolvedIds ids, HttpStatus status) {
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
            body.put("so", so != null ? so : "");
            body.put("orderNo", ids != null ? ids.orderNo : "");
            body.put("compId", ids != null ? ids.compId : "");
            body.put("CompId", ids != null ? ids.compId : "");
            body.put("compIdResolved", ids != null && !ids.compId.isBlank());
            bodyJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            bodyJson = ok ? "{\"ok\":true}" : "{\"ok\":false}";
        }
        return NotifyReceiveOutcome.json(bodyJson, status, headers);
    }

    private static final class ResolvedIds {
        final String compId;
        final String orderNo;

        ResolvedIds(String compId, String orderNo) {
            this.compId = compId != null ? compId : "";
            this.orderNo = orderNo != null ? orderNo : "";
        }
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
