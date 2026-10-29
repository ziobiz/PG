package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PgTrnsctnOrderLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** QBANG QR 승인 전후 {@code pg_trnsctn}. */
@Service
public class QbangSaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(QbangSaleRecordService.class);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;

    public QbangSaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                  OrgUnitRepository orgUnitRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String qbangTxnNo, String txnOrigin,
                                     BigDecimal shopperDisplayAmount, String shopperDisplayCurrency,
                                     String buyerName, String buyerEmail, String buyerTel) {
        try {
            Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
            if (ouOpt.isEmpty() || orderNo == null || orderNo.isBlank()) {
                return;
            }
            String mid = ouOpt.get().getCode();
            Optional<PgTrnsctn> existing = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                    pgTrnsctnRepository, mid, orderNo.trim());
            PgTrnsctn t = existing.filter(x -> PgVendor.isQbangFamily(x.getVan())).orElse(null);
            if (t == null) {
                t = new PgTrnsctn();
                t.setTrnId("QB" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
                t.setMerchantId(mid);
                t.setOrderNo(orderNo.trim());
                t.setCreatedAt(LocalDateTime.now());
            }
            t.setVan(PgVendor.QBANG);
            t.setStatus("08");
            t.setPaymentChannel("QR");
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
            } else if (productName != null && !productName.isBlank()
                    && (t.getCustomerNm() == null || t.getCustomerNm().isBlank())) {
                /* 상품명은 고객란에 넣지 않음 — 비워 둠 */
            }
            String email = trimTo(buyerEmail, 100);
            if (!email.isBlank()) {
                t.setCustomerId(email);
            }
            String tel = trimTo(buyerTel, 50);
            if (!tel.isBlank()) {
                t.setCustomerTel(tel);
            }
            if (qbangTxnNo != null && !qbangTxnNo.isBlank()) {
                t.setChillTransactionId(qbangTxnNo.trim());
                t.setPayNo(qbangTxnNo.trim());
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
            pgTrnsctnRepository.save(t);
        } catch (Exception e) {
            log.warn("QBANG pending txn 실패: {}", e.getMessage());
        }
    }

    @Transactional
    public Optional<PgTrnsctn> applyPaidOutcome(String orderNo, String compId, String qbangTxnNo,
                                                String approvalNo, String paymentBrand) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty() || !PgVendor.isQbangFamily(txn.get().getVan())) {
            return Optional.empty();
        }
        PgTrnsctn t = txn.get();
        t.setStatus("10");
        t.setPaymentChannel("QR");
        if (qbangTxnNo != null && !qbangTxnNo.isBlank()) {
            t.setChillTransactionId(qbangTxnNo.trim());
            t.setPayNo(qbangTxnNo.trim());
        }
        String appr = approvalNo != null ? approvalNo.trim() : "";
        if (appr.isBlank()) {
            appr = qbangTxnNo != null ? qbangTxnNo.trim() : "";
        }
        if (!appr.isBlank()) {
            t.setApprovalNo(appr.length() > 20 ? appr.substring(0, 20) : appr);
        }
        if (paymentBrand != null && !paymentBrand.isBlank()
                && (t.getCustomerNm() == null || t.getCustomerNm().isBlank())) {
            /* brand 는 고객명이 아님 — skip */
        }
        pgTrnsctnRepository.save(t);
        return Optional.of(t);
    }

    @Transactional
    public Optional<PgTrnsctn> applyFailedOutcome(String orderNo, String compId, String qbangTxnNo) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty() || !PgVendor.isQbangFamily(txn.get().getVan())) {
            return Optional.empty();
        }
        PgTrnsctn t = txn.get();
        t.setStatus("99");
        if (qbangTxnNo != null && !qbangTxnNo.isBlank()) {
            t.setChillTransactionId(qbangTxnNo.trim());
        }
        pgTrnsctnRepository.save(t);
        return Optional.of(t);
    }

    private static String trimTo(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return "";
        }
        return t.length() > max ? t.substring(0, max) : t;
    }
}
