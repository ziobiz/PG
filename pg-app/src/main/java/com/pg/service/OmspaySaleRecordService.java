package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.receipt.TransactionReceiptEmailService;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
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

/** OMSPAY 승인 전후 {@code pg_trnsctn} 대기·확정. */
@Service
public class OmspaySaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(OmspaySaleRecordService.class);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final TransactionReceiptEmailService transactionReceiptEmailService;

    public OmspaySaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   @Lazy TransactionReceiptEmailService transactionReceiptEmailService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String omsOrderCode, String txnOrigin,
                                     BigDecimal shopperDisplayAmount, String shopperDisplayCurrency,
                                     String buyerName, String buyerEmail, String buyerTel) {
        recordOrTouchPending(orgUnitId, orderNo, amount, currency, productName, omsOrderCode, txnOrigin,
                shopperDisplayAmount, shopperDisplayCurrency, buyerName, buyerEmail, buyerTel, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String omsOrderCode, String txnOrigin,
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
            if (saleBody != null && !saleBody.isEmpty()) {
                UrlPaySaleTxnFieldApplier.apply(t, saleBody);
                UrlPaySaleTxnFieldApplier.ensureUrlWebDevice(t, txnOrigin);
            } else {
                UrlPaySaleTxnFieldApplier.ensureUrlWebDevice(t, txnOrigin);
            }
            pgTrnsctnRepository.save(t);
        } catch (Exception e) {
            log.warn("OMSPAY pending txn 실패: {}", e.getMessage());
        }
    }

    /** 구매자 결과 폴링용 — 가맹 MID + 주문번호로 OMSPAY 거래 조회. */
    public Optional<PgTrnsctn> findByMerchantAndOrder(String merchantId, String orderNo) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, merchantId.trim(), orderNo.trim());
        if (txn.isEmpty()) {
            return Optional.empty();
        }
        if (!PgVendor.isOmspayFamily(txn.get().getVan()) && !acceptOmspayLookup(txn.get())) {
            return Optional.empty();
        }
        return txn;
    }

    @Transactional
    public Optional<PgTrnsctn> applyPaidOutcome(String orderNo, String compId, String omsOrderCode,
                                                String paymentRef) {
        return applyPaidOutcome(orderNo, compId, omsOrderCode, paymentRef, null);
    }

    /**
     * Waylar order-paid 웹훅에서 대기/승인 거래 역추적.
     * {@code so} → chill_transaction_id / pay_no / approval_no, {@code note}·orderNo → 주문번호.
     * OMSPAY van 우선, SO 코드로 유일하게 잡히면 van 이 비어 있어도 허용(헤더 Comp-Id 용).
     */
    public Optional<PgTrnsctn> resolveTxnForWebhook(String so, String note, String orderNoHint, String ref1) {
        String oms = so != null ? so.trim() : "";
        if (!oms.isBlank()) {
            Optional<PgTrnsctn> byChill = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(oms);
            if (byChill.isPresent() && acceptOmspayLookup(byChill.get())) {
                return byChill;
            }
            Optional<PgTrnsctn> byPay = pgTrnsctnRepository.findFirstByPayNoOrderByCreatedAtDesc(oms);
            if (byPay.isPresent() && acceptOmspayLookup(byPay.get())) {
                return byPay;
            }
            Optional<PgTrnsctn> byAppr = pgTrnsctnRepository.findFirstByApprovalNoOrderByCreatedAtDesc(oms);
            if (byAppr.isPresent() && acceptOmspayLookup(byAppr.get())) {
                return byAppr;
            }
        }
        String orderNo = firstNonBlank(note, orderNoHint);
        if (!orderNo.isBlank()) {
            Optional<PgTrnsctn> byOrder = pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(orderNo);
            if (byOrder.isPresent() && acceptOmspayLookup(byOrder.get())) {
                return byOrder;
            }
            /* 동일 주문 다건이면 OMSPAY van 우선 */
            for (PgTrnsctn t : pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(orderNo)) {
                if (PgVendor.isOmspayFamily(t.getVan())) {
                    return Optional.of(t);
                }
            }
        }
        String ref = ref1 != null ? ref1.trim() : "";
        if (!ref.isBlank() && ref.length() <= 64) {
            Optional<PgTrnsctn> byRefPay = pgTrnsctnRepository.findFirstByPayNoOrderByCreatedAtDesc(ref);
            if (byRefPay.isPresent() && acceptOmspayLookup(byRefPay.get())) {
                return byRefPay;
            }
        }
        return Optional.empty();
    }

    private static boolean acceptOmspayLookup(PgTrnsctn t) {
        if (t == null) {
            return false;
        }
        if (PgVendor.isOmspayFamily(t.getVan())) {
            return true;
        }
        /* SO- 로 직접 매칭된 행: van 미기입·레거시 값도 Comp-Id 반환용으로 허용 */
        String van = t.getVan() != null ? t.getVan().trim() : "";
        return van.isBlank() || "ICOPAY".equalsIgnoreCase(van);
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

    @Transactional
    public Optional<PgTrnsctn> applyPaidOutcome(String orderNo, String compId, String omsOrderCode,
                                                String paymentRef, String payerName) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty()) {
            return Optional.empty();
        }
        if (!PgVendor.isOmspayFamily(txn.get().getVan()) && !acceptOmspayLookup(txn.get())) {
            return Optional.empty();
        }
        /* van 비어 있으면 OMSPAY 로 보정 */
        if (!PgVendor.isOmspayFamily(txn.get().getVan())) {
            txn.get().setVan(PgVendor.OMSPAY);
        }
        PgTrnsctn t = txn.get();
        t.setStatus("10");
        if (t.getPaidAt() == null) {
            t.setPaidAt(LocalDateTime.now());
        }
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
        hookReceiptEmail(t);
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
        return applyFailedOutcome(orderNo, compId, omsOrderCode,
                "OMSPAY_PENDING_EXPIRED", "Payment expired (no completion webhook)");
    }

    @Transactional
    public Optional<PgTrnsctn> applyFailedOutcome(String orderNo, String compId, String omsOrderCode,
                                                  String reasonCode, String reasonText) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty() || !PgVendor.isOmspayFamily(txn.get().getVan())) {
            return Optional.empty();
        }
        PgTrnsctn t = txn.get();
        if (!"08".equals(t.getStatus() != null ? t.getStatus().trim() : "")) {
            /* 이미 성공·실패면 덮지 않음 — 실패 건은 영수증 미발송분 보완 */
            if ("10".equals(t.getStatus() != null ? t.getStatus().trim() : "")
                    || "99".equals(t.getStatus() != null ? t.getStatus().trim() : "")) {
                hookReceiptEmail(t);
                return Optional.of(t);
            }
        }
        String prev = t.getStatus();
        t.setStatus("99");
        if (omsOrderCode != null && !omsOrderCode.isBlank()) {
            t.setChillTransactionId(omsOrderCode.trim());
        }
        com.pg.util.TxnOutcomeReasonApplier.apply(t, prev, "99",
                reasonText != null && !reasonText.isBlank()
                        ? reasonText
                        : "Payment expired (no completion webhook)",
                reasonCode != null && !reasonCode.isBlank() ? reasonCode : "OMSPAY_PENDING_EXPIRED",
                com.pg.util.TxnOutcomeReasonApplier.SOURCE_ICOPAY);
        pgTrnsctnRepository.save(t);
        hookReceiptEmail(t);
        return Optional.of(t);
    }

    private void hookReceiptEmail(PgTrnsctn t) {
        if (t == null) {
            return;
        }
        try {
            transactionReceiptEmailService.scheduleIfDue(t);
        } catch (Exception ex) {
            log.warn("OMSPAY 거래 영수증 메일 연동 실패 trnId={}: {}", t.getTrnId(), ex.getMessage());
        }
    }

    /**
     * NOTI 레거시 OMSPAY lookup — 전 PG 공통 {@link PgNotiOrderLookupService} 와 동일 키로 해석.
     */
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
        String so = q.toUpperCase().startsWith("SO-") ? q : "";
        Optional<PgTrnsctn> matched = resolveTxnForWebhook(so, q, q, q);
        if (matched.isEmpty() && so.isBlank()) {
            matched = resolveTxnForWebhook(q, q, q, q);
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
        String orderNo = t.getOrderNo() != null ? t.getOrderNo().trim() : "";
        String omsSo = firstNonBlank(t.getChillTransactionId(),
                firstNonBlank(t.getPayNo(), t.getApprovalNo()));
        String merchantName = "";
        if (!compId.isBlank()) {
            merchantName = orgUnitRepository.findByCode(compId)
                    .or(() -> orgUnitRepository.findByCodeIgnoreCase(compId))
                    .map(OrgUnit::getName)
                    .orElse("");
        }
        out.put("success", true);
        out.put("compId", compId);
        out.put("CompId", compId);
        out.put("merchantId", compId);
        out.put("merchantName", merchantName != null ? merchantName : "");
        out.put("orderNo", orderNo);
        out.put("omsSo", omsSo);
        out.put("order", q);
        out.put("trnId", t.getTrnId());
        out.put("status", t.getStatus());
        out.put("van", t.getVan());
        return out;
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
