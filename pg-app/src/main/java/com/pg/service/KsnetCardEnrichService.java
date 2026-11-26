package com.pg.service;

import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.ksnet.KsnetApiClient;
import com.pg.integration.pg.ksnet.KsnetCredentials;
import com.pg.repository.PgAgencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KSNET 승인 후 WebFEP {@code /api/v1/card/trade} 로 카드·수수료·결제시각을 {@code pg_trnsctn} 에 보강.
 * 승인(recv_post) 응답에 cardno/trddt 가 비어도 결제·통합내역에 채운다.
 */
@Service
public class KsnetCardEnrichService {

    private static final Logger log = LoggerFactory.getLogger(KsnetCardEnrichService.class);

    private final KsnetApiClient ksnetApiClient;
    private final KsnetSaleRecordService ksnetSaleRecordService;
    private final PgAgencyRepository pgAgencyRepository;

    public KsnetCardEnrichService(KsnetApiClient ksnetApiClient,
                                  KsnetSaleRecordService ksnetSaleRecordService,
                                  PgAgencyRepository pgAgencyRepository) {
        this.ksnetApiClient = ksnetApiClient;
        this.ksnetSaleRecordService = ksnetSaleRecordService;
        this.pgAgencyRepository = pgAgencyRepository;
    }

    /** 커밋 후 거래조회(대사)로 카드·수수료·시각 보강. */
    public void scheduleAfterCommit(PgTrnsctn txn) {
        if (txn == null || !PgVendor.isKsnetFamily(txn.getVan())) {
            return;
        }
        if (!needsEnrichment(txn)) {
            return;
        }
        final String merchantId = txn.getMerchantId();
        final String orderNo = txn.getOrderNo();
        final String payNo = txn.getPayNo();
        final String chill = txn.getChillTransactionId();
        Runnable job = () -> enrichQuietly(merchantId, orderNo, payNo, chill);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
        }
    }

    /** 통합내역 조회 시 동기 보강(예산 내에서). true = 값이 반영됨. */
    public boolean enrichNow(PgTrnsctn txn, KsnetCredentials creds) {
        if (txn == null || creds == null || !creds.isTradeUsable()) {
            return false;
        }
        if (!needsEnrichment(txn)) {
            return false;
        }
        Map<String, Object> detail = inquire(creds, txn.getChillTransactionId(), txn.getPayNo(), txn.getOrderNo());
        if (!Boolean.TRUE.equals(detail.get("ok"))) {
            return false;
        }
        return applyDetail(txn, detail);
    }

    static boolean needsEnrichment(PgTrnsctn t) {
        if (t == null) {
            return false;
        }
        boolean cardEmpty = t.getCardPanDisplay() == null || t.getCardPanDisplay().isBlank()
                || "-".equals(t.getCardPanDisplay().trim());
        boolean feeEmpty = t.getChillFeeAmt() == null;
        boolean paidEmpty = t.getPaidAt() == null && "10".equals(trim(t.getStatus()));
        return cardEmpty || feeEmpty || paidEmpty;
    }

    private void enrichQuietly(String merchantId, String orderNo, String payNo, String chill) {
        try {
            Optional<KsnetCredentials> credOpt = resolveCredentials();
            if (credOpt.isEmpty() || !credOpt.get().isTradeUsable()) {
                return;
            }
            Map<String, Object> detail = inquire(credOpt.get(), chill, payNo, orderNo);
            if (!Boolean.TRUE.equals(detail.get("ok"))) {
                return;
            }
            Optional<PgTrnsctn> live = Optional.empty();
            if (merchantId != null && orderNo != null) {
                live = ksnetSaleRecordService.findByMerchantAndOrder(merchantId, orderNo);
            }
            if (live.isEmpty() && orderNo != null) {
                live = ksnetSaleRecordService.findAnyByOrder(orderNo);
            }
            live.ifPresent(t -> applyDetail(t, detail));
        } catch (Exception e) {
            log.debug("KSNET trade enrich skip: {}", e.getMessage());
        }
    }

    private boolean applyDetail(PgTrnsctn txn, Map<String, Object> detail) {
        String card = str(detail.get("cardMasked"));
        String tid = str(detail.get("tid"));
        BigDecimal fee = parseDecimal(str(detail.get("fee")));
        LocalDateTime tradeAt = KsnetApiClient.parseTradeDateTime(
                str(detail.get("tradeDate")), str(detail.get("tradeTime")), str(detail.get("tradeDateTime")));
        String currency = str(detail.get("currency"));
        Optional<PgTrnsctn> saved = ksnetSaleRecordService.applyTradeEnrichment(
                txn, tid, card, fee, tradeAt, currency);
        return saved.isPresent();
    }

    private Map<String, Object> inquire(KsnetCredentials creds, String chill, String payNo, String orderNo) {
        if (chill != null && !chill.isBlank()) {
            Map<String, Object> byTid = ksnetApiClient.tradeInquire(creds, "TID", chill.trim());
            if (Boolean.TRUE.equals(byTid.get("ok")) && !str(byTid.get("cardMasked")).isBlank()) {
                return byTid;
            }
            if (Boolean.TRUE.equals(byTid.get("ok"))) {
                return byTid;
            }
        }
        String ord = firstNonBlank(payNo, orderNo);
        if (!ord.isBlank()) {
            return ksnetApiClient.tradeInquire(creds, "ORDER_NUMB", ord);
        }
        return Map.of("ok", false);
    }

    public Optional<KsnetCredentials> resolveCredentials() {
        List<PgAgency> agencies = pgAgencyRepository.findAllByOrderByPgCdAsc();
        KsnetCredentials sandboxFallback = null;
        for (PgAgency a : agencies) {
            if (a == null || !PgVendor.isKsnetFamily(a.getPgCd())) {
                continue;
            }
            KsnetCredentials c = KsnetCredentials.fromAgency(a);
            if (!c.isTradeUsable()) {
                continue;
            }
            if (!c.sandbox()) {
                return Optional.of(c);
            }
            if (sandboxFallback == null) {
                sandboxFallback = c;
            }
        }
        return Optional.ofNullable(sandboxFallback);
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
