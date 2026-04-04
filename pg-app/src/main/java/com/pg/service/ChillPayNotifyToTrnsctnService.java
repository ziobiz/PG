package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgTrnsctnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    public ChillPayNotifyToTrnsctnService(PgTrnsctnRepository pgTrnsctnRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    /**
     * 수신 저장은 이미 끝난 {@code in}을 기준으로 시도합니다. 실패해도 예외를 던지지 않습니다.
     */
    @Transactional
    public void recordFromInbound(PgNotifyInbound in) {
        try {
            doRecord(in);
        } catch (Exception e) {
            log.warn("ChillPay 노티→pg_trnsctn 적재 실패 (수신 로그는 유지): {}", e.getMessage());
        }
    }

    private void doRecord(PgNotifyInbound in) {
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
        if (!looksLikeChillPayNotify(in, root)) {
            return;
        }

        String chillTxnId = textDeep(root, "TransactionId", "transactionId");
        String orderNo = textDeep(root, "OrderNo", "orderNo");
        if ((chillTxnId == null || chillTxnId.isBlank()) && (orderNo == null || orderNo.isBlank())) {
            log.debug("ChillPay 노티에 TransactionId·OrderNo 없음 — 거래 적재 생략");
            return;
        }

        long amountMinor = resolveAmountMinor(root);
        if (amountMinor <= 0) {
            log.debug("ChillPay 노티 금액 없음 또는 0 — 거래 적재 생략 orderNo={} chillTxn={}", orderNo, chillTxnId);
            return;
        }

        String paymentStatus = textDeep(root, "PaymentStatus", "paymentStatus", "Paymentstatus");
        String statusField = textDeep(root, "Status", "status");
        String internalStatus = mapInternalStatus(paymentStatus, statusField);
        if (internalStatus == null) {
            internalStatus = STATUS_AUTH_PENDING;
        }

        PgTrnsctn t = findExisting(merchantCode.trim(), chillTxnId, orderNo).orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            return x;
        });

        t.setMerchantId(merchantCode.trim());
        t.setServiceType("NOTI");
        t.setStatus(internalStatus);
        t.setCurType(firstCurrency(root));
        t.setAmtKrw(BigDecimal.valueOf(amountMinor));
        t.setVan("CHILLPAY");
        t.setOrigin(ORIGIN_NOTI);

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
            t.setTotalAmt(BigDecimal.valueOf(amountMinor));
        }
        parseOptionalDecimal(root, "Icopay", "icopay", "IcoPay").ifPresent(t::setIcopayAmt);

        if (STATUS_PAID.equals(internalStatus)) {
            LocalDateTime paid = parsePaymentDate(root);
            t.setPaidAt(paid != null ? paid : LocalDateTime.now());
        } else {
            t.setPaidAt(null);
        }

        if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
            t.setSettledYn("N");
        }

        pgTrnsctnRepository.save(t);
        log.info("ChillPay 노티 거래 적재 trnId={} merchantId={} orderNo={} chillTxn={} status={}",
                t.getTrnId(), t.getMerchantId(), t.getOrderNo(), t.getChillTransactionId(), t.getStatus());
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

    private static long resolveAmountMinor(JsonNode root) {
        String a = textDeep(root, "Amount", "amount");
        if (a != null) {
            long v = parseMoneyLong(a);
            if (v > 0) {
                return v;
            }
        }
        String total = textDeep(root, "TotalAmount", "totalAmount");
        if (total != null) {
            return parseMoneyLong(total);
        }
        return 0L;
    }

    private static long parseMoneyLong(String raw) {
        if (raw == null) {
            return 0L;
        }
        String s = raw.trim().replace(",", "");
        if (s.isEmpty()) {
            return 0L;
        }
        int dot = s.indexOf('.');
        if (dot >= 0) {
            try {
                return new BigDecimal(s).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
            } catch (Exception e) {
                return 0L;
            }
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
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

    private static String mapInternalStatus(String paymentStatus, String status) {
        String p = paymentStatus != null ? paymentStatus.trim().toLowerCase(Locale.ROOT) : "";
        String s = status != null ? status.trim().toLowerCase(Locale.ROOT) : "";
        if (!p.isEmpty()) {
            if (p.contains("paid") || p.contains("success") || "complete".equals(p)) {
                return STATUS_PAID;
            }
            if (p.contains("wait") || p.contains("authorize") || p.contains("pending") || p.contains("request")) {
                return STATUS_AUTH_PENDING;
            }
            if (p.contains("cancel") || p.contains("void")) {
                return STATUS_CANCEL;
            }
            if (p.contains("refund")) {
                return STATUS_REFUND;
            }
            if (p.contains("fail") || p.contains("error")) {
                return STATUS_FAIL;
            }
        }
        if (!s.isEmpty()) {
            if ("10".equals(s) || "paid".equals(s) || "success".equals(s)) {
                return STATUS_PAID;
            }
            if ("20".equals(s) || "cancel".equals(s)) {
                return STATUS_CANCEL;
            }
            if ("30".equals(s) || s.contains("refund")) {
                return STATUS_REFUND;
            }
            if ("99".equals(s) || "f0".equals(s) || s.contains("fail")) {
                return STATUS_FAIL;
            }
            if ("08".equals(s)) {
                return STATUS_AUTH_PENDING;
            }
        }
        return null;
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
        return null;
    }
}
