package com.pg.service;

import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.noti.NotiProvisionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QBANG QR 승인 확정 후 ICOPAY → NOTI {@code /noti/qbang} 미러 (폴링 PG · 운영 탭·로그용).
 */
@Service
public class QbangNotiMirrorService {

    private static final Logger log = LoggerFactory.getLogger(QbangNotiMirrorService.class);

    public static final String NOTI_WEBHOOK_PATH = "/noti/qbang";
    public static final String NOTI_RESULT_PATH = "/noti/result/qbang";
    public static final String SOURCE_PL_CONFIRM = "qbang_pl_confirm";

    private final HqNotifyEnvService hqNotifyEnvService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentHashMap<String, Long> recentlyMirrored = new ConcurrentHashMap<>();

    public QbangNotiMirrorService(HqNotifyEnvService hqNotifyEnvService) {
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    public void schedulePaidMirrorAfterCommit(PgTrnsctn txn) {
        if (txn == null || txn.getOrderNo() == null || txn.getOrderNo().isBlank()) {
            return;
        }
        if (!PgVendor.isQbangFamily(txn.getVan())) {
            return;
        }
        final String orderNo = txn.getOrderNo().trim();
        final String compId = txn.getMerchantId() != null ? txn.getMerchantId().trim() : "";
        final String amount = txn.getAmtKrw() != null
                ? txn.getAmtKrw().stripTrailingZeros().toPlainString()
                : (txn.getTotalAmt() != null ? txn.getTotalAmt().stripTrailingZeros().toPlainString() : "");
        final String currency = txn.getCurType() != null ? txn.getCurType().trim() : "";
        final String txnNo = txn.getChillTransactionId() != null ? txn.getChillTransactionId().trim()
                : (txn.getPayNo() != null ? txn.getPayNo().trim() : "");
        final String approvalNo = txn.getApprovalNo() != null ? txn.getApprovalNo().trim() : "";
        final String buyerEmail = txn.getCustomerId() != null ? txn.getCustomerId().trim() : "";
        final String buyerName = txn.getCustomerNm() != null ? txn.getCustomerNm().trim() : "";

        Runnable job = () -> mirrorPaidToNoti(orderNo, compId, amount, currency, txnNo, approvalNo,
                buyerEmail, buyerName, true, false, SOURCE_PL_CONFIRM);
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

    public Map<String, Object> mirrorPaidToNoti(String orderNo, String compId, String amount, String currency,
                                                String transactionNo, String approvalNo,
                                                String buyerEmail, String buyerName,
                                                boolean paid, boolean force, String icopaySource) {
        Map<String, Object> result = new LinkedHashMap<>();
        String dedupeKey = (orderNo != null ? orderNo : "") + "|" + (paid ? "PAID" : "FAIL");
        long now = System.currentTimeMillis();
        if (!force) {
            Long prev = recentlyMirrored.putIfAbsent(dedupeKey, now);
            if (prev != null && now - prev < 120_000L) {
                result.put("success", false);
                result.put("errorCode", "DEDUPE_SKIP");
                return result;
            }
        }
        recentlyMirrored.put(dedupeKey, now);

        String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(
                hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl());
        String url = trimSlash(notiBase) + NOTI_WEBHOOK_PATH;
        result.put("notiUrl", url);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("method", paid ? "confirm" : "fail");
        body.put("order", orderNo != null ? orderNo : "");
        body.put("orderNo", orderNo != null ? orderNo : "");
        body.put("amount", amount != null ? amount : "");
        body.put("currency", currency != null ? currency : "");
        body.put("transactionNo", transactionNo != null ? transactionNo : "");
        body.put("transaction_id", transactionNo != null ? transactionNo : "");
        body.put("approvalNo", approvalNo != null ? approvalNo : "");
        body.put("status", paid ? "PAID" : "FAILED");
        body.put("paymentStatus", paid ? "Succeeded" : "Failed");
        body.put("timestamp", Instant.now().getEpochSecond());
        body.put("pgKind", "qbang");
        body.put("van", PgVendor.QBANG);
        body.put("icopay_source", icopaySource != null ? icopaySource : SOURCE_PL_CONFIRM);
        if (compId != null && !compId.isBlank()) {
            body.put("compId", compId);
            body.put("merchantId", compId);
            body.put("Comp-Id", compId);
        }
        if (buyerEmail != null && !buyerEmail.isBlank()) {
            body.put("buyerEmail", buyerEmail);
            body.put("CustomerId", buyerEmail);
        }
        if (buyerName != null && !buyerName.isBlank()) {
            body.put("buyerName", buyerName);
            body.put("CustomerName", buyerName);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Icopay-Qbang-Mirror", icopaySource != null ? icopaySource : SOURCE_PL_CONFIRM);
        if (compId != null && !compId.isBlank()) {
            headers.set("X-Icopay-Comp-Id", compId);
        }
        String apiKey = hqNotifyEnvService.getOrCreate().getNotiProvisionApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("X-Api-Key", apiKey.trim());
        }

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);
            int http = resp.getStatusCode().value();
            log.info("QBANG NOTI mirror order={} http={} noti={}", orderNo, http, url);
            result.put("success", http >= 200 && http < 400);
            result.put("httpStatus", http);
            return result;
        } catch (Exception e) {
            log.warn("QBANG NOTI mirror 실패 order={}: {}", orderNo, e.getMessage());
            result.put("success", false);
            result.put("errorCode", "NOTI_POST_FAILED");
            result.put("message", e.getMessage());
            return result;
        }
    }

    public static Map<String, String> fixedNotiUrls(String notiPublicBase) {
        String base = notiPublicBase != null ? notiPublicBase.trim().replaceAll("/+$", "")
                : NotiProvisionClient.defaultBaseUrlIfBlank(null);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("qbangWebhookUrl", base + NOTI_WEBHOOK_PATH);
        m.put("qbangResultUrl", base + NOTI_RESULT_PATH);
        return m;
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
