package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.receipt.TransactionReceiptEmailService;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PgTrnsctnOrderLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** QXIPAY 승인 전후 {@code pg_trnsctn} 대기·확정. */
@Service
public class QxipaySaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(QxipaySaleRecordService.class);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final TransactionReceiptEmailService transactionReceiptEmailService;

    public QxipaySaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   @Lazy TransactionReceiptEmailService transactionReceiptEmailService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String chargeId, String txnOrigin,
                                     BigDecimal shopperDisplayAmount, String shopperDisplayCurrency) {
        recordOrTouchPending(orgUnitId, orderNo, amount, currency, productName, chargeId, txnOrigin,
                shopperDisplayAmount, shopperDisplayCurrency, null, null);
    }

    /**
     * @param pgOrderRef ICOPAY→QXIPAY {@code reference}(불투명). {@code pay_no} 에 보관, charge id 는 {@code chill_transaction_id}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String chargeId, String txnOrigin,
                                     BigDecimal shopperDisplayAmount, String shopperDisplayCurrency,
                                     String pgOrderRef) {
        recordOrTouchPending(orgUnitId, orderNo, amount, currency, productName, chargeId, txnOrigin,
                shopperDisplayAmount, shopperDisplayCurrency, pgOrderRef, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String chargeId, String txnOrigin,
                                     BigDecimal shopperDisplayAmount, String shopperDisplayCurrency,
                                     String pgOrderRef, java.util.Map<String, Object> saleBody) {
        try {
            Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
            if (ouOpt.isEmpty() || orderNo == null || orderNo.isBlank()) {
                return;
            }
            String mid = ouOpt.get().getCode();
            Optional<PgTrnsctn> existing = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                    pgTrnsctnRepository, mid, orderNo.trim());
            PgTrnsctn t = existing.filter(x -> PgVendor.isQxipayFamily(x.getVan())).orElse(null);
            if (t == null) {
                t = new PgTrnsctn();
                t.setTrnId("QX" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
                t.setMerchantId(mid);
                t.setOrderNo(orderNo.trim());
                t.setCreatedAt(LocalDateTime.now());
            }
            t.setVan(PgVendor.QXIPAY);
            t.setStatus("08");
            if (amount != null) {
                t.setAmtKrw(amount);
                t.setTotalAmt(amount);
            }
            if (currency != null && !currency.isBlank()) {
                t.setCurType(currency.trim().toUpperCase());
            }
            if (productName != null && !productName.isBlank()) {
                String pn = productName.length() > 100 ? productName.substring(0, 100) : productName;
                t.setCustomerNm(pn);
            }
            if (pgOrderRef != null && !pgOrderRef.isBlank()) {
                String pr = pgOrderRef.trim();
                t.setPayNo(pr.length() > 50 ? pr.substring(0, 50) : pr);
            } else if (t.getPayNo() == null || t.getPayNo().isBlank()) {
                t.setPayNo(orderNo.trim().length() > 50 ? orderNo.trim().substring(0, 50) : orderNo.trim());
            }
            if (chargeId != null && !chargeId.isBlank()) {
                t.setChillTransactionId(chargeId.trim());
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
                com.pg.util.UrlPaySaleTxnFieldApplier.apply(t, saleBody);
                com.pg.util.UrlPaySaleTxnFieldApplier.ensureUrlWebDevice(t, txnOrigin);
            }
            pgTrnsctnRepository.save(t);
        } catch (Exception e) {
            log.warn("QXIPAY pending txn 실패: {}", e.getMessage());
        }
    }

    @Transactional
    public Optional<PgTrnsctn> applyChargeOutcome(String orderNo, String compId, String chargeId,
                                                  String status, String approvalNo) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        /* orderNo = 가맹 order_no 또는 QXIPAY reference(ICOPAY ref = pay_no) */
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findByMerchantOrderOrPayNo(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty() || !PgVendor.isQxipayFamily(txn.get().getVan())) {
            return Optional.empty();
        }
        PgTrnsctn t = txn.get();
        String st = status != null ? status.trim().toLowerCase() : "";
        if ("succeeded".equals(st) || "paid".equals(st) || "success".equals(st)) {
            t.setStatus("10");
            t.setPaidAt(LocalDateTime.now());
        } else if ("failed".equals(st) || "expired".equals(st)) {
            t.setStatus("99");
        } else if ("pending".equals(st)) {
            t.setStatus("08");
        }
        if (chargeId != null && !chargeId.isBlank()) {
            t.setChillTransactionId(chargeId.trim());
        }
        if (approvalNo != null && !approvalNo.isBlank()) {
            t.setApprovalNo(approvalNo.trim());
        }
        t.setVan(PgVendor.QXIPAY);
        PgTrnsctn saved = pgTrnsctnRepository.save(t);
        try {
            transactionReceiptEmailService.scheduleIfDue(saved);
        } catch (Exception ex) {
            log.warn("QXIPAY 거래 영수증 메일 연동 실패 trnId={}: {}", saved.getTrnId(), ex.getMessage());
        }
        return Optional.of(saved);
    }
}
