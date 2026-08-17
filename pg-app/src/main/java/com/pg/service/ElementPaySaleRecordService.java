package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.ElementPayCallbackEventUtil;
import com.pg.util.PgTrnsctnOrderLookup;
import com.pg.util.RouteNoDisplayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * ElementPay initPayment 직후·웹훅(pay) 시 {@link PgTrnsctn} 적재·갱신.
 */
@Service
public class ElementPaySaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(ElementPaySaleRecordService.class);

    public static final String SERVICE_TYPE = "URL_ELEMENTPAY";
    private static final String ST_PAID = "10";
    private static final String ST_PENDING = "08";
    private static final String ST_FAIL = "99";
    private static final String ST_CANCEL = "20";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    public ElementPaySaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                       OrgUnitRepository orgUnitRepository,
                                       HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId,
                                     String orderNo,
                                     BigDecimal amount,
                                     String currency,
                                     Integer routeNo,
                                     String productName,
                                     String txnOrigin,
                                     String buyerName,
                                     String buyerEmail,
                                     String paymentMethodChannel,
                                     BigDecimal shopperDisplayAmount,
                                     String shopperDisplayCurrency,
                                     boolean subscription,
                                     String elementPayPaymentId) {
        try {
            doRecord(orgUnitId, orderNo, amount, currency, routeNo, productName, txnOrigin,
                    buyerName, buyerEmail, paymentMethodChannel, shopperDisplayAmount, shopperDisplayCurrency,
                    subscription, elementPayPaymentId);
        } catch (Exception e) {
            log.warn("ElementPay ready 거래 적재(대기) 실패: {}", e.getMessage());
        }
    }

    private void doRecord(Long orgUnitId,
                          String orderNo,
                          BigDecimal amount,
                          String currency,
                          Integer routeNo,
                          String productName,
                          String txnOrigin,
                          String buyerName,
                          String buyerEmail,
                          String paymentMethodChannel,
                          BigDecimal shopperDisplayAmount,
                          String shopperDisplayCurrency,
                          boolean subscription,
                          String elementPayPaymentId) {
        if (orgUnitId == null || orderNo == null || orderNo.isBlank()) {
            return;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            return;
        }
        String merchantId = ou.get().getCode();
        if (merchantId == null || merchantId.isBlank()) {
            return;
        }
        String on = orderNo.trim();
        if (on.length() > 64) {
            on = on.substring(0, 64);
        }
        String origin = resolveOrigin(txnOrigin);
        Optional<PgTrnsctn> ex = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(pgTrnsctnRepository, merchantId, on);
        final String mid = truncate(merchantId.trim(), 20);
        PgTrnsctn t = ex.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            x.setMerchantId(mid);
            x.setServiceType(SERVICE_TYPE);
            x.setOrigin(origin);
            return x;
        });
        t.setStatus(ST_PENDING);
        t.setVan(PgVendor.ELEMENTPAY);
        t.setOrderNo(on);
        t.setPayNo(on.length() > 50 ? on.substring(0, 50) : on);
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            t.setAmtKrw(amount);
        }
        String cur = currency != null && !currency.isBlank()
                ? currency.trim().toUpperCase(Locale.ROOT) : "THB";
        t.setCurType(cur.length() > 3 ? cur.substring(0, 3) : cur);
        String routeStored = RouteNoDisplayUtil.normalizeForStorage(routeNo != null ? routeNo : 0);
        if (routeStored != null) {
            t.setRouteNo(routeStored);
        }
        if (buyerName != null && !buyerName.isBlank()) {
            t.setCustomerNm(truncate(buyerName.trim(), 200));
        }
        if (buyerEmail != null && !buyerEmail.isBlank()) {
            t.setCustomerId(truncate(buyerEmail.trim(), 100));
        }
        if (t.getCustomerId() == null || t.getCustomerId().isBlank()) {
            t.setCustomerId("guest");
        }
        String desc = "ELEMENTPAY_URL";
        if (productName != null && !productName.isBlank()) {
            desc = desc + " " + productName.trim();
        }
        t.setChillPaymentStatus(truncate(desc, 50));
        String channel = paymentMethodChannel != null && !paymentMethodChannel.isBlank()
                ? paymentMethodChannel.trim().toUpperCase(Locale.ROOT) : "CARD";
        t.setPaymentChannel(truncate(channel, 80));
        if (elementPayPaymentId != null && !elementPayPaymentId.isBlank()) {
            t.setChillTransactionId(truncate(elementPayPaymentId.trim(), 64));
        }
        if (shopperDisplayAmount != null && shopperDisplayAmount.compareTo(BigDecimal.ZERO) > 0
                && shopperDisplayCurrency != null && !shopperDisplayCurrency.isBlank()) {
            t.setDisplayAmt(shopperDisplayAmount);
            String dc = shopperDisplayCurrency.trim().toUpperCase(Locale.ROOT);
            t.setDisplayCurType(dc.length() > 3 ? dc.substring(0, 3) : dc);
        }
        if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
            t.setSettledYn("N");
        }
        pgTrnsctnRepository.save(t);
    }

    @Transactional
    public Optional<PgTrnsctn> applyOutcome(String merchantId, String orderNo, boolean paid,
                                            String paymentId, String msg) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        try {
            Optional<PgTrnsctn> ex = findTxnForOrder(merchantId.trim(), orderNo.trim());
            if (ex.isEmpty()) {
                return Optional.empty();
            }
            PgTrnsctn t = ex.get();
            if (paymentId != null && !paymentId.isBlank()) {
                t.setChillTransactionId(truncate(paymentId.trim(), 64));
                t.setApprovalNo(truncate(paymentId.trim(), 20));
            }
            if (paid) {
                if (ST_PAID.equals(t.getStatus())) {
                    return Optional.of(t);
                }
                t.setStatus(ST_PAID);
                ZoneId wall = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
                t.setPaidAt(LocalDateTime.now(wall));
                t.setChillPaymentStatus(truncate(msg != null && !msg.isBlank() ? msg.trim() : "Success", 50));
            } else {
                String cur = t.getStatus() != null ? t.getStatus().trim() : "";
                if (ST_PAID.equals(cur) || isRefundOrChargebackStatus(cur)) {
                    return Optional.of(t);
                }
                t.setStatus(ST_FAIL);
                t.setPaidAt(null);
                if (msg != null && !msg.isBlank()) {
                    t.setChillPaymentStatus(truncate(msg.trim(), 50));
                    t.setOutcomeReason(msg.trim());
                    t.setOutcomeReasonSource("ELEMENTPAY");
                    t.setOutcomeReasonAt(LocalDateTime.now());
                }
            }
            pgTrnsctnRepository.save(t);
            return Optional.of(t);
        } catch (Exception e) {
            log.warn("ElementPay 결과 반영 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Callback {@code payment.*}/{@code refund.*} — 스펙 이벤트별 내부 상태.
     */
    @Transactional
    public Optional<PgTrnsctn> applyAsyncEvent(String merchantId, String orderNo, String paymentId,
                                               ElementPayCallbackEventUtil.Spec spec, String rawMethod) {
        if (spec == null || !spec.changesTxn()) {
            return Optional.empty();
        }
        if (orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> found = Optional.empty();
        if (merchantId != null && !merchantId.isBlank()) {
            found = findTxnForOrder(merchantId.trim(), orderNo.trim());
        }
        if (found.isEmpty()) {
            found = findAnyByOrder(orderNo.trim());
        }
        if (found.isEmpty() && paymentId != null && !paymentId.isBlank()) {
            found = findAnyByPaymentId(paymentId.trim());
        }
        if (found.isEmpty()) {
            return Optional.empty();
        }
        PgTrnsctn t = found.get();
        String cur = t.getStatus() != null ? t.getStatus().trim() : "";
        String msg = ElementPayCallbackEventUtil.defaultMessage(spec, rawMethod);
        if (paymentId != null && !paymentId.isBlank()) {
            t.setChillTransactionId(truncate(paymentId.trim(), 64));
        }
        switch (spec.kind()) {
            case PAY_REJECT -> {
                if (ST_PAID.equals(cur) || isRefundOrChargebackStatus(cur)) {
                    return Optional.of(t);
                }
                applyFailFields(t, msg);
            }
            case PAY_REVERSED -> {
                if ("31".equals(cur)) {
                    return Optional.of(t);
                }
                t.setStatus("31");
                t.setPaidAt(null);
                applyReason(t, msg);
            }
            case PAY_REFUNDED -> {
                applyRefundStatus(t, "42", paymentId, msg);
                return Optional.of(t);
            }
            case WRONG_PAYER -> {
                if (ST_PAID.equals(cur)) {
                    t.setStatus("31");
                    t.setPaidAt(null);
                    applyReason(t, msg);
                } else if (isRefundOrChargebackStatus(cur)) {
                    return Optional.of(t);
                } else {
                    applyFailFields(t, msg);
                }
            }
            case REFUND_CREATED, REFUND_CANCELED -> applyReason(t, msg);
            default -> {
                return Optional.of(t);
            }
        }
        pgTrnsctnRepository.save(t);
        return Optional.of(t);
    }

    private static void applyFailFields(PgTrnsctn t, String msg) {
        t.setStatus(ST_FAIL);
        t.setPaidAt(null);
        applyReason(t, msg);
    }

    private static void applyReason(PgTrnsctn t, String msg) {
        if (msg == null || msg.isBlank()) {
            return;
        }
        t.setChillPaymentStatus(truncate(msg.trim(), 50));
        t.setOutcomeReason(msg.trim());
        t.setOutcomeReasonSource("ELEMENTPAY");
        t.setOutcomeReasonAt(LocalDateTime.now());
    }

    private static boolean isRefundOrChargebackStatus(String cur) {
        return "42".equals(cur) || "31".equals(cur) || "30".equals(cur)
                || "21".equals(cur) || "22".equals(cur);
    }

    /**
     * getStatus 207(refunded) 등 — 로컬을 환불 상태(기본 42)로 맞춤.
     * 이미 환불·강제환불·수동환불이면 유지.
     */
    @Transactional
    public void applyRefundStatus(PgTrnsctn t, String statusCode, String paymentId, String msg) {
        if (t == null || !PgVendor.isElementPayFamily(t.getVan())) {
            return;
        }
        String cur = t.getStatus() != null ? t.getStatus().trim() : "";
        if ("42".equals(cur) || "31".equals(cur) || "30".equals(cur)) {
            return;
        }
        String st = (statusCode != null && !statusCode.isBlank()) ? statusCode.trim() : "42";
        t.setStatus(st);
        t.setPaidAt(null);
        if (paymentId != null && !paymentId.isBlank()) {
            t.setChillTransactionId(truncate(paymentId.trim(), 64));
        }
        String reason = msg != null && !msg.isBlank() ? msg.trim() : "Payment is refunded";
        t.setChillPaymentStatus(truncate(reason, 50));
        t.setOutcomeReason(reason);
        t.setOutcomeReasonSource("ELEMENTPAY");
        t.setOutcomeReasonAt(LocalDateTime.now());
        pgTrnsctnRepository.save(t);
    }

    public Optional<PgTrnsctn> findAnyByOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        return pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(orderNo.trim()).stream()
                .filter(t -> PgVendor.isElementPayFamily(t.getVan()))
                .findFirst();
    }

    public Optional<PgTrnsctn> findAnyByPaymentId(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            return Optional.empty();
        }
        return pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(paymentId.trim())
                .filter(t -> PgVendor.isElementPayFamily(t.getVan()));
    }

    public Optional<PgTrnsctn> findByMerchantAndOrder(String merchantId, String orderNo) {
        return findTxnForOrder(merchantId, orderNo);
    }

    private Optional<PgTrnsctn> findTxnForOrder(String merchantId, String orderNo) {
        Optional<PgTrnsctn> t = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, merchantId, orderNo);
        if (t.isPresent() && PgVendor.isElementPayFamily(t.get().getVan())) {
            return t;
        }
        return Optional.empty();
    }

    private static String resolveOrigin(String txnOrigin) {
        if (txnOrigin == null || txnOrigin.isBlank()) {
            return "URL";
        }
        return txnOrigin.trim().toUpperCase(Locale.ROOT);
    }

    private static String newTrnId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
