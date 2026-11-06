package com.pg.crypto.chain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/** TronGrid — TRC20 Tx 검증·주소 입금 스캔. */
@Component
public class CryptoTronGridClient {

    private static final Logger log = LoggerFactory.getLogger(CryptoTronGridClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pg.crypto.chain.trongrid-base-url:https://api.trongrid.io}")
    private String baseUrl;

    @Value("${pg.crypto.chain.trongrid-api-key:}")
    private String apiKey;

    public Optional<CryptoOnChainTransfer> findTransferByTxHash(String txHash,
                                                                String expectedTo,
                                                                String expectedContract,
                                                                int decimals) {
        String hash = normalizeHash(txHash);
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        try {
            /* events API: contract Transfer */
            String url = UriComponentsBuilder
                    .fromUriString(trimSlash(baseUrl) + "/v1/transactions/" + hash + "/events")
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
            String wantTo = normalizeTron(expectedTo);
            String wantContract = normalizeTron(expectedContract);
            for (JsonNode ev : data) {
                String event = text(ev, "event_name");
                if (!"Transfer".equalsIgnoreCase(event)) {
                    continue;
                }
                String contract = normalizeTron(text(ev, "contract_address"));
                if (!wantContract.isEmpty() && !wantContract.equalsIgnoreCase(contract)) {
                    /* hex contract vs base58 — 느슨 비교 */
                    if (!contractEquals(wantContract, contract)) {
                        continue;
                    }
                }
                JsonNode result = ev.get("result");
                if (result == null) {
                    continue;
                }
                String to = normalizeTron(first(result, "to", "1"));
                if (!wantTo.equalsIgnoreCase(to) && !contractEquals(wantTo, to)) {
                    continue;
                }
                String valueRaw = first(result, "value", "2");
                BigDecimal amt = decodeTrc20Amount(valueRaw, decimals);
                if (amt == null) {
                    continue;
                }
                String from = normalizeTron(first(result, "from", "0"));
                Instant at = Instant.now();
                long ms = ev.has("block_timestamp") ? ev.get("block_timestamp").asLong(0) : 0;
                if (ms > 0) {
                    at = Instant.ofEpochMilli(ms);
                }
                return Optional.of(new CryptoOnChainTransfer(hash, to, from, contract, amt, at, 1));
            }
        } catch (Exception e) {
            log.warn("Tron tx lookup failed hash={}: {}", hash, e.getMessage());
        }
        return Optional.empty();
    }

    public List<CryptoOnChainTransfer> findIncomingTransfers(String toAddress,
                                                             String contractAddress,
                                                             int decimals,
                                                             Instant notBefore) {
        try {
            UriComponentsBuilder b = UriComponentsBuilder
                    .fromUriString(trimSlash(baseUrl) + "/v1/accounts/" + toAddress.trim() + "/transactions/trc20")
                    .queryParam("only_to", "true")
                    .queryParam("limit", "50")
                    .queryParam("only_confirmed", "true");
            if (contractAddress != null && !contractAddress.isBlank()) {
                b.queryParam("contract_address", contractAddress.trim());
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
                String hash = normalizeHash(text(t, "transaction_id"));
                BigDecimal amt = decodeTrc20Amount(text(t, "value"), decimals);
                if (amt == null) {
                    continue;
                }
                Instant at = Instant.now();
                long ms = t.has("block_timestamp") ? t.get("block_timestamp").asLong(0) : 0;
                if (ms > 0) {
                    at = Instant.ofEpochMilli(ms);
                }
                if (notBefore != null && at.isBefore(notBefore)) {
                    continue;
                }
                out.add(new CryptoOnChainTransfer(
                        hash,
                        normalizeTron(text(t, "to")),
                        normalizeTron(text(t, "from")),
                        normalizeTron(text(t, "token_info") != null && t.get("token_info").has("address")
                                ? t.get("token_info").get("address").asText()
                                : contractAddress),
                        amt,
                        at,
                        1));
            }
            return out;
        } catch (Exception e) {
            log.warn("Tron incoming scan failed: {}", e.getMessage());
            return List.of();
        }
    }

    private JsonNode getJson(String url) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("TRON-PRO-API-KEY", apiKey.trim());
        }
        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            return null;
        }
        return objectMapper.readTree(resp.getBody());
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

    private static String normalizeTron(String addr) {
        return addr == null ? "" : addr.trim();
    }

    private static boolean contractEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
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
