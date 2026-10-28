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

/** QXIPAY 승인 전후 {@code pg_trnsctn} 대기·확정. */
@Service
public class QxipaySaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(QxipaySaleRecordService.class);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;

    public QxipaySaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                   OrgUnitRepository orgUnitRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String chargeId, String txnOrigin,
                                     BigDecimal shopperDisplayAmount, String shopperDisplayCurrency) {
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
            if (chargeId != null && !chargeId.isBlank()) {
                t.setChillTransactionId(chargeId.trim());
                t.setPayNo(chargeId.trim());
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
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
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
            t.setPayNo(chargeId.trim());
        }
        if (approvalNo != null && !approvalNo.isBlank()) {
            t.setApprovalNo(approvalNo.trim());
        }
        t.setVan(PgVendor.QXIPAY);
        return Optional.of(pgTrnsctnRepository.save(t));
    }
}
