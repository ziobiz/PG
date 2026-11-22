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

/** PAYSO 승인 전후 {@code pg_trnsctn} 대기·확정. */
@Service
public class PaysoSaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(PaysoSaleRecordService.class);

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final TransactionReceiptEmailService transactionReceiptEmailService;

    public PaysoSaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   @Lazy TransactionReceiptEmailService transactionReceiptEmailService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String paysoTxnId, String txnOrigin,
                                     BigDecimal shopperDisplayAmount, String shopperDisplayCurrency,
                                     String buyerName, String buyerEmail, String buyerTel) {
        recordOrTouchPending(orgUnitId, orderNo, amount, currency, productName, paysoTxnId, txnOrigin,
                shopperDisplayAmount, shopperDisplayCurrency, buyerName, buyerEmail, buyerTel, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     String productName, String paysoTxnId, String txnOrigin,
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
            PgTrnsctn t = existing.filter(x -> PgVendor.isPaysoFamily(x.getVan())).orElse(null);
            if (t == null) {
                t = new PgTrnsctn();
                t.setTrnId("PS" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
                t.setMerchantId(mid);
                t.setOrderNo(orderNo.trim());
                t.setCreatedAt(LocalDateTime.now());
            }
            t.setVan(PgVendor.PAYSO);
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
            if (paysoTxnId != null && !paysoTxnId.isBlank()) {
                String id = paysoTxnId.trim();
                /*
                 * pay_no = ICOPAY Reference 만 (숫자 12자리, 전 가맹 동일 형식).
                 * hex transactionId·가맹 order 는 pay_no 에 넣지 않음.
                 */
                if (isPaysoReferenceNo(id)) {
                    t.setPayNo(id);
                    if (t.getApprovalNo() == null || t.getApprovalNo().isBlank()) {
                        t.setApprovalNo(id);
                    }
                } else {
                    t.setChillTransactionId(id.length() > 64 ? id.substring(0, 64) : id);
                }
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
            log.warn("PAYSO pending txn 실패: {}", e.getMessage());
        }
    }

    /** 구매자 결과 폴링용 — 가맹 MID + 주문번호로 PAYSO 거래 조회. */
    public Optional<PgTrnsctn> findByMerchantAndOrder(String merchantId, String orderNo) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, merchantId.trim(), orderNo.trim());
        if (txn.isEmpty()) {
            return Optional.empty();
        }
        if (!PgVendor.isPaysoFamily(txn.get().getVan()) && !acceptPaysoLookup(txn.get())) {
            return Optional.empty();
        }
        return txn;
    }

    @Transactional
    public Optional<PgTrnsctn> applyPaidOutcome(String orderNo, String compId, String paysoTxnId,
                                                String paymentRef) {
        return applyPaidOutcome(orderNo, compId, paysoTxnId, paymentRef, null);
    }

    /**
     * Waylar order-paid 웹훅에서 대기/승인 거래 역추적.
     * {@code so} → chill_transaction_id / pay_no / approval_no, {@code note}·orderNo → 주문번호.
     * PAYSO van 우선, SO 코드로 유일하게 잡히면 van 이 비어 있어도 허용(헤더 Comp-Id 용).
     */
    public Optional<PgTrnsctn> resolveTxnForWebhook(String so, String note, String orderNoHint, String ref1) {
        String oms = so != null ? so.trim() : "";
        if (!oms.isBlank()) {
            Optional<PgTrnsctn> byChill = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(oms);
            if (byChill.isPresent() && acceptPaysoLookup(byChill.get())) {
                return byChill;
            }
            Optional<PgTrnsctn> byPay = pgTrnsctnRepository.findFirstByPayNoOrderByCreatedAtDesc(oms);
            if (byPay.isPresent() && acceptPaysoLookup(byPay.get())) {
                return byPay;
            }
            Optional<PgTrnsctn> byAppr = pgTrnsctnRepository.findFirstByApprovalNoOrderByCreatedAtDesc(oms);
            if (byAppr.isPresent() && acceptPaysoLookup(byAppr.get())) {
                return byAppr;
            }
        }
        String orderNo = firstNonBlank(note, orderNoHint);
        if (!orderNo.isBlank()) {
            Optional<PgTrnsctn> byOrder = pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(orderNo);
            if (byOrder.isPresent() && acceptPaysoLookup(byOrder.get())) {
                return byOrder;
            }
            /* 동일 주문 다건이면 PAYSO van 우선 */
            for (PgTrnsctn t : pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(orderNo)) {
                if (PgVendor.isPaysoFamily(t.getVan())) {
                    return Optional.of(t);
                }
            }
        }
        String ref = ref1 != null ? ref1.trim() : "";
        /* Beam charge id(ch_…)는 역추적 키에서 제외 — SO-/note/orderNo 우선 (V5.92 동등) */
        if (!ref.isBlank() && ref.length() <= 64 && !ref.regionMatches(true, 0, "ch_", 0, 3)) {
            Optional<PgTrnsctn> byRefPay = pgTrnsctnRepository.findFirstByPayNoOrderByCreatedAtDesc(ref);
            if (byRefPay.isPresent() && acceptPaysoLookup(byRefPay.get())) {
                return byRefPay;
            }
        }
        return Optional.empty();
    }

    private static boolean acceptPaysoLookup(PgTrnsctn t) {
        if (t == null) {
            return false;
        }
        if (PgVendor.isPaysoFamily(t.getVan())) {
            return true;
        }
        /* __KEEP__ 로 직접 매칭된 행: van 미기입·레거시 값도 Comp-Id 반환용으로 허용 */
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
    public Optional<PgTrnsctn> applyPaidOutcome(String orderNo, String compId, String paysoTxnId,
                                                String paymentRef, String payerName) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty()) {
            return Optional.empty();
        }
        if (!PgVendor.isPaysoFamily(txn.get().getVan()) && !acceptPaysoLookup(txn.get())) {
            return Optional.empty();
        }
        /* van 비어 있으면 PAYSO 로 보정 */
        if (!PgVendor.isPaysoFamily(txn.get().getVan())) {
            txn.get().setVan(PgVendor.PAYSO);
        }
        PgTrnsctn t = txn.get();
        /* 가맹 orderNo 를 PG 승인키로 쓰면 미결제인데 성공으로 찍힘 — 거부 */
        if (isMerchantOrderUsedAsPaysoKey(orderNo.trim(), paysoTxnId, paymentRef)) {
            log.warn("PAYSO applyPaid 거부 — PG키=가맹주문번호 mid={} order={} txnId={} ref={}",
                    compId, orderNo, paysoTxnId, paymentRef);
            return Optional.empty();
        }
        t.setStatus("10");
        if (t.getPaidAt() == null) {
            t.setPaidAt(LocalDateTime.now());
        }
        /*
         * pay_no = ICOPAY→PAYSO Reference(숫자 12자리·전 가맹 동일).
         * chill = PAYSO transactionId/Order No. hex·가맹 order 는 pay_no 금지.
         */
        String ref = paymentRef != null ? paymentRef.trim() : "";
        String pgTxn = paysoTxnId != null ? paysoTxnId.trim() : "";
        String payNow = t.getPayNo() != null ? t.getPayNo().trim() : "";
        if (!isPaysoReferenceNo(payNow) || payNow.equalsIgnoreCase(orderNo.trim())) {
            if (isPaysoReferenceNo(ref) && !ref.equalsIgnoreCase(orderNo.trim())) {
                t.setPayNo(ref);
            } else if (isPaysoReferenceNo(pgTxn) && !pgTxn.equalsIgnoreCase(orderNo.trim())) {
                t.setPayNo(pgTxn);
            }
        } else if (isPaysoReferenceNo(ref) && !ref.equals(payNow) && !ref.equalsIgnoreCase(orderNo.trim())) {
            /* 웹훅 ref 가 정본 — hex 로 덮인 pay_no 복구 */
            if (!isPaysoReferenceNo(payNow)) {
                t.setPayNo(ref);
            }
        }
        if (!pgTxn.isBlank() && !pgTxn.equalsIgnoreCase(orderNo.trim()) && !isPaysoReferenceNo(pgTxn)) {
            t.setChillTransactionId(pgTxn.length() > 64 ? pgTxn.substring(0, 64) : pgTxn);
        } else if (!pgTxn.isBlank() && isPaysoReferenceNo(pgTxn)
                && (t.getChillTransactionId() == null || t.getChillTransactionId().isBlank())) {
            /* ref 만 있고 PG txn 이 아직 없으면 chill 비워 둠 */
        }
        String join = firstPaysoReference(t.getPayNo(), ref, t.getApprovalNo(), pgTxn);
        if (!join.isBlank()) {
            if (!isPaysoReferenceNo(t.getPayNo() != null ? t.getPayNo().trim() : "")) {
                t.setPayNo(join);
            }
            t.setApprovalNo(join.length() > 20 ? join.substring(0, 20) : join);
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
    public Optional<PgTrnsctn> applyFailedOutcome(String orderNo, String compId, String paysoTxnId) {
        return applyFailedOutcome(orderNo, compId, paysoTxnId,
                "PAYSO_PENDING_EXPIRED",
                PendingTxnExpireService.expireReasonMessage(30));
    }

    @Transactional
    public Optional<PgTrnsctn> applyFailedOutcome(String orderNo, String compId, String paysoTxnId,
                                                  String reasonCode, String reasonText) {
        return applyFailedOutcome(orderNo, compId, paysoTxnId, reasonCode, reasonText, false);
    }

    /**
     * @param forceCorrectFalsePaid true 이면 「승인키=가맹주문번호」인 오성공(10)을 실패(99)로 정정 허용
     */
    @Transactional
    public Optional<PgTrnsctn> applyFailedOutcome(String orderNo, String compId, String paysoTxnId,
                                                  String reasonCode, String reasonText,
                                                  boolean forceCorrectFalsePaid) {
        if (orderNo == null || orderNo.isBlank() || compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findByMerchantOrderOrPayNo(
                pgTrnsctnRepository, compId.trim(), orderNo.trim());
        if (txn.isEmpty()) {
            txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                    pgTrnsctnRepository, compId.trim(), orderNo.trim());
        }
        if (txn.isEmpty() || !acceptPaysoLookup(txn.get())) {
            return Optional.empty();
        }
        PgTrnsctn t = txn.get();
        if (!PgVendor.isPaysoFamily(t.getVan())) {
            t.setVan(PgVendor.PAYSO);
        }
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        if (!"08".equals(st)) {
            if ("99".equals(st)) {
                hookReceiptEmail(t);
                return Optional.of(t);
            }
            if ("10".equals(st)) {
                if (!forceCorrectFalsePaid || !looksLikeFalsePaidRecord(t)) {
                    hookReceiptEmail(t);
                    return Optional.of(t);
                }
                log.warn("PAYSO 오성공 정정 10→99 mid={} order={} trnId={} chill={}",
                        compId, orderNo, t.getTrnId(), t.getChillTransactionId());
            } else {
                return Optional.empty();
            }
        }
        String prev = t.getStatus();
        t.setStatus("99");
        t.setPaidAt(null);
        t.setMwOutboundLastSentStatus(null);
        if (paysoTxnId != null && !paysoTxnId.isBlank()
                && !paysoTxnId.trim().equalsIgnoreCase(orderNo.trim())) {
            t.setChillTransactionId(paysoTxnId.trim());
        }
        com.pg.util.TxnOutcomeReasonApplier.apply(t, prev, "99",
                reasonText != null && !reasonText.isBlank()
                        ? reasonText
                        : "Payment expired (no completion webhook)",
                reasonCode != null && !reasonCode.isBlank() ? reasonCode : "PAYSO_PENDING_EXPIRED",
                com.pg.util.TxnOutcomeReasonApplier.SOURCE_ICOPAY);
        pgTrnsctnRepository.save(t);
        hookReceiptEmail(t);
        return Optional.of(t);
    }

    /** chill/pay_no/approval 이 가맹 orderNo 와 같으면 PAYSO 미결제 오성공으로 본다. */
    static boolean looksLikeFalsePaidRecord(PgTrnsctn t) {
        if (t == null || t.getOrderNo() == null || t.getOrderNo().isBlank()) {
            return false;
        }
        String on = t.getOrderNo().trim();
        return eqIgnore(on, t.getChillTransactionId())
                || eqIgnore(on, t.getPayNo())
                || (t.getApprovalNo() != null && !t.getApprovalNo().isBlank()
                && on.startsWith(t.getApprovalNo().trim()));
    }

    /** 웹훅/복귀 키가 가맹 주문번호만이면 승인으로 쓰지 않음. */
    static boolean isMerchantOrderUsedAsPaysoKey(String orderNo, String paysoTxnId, String paymentRef) {
        if (orderNo == null || orderNo.isBlank()) {
            return false;
        }
        String on = orderNo.trim();
        String a = paysoTxnId != null ? paysoTxnId.trim() : "";
        String b = paymentRef != null ? paymentRef.trim() : "";
        boolean aIsOrder = !a.isBlank() && a.equalsIgnoreCase(on);
        boolean bIsOrder = !b.isBlank() && b.equalsIgnoreCase(on);
        if (aIsOrder && (b.isBlank() || bIsOrder)) {
            return true;
        }
        if (bIsOrder && a.isBlank()) {
            return true;
        }
        /* 둘 다 주문번호이거나, 유일한 키가 주문번호 */
        return aIsOrder && bIsOrder;
    }

    private static boolean eqIgnore(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b.trim());
    }

    private void hookReceiptEmail(PgTrnsctn t) {
        if (t == null) {
            return;
        }
        try {
            transactionReceiptEmailService.scheduleIfDue(t);
        } catch (Exception ex) {
            log.warn("PAYSO 거래 영수증 메일 연동 실패 trnId={}: {}", t.getTrnId(), ex.getMessage());
        }
    }

    /**
     * NOTI 레거시 PAYSO lookup — 전 PG 공통 {@link PgNotiOrderLookupService} 와 동일 키로 해석.
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
        String so = q.toUpperCase().startsWith("__KEEP__") ? q : "";
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

    /** PAYSO ICOPAY Reference — 전 가맹 동일: 숫자 정확히 12자리. */
    public static boolean isPaysoReferenceNo(String value) {
        return value != null && value.trim().matches("^[0-9]{12}$");
    }

    static String firstPaysoReference(String... cands) {
        if (cands == null) {
            return "";
        }
        for (String c : cands) {
            if (isPaysoReferenceNo(c)) {
                return c.trim();
            }
        }
        return "";
    }

    /** {@code pg_trnsctn.approval_no} — Reference(12자리)만. */
    static String firstFitApproval(String payNo, String paymentRef, String paysoTxnId) {
        return firstPaysoReference(payNo, paymentRef, paysoTxnId);
    }

    /** 레거시 시그니처 — Reference 우선. */
    static String firstFitApproval(String paysoTxnId, String paymentRef) {
        return firstFitApproval(null, paymentRef, paysoTxnId);
    }

    /**
     * 통합내역(Inquiry) 대사 — 마스킹 카드·PAYSO 수수료·Reference({@code pay_no}) 보강.
     */
    @Transactional
    public void applyInquiryEnrichment(PgTrnsctn txn, String referenceNo, String paysoOrderNo,
                                       String paysoTxnId, String cardPanDisplay, BigDecimal feeAmt) {
        if (txn == null) {
            return;
        }
        boolean dirty = false;
        String order = txn.getOrderNo() != null ? txn.getOrderNo().trim() : "";
        String ref = referenceNo != null ? referenceNo.trim() : "";
        String join = firstPaysoReference(ref, txn.getPayNo(), txn.getApprovalNo(), paysoTxnId);
        if (!join.isBlank() && !join.equalsIgnoreCase(order)) {
            String pay = txn.getPayNo() != null ? txn.getPayNo().trim() : "";
            if (!isPaysoReferenceNo(pay) || pay.equalsIgnoreCase(order) || !pay.equals(join)) {
                txn.setPayNo(join);
                dirty = true;
            }
            String appr = txn.getApprovalNo() != null ? txn.getApprovalNo().trim() : "";
            if (!join.equals(appr)) {
                txn.setApprovalNo(join.length() > 20 ? join.substring(0, 20) : join);
                dirty = true;
            }
        }
        String pgOrd = paysoOrderNo != null ? paysoOrderNo.trim() : "";
        String txnId = paysoTxnId != null ? paysoTxnId.trim() : "";
        String chillCand = !pgOrd.isBlank() ? pgOrd : txnId;
        if (!chillCand.isBlank() && !chillCand.equalsIgnoreCase(order)) {
            String pay = txn.getPayNo() != null ? txn.getPayNo().trim() : "";
            if (!chillCand.equals(pay)
                    && (txn.getChillTransactionId() == null || txn.getChillTransactionId().isBlank()
                    || txn.getChillTransactionId().trim().equals(pay)
                    || txn.getChillTransactionId().trim().equalsIgnoreCase(order))) {
                txn.setChillTransactionId(chillCand.length() > 64 ? chillCand.substring(0, 64) : chillCand);
                dirty = true;
            }
        }
        if (cardPanDisplay != null && !cardPanDisplay.isBlank()) {
            String mask = normalizePaysoCardMask(cardPanDisplay);
            if (!mask.isBlank() && (txn.getCardPanDisplay() == null || txn.getCardPanDisplay().isBlank())) {
                txn.setCardPanDisplay(mask.length() > 32 ? mask.substring(0, 32) : mask);
                dirty = true;
            }
        }
        if (feeAmt != null && feeAmt.compareTo(BigDecimal.ZERO) >= 0 && txn.getChillFeeAmt() == null) {
            txn.setChillFeeAmt(feeAmt);
            dirty = true;
        }
        if (dirty) {
            pgTrnsctnRepository.save(txn);
        }
    }

    /** PAYSO 포털형 {@code 414520-####-8306} → {@code 414520***8306}. */
    public static String normalizePaysoCardMask(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim();
        String digitsAndStar = s.replaceAll("[^0-9*#xX]", "");
        digitsAndStar = digitsAndStar.replace('#', '*').replace('x', '*').replace('X', '*');
        if (digitsAndStar.matches("^[0-9]{6}\\*+[0-9]{4}$")) {
            String bin = digitsAndStar.substring(0, 6);
            String last4 = digitsAndStar.substring(digitsAndStar.length() - 4);
            return bin + "***" + last4;
        }
        String digits = digitsAndStar.replace("*", "");
        if (digits.length() >= 10) {
            return com.pg.util.PayCardPanHashUtil.maskForDisplay(digits);
        }
        if (digitsAndStar.length() >= 8) {
            return digitsAndStar.length() > 32 ? digitsAndStar.substring(0, 32) : digitsAndStar;
        }
        return "";
    }
}

