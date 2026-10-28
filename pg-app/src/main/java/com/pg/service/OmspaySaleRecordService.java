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

/** OMSPAY 승인 전후 {@code pg_trnsctn} 대기·확정. */
@Service
public class OmspaySaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(OmspaySaleRecordService.class);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;

    public OmspaySaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                   OrgUnitRepository orgUnitRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String omsOrderCode, String txnOrigin,
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
            PgTrnsctn t = existing.filter(x -> PgVendor.isOmspayFamily(x.getVan())).orElse(null);
            if (t == null) {
                t = new PgTrnsctn();
                t.setTrnId("OM" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
                t.setMerchantId(mid);
                t.setOrderNo(orderNo.trim());
                t.setCreatedAt(LocalDateTime.now());
            }
            t.setVan(PgVendor.OMSPAY);
            t.setStatus("08");
            if (amount != null) {
                t.setAmtKrw(amount);
                t.setTotalAmt(amount);
            }
            if (currency != null && !currency.isBlank()) {
                t.setCurType(currency.trim().toUpperCase());
            }
            /* 고객열 = 구매자(성명). 상품명은 고객명에 넣지 않음. */
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
            if (omsOrderCode != null && !omsOrderCode.isBlank()) {
                t.setChillTransactionId(omsOrderCode.trim());
                t.setPayNo(omsOrderCode.trim());
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
            log.warn("OMSPAY pending txn 실패: {}", e.getMessage());
        }
    }

    @Transactional
    public Optional<PgTrnsctn> applyPaidOutcome(String orderNo, String compId, String omsOrderCode,
                                                String paymentRef) {
        return applyPaidOutcome(orderNo, compId, omsOrderCode, paymentRef, null);
    }

    @Transactional
    public Optional<PgTrnsctn> applyPaidOutcome(String orderNo, String compId, String omsOrderCode,
                                                String paymentRef, String payerName) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty() || !PgVendor.isOmspayFamily(txn.get().getVan())) {
            return Optional.empty();
        }
        PgTrnsctn t = txn.get();
        t.setStatus("10");
        if (omsOrderCode != null && !omsOrderCode.isBlank()) {
            t.setChillTransactionId(omsOrderCode.trim());
            t.setPayNo(omsOrderCode.trim());
        }
        /* approval_no = varchar(20). Beam charge id(ch_…) exceeds 20 — prefer OMS SO code. */
        String approval = firstFitApproval(omsOrderCode, paymentRef);
        if (approval != null && !approval.isBlank()) {
            t.setApprovalNo(approval);
        }
        /* Beam payer_name 은 카드결제에서 null 인 경우가 많음 — 비어 있을 때만 보강 */
        if ((t.getCustomerNm() == null || t.getCustomerNm().isBlank()) && payerName != null) {
            String pn = trimTo(payerName, 200);
            if (!pn.isBlank()) {
                t.setCustomerNm(pn);
            }
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

    @Transactional
    public Optional<PgTrnsctn> applyFailedOutcome(String orderNo, String compId, String omsOrderCode) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty() || !PgVendor.isOmspayFamily(txn.get().getVan())) {
            return Optional.empty();
        }
        PgTrnsctn t = txn.get();
        t.setStatus("99");
        if (omsOrderCode != null && !omsOrderCode.isBlank()) {
            t.setChillTransactionId(omsOrderCode.trim());
        }
        pgTrnsctnRepository.save(t);
        return Optional.of(t);
    }

    /** {@code pg_trnsctn.approval_no} 길이 20에 맞는 승인 참조. */
    static String firstFitApproval(String omsOrderCode, String paymentRef) {
        String so = omsOrderCode != null ? omsOrderCode.trim() : "";
        String ref = paymentRef != null ? paymentRef.trim() : "";
        if (!so.isBlank() && so.length() <= 20) {
            return so;
        }
        if (!ref.isBlank() && ref.length() <= 20) {
            return ref;
        }
        if (!so.isBlank()) {
            return so.length() > 20 ? so.substring(0, 20) : so;
        }
        if (!ref.isBlank()) {
            return ref.substring(0, Math.min(20, ref.length()));
        }
        return "";
    }
}
