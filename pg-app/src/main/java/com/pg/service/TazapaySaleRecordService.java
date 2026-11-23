package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.receipt.TransactionReceiptEmailService;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.IcopayPgOrderRef;
import com.pg.util.PgTrnsctnOrderLookup;
import com.pg.util.UrlPaySaleTxnFieldApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Tazapay 승인 전후 {@code pg_trnsctn} 대기·확정. */
@Service
public class TazapaySaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(TazapaySaleRecordService.class);
    private static final String ST_PENDING = "08";
    private static final String ST_PAID = "10";
    private static final String ST_FAIL = "99";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final TransactionReceiptEmailService transactionReceiptEmailService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;

    public TazapaySaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                    OrgUnitRepository orgUnitRepository,
                                    @Lazy TransactionReceiptEmailService transactionReceiptEmailService,
                                    @Lazy MerchantOutboundNotifyService merchantOutboundNotifyService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String payinId, String txnOrigin,
                                     BigDecimal shopperDisplayAmount, String shopperDisplayCurrency,
                                     String buyerName, String buyerEmail, String buyerTel,
                                     Map<String, Object> saleBody) {
        try {
            Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
            if (ouOpt.isEmpty() || orderNo == null || orderNo.isBlank()) {
                return;
            }
            String mid = ouOpt.get().getCode();
            Optional<PgTrnsctn> existing = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                    pgTrnsctnRepository, mid, orderNo.trim());
            PgTrnsctn t = existing.filter(x -> PgVendor.isTazapayFamily(x.getVan())).orElse(null);
            if (t == null) {
                t = new PgTrnsctn();
                t.setTrnId("TZ" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
                t.setMerchantId(mid);
                t.setOrderNo(orderNo.trim());
                t.setCreatedAt(LocalDateTime.now());
            }
            t.setVan(PgVendor.TAZAPAY);
            t.setStatus(ST_PENDING);
            if (amount != null) {
                t.setAmtKrw(amount);
                t.setTotalAmt(amount);
            }
            if (currency != null && !currency.isBlank()) {
                t.setCurType(currency.trim().toUpperCase());
            }
            String nm = trimTo(buyerName, 200);
            if (!nm.isBlank()) {
                t.setCustomerNm(nm);
            }
            String email = trimTo(buyerEmail, 100);
            if (!email.isBlank()) {
                t.setCustomerId(email);
            }
            String tel = trimTo(buyerTel, 50);
            if (!tel.isBlank()) {
                t.setCustomerTel(tel);
            }
            if (payinId != null && !payinId.isBlank()) {
                t.setChillTransactionId(trimTo(payinId, 64));
            }
            if (saleBody != null) {
                IcopayPgOrderRef.ensureAndApply(t, IcopayPgOrderRef.Profile.ALNUM_50, saleBody);
            }
            if (txnOrigin != null && !txnOrigin.isBlank()) {
                t.setOrigin(txnOrigin);
            }
            if (shopperDisplayAmount != null) {
                t.setDisplayAmt(shopperDisplayAmount);
            }
            if (shopperDisplayCurrency != null && !shopperDisplayCurrency.isBlank()) {
                t.setDisplayCurType(shopperDisplayCurrency.trim().toUpperCase());
            }
            t.setPaymentChannel("CARD");
            if (saleBody != null && !saleBody.isEmpty()) {
                UrlPaySaleTxnFieldApplier.apply(t, saleBody);
                UrlPaySaleTxnFieldApplier.ensureUrlWebDevice(t, txnOrigin);
            } else {
                UrlPaySaleTxnFieldApplier.ensureUrlWebDevice(t, txnOrigin);
            }
            pgTrnsctnRepository.save(t);
        } catch (Exception e) {
            log.warn("TAZAPAY pending txn 실패: {}", e.getMessage());
        }
    }

    public Optional<PgTrnsctn> findByMerchantAndOrder(String merchantId, String orderNo) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, merchantId.trim(), orderNo.trim());
        if (txn.isEmpty() || !PgVendor.isTazapayFamily(txn.get().getVan())) {
            return Optional.empty();
        }
        return txn;
    }

    public Optional<PgTrnsctn> resolveTxnForWebhook(String payinId, String referenceId, String orderNoHint) {
        String payin = payinId != null ? payinId.trim() : "";
        if (!payin.isBlank()) {
            Optional<PgTrnsctn> byChill = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(payin);
            if (byChill.isPresent() && PgVendor.isTazapayFamily(byChill.get().getVan())) {
                return byChill;
            }
        }
        String ref = referenceId != null ? referenceId.trim() : "";
        if (!ref.isBlank()) {
            Optional<PgTrnsctn> byPay = pgTrnsctnRepository.findFirstByPayNoOrderByCreatedAtDesc(ref);
            if (byPay.isPresent() && PgVendor.isTazapayFamily(byPay.get().getVan())) {
                return byPay;
            }
        }
        String orderNo = orderNoHint != null ? orderNoHint.trim() : "";
        if (!orderNo.isBlank()) {
            for (PgTrnsctn t : pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(orderNo)) {
                if (PgVendor.isTazapayFamily(t.getVan())) {
                    return Optional.of(t);
                }
            }
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<PgTrnsctn> applyPaidOutcome(String orderNo, String compId, String payinId,
                                                String referenceId) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty() || !PgVendor.isTazapayFamily(txn.get().getVan())) {
            return Optional.empty();
        }
        PgTrnsctn t = txn.get();
        String stNow = t.getStatus() != null ? t.getStatus().trim() : "";
        if (ST_PAID.equals(stNow) || "00".equals(stNow)) {
            return Optional.of(t);
        }
        if (ST_FAIL.equals(stNow)) {
            return Optional.empty();
        }
        if (payinId != null && !payinId.isBlank()) {
            t.setChillTransactionId(trimTo(payinId, 64));
        }
        if (referenceId != null && !referenceId.isBlank()) {
            t.setPayNo(trimTo(referenceId, 50));
        }
        t.setStatus(ST_PAID);
        t.setPaidAt(LocalDateTime.now());
        t.setChillPaymentStatus("Paid");
        pgTrnsctnRepository.save(t);
        hookReceiptEmail(t);
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "TAZAPAY_WEBHOOK");
        } catch (Exception e) {
            log.warn("TAZAPAY paid notify: {}", e.getMessage());
        }
        return Optional.of(t);
    }

    @Transactional
    public Optional<PgTrnsctn> applyFailedOutcome(String orderNo, String compId, String payinId,
                                                 String reasonCode, String reason) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty() || !PgVendor.isTazapayFamily(txn.get().getVan())) {
            return Optional.empty();
        }
        PgTrnsctn t = txn.get();
        if (!ST_PENDING.equals(trim(t.getStatus()))) {
            return Optional.of(t);
        }
        t.setStatus(ST_FAIL);
        String msg = reason != null && !reason.isBlank() ? reason.trim() : "Tazapay payment failed";
        t.setChillPaymentStatus(trimTo(msg, 50));
        com.pg.util.TxnOutcomeReasonApplier.apply(t, ST_PENDING, ST_FAIL, msg,
                reasonCode != null && !reasonCode.isBlank() ? reasonCode : "TAZAPAY_WEBHOOK_FAIL",
                com.pg.util.TxnOutcomeReasonApplier.SOURCE_ICOPAY);
        pgTrnsctnRepository.save(t);
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, reasonCode);
        } catch (Exception e) {
            log.warn("TAZAPAY fail notify: {}", e.getMessage());
        }
        return Optional.of(t);
    }

    @Transactional
    public Optional<PgTrnsctn> applyPendingExpired(String merchantId, String orderNo, int staleMinutes) {
        return applyFailedOutcome(orderNo, merchantId, null,
                "TAZAPAY_PENDING_EXPIRED", PendingTxnExpireService.expireReasonMessage(staleMinutes));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> lookupCompIdForNoti(String orderHint) {
        Map<String, Object> out = new LinkedHashMap<>();
        String q = orderHint != null ? orderHint.trim() : "";
        if (q.isBlank()) {
            out.put("success", false);
            out.put("errorCode", "INVALID_ORDER");
            out.put("message", "order가 필요합니다.");
            return out;
        }
        Optional<PgTrnsctn> matched = pgTrnsctnRepository.findFirstByPayNoOrderByCreatedAtDesc(q)
                .filter(x -> PgVendor.isTazapayFamily(x.getVan()));
        if (matched.isEmpty()) {
            matched = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(q)
                    .filter(x -> PgVendor.isTazapayFamily(x.getVan()));
        }
        if (matched.isEmpty()) {
            for (PgTrnsctn t : pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(q)) {
                if (PgVendor.isTazapayFamily(t.getVan())) {
                    matched = Optional.of(t);
                    break;
                }
            }
        }
        if (matched.isEmpty()) {
            out.put("success", false);
            out.put("errorCode", "NOT_FOUND");
            out.put("message", "주문을 찾을 수 없습니다.");
            out.put("order", q);
            return out;
        }
        PgTrnsctn t = matched.get();
        String compId = t.getMerchantId() != null ? t.getMerchantId().trim() : "";
        out.put("success", true);
        out.put("compId", compId);
        out.put("CompId", compId);
        out.put("merchantId", compId);
        out.put("orderNo", t.getOrderNo() != null ? t.getOrderNo().trim() : "");
        out.put("order", q);
        out.put("trnId", t.getTrnId());
        out.put("status", t.getStatus());
        out.put("van", t.getVan());
        return out;
    }

    private void hookReceiptEmail(PgTrnsctn t) {
        try {
            transactionReceiptEmailService.scheduleIfDue(t);
        } catch (Exception ex) {
            log.warn("TAZAPAY receipt email: {}", ex.getMessage());
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimTo(String s, int max) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}