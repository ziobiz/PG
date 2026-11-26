package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.receipt.TransactionReceiptEmailService;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PgTrnsctnOrderLookup;
import com.pg.util.RouteNoDisplayUtil;
import com.pg.util.UrlPaySaleTxnFieldApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * KSNET(KSPAY 해외 USD) — requestPay 직전 {@code pg_trnsctn} 대기(08), recv_post.jsp 승인 성공 시 확정(10).
 * 인증(payKey 발급)만으로는 결제 완료로 처리하지 않는다. ICOPAY DB 에는 실제 USD 금액을 둔다(KSNET 전송값은 ×1000).
 */
@Service
public class KsnetSaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(KsnetSaleRecordService.class);

    public static final String SERVICE_TYPE = "URL_KSNET";
    private static final String ORIGIN_URL = "URL";
    private static final String ORIGIN_MERCHANT_API = "MERCHANT_API";
    private static final String ST_PAID = "10";
    private static final String ST_PENDING = "08";
    private static final String ST_FAIL = "99";
    private static final String ST_CANCEL = "20";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final TransactionReceiptEmailService transactionReceiptEmailService;

    public KsnetSaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                  OrgUnitRepository orgUnitRepository,
                                  HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                  @Lazy TransactionReceiptEmailService transactionReceiptEmailService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
    }

    @Transactional
    public void recordOrTouchPending(Long orgUnitId, String orderNo, BigDecimal amount, String currency,
                                     Integer routeNo, String productName, String txnOrigin,
                                     BigDecimal shopperDisplayAmount, String shopperDisplayCurrency,
                                     String buyerName, String buyerEmail, String buyerTel,
                                     Map<String, Object> saleBody) {
        try {
            if (orgUnitId == null || orderNo == null || orderNo.isBlank()) {
                return;
            }
            Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
            if (ouOpt.isEmpty() || ouOpt.get().getCode() == null || ouOpt.get().getCode().isBlank()) {
                return;
            }
            String mid = truncate(ouOpt.get().getCode().trim(), 20);
            String on = truncate(orderNo.trim(), 64);
            Optional<PgTrnsctn> existing = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                    pgTrnsctnRepository, mid, on);
            PgTrnsctn t = existing.filter(x -> PgVendor.isKsnetFamily(x.getVan())).orElse(null);
            if (t == null) {
                t = new PgTrnsctn();
                t.setTrnId(newTrnId());
                t.setMerchantId(mid);
                t.setServiceType(SERVICE_TYPE);
                t.setOrigin(resolveOrigin(txnOrigin));
                t.setCreatedAt(LocalDateTime.now());
            } else if (ST_PAID.equals(trim(t.getStatus()))) {
                /* 이미 승인 완료 — 대기로 되돌리지 않음 */
                return;
            }
            t.setVan(PgVendor.KSNET);
            t.setStatus(ST_PENDING);
            t.setOrderNo(on);
            if (t.getPayNo() == null || t.getPayNo().isBlank()
                    || !com.pg.util.IcopayPgOrderRef.looksLikeIcopayRef(t.getPayNo(), com.pg.util.IcopayPgOrderRef.Profile.DIGITS_12)) {
                /* saleBody.icopayPgOrderRef(숫자 12)가 있으면 UrlPaySaleTxnFieldApplier 가 pay_no·approval_no 세팅 */
                String fromBody = saleBody != null ? com.pg.util.IcopayPgOrderRef.fromSaleBody(saleBody) : "";
                if (com.pg.util.IcopayPgOrderRef.looksLikeIcopayRef(fromBody, com.pg.util.IcopayPgOrderRef.Profile.DIGITS_12)) {
                    t.setPayNo(fromBody);
                    if (t.getApprovalNo() == null || t.getApprovalNo().isBlank()) {
                        t.setApprovalNo(fromBody);
                    }
                } else {
                    t.setPayNo(on.length() > 50 ? on.substring(0, 50) : on);
                }
            }
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                t.setAmtKrw(amount);
                t.setTotalAmt(amount);
            }
            String cur = currency != null && !currency.isBlank()
                    ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
            t.setCurType(cur.length() > 3 ? cur.substring(0, 3) : cur);
            String routeStored = RouteNoDisplayUtil.normalizeForStorage(routeNo != null ? routeNo : 0);
            if (routeStored != null) {
                t.setRouteNo(routeStored);
            }
            String nm = truncate(buyerName, 200);
            if (nm != null && !nm.isBlank()) {
                t.setCustomerNm(nm.trim());
            }
            String email = truncate(buyerEmail, 100);
            if (email != null && !email.isBlank()) {
                t.setCustomerId(email.trim());
            }
            if (t.getCustomerId() == null || t.getCustomerId().isBlank()) {
                t.setCustomerId("guest");
            }
            String tel = truncate(buyerTel, 50);
            if (tel != null && !tel.isBlank()) {
                t.setCustomerTel(tel.trim());
            }
            String desc = "KSNET_URL";
            if (productName != null && !productName.isBlank()) {
                desc = desc + " " + productName.trim();
            }
            t.setChillPaymentStatus(truncate(desc, 50));
            t.setPaymentChannel("CARD");
            if (shopperDisplayAmount != null && shopperDisplayAmount.compareTo(BigDecimal.ZERO) > 0
                    && shopperDisplayCurrency != null && !shopperDisplayCurrency.isBlank()) {
                t.setDisplayAmt(shopperDisplayAmount);
                String dc = shopperDisplayCurrency.trim().toUpperCase(Locale.ROOT);
                t.setDisplayCurType(dc.length() > 3 ? dc.substring(0, 3) : dc);
            }
            if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
                t.setSettledYn("N");
            }
            if (saleBody != null && !saleBody.isEmpty()) {
                UrlPaySaleTxnFieldApplier.apply(t, saleBody);
            }
            UrlPaySaleTxnFieldApplier.ensureUrlWebDevice(t, txnOrigin);
            pgTrnsctnRepository.save(t);
        } catch (Exception e) {
            log.warn("KSNET pending txn 실패: {}", e.getMessage());
        }
    }

    /** 가맹 MID + 주문번호로 KSNET 거래 조회. */
    @Transactional(readOnly = true)
    public Optional<PgTrnsctn> findByMerchantAndOrder(String merchantId, String orderNo) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        /* orderNo = 가맹 order_no 또는 KSNET orderNumb(ICOPAY ref = pay_no) */
        return PgTrnsctnOrderLookup.findByMerchantOrderOrPayNo(
                        pgTrnsctnRepository, merchantId.trim(), orderNo.trim())
                .filter(x -> PgVendor.isKsnetFamily(x.getVan()));
    }

    /** 가맹 미상(모바일 복귀 등)일 때 주문번호로 KSNET 거래 역추적. */
    @Transactional(readOnly = true)
    public Optional<PgTrnsctn> findAnyByOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        String on = orderNo.trim();
        for (PgTrnsctn t : pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(on)) {
            if (PgVendor.isKsnetFamily(t.getVan())) {
                return Optional.of(t);
            }
        }
        /* KSNET orderNumb = ICOPAY ref → pay_no */
        return PgTrnsctnOrderLookup.findByOrderNoOrPayNo(pgTrnsctnRepository, on)
                .filter(t -> PgVendor.isKsnetFamily(t.getVan()));
    }

    /**
     * recv_post.jsp 승인 결과 반영. 승인 성공만 10, 실패는 99.
     * 이미 10 이면 그대로 반환(멱등).
     */
    @Transactional
    public Optional<PgTrnsctn> applyApproveOutcome(String merchantId, String orderNo, boolean paid,
                                                   String tid, String authNo, String msg, String maskedCard,
                                                   String reasonCode) {
        return applyApproveOutcome(merchantId, orderNo, paid, tid, authNo, msg, maskedCard, reasonCode, null);
    }

    public Optional<PgTrnsctn> applyApproveOutcome(String merchantId, String orderNo, boolean paid,
                                                   String tid, String authNo, String msg, String maskedCard,
                                                   String reasonCode, LocalDateTime tradeAt) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        try {
            Optional<PgTrnsctn> ex = findByMerchantAndOrder(merchantId, orderNo);
            if (ex.isEmpty()) {
                return Optional.empty();
            }
            PgTrnsctn t = ex.get();
            String prev = trim(t.getStatus());
            if (ST_PAID.equals(prev)) {
                /* 이미 승인이어도 마스킹 카드·tid·승인번호(12자리 ref)·결제시각 보강 */
                boolean changed = fillMaskedCardIfEmpty(t, maskedCard);
                if (tid != null && !tid.isBlank()
                        && (t.getChillTransactionId() == null || t.getChillTransactionId().isBlank())) {
                    t.setChillTransactionId(truncate(tid.trim(), 64));
                    changed = true;
                }
                if (tradeAt != null && t.getPaidAt() == null) {
                    t.setPaidAt(tradeAt);
                    changed = true;
                }
                String payRefPaid = t.getPayNo() != null ? t.getPayNo().trim() : "";
                if (com.pg.util.IcopayPgOrderRef.looksLikeIcopayRef(payRefPaid, com.pg.util.IcopayPgOrderRef.Profile.DIGITS_12)
                        && (t.getApprovalNo() == null || t.getApprovalNo().isBlank()
                        || !com.pg.util.IcopayPgOrderRef.looksLikeIcopayRef(t.getApprovalNo(), com.pg.util.IcopayPgOrderRef.Profile.DIGITS_12))) {
                    t.setApprovalNo(payRefPaid);
                    changed = true;
                }
                if (changed) {
                    pgTrnsctnRepository.save(t);
                }
                return Optional.of(t);
            }
            if (tid != null && !tid.isBlank()) {
                t.setChillTransactionId(truncate(tid.trim(), 64));
            }
            /* 결제내역 승인번호 = ICOPAY→KSNET orderNumb(숫자 12). KSNET tid/authNo 로 덮지 않음(tid는 chill_transaction_id). */
            String payRef = t.getPayNo() != null ? t.getPayNo().trim() : "";
            if (com.pg.util.IcopayPgOrderRef.looksLikeIcopayRef(payRef, com.pg.util.IcopayPgOrderRef.Profile.DIGITS_12)) {
                t.setApprovalNo(payRef);
            } else if (authNo != null && !authNo.isBlank()) {
                t.setApprovalNo(truncate(authNo.trim(), 20));
            }
            fillMaskedCardIfEmpty(t, maskedCard);
            if (paid) {
                t.setStatus(ST_PAID);
                ZoneId wall = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
                t.setPaidAt(tradeAt != null ? tradeAt : LocalDateTime.now(wall));
                t.setChillPaymentStatus(truncate(msg != null && !msg.isBlank() ? msg.trim() : "Success", 50));
            } else {
                t.setStatus(ST_FAIL);
                t.setPaidAt(null);
                String reason = msg != null && !msg.isBlank() ? msg.trim() : "KSNET approve failed";
                t.setChillPaymentStatus(truncate(reason, 50));
                com.pg.util.TxnOutcomeReasonApplier.apply(t, prev, ST_FAIL, reason,
                        reasonCode != null && !reasonCode.isBlank() ? reasonCode : "KSNET_APPROVE_FAILED",
                        com.pg.util.TxnOutcomeReasonApplier.SOURCE_ICOPAY);
            }
            pgTrnsctnRepository.save(t);
            if (paid) {
                hookReceiptEmail(t);
            }
            return Optional.of(t);
        } catch (Exception e) {
            log.warn("KSNET 승인 결과 반영 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** 대기(08) 장기 미완료 만료 → 실패(99). */
    @Transactional
    public Optional<PgTrnsctn> applyPendingExpired(String merchantId, String orderNo, int staleMinutes) {
        return applyAuthFailed(merchantId, orderNo,
                PendingTxnExpireService.expireReasonMessage(staleMinutes),
                "KSNET_PENDING_EXPIRED");
    }

    /** 사용자 결제창 취소·인증 실패 — 대기(08)만 실패(99)로 전환. */
    @Transactional
    public Optional<PgTrnsctn> applyAuthFailed(String merchantId, String orderNo, String reason, String reasonCode) {
        Optional<PgTrnsctn> ex = findByMerchantAndOrder(merchantId, orderNo);
        if (ex.isEmpty()) {
            return Optional.empty();
        }
        PgTrnsctn t = ex.get();
        String prev = trim(t.getStatus());
        if (!ST_PENDING.equals(prev)) {
            return Optional.of(t);
        }
        t.setStatus(ST_FAIL);
        String msg = reason != null && !reason.isBlank() ? reason.trim() : "KSNET auth canceled";
        t.setChillPaymentStatus(truncate(msg, 50));
        com.pg.util.TxnOutcomeReasonApplier.apply(t, prev, ST_FAIL, msg,
                reasonCode != null && !reasonCode.isBlank() ? reasonCode : "KSNET_AUTH_FAILED",
                com.pg.util.TxnOutcomeReasonApplier.SOURCE_ICOPAY);
        pgTrnsctnRepository.save(t);
        return Optional.of(t);
    }

    /** ICOPAY 사전 리스크 필터 차단 — 대기 거래를 취소(20)로 전환. */
    @Transactional
    public String applyIcopayPresaleRiskCancel(String merchantId, String orderNo, String reasonMessage) {
        Optional<PgTrnsctn> ex = findByMerchantAndOrder(merchantId, orderNo);
        if (ex.isEmpty()) {
            return null;
        }
        try {
            PgTrnsctn t = ex.get();
            t.setStatus(ST_CANCEL);
            t.setPaidAt(null);
            String reason = reasonMessage != null ? reasonMessage.trim() : "";
            if (!reason.isEmpty()) {
                t.setChillPaymentStatus(truncate(reason, 50));
                t.setOutcomeReason(reason);
                t.setOutcomeReasonSource("ICOPAY");
                t.setOutcomeReasonAt(LocalDateTime.now());
            }
            pgTrnsctnRepository.save(t);
            return t.getTrnId();
        } catch (Exception e) {
            log.warn("KSNET 사전 리스크 취소 반영 오류: {}", e.getMessage());
            return null;
        }
    }

    /** NOTI·order-lookup — 주문번호로 Comp-Id 해석. */
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
        Optional<PgTrnsctn> matched = findAnyByOrder(q);
        if (matched.isEmpty()) {
            matched = pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(q)
                    .filter(x -> PgVendor.isKsnetFamily(x.getVan()));
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
        out.put("orderNo", t.getOrderNo() != null ? t.getOrderNo().trim() : "");
        out.put("order", q);
        out.put("trnId", t.getTrnId());
        out.put("status", t.getStatus());
        out.put("van", t.getVan());
        return out;
    }

    /** 마스킹 카드가 비어 있을 때만 채움. true = 값이 반영됨. KSNET 포털형 XXXX 유지. */
    private static boolean fillMaskedCardIfEmpty(PgTrnsctn t, String maskedCard) {
        if (t == null || maskedCard == null || maskedCard.isBlank()) {
            return false;
        }
        if (t.getCardPanDisplay() != null && !t.getCardPanDisplay().isBlank()) {
            return false;
        }
        String display = normalizeCardPanDisplay(maskedCard);
        if (display.isBlank()) {
            return false;
        }
        t.setCardPanDisplay(truncate(display, 32));
        return true;
    }

    /** 공개: 통합내역·거래조회 보강에서도 동일 규칙으로 카드표시. */
    public static String normalizeCardPanDisplay(String maskedCard) {
        if (maskedCard == null || maskedCard.isBlank()) {
            return "";
        }
        String raw = maskedCard.trim().replaceAll("\\s+", "");
        if (raw.length() < 4) {
            return "";
        }
        if (raw.contains("*") || raw.toUpperCase(java.util.Locale.ROOT).contains("X")) {
            return raw.length() > 32 ? raw.substring(0, 32) : raw;
        }
        String d = raw.replaceAll("[^0-9]", "");
        if (d.length() < 8) {
            return "";
        }
        return d.substring(0, Math.min(6, d.length())) + "***"
                + d.substring(Math.max(0, d.length() - 4));
    }

    /** 거래조회(대사)로 카드·tid 보강. */
    @Transactional
    public Optional<PgTrnsctn> applyTradeEnrichment(PgTrnsctn t, String tid, String maskedCard) {
        return applyTradeEnrichment(t, tid, maskedCard, null, null, null);
    }

    /**
     * 거래조회(대사) 보강 — 카드·tid·수수료·결제시각(없으면).
     * 입력통화(display*)는 ICOPAY 결젯내역 값을 유지한다.
     */
    @Transactional
    public Optional<PgTrnsctn> applyTradeEnrichment(PgTrnsctn t, String tid, String maskedCard,
                                                    BigDecimal feeAmt, LocalDateTime tradeAt, String currency) {
        if (t == null) {
            return Optional.empty();
        }
        boolean changed = fillMaskedCardIfEmpty(t, maskedCard);
        if (tid != null && !tid.isBlank()
                && (t.getChillTransactionId() == null || t.getChillTransactionId().isBlank())) {
            t.setChillTransactionId(truncate(tid.trim(), 64));
            changed = true;
        }
        if (feeAmt != null && feeAmt.compareTo(BigDecimal.ZERO) >= 0 && t.getChillFeeAmt() == null) {
            t.setChillFeeAmt(feeAmt);
            changed = true;
        }
        if (tradeAt != null && t.getPaidAt() == null && "10".equals(trim(t.getStatus()))) {
            t.setPaidAt(tradeAt);
            changed = true;
        }
        if (currency != null && !currency.isBlank()
                && (t.getCurType() == null || t.getCurType().isBlank())) {
            t.setCurType(truncate(currency.trim().toUpperCase(Locale.ROOT), 8));
            changed = true;
        }
        if (changed) {
            pgTrnsctnRepository.save(t);
        }
        return Optional.of(t);
    }

    private void hookReceiptEmail(PgTrnsctn t) {
        if (t == null) {
            return;
        }
        try {
            transactionReceiptEmailService.scheduleIfDue(t);
        } catch (Exception ex) {
            log.warn("KSNET 거래 영수증 메일 연동 실패 trnId={}: {}", t.getTrnId(), ex.getMessage());
        }
    }

    private static String resolveOrigin(String txnOrigin) {
        if (txnOrigin != null && "MERCHANT_API".equalsIgnoreCase(txnOrigin.trim())) {
            return ORIGIN_MERCHANT_API;
        }
        return ORIGIN_URL;
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return u.length() <= 20 ? u : u.substring(0, 20);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
