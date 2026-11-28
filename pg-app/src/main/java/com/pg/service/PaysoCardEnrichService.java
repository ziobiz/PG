package com.pg.service;

import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.payso.PaysoApiClient;
import com.pg.integration.pg.payso.PaysoCredentials;
import com.pg.repository.PgAgencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PAYSO 승인 후 Order Detail 로 마스킹 카드·PG Order No 를 {@code pg_trnsctn} 에 보강.
 * 웹훅에 카드가 없고 Inquiry 목록을 열지 않아도 결제내역 카드번호가 채워지도록 한다.
 */
@Service
public class PaysoCardEnrichService {

    private static final Logger log = LoggerFactory.getLogger(PaysoCardEnrichService.class);

    private final PaysoApiClient paysoApiClient;
    private final PaysoSaleRecordService paysoSaleRecordService;
    private final PgAgencyRepository pgAgencyRepository;

    public PaysoCardEnrichService(PaysoApiClient paysoApiClient,
                                  PaysoSaleRecordService paysoSaleRecordService,
                                  PgAgencyRepository pgAgencyRepository) {
        this.paysoApiClient = paysoApiClient;
        this.paysoSaleRecordService = paysoSaleRecordService;
        this.pgAgencyRepository = pgAgencyRepository;
    }

    /** 커밋 후 비동기로 Order Detail 조회·적재. */
    public void scheduleAfterCommit(PgTrnsctn txn) {
        if (txn == null || !PgVendor.isPaysoFamily(txn.getVan())) {
            return;
        }
        if (txn.getCardPanDisplay() != null && !txn.getCardPanDisplay().isBlank()) {
            String chill = txn.getChillTransactionId() != null ? txn.getChillTransactionId().trim() : "";
            String order = txn.getOrderNo() != null ? txn.getOrderNo().trim() : "";
            /* 카드 있고 PG order 도 chill 에 있으면 스킵 */
            if (!chill.isBlank() && !chill.equalsIgnoreCase(order) && !chill.matches("^[0-9]{12}$")) {
                return;
            }
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

    private void enrichQuietly(String merchantId, String orderNo, String payNo, String chill) {
        try {
            Optional<PaysoCredentials> credOpt = resolveCredentials();
            if (credOpt.isEmpty()) {
                return;
            }
            String ref = firstNonBlank(payNo);
            String ord = firstNonBlank(orderNo, chill);
            if (ref.isBlank() && ord.isBlank()) {
                return;
            }
            Map<String, Object> detail = paysoApiClient.orderDetail(credOpt.get(), ref, ord);
            if (!Boolean.TRUE.equals(detail.get("ok"))) {
                return;
            }
            Optional<PgTrnsctn> live = Optional.empty();
            if (merchantId != null && orderNo != null) {
                live = paysoSaleRecordService.findByMerchantAndOrder(merchantId, orderNo);
            }
            live.ifPresent(t -> paysoSaleRecordService.applyInquiryEnrichment(
                    t,
                    str(detail.get("referenceNo")),
                    str(detail.get("orderNo")),
                    firstNonBlank(str(detail.get("transactionId")), chill),
                    str(detail.get("cardNo")),
                    null,
                    str(detail.get("orderDateTime"))));
        } catch (Exception e) {
            log.debug("PAYSO card enrich skip: {}", e.getMessage());
        }
    }

    private Optional<PaysoCredentials> resolveCredentials() {
        List<PgAgency> agencies = pgAgencyRepository.findAllByOrderByPgCdAsc();
        PaysoCredentials sandboxFallback = null;
        for (PgAgency a : agencies) {
            if (a == null || !PgVendor.isPaysoFamily(a.getPgCd())) {
                continue;
            }
            PaysoCredentials c = PaysoCredentials.fromAgency(a);
            if (!c.isInquiryUsable()) {
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

    private static String str(Object v) {
        return v != null ? String.valueOf(v).trim() : "";
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
}
