package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.integration.pg.PgVendor;
import com.pg.splitpay.SplitPayPaymentHookService;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.util.ChillPayNotifyOutcomeAdjust;
import com.pg.util.NotifyAmountParse;
import com.pg.util.NotifyChannelMerge;
import com.pg.util.PgTrnsctnNotifyDisplayHelper;
import com.pg.util.NotifyToTxnStatusMerge;
import com.pg.util.PgNotifyInternalStatusMapper;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOTI·칠페이 서버노티(JSON) 수신 후, {@link PgNotifyInbound}가 가맹점까지 해석된 경우
 * {@link PgTrnsctn}에 반영합니다. MID({@code MerchantCode})·루트({@code RouteNo}) 매칭은
 * {@link PgNotifyReceiveService}에서 끝난 뒤 본 서비스가 본문 필드를 읽어 적재합니다.
 */
@Service
public class ChillPayNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    private static final Logger log = LoggerFactory.getLogger(ChillPayNotifyToTrnsctnService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ORIGIN_NOTI = "NOTI";
    private static final String ORIGIN_URL = "URL";
    private static final String ORIGIN_API = "API";
    private static final String STATUS_PAID = "10";
    private static final String STATUS_AUTH_PENDING = "08";
    private static final String STATUS_CANCEL = "20";
    private static final String STATUS_REFUND = "30";
    private static final String STATUS_FAIL = "99";

    private static final DateTimeFormatter PAY_DD_MM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
    /** ChillPay PaymentDescription 등 — 노티 본문에서 업체코드 재추출 ({@link PgNotifyReceiveService} 와 동일) */
    private static final Pattern ICOPAY_COMP_ID = Pattern.compile("icopayCompId=([A-Za-z0-9_.-]+)", Pattern.CASE_INSENSITIVE);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqNotifyMappingService hqNotifyMappingService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final SettlementCalcService settlementCalcService;
    private final SettlementArrearsService settlementArrearsService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final MerchantChatbotOrderService merchantChatbotOrderService;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final SplitPayPaymentHookService splitPayPaymentHookService;
    private final OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator;

    public ChillPayNotifyToTrnsctnService(PgTrnsctnRepository pgTrnsctnRepository,
                                         MerchantPgBindingRepository merchantPgBindingRepository,
                                         OrgUnitRepository orgUnitRepository,
                                         HqNotifyMappingService hqNotifyMappingService,
                                         MerchantOutboundNotifyService merchantOutboundNotifyService,
                                         SettlementCalcService settlementCalcService,
                                         SettlementArrearsService settlementArrearsService,
                                         HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                         MerchantChatbotOrderService merchantChatbotOrderService,
                                         NotifyIdempotencyLock notifyIdempotencyLock,
                                         SplitPayPaymentHookService splitPayPaymentHookService,
                                         OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqNotifyMappingService = hqNotifyMappingService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.settlementCalcService = settlementCalcService;
        this.settlementArrearsService = settlementArrearsService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.merchantChatbotOrderService = merchantChatbotOrderService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
        this.outcomeReasonWarmCoordinator = outcomeReasonWarmCoordinator;
    }

    @Override
    public int order() {
        return 0;
    }

    /**
     * ChillPay 계열 노티 → {@code pg_trnsctn} 적재. ChillPay 형태가 아니면 {@code false} 를 반환해 다음 핸들러로 넘깁니다.
     */
    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        try {
            return doTryRecord(in, notifyChannel);
        } catch (Exception e) {
            log.warn("ChillPay 노티→pg_trnsctn 적재 실패 (수신 로그는 유지): {}", e.getMessage());
            return true;
        }
    }

    /**
     * 수신 저장은 이미 끝난 {@code in}을 기준으로 시도합니다. 실패해도 예외를 던지지 않습니다.
     *
     * @deprecated 내부·테스트 외에는 {@link com.pg.integration.pg.notify.PgNotifyInboundTxnDispatcher} 를 사용하세요.
     */
    @Deprecated
    @Transactional
    public void recordFromInbound(PgNotifyInbound in) {
        tryRecord(in, "CALLBACK");
    }

    /**
     * @param notifyChannel 노티 수신 URL 경로의 대상 코드로부터 해석된 채널(CALLBACK/RESULT 등).
     *                      {@link HqNotifyMappingService} 의 채널별 fieldMappings 와 대응합니다.
     * @deprecated 내부·테스트 외에는 {@link com.pg.integration.pg.notify.PgNotifyInboundTxnDispatcher} 를 사용하세요.
     */
    @Deprecated
    @Transactional
    public void recordFromInbound(PgNotifyInbound in, String notifyChannel) {
        tryRecord(in, notifyChannel);
    }

    private boolean doTryRecord(PgNotifyInbound in, String notifyChannel) {
        if (in == null || !"PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim())) {
            if (in != null) {
                log.debug("pg_trnsctn 미적재(inbound 로그는 저장됨): processStatus={} mid={} merchantId={} err={}",
                        in.getProcessStatus(), in.getMid(), in.getMerchantId(), in.getErrorMessage());
            }
            return true;
        }
        String merchantCode = in.getMerchantId();
        if (merchantCode == null || merchantCode.isBlank()) {
            return true;
        }
        String raw = in.getRawBody();
        if (raw == null || raw.isBlank()) {
            return true;
        }
        if (looksLikeJpayServerNotifyInRaw(raw.trim())) {
            return false;
        }
        JsonNode root = resolveNotifyJsonTree(in, raw.trim());
        if (root == null || !root.isObject()) {
            return true;
        }

        /* 동시 중복 노티 직렬화(best-effort): 같은 거래의 처리가 겹치지 않도록 거래키 단위 advisory lock.
         * 현재 @Transactional 종료 시 자동 해제되며, 실패해도 기존 흐름을 그대로 진행한다. */
        acquireChillPayIdempotencyLock(merchantCode.trim(), root);

        String pgCd = resolvePgCdForInbound(in);
        String notifyCh = notifyChannel == null || notifyChannel.isBlank() ? "CALLBACK" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        if (pgCd != null && hqNotifyMappingService.hasMappableNotifyMapping(pgCd, notifyCh)) {
            Optional<PgTrnsctn> mapped = hqNotifyMappingService.tryBuildTransactionFromMappedCallback(
                    pgCd, root, in, this::findExisting, notifyCh);
            if (mapped.isPresent()) {
                pgTrnsctnRepository.save(mapped.get());
                PgTrnsctn t = mapped.get();
                if (STATUS_PAID.equals(t.getStatus()) && t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
                    try {
                        settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
                    } catch (Exception ex) {
                        log.warn("실시간 자동정산 트리거 실패 merchantId={}: {}", t.getMerchantId(), ex.getMessage());
                    }
                }
                log.info("노티매핑 적용 거래 적재 trnId={} merchantId={} pgCd={} orderNo={} chillTxn={} status={}",
                        t.getTrnId(), t.getMerchantId(), pgCd, t.getOrderNo(), t.getChillTransactionId(), t.getStatus());
                try {
                    merchantChatbotOrderService.tryConfirmOrderAfterPaidTxn(t);
                } catch (Exception ex) {
                    log.warn("챗봇 주문 확정 연동 실패(노티 적재는 유지) trnId={}: {}", t.getTrnId(), ex.getMessage());
                }
                invokeSplitPayHook(t);
                merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, notifyCh);
                return true;
            }
        }

        if (!looksLikeChillPayNotify(in, root)) {
            return false;
        }

        String chillTxnId = textDeep(root, "TransactionId", "transactionId");
        String orderNo = textDeep(root, "OrderNo", "orderNo");
        if ((chillTxnId == null || chillTxnId.isBlank()) && (orderNo == null || orderNo.isBlank())) {
            log.debug("ChillPay 노티에 TransactionId·OrderNo 없음 — 거래 적재 생략");
            return true;
        }

        Optional<PgTrnsctn> existingOpt = findExisting(merchantCode.trim(), chillTxnId, orderNo);
        boolean mergeByGlobalChill = existingOpt.isPresent() && chillTxnId != null && !chillTxnId.isBlank()
                && !merchantCode.trim().equalsIgnoreCase(
                Optional.ofNullable(existingOpt.get().getMerchantId()).orElse("").trim());
        String paymentStatus = firstNonBlankDeep(root,
                "PaymentStatus", "paymentStatus", "Paymentstatus",
                "PayResult", "payResult", "TxnStatus", "txnStatus", "PaymentResult", "paymentResult");
        String statusField = firstNonBlankDeep(root,
                "Status", "status", "ResultCode", "resultCode", "RespCode", "respCode", "ResponseCode", "responseCode");
        String computed = PgNotifyInternalStatusMapper.mapPaymentAndStatus(paymentStatus, statusField, true);
        computed = ChillPayNotifyOutcomeAdjust.reclassifyPaymentStatusTwoAfterPaid(existingOpt, paymentStatus, computed);

        Optional<BigDecimal> amountOpt = resolveAmountFromNotify(root);
        if (!NotifyAmountParse.isPositive(amountOpt) && existingOpt.isEmpty()) {
            boolean allowNewWithoutPositiveAmt = computed != null && (
                    "10".equals(computed) || "08".equals(computed)
                            || NotifyToTxnStatusMerge.isTerminalOutcome(computed));
            if (!allowNewWithoutPositiveAmt) {
                log.debug("ChillPay 노티 금액 없음 또는 0 — 신규 행 생략 orderNo={} chillTxn={}", orderNo, chillTxnId);
                return true;
            }
        }

        PgTrnsctn t = existingOpt.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            return x;
        });
        String prevStatusSnap = t.getStatus();
        String prevSettledYnSnap = t.getSettledYn();

        String mergedStatus = NotifyToTxnStatusMerge.merge(t.getStatus(), computed, notifyCh);
        if (mergedStatus == null || mergedStatus.isBlank()) {
            mergedStatus = STATUS_AUTH_PENDING;
        }

        BigDecimal amountBd;
        if (NotifyAmountParse.isPositive(amountOpt)) {
            amountBd = amountOpt.get();
        } else {
            amountBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
            if (amountBd.compareTo(BigDecimal.ZERO) <= 0) {
                /* 금액 누락·0 이라도 승인·대기·터미널(취소·무효·실패 등)이면 행 생성·갱신 허용 */
                boolean allowZeroAmt = NotifyToTxnStatusMerge.isTerminalOutcome(mergedStatus)
                        || "10".equals(mergedStatus)
                        || "08".equals(mergedStatus);
                if (!allowZeroAmt) {
                    log.debug("ChillPay 노티 금액 없음·기존 금액도 없음 — 적재 생략 orderNo={} chillTxn={}", orderNo, chillTxnId);
                    return true;
                }
                amountBd = BigDecimal.ZERO;
            }
        }
        /* 잘못 파싱된 노티가 다른 업체로 들어와도 TransactionId 로 병합할 때는 기존 금액 유지(800 vs 80000 이중 적재 방지) */
        if (mergeByGlobalChill && t.getAmtKrw() != null && t.getAmtKrw().compareTo(BigDecimal.ZERO) > 0) {
            amountBd = t.getAmtKrw();
        }

        if (existingOpt.isEmpty()) {
            t.setMerchantId(merchantCode.trim());
            t.setServiceType("NOTI");
            t.setOrigin(ORIGIN_NOTI);
        }
        t.setStatus(mergedStatus);
        t.setCurType(firstCurrency(root));
        t.setAmtKrw(amountBd);
        t.setVan(PgVendor.CHILLPAY);
        t.setNotifyChannelType(NotifyChannelMerge.mergeStored(t.getNotifyChannelType(), notifyCh));

        String payNo = orderNo != null && !orderNo.isBlank() ? orderNo.trim() : (chillTxnId != null ? chillTxnId.trim() : t.getTrnId());
        if (payNo.length() > 50) {
            payNo = payNo.substring(0, 50);
        }
        t.setPayNo(payNo);

        if (orderNo != null && !orderNo.isBlank()) {
            String on = orderNo.trim();
            t.setOrderNo(on.length() > 64 ? on.substring(0, 64) : on);
        } else if (chillTxnId != null) {
            String synthetic = "CP" + chillTxnId.trim();
            t.setOrderNo(synthetic.length() > 64 ? synthetic.substring(0, 64) : synthetic);
        }

        String customerId = textDeep(root, "CustomerId", "customerId", "Customer", "customer");
        if (customerId != null && !customerId.isBlank()) {
            String c = customerId.trim();
            t.setCustomerId(c.length() > 100 ? c.substring(0, 100) : c);
        } else {
            t.setCustomerId("guest");
        }
        String customerNm = textDeep(root, "CustomerName", "customerName", "PayerName", "payerName");
        if (customerNm != null && !customerNm.isBlank()) {
            String cn = customerNm.trim();
            t.setCustomerNm(cn.length() > 200 ? cn.substring(0, 200) : cn);
        }

        String channel = textDeep(root, "PaymentChannel", "paymentChannel", "ChannelCode", "channelCode");
        if (channel != null && !channel.isBlank()) {
            String ch = channel.trim();
            t.setPaymentChannel(ch.length() > 80 ? ch.substring(0, 80) : ch);
        }

        String route = textDeep(root, "RouteNo", "routeNo", "Routeno");
        if (route != null && !route.isBlank()) {
            String r = route.trim();
            t.setRouteNo(r.length() > 32 ? r.substring(0, 32) : r);
        } else if (in.getRootNo() != null && !in.getRootNo().isBlank()) {
            String r = in.getRootNo().trim();
            t.setRouteNo(r.length() > 32 ? r.substring(0, 32) : r);
        }

        if (chillTxnId != null && !chillTxnId.isBlank()) {
            String id = chillTxnId.trim();
            t.setChillTransactionId(id.length() > 64 ? id.substring(0, 64) : id);
        }

        String chillPs = paymentStatus != null ? paymentStatus.trim() : (statusField != null ? statusField.trim() : null);
        /* 승인 숫자(0~4)만 남은 레거시 표기인데 병합 결과가 무효·취소 등이면 DB 표시 필드도 내부코드로 맞춤 */
        if (mergedStatus != null && NotifyToTxnStatusMerge.isTerminalOutcome(mergedStatus)
                && !"10".equals(mergedStatus) && !"08".equals(mergedStatus)
                && chillPs != null && chillPs.matches("^[0-4]$")) {
            chillPs = mergedStatus;
        }
        if (chillPs != null && !chillPs.isEmpty()) {
            t.setChillPaymentStatus(chillPs.length() > 50 ? chillPs.substring(0, 50) : chillPs);
        }

        parseOptionalDecimal(root, "Fee", "fee").ifPresent(t::setChillFeeAmt);
        parseOptionalDecimal(root, "TotalAmount", "totalAmount", "Totalamount").ifPresent(t::setTotalAmt);
        if (t.getTotalAmt() == null) {
            t.setTotalAmt(amountBd);
        }
        parseOptionalDecimal(root, "Icopay", "icopay", "IcoPay").ifPresent(t::setIcopayAmt);

        if (STATUS_PAID.equals(mergedStatus)) {
            LocalDateTime paid = parsePaymentDate(root);
            t.setPaidAt(paid != null ? paid : LocalDateTime.now(hqLedgerSysSettingsService.resolveLedgerDisplayZoneId()));
        } else {
            t.setPaidAt(null);
        }

        if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
            t.setSettledYn("N");
        }

        applyMerchantFromIcopayCompInPayload(in, root, raw, t);
        PgTrnsctnNotifyDisplayHelper.mergeFromChillPayJson(root, t);
        Optional<String> recordedReason = TxnOutcomeReasonApplier.applyFromChillPayJson(t, prevStatusSnap, mergedStatus, root);

        pgTrnsctnRepository.save(t);
        outcomeReasonWarmCoordinator.onRecorded(recordedReason);
        try {
            settlementArrearsService.registerPostSettlementRecoveryIfDue(prevStatusSnap, prevSettledYnSnap, t);
        } catch (Exception ex) {
            log.warn("환수금 자동등록 실패 trnId={}: {}", t.getTrnId(), ex.getMessage());
        }
        if (STATUS_PAID.equals(mergedStatus) && t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
            try {
                settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
            } catch (Exception ex) {
                log.warn("실시간 자동정산 트리거 실패 merchantId={}: {}", t.getMerchantId(), ex.getMessage());
            }
        }
        log.info("ChillPay 노티 거래 적재 trnId={} merchantId={} orderNo={} chillTxn={} channel={} status={}",
                t.getTrnId(), t.getMerchantId(), t.getOrderNo(), t.getChillTransactionId(), notifyCh, t.getStatus());
        try {
            merchantChatbotOrderService.tryConfirmOrderAfterPaidTxn(t);
        } catch (Exception ex) {
            log.warn("챗봇 주문 확정 연동 실패(노티 적재는 유지) trnId={}: {}", t.getTrnId(), ex.getMessage());
        }
        invokeSplitPayHook(t);
        merchantOutboundNotifyService.scheduleAfterTxnCommit(t, in, notifyCh);
        return true;
    }

    private void invokeSplitPayHook(PgTrnsctn t) {
        if (t == null || t.getOrderNo() == null) {
            return;
        }
        try {
            splitPayPaymentHookService.onTxnStatusChange(t.getOrderNo(), t.getStatus(), t.getTrnId());
        } catch (Exception ex) {
            log.warn("분할결제 연동 실패 orderNo={}: {}", t.getOrderNo(), ex.getMessage());
        }
    }

    /** JPAY 비동기 노티({@code memberid}+{@code orderid}+{@code returncode}) — ChillPay 핸들러가 가로채지 않도록 */
    private boolean looksLikeJpayServerNotifyInRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String t = raw.trim();
        if (t.startsWith("{")) {
            try {
                JsonNode n = MAPPER.readTree(t);
                if (n != null && n.isObject()) {
                    String memberid = textDeep(n, "memberid", "memberId");
                    String orderid = textDeep(n, "orderid", "orderId", "orderID");
                    String ret = textDeep(n, "returncode", "returnCode");
                    String txnId = textDeep(n, "transaction_id", "transactionId");
                    if (memberid != null && !memberid.isBlank()
                            && orderid != null && !orderid.isBlank()
                            && ((ret != null && !ret.isBlank())
                            || (txnId != null && !txnId.isBlank()))) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
                /* fall through */
            }
        }
        String lower = t.toLowerCase(Locale.ROOT);
        return lower.contains("memberid=") && lower.contains("orderid=")
                && (lower.contains("returncode=") || lower.contains("transaction_id="))
                && !lower.contains("alert_type=");
    }

    /**
     * JSON 노티는 그대로 파싱하고, {@code application/x-www-form-urlencoded} 본문은
     * CALLBACK·RESULT 모두 합성 JSON 으로 올립니다. (노티미들웨어가 무효·취소 후속을 CALLBACK 폼으로 보내는 경우 포함)
     * <p>수신 로그 {@code raw_body} 는 변경하지 않습니다.</p>
     */
    private JsonNode resolveNotifyJsonTree(PgNotifyInbound in, String trimmed) {
        if (trimmed.startsWith("{")) {
            try {
                JsonNode r = MAPPER.readTree(trimmed);
                return r != null && r.isObject() ? r : null;
            } catch (Exception e) {
                return null;
            }
        }
        if (trimmed.contains("=")) {
            JsonNode synthetic = buildSyntheticChillPayJsonFromResultForm(trimmed, in);
            if (synthetic != null) {
                return synthetic;
            }
        }
        return null;
    }

    private static JsonNode buildSyntheticChillPayJsonFromResultForm(String formBody, PgNotifyInbound in) {
        if (formBody == null || formBody.isBlank() || !formBody.contains("=")) {
            return null;
        }
        Map<String, String> lm = new LinkedHashMap<>();
        parseFormLowerKeys(formBody, lm);
        String orderNo = getLoose(lm, "orderno", "order_no", "orderid", "order_id");
        String transNo = coalesceNonBlank(
                getLoose(lm, "transno", "trans_no"),
                getLoose(lm, "transactionid", "transaction_id"));
        if ((orderNo == null || orderNo.isBlank()) && (transNo == null || transNo.isBlank())) {
            return null;
        }
        ObjectNode n = MAPPER.createObjectNode();
        if (orderNo != null && !orderNo.isBlank()) {
            n.put("OrderNo", orderNo.trim());
            n.put("orderID", orderNo.trim());
            n.put("orderid", orderNo.trim());
        }
        if (transNo != null && !transNo.isBlank()) {
            n.put("TransactionId", transNo.trim());
        }
        String resp = getLoose(lm, "respcode", "resp_code");
        String paySt = getLoose(lm, "paymentstatus", "payment_status", "payresult", "pay_result",
                "txnstatus", "txn_status", "paymentresult", "payment_result");
        String effPay = coalesceNonBlank(resp, paySt);
        if (effPay != null && !effPay.isBlank()) {
            n.put("PaymentStatus", effPay.trim());
        }
        String st = coalesceNonBlank(getLoose(lm, "status"),
                coalesceNonBlank(getLoose(lm, "resultcode", "result_code"),
                        coalesceNonBlank(getLoose(lm, "returncode", "return_code"),
                                getLoose(lm, "responsecode", "response_code"))));
        if (st != null && !st.isBlank()) {
            n.put("Status", st.trim());
        }
        String retOnly = getLoose(lm, "returncode", "return_code");
        if (retOnly != null && !retOnly.isBlank() && (st == null || st.isBlank())) {
            n.put("Status", retOnly.trim());
            n.put("returncode", retOnly.trim());
        }
        if (in.getMid() != null && !in.getMid().isBlank()) {
            n.put("MerchantCode", in.getMid().trim());
        }
        if (in.getRootNo() != null && !in.getRootNo().isBlank()) {
            n.put("RouteNo", in.getRootNo().trim());
        }
        String amt = coalesceNonBlank(getLoose(lm, "amount"), getLoose(lm, "true_amount", "trueamount"));
        if (amt != null && !amt.isBlank()) {
            n.put("Amount", amt.trim());
        } else {
            n.put("Amount", "0");
        }
        String cur = getLoose(lm, "currency");
        if (cur != null && !cur.isBlank()) {
            n.put("Currency", cur.trim());
        }
        return n;
    }

    private static void parseFormLowerKeys(String body, Map<String, String> out) {
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
                    out.put(k, v);
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
    }

    private static String getLoose(Map<String, String> m, String... keys) {
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String look = key.toLowerCase(Locale.ROOT).replace('-', '_');
            String v = m.get(look);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static String coalesceNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private boolean looksLikeChillPayNotify(PgNotifyInbound in, JsonNode root) {
        boolean hasTxnOrOrder = textDeep(root, "TransactionId", "transactionId") != null
                || textDeep(root, "OrderNo", "orderNo") != null;
        /* 무효·취소 전용 노티는 Amount 없이 Status·PaymentStatus·응답코드만 오는 경우가 많음(노티거래내역과 동일하게 처리) */
        boolean hasPaySignals = textDeep(root, "PaymentStatus", "paymentStatus") != null
                || textDeep(root, "PaymentChannel", "paymentChannel") != null
                || textDeep(root, "Amount", "amount") != null
                || textDeep(root, "TotalAmount", "totalAmount") != null
                || textDeep(root, "Status", "status") != null
                || firstNonBlankDeep(root, "PayResult", "payResult", "TxnStatus", "txnStatus") != null
                || firstNonBlankDeep(root, "ResultCode", "resultCode", "RespCode", "respCode") != null;
        if (!hasTxnOrOrder || !hasPaySignals) {
            return false;
        }
        String bodyMc = textDeep(root, "MerchantCode", "merchantCode", "Merchant_Code");
        if (bodyMc != null && !bodyMc.isBlank() && in.getMid() != null && !in.getMid().isBlank()) {
            if (!bodyMc.trim().equalsIgnoreCase(in.getMid().trim())) {
                log.debug("노티 MerchantCode와 파싱 MID 불일치 — ChillPay 거래 적재 생략 body={} inboundMid={}", bodyMc, in.getMid());
                return false;
            }
        }
        String bodyRoute = textDeep(root, "RouteNo", "routeNo");
        if (bodyRoute != null && !bodyRoute.isBlank() && in.getRootNo() != null && !in.getRootNo().isBlank()) {
            if (!bodyRoute.trim().equals(in.getRootNo().trim())) {
                log.warn("노티 RouteNo 불일치(수신 root_no={}, 본문 RouteNo={}) — inbound PARSED 기준으로 적재 진행",
                        in.getRootNo(), bodyRoute);
            }
        }
        return true;
    }

    /**
     * 수신 단계에서 MID·노티 바인딩만으로 merchant 가 틀어진 경우에도,
     * 본문 {@code icopayCompId=} 또는 {@link PgNotifyInbound#getPayloadCompId()} 가 유효 업체면 {@code pg_trnsctn.merchant_id} 를 맞춥니다.
     */
    private void applyMerchantFromIcopayCompInPayload(PgNotifyInbound in, JsonNode root, String rawBody, PgTrnsctn t) {
        if (t == null) {
            return;
        }
        String comp = extractIcopayCompIdFromNotify(root, rawBody);
        if ((comp == null || comp.isBlank())
                && in != null && in.getPayloadCompId() != null && !in.getPayloadCompId().isBlank()) {
            comp = in.getPayloadCompId().trim();
        }
        if (comp == null || comp.isBlank()) {
            return;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(comp.trim());
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(comp.trim());
        }
        if (ou.isEmpty()) {
            return;
        }
        String code = ou.get().getCode();
        if (code == null || code.isBlank()) {
            return;
        }
        String normalized = code.trim();
        String cur = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        if (!normalized.equalsIgnoreCase(cur)) {
            t.setMerchantId(normalized);
        }
    }

    private static String extractIcopayCompIdFromNotify(JsonNode root, String rawBody) {
        String desc = textDeep(root, "PaymentDescription", "paymentDescription");
        if (desc != null && !desc.isBlank()) {
            Matcher m = ICOPAY_COMP_ID.matcher(desc);
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        if (rawBody != null && !rawBody.isBlank()) {
            Matcher m = ICOPAY_COMP_ID.matcher(rawBody);
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        return null;
    }

    /**
     * 수신 로그에 저장된 org_unit_id + MID(+루트)로 결제대행사 바인딩에서 PG 코드를 찾습니다.
     */
    private String resolvePgCdForInbound(PgNotifyInbound in) {
        if (in == null || in.getOrgUnitId() == null || in.getMid() == null || in.getMid().isBlank()) {
            return null;
        }
        List<MerchantPgBinding> sameMid = merchantPgBindingRepository.findByMidOrderByOperationalYnDescIdAsc(in.getMid().trim());
        List<MerchantPgBinding> orgBinds = sameMid.stream()
                .filter(b -> in.getOrgUnitId().equals(b.getOrgUnitId()))
                .toList();
        if (orgBinds.isEmpty()) {
            return null;
        }
        String root = in.getRootNo();
        if (root != null && !root.isBlank()) {
            String r = root.trim();
            Optional<MerchantPgBinding> exact = orgBinds.stream()
                    .filter(b -> b.getRootNo() != null && r.equals(b.getRootNo().trim()))
                    .findFirst();
            if (exact.isPresent()) {
                return exact.get().getPgCd();
            }
            Optional<MerchantPgBinding> loose = orgBinds.stream()
                    .filter(b -> b.getRootNo() == null || b.getRootNo().isBlank())
                    .findFirst();
            if (loose.isPresent()) {
                return loose.get().getPgCd();
            }
        }
        return orgBinds.get(0).getPgCd();
    }

    /**
     * ChillPay 노티의 거래 식별 키로 동시 중복 처리를 직렬화한다(best-effort).
     * 거래ID(TransactionId)가 있으면 그것을, 없으면 가맹점|주문번호를 키로 사용한다.
     * 같은 거래의 중복 노티는 동일 키를 산출하므로 직렬화되고, 다른 거래는 영향받지 않는다.
     */
    private void acquireChillPayIdempotencyLock(String merchantCode, JsonNode root) {
        String chillTxnId = textDeep(root, "TransactionId", "transactionId");
        String orderNo = textDeep(root, "OrderNo", "orderNo");
        String key;
        if (chillTxnId != null && !chillTxnId.isBlank()) {
            key = "TXN:" + chillTxnId.trim();
        } else if (orderNo != null && !orderNo.isBlank()) {
            key = "ORD:" + merchantCode + "|" + orderNo.trim();
        } else {
            return;
        }
        notifyIdempotencyLock.lock("CHILLPAY", key);
    }

    private Optional<PgTrnsctn> findExisting(String merchantId, String chillTxnId, String orderNo) {
        if (chillTxnId != null && !chillTxnId.isBlank()) {
            String tid = chillTxnId.trim();
            Optional<PgTrnsctn> byChill = pgTrnsctnRepository.findFirstByChillTransactionIdAndMerchantId(tid, merchantId);
            if (byChill.isPresent()) {
                return byChill;
            }
            Optional<PgTrnsctn> byChillGlobal = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(tid);
            if (byChillGlobal.isPresent()) {
                log.info("Chill TransactionId={} 기존 행을 merchant 무관으로 매칭 (노티 merchantId={}, DB merchantId={})",
                        tid, merchantId, byChillGlobal.get().getMerchantId());
                return byChillGlobal;
            }
        }
        if (orderNo != null && !orderNo.isBlank()) {
            String on = orderNo.trim();
            Optional<PgTrnsctn> n = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(merchantId, on, ORIGIN_NOTI);
            if (n.isPresent()) {
                return n;
            }
            n = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(merchantId, on, ORIGIN_URL);
            if (n.isPresent()) {
                return n;
            }
            return pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(merchantId, on, ORIGIN_API);
        }
        return Optional.empty();
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return u.length() <= 20 ? u : u.substring(0, 20);
    }

    private static String textDeep(JsonNode root, String... names) {
        String t = text(root, names);
        if (t != null) {
            return t;
        }
        JsonNode d = root.get("data");
        if (d != null && d.isObject()) {
            return text(d, names);
        }
        return null;
    }

    private static String text(JsonNode n, String... names) {
        if (n == null || !n.isObject()) {
            return null;
        }
        for (String c : names) {
            JsonNode x = n.get(c);
            if (x != null && !x.isNull()) {
                if (x.isTextual()) {
                    String s = x.asText().trim();
                    if (!s.isEmpty()) {
                        return s;
                    }
                }
                if (x.isNumber()) {
                    return x.asText();
                }
                if (x.isBoolean()) {
                    return x.asBoolean() ? "true" : "false";
                }
            }
        }
        return null;
    }

    /** {@link #textDeep} 와 동일 탐색이나, 후보 키 중 첫 비어 있지 않은 값 */
    private static String firstNonBlankDeep(JsonNode root, String... names) {
        for (String name : names) {
            String v = textDeep(root, name);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /** Amount 우선, 없거나 0 이하이면 TotalAmount — 반올림 없이 노티 원문 그대로 */
    private static Optional<BigDecimal> resolveAmountFromNotify(JsonNode root) {
        String a = textDeep(root, "Amount", "amount");
        if (a != null) {
            Optional<BigDecimal> o = NotifyAmountParse.parsePlain(a);
            if (NotifyAmountParse.isPositive(o)) {
                return o;
            }
        }
        String total = textDeep(root, "TotalAmount", "totalAmount");
        if (total != null) {
            return NotifyAmountParse.parsePlain(total);
        }
        return Optional.empty();
    }

    private static Optional<BigDecimal> parseOptionalDecimal(JsonNode root, String... names) {
        String s = textDeep(root, names);
        if (s == null || s.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(s.trim().replace(",", "")));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String firstCurrency(JsonNode root) {
        String c = textDeep(root, "Currency", "currency", "CurrencyCode", "currencyCode");
        if (c != null && !c.isBlank()) {
            String u = c.trim().toUpperCase(Locale.ROOT);
            return u.length() > 3 ? u.substring(0, 3) : u;
        }
        return "THB";
    }

    private static LocalDateTime parsePaymentDate(JsonNode root) {
        String pd = textDeep(root, "PaymentDate", "paymentDate", "PaidAt", "paidAt", "TransactionDate", "transactionDate");
        if (pd == null || pd.isBlank()) {
            return null;
        }
        String t = pd.trim();
        try {
            return LocalDateTime.parse(t, PAY_DD_MM);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(t, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            if (t.length() >= 10) {
                return LocalDateTime.parse(t.substring(0, 10) + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (DateTimeParseException ignored) {
        }
        /* ChillPay·노티미들서버 계열: 20240405190733 */
        try {
            if (t.matches("^\\d{14}$")) {
                return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT));
            }
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}
