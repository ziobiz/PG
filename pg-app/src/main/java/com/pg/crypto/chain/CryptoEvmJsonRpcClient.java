package com.pg.crypto.chain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * EVM JSON-RPC — Tx 영수증 Transfer 이벤트 검증 + (선택) Alchemy assetTransfers 주소 스캔.
 */
@Component
public class CryptoEvmJsonRpcClient {

    private static final Logger log = LoggerFactory.getLogger(CryptoEvmJsonRpcClient.class);
    /** ERC-20 Transfer(address,address,uint256) */
    private static final String TRANSFER_TOPIC =
            "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pg.crypto.chain.alchemy-api-key:}")
    private String alchemyApiKey;

    @Value("${pg.crypto.chain.evm-lookback-blocks:5000}")
    private long lookbackBlocks;

    public Optional<CryptoOnChainTransfer> findTransferByTxHash(String networkCode,
                                                                String txHash,
                                                                String expectedTo,
                                                                String expectedContract,
                                                                int decimals) {
        String rpc = resolveRpcUrl(networkCode);
        if (rpc == null || txHash == null || txHash.isBlank()) {
            return Optional.empty();
        }
        String hash = normalizeTxHash(txHash);
        try {
            JsonNode receipt = rpc(rpc, "eth_getTransactionReceipt", List.of(hash));
            if (receipt == null || receipt.isNull()) {
                return Optional.empty();
            }
            String status = text(receipt, "status");
            if ("0x0".equalsIgnoreCase(status)) {
                return Optional.empty();
            }
            long head = hexToLong(rpc(rpc, "eth_blockNumber", List.of()));
            long txBlock = hexToLong(receipt.get("blockNumber"));
            long conf = head > 0 && txBlock > 0 ? Math.max(0, head - txBlock + 1) : 0;
            String wantTo = normalizeAddr(expectedTo);
            String wantContract = normalizeAddr(expectedContract);
            JsonNode logs = receipt.get("logs");
            if (logs == null || !logs.isArray()) {
                return Optional.empty();
            }
            for (JsonNode lg : logs) {
                String contract = normalizeAddr(text(lg, "address"));
                if (!wantContract.isEmpty() && !wantContract.equalsIgnoreCase(contract)) {
                    continue;
                }
                JsonNode topics = lg.get("topics");
                if (topics == null || topics.size() < 3) {
                    continue;
                }
                if (!TRANSFER_TOPIC.equalsIgnoreCase(textAt(topics, 0))) {
                    continue;
                }
                String to = topicToAddress(textAt(topics, 2));
                if (!wantTo.equalsIgnoreCase(to)) {
                    continue;
                }
                BigDecimal amt = decodeAmount(text(lg, "data"), decimals);
                if (amt == null) {
                    continue;
                }
                String from = topicToAddress(textAt(topics, 1));
                return Optional.of(new CryptoOnChainTransfer(
                        hash, to, from, contract, amt, Instant.now(), conf));
            }
        } catch (Exception e) {
            log.warn("EVM tx lookup failed network={} hash={}: {}", networkCode, hash, e.getMessage());
        }
        return Optional.empty();
    }

    public List<CryptoOnChainTransfer> findIncomingTransfers(String networkCode,
                                                             String toAddress,
                                                             String contractAddress,
                                                             int decimals,
                                                             Instant notBefore) {
        String rpc = resolveRpcUrl(networkCode);
        if (rpc == null) {
            return List.of();
        }
        /* Alchemy 키가 있으면 getAssetTransfers 가 안정적. 없으면 eth_getLogs 룩백. */
        if (alchemyApiKey != null && !alchemyApiKey.isBlank() && alchemyNetwork(networkCode) != null) {
            return alchemyTransfers(networkCode, toAddress, contractAddress, decimals, notBefore);
        }
        return ethGetLogs(rpc, toAddress, contractAddress, decimals, notBefore);
    }

    private List<CryptoOnChainTransfer> alchemyTransfers(String networkCode,
                                                         String toAddress,
                                                         String contractAddress,
                                                         int decimals,
                                                         Instant notBefore) {
        String rpc = alchemyRpc(networkCode);
        if (rpc == null) {
            return List.of();
        }
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("fromBlock", "0x0");
            params.put("toBlock", "latest");
            params.put("toAddress", normalizeAddr(toAddress));
            params.put("contractAddresses", List.of(normalizeAddr(contractAddress)));
            params.put("category", List.of("erc20"));
            params.put("withMetadata", true);
            params.put("maxCount", "0x32");
            params.put("order", "desc");
            JsonNode result = rpc(rpc, "alchemy_getAssetTransfers", List.of(params));
            if (result == null) {
                return List.of();
            }
            JsonNode transfers = result.get("transfers");
            if (transfers == null || !transfers.isArray()) {
                return List.of();
            }
            List<CryptoOnChainTransfer> out = new ArrayList<>();
            for (JsonNode t : transfers) {
                String hash = normalizeTxHash(text(t, "hash"));
                BigDecimal amt = null;
                if (t.has("value") && !t.get("value").isNull()) {
                    amt = new BigDecimal(t.get("value").asText());
                }
                if (amt == null) {
                    continue;
                }
                Instant at = Instant.now();
                JsonNode meta = t.get("metadata");
                if (meta != null && meta.has("blockTimestamp")) {
                    try {
                        at = Instant.parse(meta.get("blockTimestamp").asText());
                    } catch (Exception ignored) {
                    }
                }
                if (notBefore != null && at.isBefore(notBefore)) {
                    continue;
                }
                out.add(new CryptoOnChainTransfer(
                        hash,
                        normalizeAddr(text(t, "to")),
                        normalizeAddr(text(t, "from")),
                        normalizeAddr(contractAddress),
                        amt,
                        at,
                        0));
            }
            return out;
        } catch (Exception e) {
            log.warn("Alchemy transfers failed network={}: {}", networkCode, e.getMessage());
            return List.of();
        }
    }

    private List<CryptoOnChainTransfer> ethGetLogs(String rpc,
                                                   String toAddress,
                                                   String contractAddress,
                                                   int decimals,
                                                   Instant notBefore) {
        try {
            long head = hexToLong(rpc(rpc, "eth_blockNumber", List.of()));
            if (head <= 0) {
                return List.of();
            }
            long fromBlockNum = Math.max(0, head - Math.max(100, lookbackBlocks));
            Map<String, Object> filter = new LinkedHashMap<>();
            filter.put("fromBlock", "0x" + Long.toHexString(fromBlockNum));
            filter.put("toBlock", "latest");
            filter.put("address", normalizeAddr(contractAddress));
            filter.put("topics", List.of(
                    TRANSFER_TOPIC,
                    null,
                    addressToTopic(toAddress)
            ));
            JsonNode logs = rpc(rpc, "eth_getLogs", List.of(filter));
            if (logs == null || !logs.isArray()) {
                return List.of();
            }
            List<CryptoOnChainTransfer> out = new ArrayList<>();
            for (JsonNode lg : logs) {
                BigDecimal amt = decodeAmount(text(lg, "data"), decimals);
                if (amt == null) {
                    continue;
                }
                String hash = normalizeTxHash(text(lg, "transactionHash"));
                String fromAddr = topicToAddress(textAt(lg.get("topics"), 1));
                String toAddr = topicToAddress(textAt(lg.get("topics"), 2));
                /* 블록 시각 미조회 — 시간창은 lookback blocks 로만 제한 */
                out.add(new CryptoOnChainTransfer(
                        hash, toAddr, fromAddr, normalizeAddr(contractAddress), amt,
                        notBefore != null ? notBefore : Instant.now(), 0));
            }
            return out;
        } catch (Exception e) {
            log.warn("eth_getLogs failed: {}", e.getMessage());
            return List.of();
        }
    }

    private JsonNode rpc(String url, String method, List<Object> params) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", 1);
        body.put("method", method);
        body.put("params", params);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            return null;
        }
        JsonNode root = objectMapper.readTree(resp.getBody());
        if (root.has("error") && !root.get("error").isNull()) {
            log.debug("RPC error {}: {}", method, root.get("error"));
            return null;
        }
        return root.get("result");
    }

    String resolveRpcUrl(String networkCode) {
        String n = networkCode == null ? "" : networkCode.trim().toUpperCase(Locale.ROOT);
        String alchemy = alchemyRpc(n);
        if (alchemy != null) {
            return alchemy;
        }
        return switch (n) {
            case "ERC20" -> "https://eth.llamarpc.com";
            case "BEP20" -> "https://bsc-dataseed.binance.org";
            case "POLYGON" -> "https://polygon-rpc.com";
            case "ARBITRUM" -> "https://arb1.arbitrum.io/rpc";
            case "BASE" -> "https://mainnet.base.org";
            case "OPTIMISM" -> "https://mainnet.optimism.io";
            case "AVAXC" -> "https://api.avax.network/ext/bc/C/rpc";
            case "OPBNB" -> "https://opbnb-mainnet-rpc.bnbchain.org";
            default -> null;
        };
    }

    private String alchemyRpc(String networkCode) {
        if (alchemyApiKey == null || alchemyApiKey.isBlank()) {
            return null;
        }
        String host = alchemyNetwork(networkCode);
        if (host == null) {
            return null;
        }
        return "https://" + host + ".g.alchemy.com/v2/" + alchemyApiKey.trim();
    }

    private static String alchemyNetwork(String networkCode) {
        String n = networkCode == null ? "" : networkCode.trim().toUpperCase(Locale.ROOT);
        return switch (n) {
            case "ERC20" -> "eth-mainnet";
            case "POLYGON" -> "polygon-mainnet";
            case "ARBITRUM" -> "arb-mainnet";
            case "BASE" -> "base-mainnet";
            case "OPTIMISM" -> "opt-mainnet";
            case "AVAXC" -> "avax-mainnet";
            /* BSC 는 Alchemy 네트워크가 환경마다 다름 — 공개 RPC 사용 */
            default -> null;
        };
    }

    static String normalizeTxHash(String hash) {
        String h = hash == null ? "" : hash.trim();
        if (h.isEmpty()) {
            return "";
        }
        if (!h.startsWith("0x") && !h.startsWith("0X")) {
            h = "0x" + h;
        }
        return h.toLowerCase(Locale.ROOT);
    }

    static String normalizeAddr(String addr) {
        if (addr == null) {
            return "";
        }
        String a = addr.trim();
        if (a.startsWith("0x") || a.startsWith("0X")) {
            return a.toLowerCase(Locale.ROOT);
        }
        if (a.matches("(?i)[0-9a-f]{40}")) {
            return ("0x" + a).toLowerCase(Locale.ROOT);
        }
        return a;
    }

    private static String addressToTopic(String addr) {
        String a = normalizeAddr(addr);
        if (a.startsWith("0x")) {
            a = a.substring(2);
        }
        if (a.length() > 40) {
            a = a.substring(a.length() - 40);
        }
        return "0x" + "0".repeat(24) + a.toLowerCase(Locale.ROOT);
    }

    private static String topicToAddress(String topic) {
        if (topic == null || topic.length() < 40) {
            return "";
        }
        String t = topic.startsWith("0x") ? topic.substring(2) : topic;
        if (t.length() < 40) {
            return "";
        }
        return ("0x" + t.substring(t.length() - 40)).toLowerCase(Locale.ROOT);
    }

    private static BigDecimal decodeAmount(String dataHex, int decimals) {
        if (dataHex == null || dataHex.isBlank() || "0x".equals(dataHex)) {
            return BigDecimal.ZERO;
        }
        try {
            String h = dataHex.startsWith("0x") ? dataHex.substring(2) : dataHex;
            BigInteger raw = new BigInteger(h, 16);
            return new BigDecimal(raw).movePointLeft(Math.max(0, decimals)).setScale(8, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private static long hexToLong(JsonNode n) {
        if (n == null || n.isNull()) {
            return 0;
        }
        return hexToLong(n.asText());
    }

    private static long hexToLong(String hex) {
        if (hex == null || hex.isBlank()) {
            return 0;
        }
        try {
            String h = hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex;
            return Long.parseLong(h, 16);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String text(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return "";
        }
        return n.get(field).asText("");
    }

    private static String textAt(JsonNode arr, int idx) {
        if (arr == null || !arr.isArray() || arr.size() <= idx || arr.get(idx).isNull()) {
            return "";
        }
        return arr.get(idx).asText("");
    }
}
