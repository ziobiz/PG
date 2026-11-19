package com.pg.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.CryptoPayoutQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * AUTO 출금 — HTTP 지갑/커스터디 API.
 * {@code pg.crypto.payout.auto-execute-enabled=true} 이고 URL 이 있을 때만 호출.
 * 응답 JSON: {@code payoutTxHash} 또는 {@code txHash} (success=false 면 실패).
 */
@Component
public class CryptoHttpPayoutWalletGateway implements CryptoPayoutWalletGateway {

    private static final Logger log = LoggerFactory.getLogger(CryptoHttpPayoutWalletGateway.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${pg.crypto.payout.auto-execute-enabled:false}")
    private boolean autoExecuteEnabled;

    @Value("${pg.crypto.payout.wallet-api-url:}")
    private String walletApiUrl;

    @Value("${pg.crypto.payout.wallet-api-key:}")
    private String walletApiKey;

    @Value("${pg.crypto.payout.wallet-api-timeout-ms:45000}")
    private long timeoutMs;

    @Override
    public boolean isConfigured() {
        return walletApiUrl != null && !walletApiUrl.isBlank();
    }

    @Override
    public boolean isExecuteEnabled() {
        return autoExecuteEnabled && isConfigured();
    }

    @Override
    public String disabledReason() {
        if (!autoExecuteEnabled) {
            return "AUTO 출금 실행이 비활성입니다(pg.crypto.payout.auto-execute-enabled). 반자동 Tx 등록을 사용하세요.";
        }
        if (!isConfigured()) {
            return "지갑 API URL 미설정(pg.crypto.payout.wallet-api-url). 반자동 Tx 등록을 사용하세요.";
        }
        return "";
    }

    @Override
    public Result send(CryptoPayoutQueue queue) {
        if (!isExecuteEnabled()) {
            return Result.fail(disabledReason(), true);
        }
        if (queue.getSellerAddress() == null || queue.getSellerAddress().isBlank()) {
            return Result.fail("가맹 수취 지갑 주소가 없습니다.", false);
        }
        if (queue.getAmountNet() == null || queue.getAmountNet().signum() <= 0) {
            return Result.fail("출금 순액(amountNet)이 없습니다.", false);
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("payoutQueueId", queue.getId());
            body.put("depositTxnId", queue.getDepositTxnId());
            body.put("compId", queue.getCompId());
            body.put("orderNo", queue.getOrderNo());
            body.put("assetCode", queue.getAssetCode());
            body.put("networkCode", queue.getNetworkCode());
            body.put("amountNet", queue.getAmountNet());
            body.put("toAddress", queue.getSellerAddress());
            String json = JSON.writeValueAsString(body);

            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(walletApiUrl.trim()))
                    .timeout(Duration.ofMillis(Math.max(5000L, timeoutMs)))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            if (walletApiKey != null && !walletApiKey.isBlank()) {
                b.header("Authorization", "Bearer " + walletApiKey.trim());
                b.header("X-Api-Key", walletApiKey.trim());
            }
            HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.warn("crypto payout wallet HTTP {} body={}", resp.statusCode(), truncate(resp.body()));
                boolean retry = resp.statusCode() >= 500 || resp.statusCode() == 429;
                return Result.fail("지갑 API HTTP " + resp.statusCode(), retry);
            }
            JsonNode root = JSON.readTree(resp.body() == null || resp.body().isBlank() ? "{}" : resp.body());
            if (root.has("success") && !root.get("success").asBoolean(true)) {
                String msg = text(root, "message", "error", "errorMessage");
                return Result.fail(msg != null ? msg : "지갑 API 거절", false);
            }
            String tx = text(root, "payoutTxHash", "txHash", "hash", "txid");
            if (tx == null || tx.isBlank()) {
                return Result.fail("지갑 API 응답에 Tx 해시가 없습니다.", true);
            }
            tx = tx.trim();
            if (isStubHash(tx)) {
                return Result.fail("지갑 API가 스텁 Tx 해시를 반환했습니다.", false);
            }
            return Result.ok(tx);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail("지갑 API 호출 중단", true);
        } catch (Exception e) {
            log.warn("crypto payout wallet call failed id={}: {}", queue.getId(), e.getMessage());
            return Result.fail("지갑 API 호출 실패: " + e.getMessage(), true);
        }
    }

    static boolean isStubHash(String tx) {
        if (tx == null) return true;
        String u = tx.trim().toUpperCase(Locale.ROOT);
        return u.startsWith("AUTO_QUEUED_") || u.startsWith("BUYER_ACK") || u.startsWith("STUB_");
    }

    private static String text(JsonNode root, String... keys) {
        for (String k : keys) {
            if (root.has(k) && !root.get(k).isNull()) {
                String v = root.get(k).asText("");
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }
}
