package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.splitpay.SplitPayPaymentHookService;
import com.pg.util.JpayBuyerContactApplier;
import com.pg.util.JpayDisputeNotifyStatusResolver;
import com.pg.util.JpayNotifyStatusResolver;
import com.pg.util.JpaySignatureUtil;
import com.pg.util.JpayTransactionIdApplier;
import com.pg.util.NotifyToTxnStatusMerge;
import com.pg.util.TxnOutcomeReasonApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JPAY 비동기 노티({@code memberid},{@code orderid},{@code transaction_id},{@code returncode},{@code sign} 등)를
 * {@link PgTrnsctn}에 반영합니다. 서명 검증에 성공한 건만 갱신합니다.
 */
@Service
public class JpayNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(JpayNotifyToTrnsctnService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ORIGIN_URL = "URL";
    private static final String ST_PAID = JpayNotifyStatusResolver.ST_PAID;
    private static final String ST_FAIL = JpayNotifyStatusResolver.ST_FAIL;

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final SettlementCalcService settlementCalcService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final JpaySubscriptionNotifyService jpaySubscriptionNotifyService;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final SettlementArrearsService settlementArrearsService;
    private final SplitPayPaymentHookService splitPayPaymentHookService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator;
    private final PayCardFailCooldownService payCardFailCooldownService;
    private final OrgUnitRepository orgUnitRepository;

    public JpayNotifyToTrnsctnService(PgTrnsctnRepository pgTrnsctnRepository,
                                      PgAgencyRepository pgAgencyRepository,
                                      SettlementCalcService settlementCalcService,
                                      HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                      JpaySubscriptionNotifyService jpaySubscriptionNotifyService,
                                      NotifyIdempotencyLock notifyIdempotencyLock,
                                      SettlementArrearsService settlementArrearsService,
                                      SplitPayPaymentHookService splitPayPaymentHookService,
                                      MerchantOutboundNotifyService merchantOutboundNotifyService,
                                      OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator,
                                      PayCardFailCooldownService payCardFailCooldownService,
                                      OrgUnitRepository orgUnitRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.settlementCalcService = settlementCalcService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.jpaySubscriptionNotifyService = jpaySubscriptionNotifyService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.settlementArrearsService = settlementArrearsService;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.outcomeReasonWarmCoordinator = outcomeReasonWarmCoordinator;
        this.payCardFailCooldownService = payCardFailCooldownService;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Override
    public int order() {
        return -20;
    }

    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        try {
            return doTry(in, notifyChannel);
        } catch (Exception e) {
            log.warn("JPAY 노티 적재 예외: {}", e.getMessage());
            return true;
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
        Map<String, String> form = parseJpayNotifyFields(raw.trim());
        String ch = notifyChannel == null || notifyChannel.isBlank() ? "CALLBACK" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        if ("RESULT".equals(ch) && applyJpaySyncResultIfMinimal(in, form, ch)) {
            return true;
        }
        if (!looksLikeJpayServerNotify(form)) {
            return false;
        }
        String memberid = first(form, "memberid");
        String orderid = first(form, "orderid");
        String ret = first(form, "returncode");
        String paySt = first(form, "paymentstatus");
        String txnId = first(form, "transaction_id");
        if (memberid.isBlank() || orderid.isBlank() || (ret.isBlank() && paySt.isBlank())) {
            return false;
        }
        Optional<PgAgency> agOpt = findJpayAgencyByMerchantMid(memberid);
        if (agOpt.isEmpty()) {
            log.debug("JPAY 노티 MID에 해당하는 tb_pg_agency 없음 memberid={}", memberid);
            return false;
        }
        PgAgency ag = agOpt.get();
        String apiKey = ag.getApiKey() != null ? ag.getApiKey().trim() : "";
        String sign = first(form, "sign");
        boolean resultChannel = "RESULT".equals(ch);
        if (apiKey.isEmpty() || sign.isBlank()) {
            /* 3DS 동기 복귀(rsJpay)는 sign 없음 — ChillPay 노티매핑(RESULT·paymentStatus)으로 위임 */
            log.info("JPAY 노티 sign 없음 — 다음 핸들러(노티매핑)로 위임 orderid={} channel={}", orderid, ch);
            return false;
        }
        if (!JpaySignatureUtil.verifyNotifySignWithMiddlewareRetry(form, apiKey, sign)) {
            if (!allowMiddlewareRelayWithoutSign(form, in) && !allowTrustedIngressWithoutSign(form, in)) {
                log.warn("JPAY 노티 서명 불일치 orderid={} channel={} — 노티매핑 핸들러로 위임", orderid, ch);
                return false;
            }
            log.warn("JPAY 노티 서명 불일치 — ICOPAY ingress returncode 적용 orderid={} channel={} returncode={}",
                    orderid, ch, ret);
        }
        String merchantId = resolveMerchantIdForNotify(in, orderid);
        if (merchantId == null || merchantId.isBlank()) {
            log.warn("JPAY 노티 가맹점 미해석 orderid={} — 노티매핑 핸들러로 위임", orderid);
            return false;
        }
        /* 동시 중복 노티 직렬화(best-effort): 같은 거래(가맹점|주문번호)의 처리가 겹치지 않도록 advisory lock.
         * 현재 @Transactional 종료 시 자동 해제되며, 실패해도 기존 흐름을 그대로 진행한다. */
        notifyIdempotencyLock.lock("JPAY", "ORD:" + merchantId.trim() + "|" + orderid.trim());
        if (jpaySubscriptionNotifyService.isSubscriptionNotify(form)) {
            jpaySubscriptionNotifyService.applySubscriptionNotify(merchantId.trim(), form, notifyChannel);
            return true;
        }
        String on = orderid.trim();
        if (on.length() > 64) {
            on = on.substring(0, 64);
        }
        final String orderKey = on;
        Optional<PgTrnsctn> ex = findJpayTxn(merchantId.trim(), orderKey);
        PgTrnsctn t = ex.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            x.setMerchantId(merchantId.trim());
            x.setServiceType("URL_JPAY");
            x.setOrigin(resolveOriginForNewNotifyTxn(merchantId.trim(), orderKey));
            return x;
        });
        t.setVan(PgVendor.JPAY.length() > 10 ? PgVendor.JPAY.substring(0, 10) : PgVendor.JPAY);
        t.setOrderNo(orderKey);
        t.setPayNo(orderKey.length() > 50 ? orderKey.substring(0, 50) : orderKey);
        JpayTransactionIdApplier.apply(t, txnId);
        String amtStr = first(form, "true_amount");
        if (amtStr.isBlank()) {
            amtStr = first(form, "amount");
        }
        if (!amtStr.isBlank()) {
            try {
                BigDecimal a = new BigDecimal(amtStr.replace(",", "").trim());
                if (a.compareTo(BigDecimal.ZERO) > 0) {
                    t.setAmtKrw(a);
                }
            } catch (Exception ignored) {
                /* keep */
            }
        }
        String cur = first(form, "currency");
        if (!cur.isBlank()) {
            String u = cur.trim().toUpperCase(Locale.ROOT);
            t.setCurType(u.length() > 3 ? u.substring(0, 3) : u);
        }
        t.setNotifyChannelType(ch);

        String prevStatus = t.getStatus();
        String prevSettledYn = t.getSettledYn();
        boolean icopayManualEcho = "Y".equalsIgnoreCase(first(form, JpayManualFollowUpNotifyService.ICOPAY_MANUAL_FOLLOWUP_FLAG));

        String next = JpayNotifyStatusResolver.resolveFromForm(form);
        if (next == null || next.isBlank()) {
            next = ST_FAIL;
        }
        String merged = NotifyToTxnStatusMerge.merge(t.getStatus(), next, ch, t.getOutcomeReasonCode());
        if (merged == null || merged.isBlank()) {
            merged = next;
        }
        t.setStatus(merged);
        t.setChillPaymentStatus(JpayNotifyStatusResolver.chillPaymentStatusLabel(merged, ret));
        Optional<String> recordedReason = TxnOutcomeReasonApplier.applyFromJpayNotifyForm(t, prevStatus, merged, form);
        applyPaidAtForNotifyOutcome(t, merged);
        if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
            t.setSettledYn("N");
        }
        if (t.getCustomerId() == null || t.getCustomerId().isBlank()) {
            t.setCustomerId("guest");
        }
        t.setPaymentChannel("CARD");
        if (in.getRootNo() != null && !in.getRootNo().isBlank()) {
            String r = in.getRootNo().trim();
            t.setRouteNo(r.length() > 32 ? r.substring(0, 32) : r);
        }
        JpayBuyerContactApplier.mergeFromNotifyForm(t, form);

        pgTrnsctnRepository.save(t);
        outcomeReasonWarmCoordinator.onRecorded(recordedReason);
        applyCardFailCooldownFromTxn(t, merged);
        hookSplitPayInstallment(t);
        try {
            settlementArrearsService.registerPostSettlementRecoveryIfDue(prevStatus, prevSettledYn, t);
        } catch (Exception recoveryEx) {
            log.warn("환수금 자동등록 실패 trnId={}: {}", t.getTrnId(), recoveryEx.getMessage());
        }
        if (ST_PAID.equals(merged) && t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
            try {
                settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
            } catch (Exception rtEx) {
                log.warn("실시간 자동정산 트리거 실패 merchantId={}: {}", t.getMerchantId(), rtEx.getMessage());
            }
        }
        log.info("JPAY 노티 반영 trnId={} merchantId={} orderNo={} returncode={} manualEcho={}",
                t.getTrnId(), merchantId, orderKey, ret, icopayManualEcho);
        merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, ch);
        return true;
    }

    /**
     * 3DS 동기 복귀(rsJpay) — {@code paymentStatus=succeeded&orderID=…} 처럼 memberid·sign 없이 오는 경우.
     * {@link PgNotifyReceiveService} 가 기존 URL 결제 행으로 가맹점을 해석한 뒤({@code PARSED}) 호출됩니다.
     */
    private boolean applyJpaySyncResultIfMinimal(PgNotifyInbound in, Map<String, String> form, String ch) {
        if (!first(form, "memberid").isBlank()) {
            return false;
        }
        String merchantId = in.getMerchantId();
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        String orderid = first(form, "orderid");
        if (orderid.isBlank()) {
            return false;
        }
        String ret = first(form, "returncode");
        String paySt = first(form, "paymentstatus");
        if (ret.isBlank() && paySt.isBlank()) {
            return false;
        }
        String on = orderid.trim();
        if (on.length() > 64) {
            on = on.substring(0, 64);
        }
        Optional<PgTrnsctn> ex = findJpayTxn(merchantId.trim(), on);
        if (ex.isEmpty()) {
            return false;
        }
        PgTrnsctn t = ex.get();
        String prevStatus = t.getStatus();
        JpayTransactionIdApplier.apply(t, first(form, "transaction_id"));
        t.setNotifyChannelType(ch);
        String next = JpayNotifyStatusResolver.resolve(ret, first(form, "_middleware_manualfollowup"), paySt);
        if (next == null || next.isBlank()) {
            next = ST_FAIL;
        }
        String merged = NotifyToTxnStatusMerge.merge(t.getStatus(), next, ch, t.getOutcomeReasonCode());
        if (merged == null || merged.isBlank()) {
            merged = next;
        }
        t.setStatus(merged);
        t.setChillPaymentStatus(JpayNotifyStatusResolver.chillPaymentStatusLabel(merged, ret));
        Optional<String> recordedReason2 = TxnOutcomeReasonApplier.applyFromJpayNotifyForm(t, prevStatus, merged, form);
        applyPaidAtForNotifyOutcome(t, merged);
        JpayBuyerContactApplier.mergeFromNotifyForm(t, form);
        pgTrnsctnRepository.save(t);
        outcomeReasonWarmCoordinator.onRecorded(recordedReason2);
        applyCardFailCooldownFromTxn(t, merged);
        hookSplitPayInstallment(t);
        if (ST_PAID.equals(merged)) {
            try {
                settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
            } catch (Exception rtEx) {
                log.warn("실시간 자동정산 트리거 실패 merchantId={}: {}", t.getMerchantId(), rtEx.getMessage());
            }
        }
        log.info("JPAY 3DS 동기 복귀 반영 trnId={} merchantId={} orderNo={} paymentStatus={}", t.getTrnId(), merchantId, on, paySt);
        merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, ch);
        return true;
    }

    private void hookSplitPayInstallment(PgTrnsctn t) {
        if (t == null || t.getOrderNo() == null || t.getOrderNo().isBlank()) {
            return;
        }
        try {
            splitPayPaymentHookService.onTxnStatusChange(t.getOrderNo(), t.getStatus(), t.getTrnId());
        } catch (Exception ex) {
            log.warn("분할결제 연동 실패 orderNo={}: {}", t.getOrderNo(), ex.getMessage());
        }
    }

    private void applyPaidAtForNotifyOutcome(PgTrnsctn t, String merged) {
        if (ST_PAID.equals(merged)) {
            ZoneId wall = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
            t.setPaidAt(LocalDateTime.now(wall));
        } else if (ST_FAIL.equals(merged) || "08".equals(merged)) {
            t.setPaidAt(null);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }

    private String resolveMerchantIdForNotify(PgNotifyInbound in, String orderid) {
        String merchantId = in != null && in.getMerchantId() != null ? in.getMerchantId().trim() : "";
        if (!merchantId.isBlank()) {
            return merchantId;
        }
        if (orderid == null || orderid.isBlank()) {
            return "";
        }
        String on = orderid.trim();
        Optional<PgTrnsctn> url = pgTrnsctnRepository.findFirstByOrderNoAndOriginOrderByCreatedAtDesc(on, ORIGIN_URL);
        if (url.isPresent() && url.get().getMerchantId() != null && !url.get().getMerchantId().isBlank()) {
            return url.get().getMerchantId().trim();
        }
        Optional<PgTrnsctn> api = pgTrnsctnRepository.findFirstByOrderNoAndOriginOrderByCreatedAtDesc(on, "MERCHANT_API");
        if (api.isPresent() && api.get().getMerchantId() != null && !api.get().getMerchantId().isBlank()) {
            return api.get().getMerchantId().trim();
        }
        return "";
    }

    private Optional<PgTrnsctn> findJpayTxn(String merchantId, String orderNo) {
        Optional<PgTrnsctn> sub = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                merchantId, orderNo, "SUBSCRIPTION");
        if (sub.isPresent()) {
            return sub;
        }
        Optional<PgTrnsctn> api = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                merchantId, orderNo, "MERCHANT_API");
        if (api.isPresent()) {
            return api;
        }
        return pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                merchantId, orderNo, ORIGIN_URL);
    }

    private Optional<PgAgency> findJpayAgencyByMerchantMid(String memberid) {
        String m = memberid.trim();
        return pgAgencyRepository.findByMerchantMidOrderByIdAsc(m).stream()
                .filter(a -> PgVendor.isJpayFamily(a.getPgCd()))
                .findFirst();
    }

    private static boolean looksLikeJpayServerNotify(Map<String, String> f) {
        if (JpayDisputeNotifyStatusResolver.looksLikeDisputeWebhook(f)) {
            return false;
        }
        if (first(f, "memberid").isBlank() || first(f, "orderid").isBlank()) {
            return false;
        }
        if (!first(f, "transaction_id").isBlank()) {
            return !first(f, "returncode").isBlank() || isJpaySuccessPaymentStatus(first(f, "paymentstatus"));
        }
        return !first(f, "returncode").isBlank() || isJpaySuccessPaymentStatus(first(f, "paymentstatus"));
    }

    /**
     * NOTI 미들웨어가 JPAY 원문을 ICOPAY로 릴레이한 경우 — 수신 단계에서 가맹점이 이미 PARSED 이고
     * returncode 가 JPAY 규칙으로 해석 가능하면, 서명 불일치(미들웨어가 msg 등 비스펙 필드를 붙인 경우 등)에도
     * 기존 MERCHANT_API·URL 대기 건을 갱신합니다.
     */
    private static boolean allowMiddlewareRelayWithoutSign(Map<String, String> form, PgNotifyInbound in) {
        if (form == null || in == null) {
            return false;
        }
        if (in.getMerchantId() == null || in.getMerchantId().isBlank()) {
            return false;
        }
        if (JpayNotifyStatusResolver.resolveFromForm(form) == null) {
            return false;
        }
        if (!first(form, "_middleware_incomingcontenttype").isBlank()
                || !first(form, "_middleware_rawbodylength").isBlank()) {
            return true;
        }
        /* NOTI 미들웨어 실패 릴레이 — JPAY 원문 returncode=2 + msg("No Card record" 등) */
        if (!first(form, "msg").isBlank()) {
            return true;
        }
        if (!first(form, "icopaycompid").isBlank()) {
            return true;
        }
        return false;
    }

    /**
     * ICOPAY 수신 단계에서 가맹점·주문이 이미 확정(PARSED)된 JPAY 노티 —
     * 서명 불일치여도 JPAY {@code returncode}(00·2·09 등) 규칙으로 반영합니다.
     */
    private static boolean allowTrustedIngressWithoutSign(Map<String, String> form, PgNotifyInbound in) {
        if (form == null || in == null) {
            return false;
        }
        if (in.getMerchantId() == null || in.getMerchantId().isBlank()) {
            return false;
        }
        return JpayNotifyStatusResolver.resolveFromForm(form) != null;
    }

    private String resolveOriginForNewNotifyTxn(String merchantId, String orderNo) {
        if (pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(merchantId, orderNo, "MERCHANT_API").isPresent()) {
            return "MERCHANT_API";
        }
        if (pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(merchantId, orderNo, ORIGIN_URL).isPresent()) {
            return ORIGIN_URL;
        }
        return ORIGIN_URL;
    }

    private static boolean isJpaySuccessPaymentStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String p = raw.trim().toLowerCase(Locale.ROOT);
        return "succeeded".equals(p) || "success".equals(p) || "paid".equals(p) || "00".equals(p);
    }

    private Map<String, String> parseJpayNotifyFields(String body) {
        if (body.startsWith("{")) {
            return parseJsonLowerKeys(body);
        }
        return parseFormLowerKeys(body);
    }

    private static Map<String, String> parseJsonLowerKeys(String json) {
        Map<String, String> m = new LinkedHashMap<>();
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root != null && root.isObject()) {
                Iterator<String> it = root.fieldNames();
                while (it.hasNext()) {
                    String k = it.next();
                    JsonNode v = root.get(k);
                    if (v != null && !v.isNull() && v.isValueNode()) {
                        String val = v.asText();
                        if (val != null && !val.isBlank()) {
                            m.put(k.toLowerCase(Locale.ROOT), val.trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        return m;
    }

    private static Map<String, String> parseFormLowerKeys(String body) {
        Map<String, String> m = new LinkedHashMap<>();
        try {
            for (String pair : body.split("&")) {
                int i = pair.indexOf('=');
                if (i <= 0) {
                    continue;
                }
                String k = URLDecoder.decode(pair.substring(0, i).trim(), StandardCharsets.UTF_8)
                        .toLowerCase(Locale.ROOT);
                String v = URLDecoder.decode(pair.substring(i + 1).trim(), StandardCharsets.UTF_8);
                if (!v.isEmpty()) {
                    m.put(k, v);
                }
            }
        } catch (Exception ignored) {
        }
        return m;
    }

    private void applyCardFailCooldownFromTxn(PgTrnsctn t, String merged) {
        if (t == null || t.getCardPanHash() == null || t.getCardPanHash().isBlank()) {
            return;
        }
        String hash = t.getCardPanHash().trim();
        String mask = t.getCardPanDisplay();
        Long orgUnitId = resolveOrgUnitId(t);
        if (ST_PAID.equals(merged)) {
            payCardFailCooldownService.clearOnSuccessByHash(PgVendor.JPAY, hash, orgUnitId);
            return;
        }
        if (ST_FAIL.equals(merged)) {
            payCardFailCooldownService.recordFromTxnHash(PgVendor.JPAY, hash, mask, "FAIL", t.getOutcomeReason(), orgUnitId);
            return;
        }
        String oc = t.getOutcomeReasonCode();
        if (oc != null && NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL.equalsIgnoreCase(oc.trim())) {
            payCardFailCooldownService.recordFromTxnHash(PgVendor.JPAY, hash, mask,
                    NotifyToTxnStatusMerge.OUTCOME_CODE_UNPAID_PROVISIONAL, t.getOutcomeReason(), orgUnitId);
        }
    }

    private Long resolveOrgUnitId(PgTrnsctn t) {
        if (t == null || t.getMerchantId() == null || t.getMerchantId().isBlank()) {
            return null;
        }
        return orgUnitRepository.findByCode(t.getMerchantId().trim()).map(OrgUnit::getId).orElse(null);
    }

    private static String first(Map<String, String> m, String key) {
        String v = m.get(key.toLowerCase(Locale.ROOT));
        return v != null ? v.trim() : "";
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return u.length() <= 20 ? u : u.substring(0, 20);
    }
}
