package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PgTrnsctnOrderLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * NOTI {@code /noti/ox} → ICOPAY {@code …/OX} 수신.
 * 가맹 통보와 같은 필드(returncode, orderid)로 성공/실패를 반영합니다.
 * OxPay 원문 필드 매핑은 상세 스펙 이후입니다.
 */
@Service
public class OxNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(OxNotifyToTrnsctnService.class);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final ObjectMapper objectMapper;

    public OxNotifyToTrnsctnService(PgTrnsctnRepository pgTrnsctnRepository,
                                    NotifyIdempotencyLock notifyIdempotencyLock,
                                    MerchantOutboundNotifyService merchantOutboundNotifyService,
                                    ObjectMapper objectMapper) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public int order() {
        return -18;
    }

    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        if (in == null) {
            return false;
        }
        Map<String, String> f = parse(in.getRawBody());
        if (!looksLikeOx(in, f)) {
            return false;
        }
        String orderNo = first(f, "orderid", "orderno", "order", "orderNo");
        String comp = first(f, "compid", "merchantid");
        if (comp.isBlank() && in.getMerchantId() != null) {
            comp = in.getMerchantId().trim();
        }
        if (orderNo.isBlank() || comp.isBlank()) {
            return true;
        }
        notifyIdempotencyLock.lock("OX", "ORD:" + comp + "|" + orderNo);
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, comp, orderNo);
        if (txn.isEmpty() || !PgVendor.isOxFamily(txn.get().getVan())) {
            return true;
        }
        PgTrnsctn t = txn.get();
        String code = first(f, "returncode", "status");
        boolean paid = "00".equals(code) || "0".equals(code) || "succeeded".equalsIgnoreCase(code)
                || "success".equalsIgnoreCase(code) || "paid".equalsIgnoreCase(code);
        t.setStatus(paid ? "10" : "99");
        t.setVan(PgVendor.OX);
        pgTrnsctnRepository.save(t);
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, notifyChannel);
        } catch (Exception e) {
            log.warn("ox 가맹 통보 예약 실패: {}", e.getMessage());
        }
        return true;
    }

    private boolean looksLikeOx(PgNotifyInbound in, Map<String, String> f) {
        String target = in.getNotifyTargetCode() != null ? in.getNotifyTargetCode().trim() : "";
        if (PgVendor.isOxVendorCode(target)) {
            return true;
        }
        String pg = first(f, "pgkind", "van");
        return "ox".equalsIgnoreCase(pg) || "oxpay".equalsIgnoreCase(pg);
    }

    private Map<String, String> parse(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return m;
        }
        String body = raw.trim();
        if (body.startsWith("{")) {
            try {
                JsonNode n = objectMapper.readTree(body);
                n.fields().forEachRemaining(e -> {
                    if (e.getValue() != null && !e.getValue().isContainerNode()) {
                        m.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().asText(""));
                    }
                });
                return m;
            } catch (Exception ignored) {
                return m;
            }
        }
        for (String pair : body.split("&")) {
            int i = pair.indexOf('=');
            if (i <= 0) {
                continue;
            }
            String k = decode(pair.substring(0, i)).toLowerCase(Locale.ROOT);
            String v = decode(pair.substring(i + 1));
            if (!k.isEmpty()) {
                m.put(k, v);
            }
        }
        return m;
    }

    private static String first(Map<String, String> f, String... keys) {
        if (f == null) {
            return "";
        }
        for (String k : keys) {
            String v = f.get(k.toLowerCase(Locale.ROOT));
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
