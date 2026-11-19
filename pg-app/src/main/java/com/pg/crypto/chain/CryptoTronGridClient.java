package com.pg.crypto.chain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * TronGrid V1 — TRC20 Tx 검증·수탁 주소 입금 스캔.
 * @see <a href="https://developers.tron.network/reference/trongrid-v1-api-overview">TronGrid V1 overview</a>
 * Header: {@code TRON-PRO-API-KEY}. Mainnet base: {@code https://api.trongrid.io}
 */
@Component
public class CryptoTronGridClient {

    private static final Logger log = LoggerFactory.getLogger(CryptoTronGridClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CryptoChainCredentialsService credentials;

    public CryptoTronGridClient(CryptoChainCredentialsService credentials) {
        this.credentials = credentials;
    }

    public Optional<CryptoOnChainTransfer> findTransferByTxHash(String txHash,
                                                                String expectedTo,
                                                                String expectedContract,
                                                                int decimals) {
        String hash = normalizeHash(txHash);
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        Optional<CryptoOnChainTransfer> fromEvents = findByEventsApi(hash, expectedTo, expectedContract, decimals);
        if (fromEvents.isPresent()) {
            return fromEvents;
        }
        /* 폴백: 수탁 주소 TRC20 이력에서 동일 Tx 검색 (이벤트 hex/Base58 불일치 보완) */
        return findByAccountTrc20Fingerprint(hash, expectedTo, expectedContract, decimals);
    }

    public List<CryptoOnChainTransfer> findIncomingTransfers(String toAddress,
                                                             String contractAddress,
                                                             int decimals,
                                                             Instant notBefore) {
        try {
            UriComponentsBuilder b = UriComponentsBuilder
                    .fromUriString(trimSlash(credentials.trongridBaseUrl())
                            + "/v1/accounts/" + toAddress.trim() + "/transactions/trc20")
                    .queryParam("only_to", "true")
                    .queryParam("limit", "50")
                    .queryParam("only_confirmed", "true");
            if (contractAddress != null && !contractAddress.isBlank()) {
                b.queryParam("contract_address", contractAddress.trim());
            }
            if (notBefore != null) {
                b.queryParam("min_timestamp", notBefore.toEpochMilli());
            }
            JsonNode root = getJson(b.toUriString());
            if (root == null) {
                return List.of();
            }
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return List.of();
            }
            List<CryptoOnChainTransfer> out = new ArrayList<>();
            for (JsonNode t : data) {
                CryptoOnChainTransfer row = parseTrc20Row(t, contractAddress, decimals);
                if (row == null) {
                    continue;
                }
                if (notBefore != null && row.observedAt().isBefore(notBefore)) {
                    continue;
                }
                if (!CryptoTronAddressUtil.sameAddress(row.toAddress(), toAddress)) {
                    continue;
                }
                out.add(row);
            }
            return out;
        } catch (Exception e) {
            log.warn("Tron incoming scan failed: {}", e.getMessage());
            return List.of();
        }
    }

    /** 키·엔드포인트 연결 확인 (본사 UI 테스트). */
    public Map<String, Object> ping() {
        Map<String, Object> out = new LinkedHashMap<>();
        String base = trimSlash(credentials.trongridBaseUrl());
        out.put("baseUrl", base);
        out.put("apiKeyConfigured", credentials.hasTrongridKey());
        out.put("apiKeySource", credentials.hqTrongridApiKeyRaw().isBlank()
                ? (credentials.hasTrongridKey() ? "ENV" : "NONE")
                : "HQ");
        try {
            /* 공개 헬스: USDT TRC20 컨트랙트 메타 (키가 있으면 헤더 포함) */
            String url = base + "/v1/contracts/TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
            JsonNode root = getJson(url);
            boolean ok = root != null && (root.has("data") || root.has("name") || root.has("address"));
            out.put("success", ok);
            out.put("httpOk", ok);
            if (!ok) {
                out.put("message", "TronGrid 응답이 비어 있거나 거부되었습니다. API Key·쿼터를 확인하세요.");
            } else {
                out.put("message", "TronGrid Mainnet 연결 정상");
            }
        } catch (Exception e) {
            out.put("success", false);
            out.put("message", "TronGrid 호출 실패: " + e.getMessage());
        }
        return out;
    }

    private Optional<CryptoOnChainTransfer> findByEventsApi(String hash,
                                                            String expectedTo,
                                                            String expectedContract,
                                                            int decimals) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(trimSlash(credentials.trongridBaseUrl()) + "/v1/transactions/" + hash + "/events")
                    .queryParam("only_confirmed", "true")
                    .toUriString();
            JsonNode root = getJson(url);
            if (root == null) {
                return Optional.empty();
            }
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return Optional.empty();
            }
            for (JsonNode ev : data) {
                String event = text(ev, "event_name");
                if (!"Transfer".equalsIgnoreCase(event)) {
                    continue;
                }
                String contract = text(ev, "contract_address");
                if (!contractMatches(expectedContract, contract)) {
                    continue;
                }
                JsonNode result = ev.get("result");
                if (result == null) {
                    continue;
                }
                String to = first(result, "to", "1", "_to");
                if (!CryptoTronAddressUtil.sameAddress(expectedTo, to)) {
                    continue;
                }
                String valueRaw = first(result, "value", "2", "_value");
                BigDecimal amt = decodeTrc20Amount(valueRaw, decimals);
                if (amt == null) {
                    continue;
                }
                String from = first(result, "from", "0", "_from");
                Instant at = Instant.now();
                long ms = ev.has("block_timestamp") ? ev.get("block_timestamp").asLong(0) : 0;
                if (ms > 0) {
                    at = Instant.ofEpochMilli(ms);
                }
                return Optional.of(new CryptoOnChainTransfer(
                        hash, to, from, contract, amt, at, 1));
            }
        } catch (Exception e) {
            log.warn("Tron events lookup failed hash={}: {}", hash, e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<CryptoOnChainTransfer> findByAccountTrc20Fingerprint(String hash,
                                                                          String expectedTo,
                                                                          String expectedContract,
                                                                          int decimals) {
        if (expectedTo == null || expectedTo.isBlank()) {
            return Optional.empty();
        }
        try {
            UriComponentsBuilder b = UriComponentsBuilder
                    .fromUriString(trimSlash(credentials.trongridBaseUrl())
                            + "/v1/accounts/" + expectedTo.trim() + "/transactions/trc20")
                    .queryParam("only_to", "true")
                    .queryParam("limit", "50")
                    .queryParam("only_confirmed", "true")
                    .queryParam("fingerprint", hash);
            if (expectedContract != null && !expectedContract.isBlank()) {
                b.queryParam("contract_address", expectedContract.trim());
            }
            JsonNode root = getJson(b.toUriString());
            /* fingerprint 미지원 환경 대비: 파라미터 없이 재조회 후 hash 필터 */
            if (root == null || root.get("data") == null || !root.get("data").isArray()
                    || root.get("data").isEmpty()) {
                b = UriComponentsBuilder
                        .fromUriString(trimSlash(credentials.trongridBaseUrl())
                                + "/v1/accounts/" + expectedTo.trim() + "/transactions/trc20")
                        .queryParam("only_to", "true")
                        .queryParam("limit", "50")
                        .queryParam("only_confirmed", "true");
                if (expectedContract != null && !expectedContract.isBlank()) {
                    b.queryParam("contract_address", expectedContract.trim());
                }
                root = getJson(b.toUriString());
            }
            if (root == null) {
                return Optional.empty();
            }
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return Optional.empty();
            }
            for (JsonNode t : data) {
                String tid = normalizeHash(text(t, "transaction_id"));
                if (!hash.equalsIgnoreCase(tid)) {
                    continue;
                }
                CryptoOnChainTransfer row = parseTrc20Row(t, expectedContract, decimals);
                if (row == null) {
                    continue;
                }
                if (!CryptoTronAddressUtil.sameAddress(expectedTo, row.toAddress())) {
                    continue;
                }
                if (!contractMatches(expectedContract, row.contractAddress())) {
                    continue;
                }
                return Optional.of(row);
            }
        } catch (Exception e) {
            log.warn("Tron account trc20 fingerprint failed hash={}: {}", hash, e.getMessage());
        }
        return Optional.empty();
    }

    private CryptoOnChainTransfer parseTrc20Row(JsonNode t, String fallbackContract, int decimals) {
        String hash = normalizeHash(text(t, "transaction_id"));
        BigDecimal amt = decodeTrc20Amount(text(t, "value"), decimals);
        if (amt == null || hash.isEmpty()) {
            return null;
        }
        Instant at = Instant.now();
        long ms = t.has("block_timestamp") ? t.get("block_timestamp").asLong(0) : 0;
        if (ms > 0) {
            at = Instant.ofEpochMilli(ms);
        }
        String contract = fallbackContract;
        if (t.has("token_info") && t.get("token_info").has("address")) {
            contract = t.get("token_info").get("address").asText(fallbackContract);
        }
        return new CryptoOnChainTransfer(
                hash,
                text(t, "to"),
                text(t, "from"),
                contract,
                amt,
                at,
                1);
    }

    private JsonNode getJson(String url) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String apiKey = credentials.trongridApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            /* TronGrid 공식: TRON-PRO-API-KEY */
            headers.set("TRON-PRO-API-KEY", apiKey.trim());
        }
        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        if (resp.getStatusCode().value() == 429 || resp.getStatusCode().value() == 403) {
            log.warn("TronGrid rate/auth HTTP {} url={}", resp.getStatusCode().value(), url);
            return null;
        }
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            return null;
        }
        return objectMapper.readTree(resp.getBody());
    }

    private static boolean contractMatches(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (actual == null || actual.isBlank()) {
            return false;
        }
        return CryptoTronAddressUtil.sameAddress(expected, actual);
    }

    private static BigDecimal decodeTrc20Amount(String raw, int decimals) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            BigInteger bi = new BigInteger(raw.trim());
            return new BigDecimal(bi).movePointLeft(Math.max(0, decimals)).setScale(8, RoundingMode.HALF_UP);
        } catch (Exception e) {
            try {
                return new BigDecimal(raw.trim()).setScale(8, RoundingMode.HALF_UP);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    static String normalizeHash(String hash) {
        if (hash == null) {
            return "";
        }
        String h = hash.trim();
        if (h.startsWith("0x") || h.startsWith("0X")) {
            h = h.substring(2);
        }
        return h;
    }

    private static String first(JsonNode n, String... keys) {
        for (String k : keys) {
            if (n.has(k) && !n.get(k).isNull()) {
                return n.get(k).asText("");
            }
        }
        return "";
    }

    private static String text(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return "";
        }
        return n.get(field).asText("");
    }

    private static String trimSlash(String u) {
        if (u == null || u.isBlank()) {
            return "https://api.trongrid.io";
        }
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }
}
