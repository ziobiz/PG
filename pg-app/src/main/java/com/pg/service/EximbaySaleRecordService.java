package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PgTrnsctnOrderLookup;
import com.pg.util.RouteNoDisplayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Eximbay {@code /v1/payments/ready} 직후 {@link PgTrnsctn} 에 URL 결제 출처(대기) 행을 남기고,
 * 결제창 결과·webhook(statusurl) 로 후속 상태를 갱신한다.
 * <p>Eximbay 는 카드정보를 보유하지 않는 호스티드 결제창이므로 PAN 해시 등은 저장하지 않는다.
 */
@Service
public class EximbaySaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(EximbaySaleRecordService.class);

    public static final String SERVICE_TYPE = "URL_EXIMBAY";
    public static final String SERVICE_TYPE_SUB = "URL_EXIMBAY_SUB";
    private static final String ORIGIN_URL = "URL";
    private static final String ORIGIN_MERCHANT_API = "MERCHANT_API";
    private static final String ORIGIN_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String ST_PAID = "10";
    private static final String ST_PENDING = "08";
    private static final String ST_FAIL = "99";
    private static final String ST_CANCEL = "20";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    public EximbaySaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                    OrgUnitRepository orgUnitRepository,
                                    HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    @Transactional
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
                                     boolean subscription) {
        try {
            doRecord(orgUnitId, orderNo, amount, currency, routeNo, productName, txnOrigin,
                    buyerName, buyerEmail, paymentMethodChannel, shopperDisplayAmount, shopperDisplayCurrency, subscription);
        } catch (Exception e) {
            log.warn("Eximbay ready 거래 적재(대기) 실패: {}", e.getMessage());
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
                          boolean subscription) {
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
        final String svc = subscription ? SERVICE_TYPE_SUB : SERVICE_TYPE;
        PgTrnsctn t = ex.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            x.setMerchantId(mid);
            x.setServiceType(svc);
            x.setOrigin(origin);
            return x;
        });
        t.setStatus(ST_PENDING);
        t.setVan(PgVendor.EXIMBAY);
        t.setOrderNo(on);
        t.setPayNo(on.length() > 50 ? on.substring(0, 50) : on);
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            t.setAmtKrw(amount);
        }
        String cur = currency != null && !currency.isBlank()
                ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
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
        String desc = subscription ? "EXIMBAY_SUB" : "EXIMBAY_URL";
        if (productName != null && !productName.isBlank()) {
            desc = desc + " " + productName.trim();
        }
        t.setChillPaymentStatus(truncate(desc, 50));
        String channel = paymentMethodChannel != null && !paymentMethodChannel.isBlank()
                ? paymentMethodChannel.trim().toUpperCase(Locale.ROOT) : "EXIMBAY";
        t.setPaymentChannel(truncate(channel, 80));
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

    /** 결제창 즉시 결과·webhook 로 승인/실패 반영 (transaction_id 는 chill_transaction_id 에 저장). */
    @Transactional
    public Optional<PgTrnsctn> applyOutcome(String merchantId, String orderNo, boolean paid,
                                            String transactionId, String msg, String txnOrigin,
                                            String maskedCard) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        try {
            String on = orderNo.trim();
            Optional<PgTrnsctn> ex = findTxnForOrder(merchantId.trim(), on, txnOrigin);
            if (ex.isEmpty()) {
                return Optional.empty();
            }
            PgTrnsctn t = ex.get();
            if (transactionId != null && !transactionId.isBlank()) {
                t.setChillTransactionId(truncate(transactionId.trim(), 64));
                t.setApprovalNo(truncate(transactionId.trim(), 20));
            }
            if (maskedCard != null && !maskedCard.isBlank()) {
                t.setCardPanDisplay(truncate(maskedCard.trim(), 32));
            }
            if (paid) {
                t.setStatus(ST_PAID);
                ZoneId wall = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
                t.setPaidAt(LocalDateTime.now(wall));
                t.setChillPaymentStatus(truncate(msg != null && !msg.isBlank() ? msg.trim() : "Success", 50));
            } else {
                t.setStatus(ST_FAIL);
                t.setPaidAt(null);
                if (msg != null && !msg.isBlank()) {
                    t.setChillPaymentStatus(truncate(msg.trim(), 50));
                    t.setOutcomeReason(msg.trim());
                    t.setOutcomeReasonSource("EXIMBAY");
                    t.setOutcomeReasonAt(LocalDateTime.now());
                }
            }
            pgTrnsctnRepository.save(t);
            return Optional.of(t);
        } catch (Exception e) {
            log.warn("Eximbay 결과 반영 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * ICOPAY 사전 리스크 필터 차단 — 기존(대기) 거래를 취소(20)로 전환하고 사유를 적재한다.
     * JPAY·ChillPay 와 동일하게 결제 전 단계에서 차단된 요청도 결제목록에 남긴다.
     */
    @Transactional
    public String applyIcopayPresaleRiskCancel(String merchantId, String orderNo, String txnOrigin,
                                               String reasonMessage) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return null;
        }
        try {
            Optional<PgTrnsctn> ex = findTxnForOrder(merchantId.trim(), orderNo.trim(), txnOrigin);
            if (ex.isEmpty()) {
                return null;
            }
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
            log.warn("Eximbay 사전 리스크 취소 반영 오류: {}", e.getMessage());
            return null;
        }
    }

    /** 가맹점 미상 상태에서 orderNo 로 Eximbay 대기/기존 거래를 역추적(가맹점 코드 복구용). */
    @Transactional(readOnly = true)
    public Optional<PgTrnsctn> findAnyByOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        String on = orderNo.trim();
        Optional<PgTrnsctn> url = pgTrnsctnRepository.findFirstByOrderNoAndOriginOrderByCreatedAtDesc(on, ORIGIN_URL);
        if (url.isPresent()) {
            return url;
        }
        Optional<PgTrnsctn> sub = pgTrnsctnRepository.findFirstByOrderNoAndOriginOrderByCreatedAtDesc(on, ORIGIN_SUBSCRIPTION);
        if (sub.isPresent()) {
            return sub;
        }
        Optional<PgTrnsctn> api = pgTrnsctnRepository.findFirstByOrderNoAndOriginOrderByCreatedAtDesc(on, ORIGIN_MERCHANT_API);
        if (api.isPresent()) {
            return api;
        }
        return pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(on);
    }

    private Optional<PgTrnsctn> findTxnForOrder(String merchantId, String orderNo, String txnOrigin) {
        if (txnOrigin != null && "SUBSCRIPTION".equalsIgnoreCase(txnOrigin.trim())) {
            Optional<PgTrnsctn> sub = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                    merchantId, orderNo, ORIGIN_SUBSCRIPTION);
            if (sub.isPresent()) {
                return sub;
            }
        }
        return PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(pgTrnsctnRepository, merchantId, orderNo);
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

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return u.length() <= 20 ? u : u.substring(0, 20);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
