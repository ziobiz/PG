package com.pg.service;

import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.receipt.TransactionReceiptEmailService;
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

/**
 * Eximbay {@code status_url}(서버-서버 웹훅) 결과를 {@link PgTrnsctn} 에 반영합니다.
 *
 * <p>Eximbay 는 결과를 <b>쿼리스트링</b>(JSON 아님)으로 전송하며, {@code return_url}·{@code status_url} 의
 * 파라미터는 동일합니다. 성공은 {@code rescode=0000} 이고, 위·변조 방지를 위해 원본 쿼리스트링을
 * {@code /v1/payments/verify}({@code {"data": ...}}) 로 재검증합니다. 검증에 성공한 승인만 매출 반영합니다.
 *
 * <p>토큰 전용 ingress 로 수신되므로({@link com.pg.middleware.notify.PgNotifyIngressPaths}) 별도 노티대상
 * 등록 없이 벤더 스니핑으로 이 핸들러가 자기 페이로드만 처리합니다. status_url 은 중복 호출될 수 있어
 * {@code transaction_id}·거래상태로 멱등 처리합니다.
 *
 * <p><b>가맹 정보 보호:</b> Eximbay 로는 우리 가맹점 코드를 일절 보내지 않으므로, 가맹점·구독 여부는
 * Eximbay 응답값이 아니라 우리가 ready 시 적재한 <b>대기거래({@code order_id})</b> 로만 복원합니다.
 */
