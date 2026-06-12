package com.pg.service;

import com.pg.entity.MerchantJpaySubscription;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantJpaySubscriptionRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.JpayTransactionIdApplier;
import com.pg.util.NotifyToTxnStatusMerge;
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

/** JPAY 구독 노티 — {@code payment_period_count}·{@code payment_transaction_id} 반영. */
@Service
public class JpaySubscriptionNotifyService {

    private static final Logger log = LoggerFactory.getLogger(JpaySubscriptionNotifyService.class);
    private static final String ORIGIN_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String ST_PAID = "10";
    private static final String ST_FAIL = "99";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantJpaySubscriptionRepository subscriptionRepository;
    private final SettlementCalcService settlementCalcService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    public JpaySubscriptionNotifyService(PgTrnsctnRepository pgTrnsctnRepository,
                                         MerchantJpaySubscriptionRepository subscriptionRepository,
                                         SettlementCalcService settlementCalcService,
                                         HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.settlementCalcService = settlementCalcService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    public boolean isSubscriptionNotify(Map<String, String> form) {
        if (form == null) {
            return false;
        }
        return !first(form, "payment_transaction_id").isBlank()
                || !first(form, "payment_period_count").isBlank();
    }

    @Transactional
    public void applySubscriptionNotify(String merchantId, Map<String, String> form, String notifyChannel) {
        if (merchantId == null || merchantId.isBlank() || form == null) {
            return;
        }
        String masterOrder = first(form, "orderid");
        if (masterOrder.isBlank()) {
            return;
        }
        String periodRaw = first(form, "payment_period_count");
        int periodParsed = 1;
        try {
            if (!periodRaw.isBlank()) {
                periodParsed = Math.max(1, Integer.parseInt(periodRaw.trim()));
            }
        } catch (NumberFormatException ignored) {
            periodParsed = 1;
        }
        final int period = periodParsed;
        String txnOrderNo = period <= 1 ? masterOrder.trim() : masterOrder.trim() + "-P" + period;
        if (txnOrderNo.length() > 64) {
            txnOrderNo = txnOrderNo.substring(0, 64);
        }

        String ret = first(form, "returncode");
        boolean ok = "00".equals(ret.trim());
        String ch = notifyChannel == null || notifyChannel.isBlank() ? "CALLBACK" : notifyChannel.trim().toUpperCase(Locale.ROOT);

        Optional<PgTrnsctn> ex = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                merchantId.trim(), txnOrderNo, ORIGIN_SUBSCRIPTION);
        PgTrnsctn t = ex.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            x.setMerchantId(merchantId.trim());
            x.setServiceType("URL_JPAY_SUB");
            x.setOrigin(ORIGIN_SUBSCRIPTION);
            return x;
        });
        t.setVan(PgVendor.JPAY.length() > 10 ? PgVendor.JPAY.substring(0, 10) : PgVendor.JPAY);
        t.setOrderNo(txnOrderNo);
        t.setPayNo(txnOrderNo.length() > 50 ? txnOrderNo.substring(0, 50) : txnOrderNo);
        JpayTransactionIdApplier.apply(t, first(form, "transaction_id"));
        String amtStr = first(form, "true_amount");
        if (amtStr.isBlank()) {
            amtStr = first(form, "amount");
        }
        if (!amtStr.isBlank()) {
            try {
                BigDecimal a = new BigDecimal(amtStr.replace(",", "").trim());
                if (a.compareTo(BigDecimal.ZERO) > 0) {
                    t.setAmtKrw(a);
                }
            } catch (Exception ignored) {
                /* keep */
            }
        }
        String cur = first(form, "currency");
        if (!cur.isBlank()) {
            String u = cur.trim().toUpperCase(Locale.ROOT);
            t.setCurType(u.length() > 3 ? u.substring(0, 3) : u);
        }
        t.setNotifyChannelType(ch);
        String next = ok ? ST_PAID : ST_FAIL;
        String merged = NotifyToTxnStatusMerge.merge(t.getStatus(), next, ch);
        if (merged == null || merged.isBlank()) {
            merged = next;
        }
        t.setStatus(merged);
        t.setChillPaymentStatus(ok ? ("JPAY_SUB_P" + period) : ("JPAY_SUB_FAIL " + ret).trim());
        if (ST_PAID.equals(merged)) {
            ZoneId wall = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
            t.setPaidAt(LocalDateTime.now(wall));
        } else {
            t.setPaidAt(null);
        }
        if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
            t.setSettledYn("N");
        }
        if (t.getCustomerId() == null || t.getCustomerId().isBlank()) {
            t.setCustomerId("guest");
        }
        t.setPaymentChannel("CARD");
        pgTrnsctnRepository.save(t);

        subscriptionRepository.findByCompCodeAndCheckoutOrderNo(merchantId.trim(), masterOrder.trim()).ifPresent(s -> {
            s.setStatus(MerchantJpaySubscription.STATUS_ACTIVE);
            s.setPeriodCount(period);
            s.setLastNotifyAt(LocalDateTime.now());
            String ptx = first(form, "payment_transaction_id");
            if (!ptx.isBlank()) {
                s.setPaymentTransactionId(ptx.trim());
            }
            String enabled = first(form, "enabled");
            if ("0".equals(enabled.trim())) {
                s.setStatus(MerchantJpaySubscription.STATUS_CANCELLED);
                s.setCancelledAt(LocalDateTime.now());
            }
            subscriptionRepository.save(s);
        });

        if (ST_PAID.equals(merged)) {
            try {
                settlementCalcService.triggerRealtimeAutoSettlementIfDue(merchantId.trim(), t);
            } catch (Exception rtEx) {
                log.warn("구독 실시간 자동정산 트리거 실패 merchantId={}: {}", merchantId, rtEx.getMessage());
            }
        }
        log.info("JPAY 구독 노티 반영 merchantId={} masterOrder={} period={} returncode={}",
                merchantId, masterOrder, period, ret);
    }

    private static String first(Map<String, String> f, String key) {
        if (f == null || key == null) {
            return "";
        }
        String v = f.get(key.toLowerCase(Locale.ROOT));
        if (v == null) {
            v = f.get(key);
        }
        return v != null ? v.trim() : "";
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return u.length() <= 20 ? u : u.substring(0, 20);
    }
}
