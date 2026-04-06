package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.NotifyAmountParse;
import com.pg.util.NotifyChannelMerge;
import com.pg.util.NotifyToTxnStatusMerge;
import com.pg.util.PgNotifyInternalStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * NOTI·칠페이 서버노티(JSON) 수신 후, {@link PgNotifyInbound}가 가맹점까지 해석된 경우
 * {@link PgTrnsctn}에 반영합니다. MID({@code MerchantCode})·루트({@code RouteNo}) 매칭은
 * {@link PgNotifyReceiveService}에서 끝난 뒤 본 서비스가 본문 필드를 읽어 적재합니다.
 */
@Service
public class ChillPayNotifyToTrnsctnService {

    private static final Logger log = LoggerFactory.getLogger(ChillPayNotifyToTrnsctnService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ORIGIN_NOTI = "NOTI";
    private static final String STATUS_PAID = "10";
    private static final String STATUS_AUTH_PENDING = "08";
    private static final String STATUS_CANCEL = "20";
    private static final String STATUS_REFUND = "30";
    private static final String STATUS_FAIL = "99";

    private static final DateTimeFormatter PAY_DD_MM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final HqNotifyMappingService hqNotifyMappingService;

    public ChillPayNotifyToTrnsctnService(PgTrnsctnRepository pgTrnsctnRepository,
                                         MerchantPgBindingRepository merchantPgBindingRepository,
                                         HqNotifyMappingService hqNotifyMappingService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.hqNotifyMappingService = hqNotifyMappingService;
    }

    /**
     * 수신 저장은 이미 끝난 {@code in}을 기준으로 시도합니다. 실패해도 예외를 던지지 않습니다.
     */
    @Transactional
    public void recordFromInbound(PgNotifyInbound in) {
        recordFromInbound(in, "CALLBACK");
    }

    /**
     * @param notifyChannel 노티 수신 URL 경로의 대상 코드로부터 해석된 채널(CALLBACK/RESULT 등).
     *                      {@link HqNotifyMappingService} 의 채널별 fieldMappings 와 대응합니다.
     */
    @Transactional
    public void recordFromInbound(PgNotifyInbound in, String notifyChannel) {
        try {
            doRecord(in, notifyChannel);
        } catch (Exception e) {
            log.warn("ChillPay 노티→pg_trnsctn 적재 실패 (수신 로그는 유지): {}", e.getMessage());
        }
    }

    private void doRecord(PgNotifyInbound in, String notifyChannel) {
        if (in == null || !"PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim())) {
            return;
        }
        String merchantCode = in.getMerchantId();
        if (merchantCode == null || merchantCode.isBlank()) {
            return;
        }
        String raw = in.getRawBody();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{")) {
            return;
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(trimmed);
        } catch (Exception e) {
            return;
        }
        if (root == null || !root.isObject()) {
            return;
        }

        String pgCd = resolvePgCdForInbound(in);
        String notifyCh = notifyChannel == null || notifyChannel.isBlank() ? "CALLBACK" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        if (pgCd != null && hqNotifyMappingService.hasMappableNotifyMapping(pgCd, notifyCh)) {
            Optional<PgTrnsctn> mapped = hqNotifyMappingService.tryBuildTransactionFromMappedCallback(
                    pgCd, root, in, this::findExisting, notifyCh);
            if (mapped.isPresent()) {
                pgTrnsctnRepository.save(mapped.get());
                PgTrnsctn t = mapped.get();
                log.info("노티매핑 적용 거래 적재 trnId={} merchantId={} pgCd={} orderNo={} chillTxn={} status={}",
                        t.getTrnId(), t.getMerchantId(), pgCd, t.getOrderNo(), t.getChillTransactionId(), t.getStatus());
                return;
            }
        }

        if (!looksLikeChillPayNotify(in, root)) {
            return;
        }

        String chillTxnId = textDeep(root, "TransactionId", "transactionId");
        String orderNo = textDeep(root, "OrderNo", "orderNo");
        if ((chillTxnId == null || chillTxnId.isBlank()) && (orderNo == null || orderNo.isBlank())) {
            log.debug("ChillPay 노티에 TransactionId·OrderNo 없음 — 거래 적재 생략");
            return;
        }

        Optional<PgTrnsctn> existingOpt = findExisting(merchantCode.trim(), chillTxnId, orderNo);
        Optional<BigDecimal> amountOpt = resolveAmountFromNotify(root);
        if (!NotifyAmountParse.isPositive(amountOpt)) {
            if (existingOpt.isEmpty()) {
                log.debug("ChillPay 노티 금액 없음 또는 0 — 신규 행 생략 orderNo={} chillTxn={}", orderNo, chillTxnId);
                return;
            }
            /* RESULT 실패 등 금액이 비어 있는 노티도 기존 행 상태만 갱신 */
        }

        String paymentStatus = textDeep(root, "PaymentStatus", "paymentStatus", "Paymentstatus");
        String statusField = textDeep(root, "Status", "status");
        String computed = PgNotifyInternalStatusMapper.mapPaymentAndStatus(paymentStatus, statusField, true);
        PgTrnsctn t = existingOpt.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            return x;
        });
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
                log.debug("ChillPay 노티 금액 없음·기존 금액도 없음 — 적재 생략 orderNo={} chillTxn={}", orderNo, chillTxnId);
                return;
            }
        }

        t.setMerchantId(merchantCode.trim());
        t.setServiceType("NOTI");
        t.setStatus(mergedStatus);
        t.setCurType(firstCurrency(root));
        t.setAmtKrw(amountBd);
        t.setVan("CHILLPAY");
        t.setOrigin(ORIGIN_NOTI);
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
            t.setPaidAt(paid != null ? paid : LocalDateTime.now());
        } else {
            t.setPaidAt(null);
        }

        if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
            t.setSettledYn("N");
        }

        pgTrnsctnRepository.save(t);
        log.info("ChillPay 노티 거래 적재 trnId={} merchantId={} orderNo={} chillTxn={} channel={} status={}",
                t.getTrnId(), t.getMerchantId(), t.getOrderNo(), t.getChillTransactionId(), notifyCh, t.getStatus());
    }

    private boolean looksLikeChillPayNotify(PgNotifyInbound in, JsonNode root) {
        boolean hasTxnOrOrder = textDeep(root, "TransactionId", "transactionId") != null
                || textDeep(root, "OrderNo", "orderNo") != null;
        boolean hasPaySignals = textDeep(root, "PaymentStatus", "paymentStatus") != null
                || textDeep(root, "PaymentChannel", "paymentChannel") != null
                || textDeep(root, "Amount", "amount") != null
                || textDeep(root, "TotalAmount", "totalAmount") != null;
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
                log.debug("노티 RouteNo와 파싱 root_no 불일치 — ChillPay 거래 적재 생략 body={} inboundRoot={}", bodyRoute, in.getRootNo());
                return false;
            }
        }
        return true;
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

    private Optional<PgTrnsctn> findExisting(String merchantId, String chillTxnId, String orderNo) {
        if (chillTxnId != null && !chillTxnId.isBlank()) {
            Optional<PgTrnsctn> byChill = pgTrnsctnRepository.findFirstByChillTransactionIdAndMerchantId(chillTxnId.trim(), merchantId);
            if (byChill.isPresent()) {
                return byChill;
            }
        }
        if (orderNo != null && !orderNo.isBlank()) {
            return pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(merchantId, orderNo.trim(), ORIGIN_NOTI);
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
