package com.pg.service;

import com.pg.integration.pg.PgVendor;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.JpayBuyerContactApplier;
import com.pg.util.JpayTransactionIdApplier;
import com.pg.util.NotifyToTxnStatusMerge;
import com.pg.util.PayCardBrandDetector;
import com.pg.util.PayCardFailOutcomeRules;
import com.pg.util.PayCardPanHashUtil;
import com.pg.splitpay.SplitPayPaymentHookService;
import com.pg.util.PgTrnsctnOrderLookup;
import com.pg.util.RouteNoDisplayUtil;
import com.pg.util.TxnOutcomeReasonApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JPAY {@code pay_index} 직접 호출(서버 프록시) 직후 {@link PgTrnsctn} 에 URL 결제 출처 행을 남깁니다.
 * 3DS·비동기 노티는 {@link JpayNotifyToTrnsctnService} 가 후속 갱신합니다.
 */
@Service
public class JpaySaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(JpaySaleRecordService.class);
    private static final String ORIGIN_URL = "URL";
    private static final String ORIGIN_MERCHANT_API = "MERCHANT_API";
    private static final String ORIGIN_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String ST_PAID = "10";
    private static final String ST_PENDING = "08";
    private static final String ST_FAIL = "99";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementCalcService settlementCalcService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final SplitPayPaymentHookService splitPayPaymentHookService;
    private final OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator;
    private final PayCardFailCooldownService payCardFailCooldownService;
    private final PgTrnsctnOrderDedupeService pgTrnsctnOrderDedupeService;

    public JpaySaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                 OrgUnitRepository orgUnitRepository,
                                 SettlementCalcService settlementCalcService,
                                 HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                 SplitPayPaymentHookService splitPayPaymentHookService,
                                 OutcomeReasonWarmCoordinator outcomeReasonWarmCoordinator,
                                 PayCardFailCooldownService payCardFailCooldownService,
                                 PgTrnsctnOrderDedupeService pgTrnsctnOrderDedupeService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementCalcService = settlementCalcService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
        this.outcomeReasonWarmCoordinator = outcomeReasonWarmCoordinator;
        this.payCardFailCooldownService = payCardFailCooldownService;
        this.pgTrnsctnOrderDedupeService = pgTrnsctnOrderDedupeService;
    }

    @Transactional
    public void recordOrTouchPending(Long orgUnitId,
                                     String orderNo,
                                     BigDecimal amount,
                                     String currency,
                                     int routeNo,
                                     String customerHint,
                                     String productName,
                                     String txnOrigin) {
        recordOrTouchPending(orgUnitId, orderNo, amount, currency, routeNo, customerHint, productName, txnOrigin,
                null, null);
    }

    public void recordOrTouchPending(Long orgUnitId,
                                     String orderNo,
                                     BigDecimal amount,
                                     String currency,
                                     int routeNo,
                                     String customerHint,
                                     String productName,
                                     String txnOrigin,
                                     BigDecimal shopperDisplayAmount,
                                     String shopperDisplayCurrency) {
        recordOrTouchPending(orgUnitId, orderNo, amount, currency, routeNo, customerHint, productName, txnOrigin,
                shopperDisplayAmount, shopperDisplayCurrency, null);
    }

    public void recordOrTouchPending(Long orgUnitId,
                                     String orderNo,
                                     BigDecimal amount,
                                     String currency,
                                     int routeNo,
                                     String customerHint,
                                     String productName,
                                     String txnOrigin,
                                     BigDecimal shopperDisplayAmount,
                                     String shopperDisplayCurrency,
                                     Map<String, Object> saleBody) {
        try {
            doRecord(orgUnitId, orderNo, amount, currency, routeNo, customerHint, productName, txnOrigin,
                    shopperDisplayAmount, shopperDisplayCurrency, saleBody);
        } catch (Exception e) {
            log.warn("JPAY sale 거래 적재(대기) 실패: {}", e.getMessage());
        }
    }

    private void doRecord(Long orgUnitId,
                          String orderNo,
                          BigDecimal amount,
                          String currency,
                          int routeNo,
                          String customerHint,
                          String productName,
                          String txnOrigin,
                          BigDecimal shopperDisplayAmount,
                          String shopperDisplayCurrency,
                          Map<String, Object> saleBody) {
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
        PgTrnsctn t = ex.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            x.setMerchantId(merchantId.trim());
            x.setServiceType("URL_JPAY");
            x.setOrigin(origin);
            return x;
        });
        t.setStatus(ST_PENDING);
        t.setVan(PgVendor.JPAY.length() > 10 ? PgVendor.JPAY.substring(0, 10) : PgVendor.JPAY);
        t.setOrderNo(on);
        String payNo = on.length() > 50 ? on.substring(0, 50) : on;
        t.setPayNo(payNo);
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            t.setAmtKrw(amount);
        }
        String cur = currency != null ? currency.trim().toUpperCase() : UrlPayCheckoutCurrencyService.DEFAULT_FALLBACK;
        t.setCurType(cur.length() > 3 ? cur.substring(0, 3) : cur);
        String routeStored = RouteNoDisplayUtil.normalizeForStorage(routeNo);
        if (routeStored != null) {
            t.setRouteNo(routeStored);
        }
        String cid = customerHint != null && !customerHint.isBlank() ? customerHint.trim() : "guest";
        t.setCustomerId(cid.length() > 100 ? cid.substring(0, 100) : cid);
        if (saleBody != null && !saleBody.isEmpty()) {
            JpayBuyerContactApplier.applyFromSaleBody(t, saleBody);
            applyCardPanHashFromSaleBody(t, saleBody);
            applyPayerContextFromSaleBody(t, saleBody);
        }
        String desc = "JPAY_URL";
        if (productName != null && !productName.isBlank()) {
            desc = desc + " " + productName.trim();
        }
        t.setChillPaymentStatus(desc.length() > 50 ? desc.substring(0, 50) : desc);
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
        pgTrnsctnRepository.save(t);
        purgeOrderDuplicates(t);
    }

    private void purgeOrderDuplicates(PgTrnsctn t) {
        if (t == null || t.getMerchantId() == null || t.getOrderNo() == null) {
            return;
        }
        try {
            pgTrnsctnOrderDedupeService.purgeGuestNotiDuplicatesIfCanonicalExists(
                    t.getMerchantId(), t.getOrderNo());
        } catch (Exception ex) {
            log.warn("중복 NOTI·guest 정리 실패 trnId={}: {}", t.getTrnId(), ex.getMessage());
        }
    }

    @Transactional
    public void applySyncApiOutcome(String merchantId, String orderNo, int status, String msg) {
        applySyncApiOutcome(merchantId, orderNo, status, msg, null, null);
    }

    @Transactional
    public void applySyncApiOutcome(String merchantId, String orderNo, int status, String msg, String txnOrigin) {
        applySyncApiOutcome(merchantId, orderNo, status, msg, txnOrigin, null);
    }

    /** pay_index·노티의 JPAY {@code transaction_id} 를 승인번호로 반영(3DS 대기 중에도 표시). */
    @Transactional
    public void applyJpayTransactionId(String merchantId, String orderNo, String transactionId, String txnOrigin) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()
                || transactionId == null || transactionId.isBlank()) {
            return;
        }
        try {
            String on = orderNo.trim();
            Optional<PgTrnsctn> ex = findTxnForOrder(merchantId.trim(), on, txnOrigin);
            if (ex.isEmpty()) {
                return;
            }
            PgTrnsctn t = ex.get();
            JpayTransactionIdApplier.apply(t, transactionId);
            pgTrnsctnRepository.save(t);
        } catch (Exception e) {
            log.warn("JPAY transaction_id 반영 실패: {}", e.getMessage());
        }
    }

    @Transactional
    public void applySyncApiOutcome(String merchantId, String orderNo, int status, String msg, String txnOrigin,
                                    String jpayTransactionId) {
        applySyncApiOutcome(merchantId, orderNo, status, msg, txnOrigin, jpayTransactionId, null);
    }

    @Transactional
    public void applySyncApiOutcome(String merchantId, String orderNo, int status, String msg, String txnOrigin,
                                    String jpayTransactionId, String panDigits) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return;
        }
        try {
            String on = orderNo.trim();
            Optional<PgTrnsctn> ex = findTxnForOrder(merchantId.trim(), on, txnOrigin);
            if (ex.isEmpty()) {
                return;
            }
            PgTrnsctn t = ex.get();
            String prevStatus = t.getStatus();
            if (status == 0) {
                t.setStatus(ST_PAID);
                ZoneId wall = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
                t.setPaidAt(LocalDateTime.now(wall));
                String m = msg != null ? msg.trim() : "OK";
                t.setChillPaymentStatus(truncate(m, 50));
            } else if (status == 2) {
                t.setStatus(ST_FAIL);
                t.setPaidAt(null);
                t.setChillPaymentStatus("2");
                Optional<String> recordedReason = TxnOutcomeReasonApplier.applyJpaySyncFail(t, prevStatus, ST_FAIL, msg);
                JpayTransactionIdApplier.apply(t, jpayTransactionId);
                pgTrnsctnRepository.save(t);
                outcomeReasonWarmCoordinator.onRecorded(recordedReason);
                hookSplitPay(t);
                recordSyncNonSuccessCooldown(t, msg, panDigits);
                return;
            }
            JpayTransactionIdApplier.apply(t, jpayTransactionId);
            /* status==1 (3DS): pending 유지 */
            pgTrnsctnRepository.save(t);
            hookSplitPay(t);
            if (status == 0 && t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
                payCardFailCooldownService.clearOnSuccessForTxn(PgVendor.JPAY, panDigits,
                        t.getCardPanHash(), resolveOrgUnitId(t));
                try {
                    settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
                } catch (Exception rtEx) {
                    log.warn("실시간 자동정산 트리거 실패 merchantId={}: {}", t.getMerchantId(), rtEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("JPAY 동기 응답 반영 실패: {}", e.getMessage());
        }
    }

    private void hookSplitPay(PgTrnsctn t) {
        if (t == null || t.getOrderNo() == null) {
            return;
        }
        try {
            splitPayPaymentHookService.onTxnStatusChange(t.getOrderNo(), t.getStatus(), t.getTrnId());
        } catch (Exception ex) {
            log.warn("분할결제 연동 실패 orderNo={}: {}", t.getOrderNo(), ex.getMessage());
        }
    }

    private static String resolveOrigin(String txnOrigin) {
        if (txnOrigin != null && "MERCHANT_API".equalsIgnoreCase(txnOrigin.trim())) {
            return ORIGIN_MERCHANT_API;
        }
        if (txnOrigin != null && "SUBSCRIPTION".equalsIgnoreCase(txnOrigin.trim())) {
            return ORIGIN_SUBSCRIPTION;
        }
        return ORIGIN_URL;
    }

    private Optional<PgTrnsctn> findTxnForOrder(String merchantId, String orderNo) {
        return findTxnForOrder(merchantId, orderNo, null);
    }

    private Optional<PgTrnsctn> findTxnForOrder(String merchantId, String orderNo, String txnOrigin) {
        if (txnOrigin != null && "SUBSCRIPTION".equalsIgnoreCase(txnOrigin.trim())) {
            return pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                    merchantId, orderNo, ORIGIN_SUBSCRIPTION);
        }
        return PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(pgTrnsctnRepository, merchantId, orderNo);
    }

    /** ICOPAY 결제창 사전 검증(카드정책 등) 실패 — 거래·처리사유 적재 */
    @Transactional
    public void applyIcopayPreSaleFail(String merchantId, String orderNo, String txnOrigin,
                                       String message, String errorCode) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return;
        }
        try {
            Optional<PgTrnsctn> ex = findTxnForOrder(merchantId.trim(), orderNo.trim(), txnOrigin);
            if (ex.isEmpty()) {
                return;
            }
            PgTrnsctn t = ex.get();
            String prevStatus = t.getStatus();
            t.setStatus(ST_FAIL);
            t.setPaidAt(null);
            String code = errorCode != null && !errorCode.isBlank() ? errorCode.trim() : "CHECKOUT_VALIDATION";
            String msg = message != null && !message.isBlank() ? message.trim() : code;
            Optional<String> recorded = TxnOutcomeReasonApplier.apply(
                    t, prevStatus, ST_FAIL, msg, code, TxnOutcomeReasonApplier.SOURCE_ICOPAY);
            pgTrnsctnRepository.save(t);
            outcomeReasonWarmCoordinator.onRecorded(recorded);
        } catch (Exception e) {
            log.warn("ICOPAY 사전검증 실패 반영 오류: {}", e.getMessage());
        }
    }

    private static void applyCardPanHashFromSaleBody(PgTrnsctn t, Map<String, Object> body) {
        if (t == null || body == null || body.isEmpty()) {
            return;
        }
        Object raw = body.get("payCardno");
        if (raw == null) {
            raw = body.get("pay_cardno");
        }
        if (raw == null) {
            return;
        }
        String pan = PayCardBrandDetector.normalizePan(raw.toString());
        if (pan.length() >= 10) {
            t.setCardPanHash(PayCardPanHashUtil.hashPan(pan));
        }
    }

    private static void applyPayerContextFromSaleBody(PgTrnsctn t, Map<String, Object> body) {
        if (t == null || body == null || body.isEmpty()) {
            return;
        }
        Object ip = body.get("_payerClientIp");
        if (ip != null && !ip.toString().isBlank()) {
            String v = ip.toString().trim();
            t.setPayerClientIp(v.length() > 64 ? v.substring(0, 64) : v);
        }
        Object dev = body.get("_payerDeviceCategory");
        if (dev != null && !dev.toString().isBlank()) {
            String v = dev.toString().trim().toUpperCase(Locale.ROOT);
            t.setPayerDeviceCategory(v.length() > 32 ? v.substring(0, 32) : v);
        }
        Object iso = body.get("_payerCountryIso2");
        if (iso != null && !iso.toString().isBlank()) {
            String v = com.pg.util.PayerCountryIso2Util.normalize(iso.toString());
            if (!v.isBlank()) {
                t.setPayerCountryIso2(v);
            }
        }
        Object city = body.get("_payerCity");
        if (city == null || city.toString().isBlank()) {
            city = body.get("payCity");
        }
        if (city == null || city.toString().isBlank()) {
            city = body.get("pay_city");
        }
        if (city != null && !city.toString().isBlank()) {
            String v = city.toString().trim();
            t.setPayerCity(v.length() > 128 ? v.substring(0, 128) : v);
        }
    }

    private Long resolveOrgUnitId(PgTrnsctn t) {
        if (t == null || t.getMerchantId() == null || t.getMerchantId().isBlank()) {
            return null;
        }
        return orgUnitRepository.findByCode(t.getMerchantId().trim()).map(OrgUnit::getId).orElse(null);
    }

    private void recordSyncNonSuccessCooldown(PgTrnsctn t, String msg, String panDigits) {
        Long orgUnitId = resolveOrgUnitId(t);
        String pan = resolvePanForCooldown(panDigits, t);
        if (pan.length() >= 10) {
            payCardFailCooldownService.recordQualifyingFailure(PgVendor.JPAY, pan, PayCardFailOutcomeRules.OUTCOME_FAIL,
                    msg, orgUnitId, t.getCustomerNm());
            return;
        }
        if (t.getCardPanHash() != null && !t.getCardPanHash().isBlank()) {
            payCardFailCooldownService.recordFromTxnHash(PgVendor.JPAY, t.getCardPanHash().trim(),
                    t.getCardPanDisplay(), PayCardFailOutcomeRules.OUTCOME_FAIL, msg, orgUnitId, t.getCustomerNm());
        }
    }

    private static String resolvePanForCooldown(String panDigits, PgTrnsctn t) {
        String pan = PayCardBrandDetector.normalizePan(panDigits);
        if (pan.length() >= 10) {
            return pan;
        }
        return "";
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return u.length() <= 20 ? u : u.substring(0, 20);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
