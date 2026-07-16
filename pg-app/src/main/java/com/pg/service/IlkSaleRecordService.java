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

/** ILK RequestAuth/Payment·노티 시 {@link PgTrnsctn} 적재·갱신. */
@Service
public class IlkSaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(IlkSaleRecordService.class);

    public static final String SERVICE_TYPE = "URL_ILK";
    private static final String ST_PAID = "10";
    private static final String ST_PENDING = "08";
    private static final String ST_FAIL = "99";
    private static final String ST_CANCEL = "20";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    public IlkSaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
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
                                     BigDecimal shopperDisplayAmount,
                                     String shopperDisplayCurrency,
                                     boolean subscription,
                                     String ilkAuthOrPaymentId) {
        try {
            doRecord(orgUnitId, orderNo, amount, currency, routeNo, productName, txnOrigin,
                    buyerName, buyerEmail, shopperDisplayAmount, shopperDisplayCurrency,
                    subscription, ilkAuthOrPaymentId);
        } catch (Exception e) {
            log.warn("ILK 대기 거래 적재 실패: {}", e.getMessage());
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
                          BigDecimal shopperDisplayAmount,
                          String shopperDisplayCurrency,
                          boolean subscription,
                          String ilkAuthOrPaymentId) {
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
        PgTrnsctn t = ex.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            x.setMerchantId(mid);
            x.setServiceType(SERVICE_TYPE);
            x.setOrigin(origin);
            return x;
        });
        t.setStatus(ST_PENDING);
        t.setVan(PgVendor.ILK);
        t.setOrderNo(on);
        t.setPayNo(on.length() > 50 ? on.substring(0, 50) : on);
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            t.setAmtKrw(amount);
        }
        String cur = currency != null && !currency.isBlank()
                ? currency.trim().toUpperCase(Locale.ROOT) : "KRW";
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
        String desc = subscription ? "ILK_SUB" : "ILK_URL";
        if (productName != null && !productName.isBlank()) {
            desc = desc + " " + productName.trim();
        }
        t.setChillPaymentStatus(truncate(desc, 50));
        t.setPaymentChannel("CARD");
        if (ilkAuthOrPaymentId != null && !ilkAuthOrPaymentId.isBlank()) {
            t.setChillTransactionId(truncate(ilkAuthOrPaymentId.trim(), 64));
        }
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

    @Transactional
    public Optional<PgTrnsctn> applyOutcome(String merchantId, String orderNo, boolean paid,
                                            String paymentId, String msg) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        try {
            Optional<PgTrnsctn> ex = findTxnForOrder(merchantId.trim(), orderNo.trim());
            if (ex.isEmpty()) {
                return Optional.empty();
            }
            PgTrnsctn t = ex.get();
            if (paymentId != null && !paymentId.isBlank()) {
                t.setChillTransactionId(truncate(paymentId.trim(), 64));
                t.setApprovalNo(truncate(paymentId.trim(), 20));
            }
            if (paid) {
                if (ST_PAID.equals(t.getStatus())) {
                    return Optional.of(t);
                }
                t.setStatus(ST_PAID);
                ZoneId wall = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
                t.setPaidAt(LocalDateTime.now(wall));
                t.setChillPaymentStatus(truncate(msg != null && !msg.isBlank() ? msg.trim() : "SUCCESS", 50));
            } else {
                t.setStatus(ST_FAIL);
                t.setPaidAt(null);
                if (msg != null && !msg.isBlank()) {
                    t.setChillPaymentStatus(truncate(msg.trim(), 50));
                    t.setOutcomeReason(msg.trim());
                    t.setOutcomeReasonSource("ILK");
                    t.setOutcomeReasonAt(LocalDateTime.now());
                }
            }
            pgTrnsctnRepository.save(t);
            return Optional.of(t);
        } catch (Exception e) {
            log.warn("ILK 결과 반영 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Transactional
    public Optional<PgTrnsctn> applyCancel(String merchantId, String orderNo, String cancelId, String msg) {
        Optional<PgTrnsctn> ex = findTxnForOrder(merchantId, orderNo);
        if (ex.isEmpty()) {
            return Optional.empty();
        }
        PgTrnsctn t = ex.get();
        t.setStatus(ST_CANCEL);
        if (cancelId != null && !cancelId.isBlank()) {
            t.setChillTransactionId(truncate(cancelId.trim(), 64));
        }
        if (msg != null && !msg.isBlank()) {
            t.setChillPaymentStatus(truncate(msg.trim(), 50));
        }
        pgTrnsctnRepository.save(t);
        return Optional.of(t);
    }

    public Optional<PgTrnsctn> findAnyByOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        return pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(orderNo.trim()).stream()
                .filter(t -> PgVendor.isIlkFamily(t.getVan()))
                .findFirst();
    }

    public Optional<PgTrnsctn> findTxnForOrder(String merchantId, String orderNo) {
        Optional<PgTrnsctn> t = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, merchantId, orderNo);
        if (t.isPresent() && PgVendor.isIlkFamily(t.get().getVan())) {
            return t;
        }
        return Optional.empty();
    }

    private static String resolveOrigin(String txnOrigin) {
        if (txnOrigin == null || txnOrigin.isBlank()) {
            return "URL";
        }
        String u = txnOrigin.trim().toUpperCase(Locale.ROOT);
        if (u.contains("API") || u.contains("BROKER") || u.contains("INLINE")) {
            return "API";
        }
        if (u.contains("SUB")) {
            return "SUB";
        }
        return "URL";
    }

    private static String newTrnId() {
        return "ILK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
