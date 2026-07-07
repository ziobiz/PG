package com.pg.service;

import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.integration.pg.PgVendor;
import com.pg.splitpay.SplitPayPaymentHookService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElementPay {@code payment.rejected} 등 비동기 이벤트 — ingress 경유 수신 시 {@link PgTrnsctn} 반영.
 * {@code check}/{@code pay} 는 {@link ElementPayCallbackService} 가 동기 처리합니다.
 */
@Service
public class ElementPayNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(ElementPayNotifyToTrnsctnService.class);
    private static final Pattern ICOPAY_COMP_ID = Pattern.compile(
            "icopayCompId=([A-Za-z0-9_.-]+)", Pattern.CASE_INSENSITIVE);

    private final ElementPaySaleRecordService elementPaySaleRecordService;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final SettlementCalcService settlementCalcService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final SplitPayPaymentHookService splitPayPaymentHookService;

    public ElementPayNotifyToTrnsctnService(ElementPaySaleRecordService elementPaySaleRecordService,
                                            NotifyIdempotencyLock notifyIdempotencyLock,
                                            SettlementCalcService settlementCalcService,
                                            MerchantOutboundNotifyService merchantOutboundNotifyService,
                                            SplitPayPaymentHookService splitPayPaymentHookService) {
        this.elementPaySaleRecordService = elementPaySaleRecordService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.settlementCalcService = settlementCalcService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
    }

    @Override
    public int order() {
        return -22;
    }

    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        try {
            return doTry(in, notifyChannel);
        } catch (Exception e) {
            log.warn("ElementPay 노티 적재 예외: {}", e.getMessage());
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
        Map<String, String> f = parseForm(raw.trim());
        if (!looksLikeElementPayAsync(f)) {
            return false;
        }
        String method = first(f, "method");
        if ("check".equalsIgnoreCase(method) || "pay".equalsIgnoreCase(method)) {
            return false;
        }
        String orderNo = first(f, "order");
        if (orderNo.isBlank()) {
            return false;
        }
        String compCode = resolveCompCode(in, f, orderNo);
        if (compCode.isBlank()) {
            return false;
        }
        notifyIdempotencyLock.lock("ELEMENTPAY", "ORD:" + compCode + "|" + orderNo);
        boolean paid = false;
        Optional<PgTrnsctn> saved = elementPaySaleRecordService.applyOutcome(
                compCode, orderNo, paid, first(f, "id"), method);
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
            log.warn("ElementPay async outbound 실패: {}", e.getMessage());
        }
        return true;
    }

    private static boolean looksLikeElementPayAsync(Map<String, String> f) {
        if (f == null || f.isEmpty()) {
            return false;
        }
        String method = first(f, "method");
        return method.startsWith("payment.") && !first(f, "hash").isBlank() && !first(f, "order").isBlank();
    }

    private String resolveCompCode(PgNotifyInbound in, Map<String, String> f, String orderNo) {
        String fromData = extractCompId(f);
        if (!fromData.isBlank()) {
            return fromData;
        }
        Optional<PgTrnsctn> pending = elementPaySaleRecordService.findAnyByOrder(orderNo);
        if (pending.isPresent() && pending.get().getMerchantId() != null) {
            return pending.get().getMerchantId().trim();
        }
        if (in != null && in.getMerchantId() != null && !in.getMerchantId().isBlank()) {
            return in.getMerchantId().trim();
        }
        return "";
    }

    private static String extractCompId(Map<String, String> f) {
        String data = first(f, "data");
        if (!data.isBlank()) {
            Matcher m = ICOPAY_COMP_ID.matcher(data);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "";
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) {
                continue;
            }
            String k = safeDecode(pair.substring(0, i).trim()).toLowerCase(Locale.ROOT);
            String v = i + 1 <= pair.length() ? safeDecode(pair.substring(i + 1).trim()) : "";
            if (!k.isEmpty()) {
                m.put(k, v);
            }
        }
        return m;
    }

    private static String safeDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static String first(Map<String, String> m, String key) {
        if (m == null || key == null) {
            return "";
        }
        String v = m.get(key.toLowerCase(Locale.ROOT));
        return v != null ? v.trim() : "";
    }
}
