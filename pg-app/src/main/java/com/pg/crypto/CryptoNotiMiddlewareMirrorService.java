package com.pg.crypto;

import com.pg.entity.CryptoDepositTxn;
import com.pg.noti.NotiProvisionClient;
import com.pg.service.HqNotifyEnvService;
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
 * 크립토 입금 확정 후 ICOPAY → NOTI {@code /noti/crypto} 미러.
 * NOTI가 Comp-Id로 기존 가맹(JPAY/EP/ox 등)을 찾아 Callback/Result 릴레이.
 * (카드와 동일: ICOPAY → NOTI → 가맹. 결제통보 URL 직통이 아님.)
 */
@Service
public class CryptoNotiMiddlewareMirrorService {

    private static final Logger log = LoggerFactory.getLogger(CryptoNotiMiddlewareMirrorService.class);

    public static final String FIELD_ICOPAY_SOURCE = "icopay_source";
    public static final String SOURCE_DEPOSIT_CONFIRM = "crypto_deposit_confirm";
    public static final String NOTI_WEBHOOK_PATH = "/noti/crypto";
    public static final String NOTI_RESULT_PATH = "/noti/result/crypto";

    private final HqNotifyEnvService hqNotifyEnvService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentHashMap<String, Long> recentlyMirrored = new ConcurrentHashMap<>();

    public CryptoNotiMiddlewareMirrorService(HqNotifyEnvService hqNotifyEnvService) {
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    /** 트랜잭션 커밋 후 NOTI로 confirm 미러. 실패는 로그만 (입금 확정은 유지). */
    public void scheduleConfirmMirrorAfterCommit(CryptoDepositTxn txn) {
        if (txn == null || txn.getOrderNo() == null || txn.getOrderNo().isBlank()) {
            return;
        }
        final Long depositId = txn.getId();
        final String orderNo = txn.getOrderNo().trim();
        final String compId = txn.getCompId() != null ? txn.getCompId().trim() : "";
        final String amount = txn.getAmount() != null
                ? txn.getAmount().stripTrailingZeros().toPlainString() : "";
        final String asset = txn.getAssetCode() != null ? txn.getAssetCode().trim() : "";
        final String network = txn.getNetworkCode() != null ? txn.getNetworkCode().trim() : "";
        final String txHash = txn.getDepositTxHash() != null ? txn.getDepositTxHash().trim() : "";
        final String payTrnId = txn.getPayTrnId() != null ? txn.getPayTrnId().trim() : "";
        final String buyerEmail = txn.getBuyerEmail() != null ? txn.getBuyerEmail().trim() : "";
        final String buyerName = txn.getBuyerName() != null ? txn.getBuyerName().trim() : "";
        final boolean confirmed = CryptoDepositTxn.ST_CONFIRMED.equals(txn.getStatus());

        Runnable job = () -> mirrorConfirmToNoti(depositId, orderNo, compId, amount, asset, network,
                txHash, payTrnId, buyerEmail, buyerName, confirmed, false, SOURCE_DEPOSIT_CONFIRM);
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

    public Map<String, Object> mirrorConfirmToNoti(Long depositId, String orderNo, String compId,
                                                   String amount, String asset, String network,
                                                   String txHash, String payTrnId,
                                                   String buyerEmail, String buyerName,
                                                   boolean confirmed, boolean force, String icopaySource) {
        Map<String, Object> result = new LinkedHashMap<>();
        String dedupeKey = (orderNo != null ? orderNo : "") + "|" + (confirmed ? "CONFIRMED" : "OTHER");
        long now = System.currentTimeMillis();
        if (!force) {
            Long prev = recentlyMirrored.putIfAbsent(dedupeKey, now);
            if (prev != null && now - prev < 120_000L) {
                log.debug("CRYPTO NOTI mirror skip duplicate order={}", orderNo);
                result.put("success", false);
                result.put("errorCode", "DEDUPE_SKIP");
                result.put("message", "120초 이내 동일 주문 미러가 있어 생략했습니다.");
                return result;
            }
        }
        recentlyMirrored.put(dedupeKey, now);
        pruneMirrorCache(now);

        String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(
                hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl());
        String url = trimSlash(notiBase) + NOTI_WEBHOOK_PATH;
        result.put("notiUrl", url);

        String source = icopaySource != null && !icopaySource.isBlank()
                ? icopaySource.trim() : SOURCE_DEPOSIT_CONFIRM;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("method", confirmed ? "confirm" : "fail");
        body.put("order", orderNo != null ? orderNo : "");
        body.put("orderNo", orderNo != null ? orderNo : "");
        body.put("amount", amount != null ? amount : "");
        body.put("currency", asset != null && !asset.isBlank() ? asset : CryptoCheckoutService.QUOTE_CURRENCY);
        body.put("quoteCurrency", CryptoCheckoutService.QUOTE_CURRENCY);
        body.put("assetCode", asset != null ? asset : "");
        body.put("networkCode", network != null ? network : "");
        body.put("depositTxHash", txHash != null ? txHash : "");
        body.put("id", txHash != null && !txHash.isBlank() ? txHash : (payTrnId != null ? payTrnId : ""));
        body.put("payTrnId", payTrnId != null ? payTrnId : "");
        body.put("status", confirmed ? "CONFIRMED" : "FAILED");
        body.put("status_message", confirmed ? "Crypto deposit confirmed" : "Crypto deposit failed");
        body.put("timestamp", Instant.now().getEpochSecond());
        body.put("pgKind", "crypto");
        body.put("van", "crypto");
        body.put(FIELD_ICOPAY_SOURCE, source);
        if (depositId != null) {
            body.put("depositId", depositId);
        }
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
        headers.set("X-Icopay-Crypto-Mirror", source);
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
            log.info("CRYPTO NOTI mirror posted order={} depositId={} http={} noti={} source={}",
                    orderNo, depositId, http, url, source);
            result.put("success", http >= 200 && http < 400);
            result.put("httpStatus", http);
            result.put("icopaySource", source);
            result.put("message", Boolean.TRUE.equals(result.get("success"))
                    ? "NOTI /noti/crypto 미러 전송 완료"
                    : "NOTI 응답 HTTP " + http);
            String respBody = resp.getBody();
            if (respBody != null && respBody.length() > 400) {
                respBody = respBody.substring(0, 400) + "…";
            }
            result.put("responsePreview", respBody);
            if (!Boolean.TRUE.equals(result.get("success"))) {
                result.put("errorCode", "NOTI_HTTP_" + http);
            }
            return result;
        } catch (Exception e) {
            log.warn("CRYPTO NOTI mirror 실패 order={} url={}: {}", orderNo, url, e.getMessage());
            result.put("success", false);
            result.put("errorCode", "NOTI_POST_FAILED");
            result.put("message", "NOTI 전송 실패: " + e.getMessage());
            return result;
        }
    }

    public static Map<String, String> fixedNotiUrls(String notiPublicBase) {
        String base = notiPublicBase != null ? notiPublicBase.trim().replaceAll("/+$", "")
                : NotiProvisionClient.defaultBaseUrlIfBlank(null);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("cryptoWebhookUrl", base + NOTI_WEBHOOK_PATH);
        m.put("cryptoResultUrl", base + NOTI_RESULT_PATH);
        return m;
    }

    private void pruneMirrorCache(long now) {
        if (recentlyMirrored.size() < 500) {
            return;
        }
        recentlyMirrored.entrySet().removeIf(e -> now - e.getValue() > 600_000L);
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
