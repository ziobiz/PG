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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * KSNET 승인 후처리 허브 + NOTI/ingress(KSNET) 수신 핸들러.
 *
 * <ul>
 *   <li>{@link #afterApproved}: ICOPAY 서버 승인(recv_post.jsp) 성공 직후 분할결제·자동정산·가맹 아웃바운드 통보 연동.</li>
 *   <li>{@link #tryRecord}: ingress 로 KSNET 벤더 페이로드(commConId/trno + 승인 표식)가 오면 대기(08) 건만 멱등 확정.
 *       서버 승인이 정본이므로, 이미 10 인 건은 건드리지 않는다.</li>
 * </ul>
 * 가맹 식별정보는 KSNET 으로 보내지 않으므로 가맹·구독 여부는 우리가 적재한 대기거래(order/tid)로만 복원한다.
 */
@Service
public class KsnetNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(KsnetNotifyToTrnsctnService.class);

    private final KsnetSaleRecordService ksnetSaleRecordService;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final KsnetPostApproveService ksnetPostApproveService;
    private final com.pg.repository.PgTrnsctnRepository pgTrnsctnRepository;
    private final ObjectMapper objectMapper;

    public KsnetNotifyToTrnsctnService(KsnetSaleRecordService ksnetSaleRecordService,
                                       NotifyIdempotencyLock notifyIdempotencyLock,
                                       KsnetPostApproveService ksnetPostApproveService,
                                       com.pg.repository.PgTrnsctnRepository pgTrnsctnRepository,
                                       ObjectMapper objectMapper) {
        this.ksnetSaleRecordService = ksnetSaleRecordService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.ksnetPostApproveService = ksnetPostApproveService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public int order() {
        return -24;
    }

    /** 서버 승인 성공 후 공통 후처리 — {@link KsnetPostApproveService} 위임. */
    private void afterApproved(PgTrnsctn t, String channel) {
        ksnetPostApproveService.afterApproved(t, channel);
    }

    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        try {
            return doTry(in, notifyChannel);
        } catch (Exception e) {
            log.warn("KSNET 노티 적재 예외: {}", e.getMessage());
            return false;
        }
    }

    private boolean doTry(PgNotifyInbound in, String notifyChannel) {
        if (in == null || !"PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim())) {
            return false;
        }
        String raw = in.getRawBody();
        if (raw == null || raw.isBlank()) {
            return false;
        }
        Map<String, String> f = parse(raw.trim());
        if (!looksLikeKsnet(f)) {
            return false;
        }
        String tid = first(f, "trno", "tid", "transactionid");
        String orderNo = first(f, "ordno", "orderno", "ordernumber", "order_id", "order");
        Optional<PgTrnsctn> txn = Optional.empty();
        if (!orderNo.isBlank()) {
            txn = ksnetSaleRecordService.findAnyByOrder(orderNo);
        }
        if (txn.isEmpty() && !tid.isBlank()) {
            txn = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(tid)
                    .filter(x -> PgVendor.isKsnetFamily(x.getVan()));
        }
        if (txn.isEmpty()) {
            log.info("KSNET 노티 대상 거래 없음 order={} tid={}", orderNo, tid);
            return true;
        }
        PgTrnsctn t = txn.get();
        String comp = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        if (comp.isBlank()) {
            return true;
        }
        notifyIdempotencyLock.lock("KSNET", "ORD:" + comp + "|" + t.getOrderNo());
        String authyn = first(f, "authyn");
        boolean approved = "O".equalsIgnoreCase(authyn);
        String status = t.getStatus() != null ? t.getStatus().trim() : "";
        if (!approved || !"08".equals(status)) {
            /* 서버 승인(approve)이 정본 — 이미 확정/실패이거나 승인 표식 없으면 무시 */
            return true;
        }
        Optional<PgTrnsctn> saved = ksnetSaleRecordService.applyApproveOutcome(
                comp, t.getOrderNo(), true, tid, first(f, "authno", "approvalno"),
                first(f, "msg2", "msg1", "message"), null, null);
        saved.ifPresent(x -> afterApproved(x, notifyChannel != null ? notifyChannel : "KSNET_NOTI"));
        return true;
    }

    private static boolean looksLikeKsnet(Map<String, String> f) {
        if (f == null || f.isEmpty()) {
            return false;
        }
        if (!first(f, "memberid").isBlank() || !first(f, "fgkey").isBlank()) {
            return false; // JPAY·Eximbay
        }
        String vendor = first(f, "pgvendor", "vendor");
        boolean vendorKsnet = !vendor.isBlank() && PgVendor.isKsnetVendorCode(vendor);
        boolean ksFields = !first(f, "commconid").isBlank()
                || (!first(f, "storeid").isBlank() && !first(f, "authyn").isBlank());
        return vendorKsnet || ksFields;
    }

    private Map<String, String> parse(String body) {
        Map<String, String> m = new LinkedHashMap<>();
        if (body.startsWith("{")) {
            try {
                JsonNode n = objectMapper.readTree(body);
                n.fields().forEachRemaining(e -> {
                    if (e.getValue() != null && e.getValue().isValueNode()) {
                        m.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().asText("").trim());
                    }
                });
            } catch (Exception ignored) {
                /* ignore */
            }
            return m;
        }
        for (String pair : body.split("&")) {
            int i = pair.indexOf('=');
            if (i <= 0) {
                continue;
            }
            String k = decode(pair.substring(0, i).trim()).toLowerCase(Locale.ROOT);
            String v = decode(pair.substring(i + 1).trim());
            if (!k.isEmpty()) {
                m.put(k, v);
            }
        }
        return m;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static String first(Map<String, String> m, String... keys) {
        for (String k : keys) {
            String v = m.get(k.toLowerCase(Locale.ROOT));
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }
}