@Service
public class EximbayNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(EximbayNotifyToTrnsctnService.class);
    private static final String ORIGIN_SUBSCRIPTION = "SUBSCRIPTION";

    private final EximbayPaymentService eximbayPaymentService;
    private final EximbaySaleRecordService eximbaySaleRecordService;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final SettlementCalcService settlementCalcService;
    private final SplitPayPaymentHookService splitPayPaymentHookService;
    private final TransactionReceiptEmailService transactionReceiptEmailService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;

    public EximbayNotifyToTrnsctnService(EximbayPaymentService eximbayPaymentService,
                                         EximbaySaleRecordService eximbaySaleRecordService,
                                         NotifyIdempotencyLock notifyIdempotencyLock,
                                         SettlementCalcService settlementCalcService,
                                         SplitPayPaymentHookService splitPayPaymentHookService,
                                         TransactionReceiptEmailService transactionReceiptEmailService,
                                         MerchantOutboundNotifyService merchantOutboundNotifyService) {
        this.eximbayPaymentService = eximbayPaymentService;
        this.eximbaySaleRecordService = eximbaySaleRecordService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.settlementCalcService = settlementCalcService;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
    }

    @Override
    public int order() {
        // JPAY(-20) 보다 먼저 자기 페이로드(fgkey+order_id+rescode)만 엄격히 스니핑
        return -25;
    }

    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        try {
            return doTry(in, notifyChannel);
        } catch (Exception e) {
            log.warn("Eximbay 노티 적재 예외: {}", e.getMessage());
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
        Map<String, String> f = parseQueryLowerKeys(raw.trim());
        if (!looksLikeEximbayResult(f)) {
            return false;
        }
        String mid = first(f, "mid");
        String orderNo = first(f, "order_id");
        String transactionId = first(f, "transaction_id");
        String rescode = first(f, "rescode");
        String resmsg = first(f, "resmsg");
        if (orderNo.isBlank()) {
            return false;
        }

        // 가맹점·구독여부는 Eximbay 응답값이 아니라, 우리가 ready 시 적재한 대기거래(order_id)로만 복원한다.
        // (Eximbay 로는 가맹점 코드를 일절 보내지 않으므로 param1 등에 의존하지 않는다.)
        Optional<PgTrnsctn> pending = eximbaySaleRecordService.findAnyByOrder(orderNo);
        String compCode = resolveCompCode(in, pending);
        if (compCode.isBlank()) {
            log.warn("Eximbay 노티 가맹점 미해석 orderNo={} mid={}", orderNo, mid);
            return false;
        }
        String ch = notifyChannel == null || notifyChannel.isBlank()
                ? "CALLBACK" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        notifyIdempotencyLock.lock("EXIMBAY", "ORD:" + compCode + "|" + orderNo);

        boolean success0000 = "0000".equals(rescode);
        boolean paid;
        if (success0000) {
            Map<String, Object> vr = eximbayPaymentService.verify(mid, raw.trim());
            boolean verifyOk = Boolean.TRUE.equals(vr.get("success"));
            if (!verifyOk) {
                log.warn("Eximbay status_url rescode=0000 이나 verify 실패 → 승인 미반영 orderNo={} mid={}", orderNo, mid);
                return true; // 이 벤더 페이로드는 처리(claim)했으나 무결성 미검증분은 매출 미반영(재시도 대비)
            }
            paid = true;
        } else {
            paid = false;
        }

        String txnOrigin = resolveTxnOrigin(pending, f);
        String maskedCard = buildMaskedCard(f);
        Optional<PgTrnsctn> saved = eximbaySaleRecordService.applyOutcome(
                compCode, orderNo, paid, transactionId, resmsg, txnOrigin, maskedCard);
        if (saved.isEmpty()) {
            log.info("Eximbay 노티 대상 거래 없음(대기행 미존재) orderNo={} comp={} paid={}", orderNo, compCode, paid);
            return true;
        }
        PgTrnsctn t = saved.get();
        log.info("Eximbay 노티 반영 trnId={} comp={} orderNo={} rescode={} paid={} channel={}",
                t.getTrnId(), compCode, orderNo, rescode, paid, ch);

        hookSplitPay(t);
        if (paid) {
            try {
                if (t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
                    settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
                }
            } catch (Exception e) {
                log.warn("Eximbay 실시간 자동정산 트리거 실패 comp={}: {}", compCode, e.getMessage());
            }
        }
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, ch);
        } catch (Exception e) {
            log.warn("Eximbay 가맹 아웃바운드 노티 예약 실패 orderNo={}: {}", orderNo, e.getMessage());
        }
        return true;
    }

    /** fgkey + order_id + rescode 를 모두 가진 Eximbay 결과 쿼리스트링만 이 핸들러가 처리. */
    private static boolean looksLikeEximbayResult(Map<String, String> f) {
        if (f == null || f.isEmpty()) {
            return false;
        }
        if (!first(f, "memberid").isBlank()) {
            return false; // JPAY 노티
        }
        return !first(f, "fgkey").isBlank()
                && !first(f, "order_id").isBlank()
                && !first(f, "rescode").isBlank();
    }

    /**
     * 가맹점 복원 — Eximbay 로는 가맹점 코드를 보내지 않으므로, 우리가 ready 시 적재한
     * 대기거래(order_id 기준) → 가맹점 순으로만 복원한다. (토큰 전용 ingress 라 in.merchantId 는 대개 비어있다.)
     */
    private String resolveCompCode(PgNotifyInbound in, Optional<PgTrnsctn> pending) {
        if (pending.isPresent() && notBlank(pending.get().getMerchantId())) {
            return pending.get().getMerchantId().trim();
        }
        if (in != null && notBlank(in.getMerchantId())) {
            return in.getMerchantId().trim();
        }
        return "";
    }

    /** 구독 여부 — 우리 대기거래 출처(SUBSCRIPTION)를 우선하고, 없으면 Eximbay 의 token/recurring 식별자로 판정한다. */
    private static String resolveTxnOrigin(Optional<PgTrnsctn> pending, Map<String, String> f) {
        if (pending.isPresent() && ORIGIN_SUBSCRIPTION.equalsIgnoreCase(trim(pending.get().getOrigin()))) {
            return ORIGIN_SUBSCRIPTION;
        }
        return subscriptionMarker(f) ? ORIGIN_SUBSCRIPTION : null;
    }

    private static boolean subscriptionMarker(Map<String, String> f) {
        return !first(f, "recurring_id").isBlank() || !first(f, "token_id").isBlank();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String buildMaskedCard(Map<String, String> f) {
        String first4 = first(f, "card_number1");
        String last4 = first(f, "card_number4");
        if (first4.isBlank() && last4.isBlank()) {
            return null;
        }
        String a = first4.isBlank() ? "****" : first4;
        String b = last4.isBlank() ? "****" : last4;
        return a + "******" + b;
    }

    private void hookSplitPay(PgTrnsctn t) {
        if (t == null) {
            return;
        }
        if (t.getOrderNo() != null && !t.getOrderNo().isBlank()) {
            try {
                splitPayPaymentHookService.onTxnStatusChange(t.getOrderNo(), t.getStatus(), t.getTrnId());
            } catch (Exception e) {
                log.warn("Eximbay 분할결제 연동 실패 orderNo={}: {}", t.getOrderNo(), e.getMessage());
            }
        }
        try {
            transactionReceiptEmailService.scheduleAfterPaid(t);
        } catch (Exception e) {
            log.warn("Eximbay 거래 영수증 메일 연동 실패 trnId={}: {}", t.getTrnId(), e.getMessage());
        }
    }

    private static Map<String, String> parseQueryLowerKeys(String body) {
        Map<String, String> m = new LinkedHashMap<>();
        String b = body;
        if (b.startsWith("{")) {
            // 방어적: JSON 으로 오는 경우도 최소 파싱
            return m;
        }
        for (String pair : b.split("&")) {
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
        String v = m.get(key.toLowerCase(Locale.ROOT));
        return v != null ? v.trim() : "";
    }
}
