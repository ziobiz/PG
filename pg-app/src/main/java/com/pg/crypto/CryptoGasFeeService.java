package com.pg.crypto;

import com.pg.entity.CryptoNetworkGasFee;
import com.pg.repository.CryptoNetworkGasFeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/** 본사 CRYPTO 네트워크별 가스피(전 가맹 동일). */
@Service
public class CryptoGasFeeService {

    private static final List<String> ASSET_TAB_ORDER = List.of("USDC", "USDT", "EURC");

    private final CryptoNetworkGasFeeRepository repository;
    private final CryptoCatalogService catalogService;

    public CryptoGasFeeService(CryptoNetworkGasFeeRepository repository, CryptoCatalogService catalogService) {
        this.repository = repository;
        this.catalogService = catalogService;
    }

    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CryptoNetworkGasFee g : repository.findAllByOrderByAssetCodeAscNetworkCodeAsc()) {
            out.add(toMap(g));
        }
        return out;
    }

    /**
     * HQ 가스피 화면용: 카탈로그 자산×네트워크 페어를 기준으로 DB 값을 합친다.
     * 없는 페어는 0·사용Y로 채워 저장 가능하게 한다.
     */
    @Transactional
    public List<Map<String, Object>> listMergedWithCatalog() {
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (CryptoNetworkGasFee g : repository.findAllByOrderByAssetCodeAscNetworkCodeAsc()) {
            Map<String, Object> m = toMap(g);
            byKey.put(key(g.getAssetCode(), g.getNetworkCode()), m);
        }
        Map<String, String> netNames = new HashMap<>();
        Map<String, Integer> netSort = new HashMap<>();
        int ord = 0;
        for (Map<String, Object> n : catalogService.networks()) {
            String code = str(n.get("code")).toUpperCase(Locale.ROOT);
            if (code.isBlank()) {
                continue;
            }
            String name = str(n.get("name"));
            if (name.isBlank()) {
                name = code;
            }
            netNames.put(code, name);
            netSort.put(code, ord++);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> p : catalogService.assetNetworks()) {
            String asset = str(p.get("assetCode")).toUpperCase(Locale.ROOT);
            String network = str(p.get("networkCode")).toUpperCase(Locale.ROOT);
            if (asset.isBlank() || network.isBlank()) {
                continue;
            }
            if (!ASSET_TAB_ORDER.contains(asset)) {
                continue;
            }
            String k = key(asset, network);
            Map<String, Object> row = byKey.get(k);
            if (row == null) {
                CryptoNetworkGasFee g = new CryptoNetworkGasFee();
                g.setAssetCode(asset);
                g.setNetworkCode(network);
                g.setGasFeeAmt(BigDecimal.ZERO);
                g.setUseYn("Y");
                g.setRemark(defaultRemark(asset, network, netNames.get(network)));
                repository.save(g);
                row = toMap(g);
                byKey.put(k, row);
            }
            row.put("networkName", netNames.getOrDefault(network, network));
            row.put("networkSort", netSort.getOrDefault(network, 999));
            out.add(row);
        }
        out.sort(Comparator
                .comparing((Map<String, Object> m) -> assetTabIndex(str(m.get("assetCode"))))
                .thenComparing(m -> {
                    Object s = m.get("networkSort");
                    return s instanceof Number ? ((Number) s).intValue() : 999;
                })
                .thenComparing(m -> str(m.get("networkCode")), String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static String defaultRemark(String asset, String network, String networkName) {
        String label = networkName != null && !networkName.isBlank() ? networkName : network;
        return asset + " · " + label;
    }

    private static int assetTabIndex(String asset) {
        int i = ASSET_TAB_ORDER.indexOf(asset.toUpperCase(Locale.ROOT));
        return i >= 0 ? i : 99;
    }

    private static String key(String asset, String network) {
        return (asset == null ? "" : asset.trim().toUpperCase(Locale.ROOT))
                + "|"
                + (network == null ? "" : network.trim().toUpperCase(Locale.ROOT));
    }

    /** 활성 가스피. 없으면 0. */
    public BigDecimal resolveGasFee(String assetCode, String networkCode) {
        if (assetCode == null || assetCode.isBlank() || networkCode == null || networkCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        return repository.findByAssetCodeAndNetworkCodeAndUseYn(
                        assetCode.trim().toUpperCase(Locale.ROOT),
                        networkCode.trim().toUpperCase(Locale.ROOT),
                        "Y")
                .map(CryptoNetworkGasFee::getGasFeeAmt)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public Map<String, Object> saveOne(Map<String, Object> body) {
        String asset = str(body.get("assetCode")).toUpperCase(Locale.ROOT);
        String network = str(body.get("networkCode")).toUpperCase(Locale.ROOT);
        if (asset.isBlank() || network.isBlank()) {
            throw new IllegalArgumentException("assetCode·networkCode가 필요합니다.");
        }
        CryptoNetworkGasFee g = repository.findByAssetCodeAndNetworkCode(asset, network)
                .orElseGet(CryptoNetworkGasFee::new);
        g.setAssetCode(asset);
        g.setNetworkCode(network);
        g.setGasFeeAmt(parseAmt(body.get("gasFeeAmt")));
        g.setUseYn(str(body.get("useYn")));
        g.setRemark(str(body.get("remark")));
        repository.save(g);
        return toMap(g);
    }

    @Transactional
    public Map<String, Object> saveAll(List<Map<String, Object>> rows) {
        int n = 0;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                saveOne(row);
                n++;
            }
        }
        return Map.of("success", true, "saved", n, "list", listMergedWithCatalog());
    }

    private static Map<String, Object> toMap(CryptoNetworkGasFee g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("assetCode", g.getAssetCode());
        m.put("networkCode", g.getNetworkCode());
        m.put("gasFeeAmt", g.getGasFeeAmt() != null ? g.getGasFeeAmt().stripTrailingZeros().toPlainString() : "0");
        m.put("useYn", g.getUseYn());
        m.put("remark", g.getRemark() != null ? g.getRemark() : "");
        m.put("updatedAt", g.getUpdatedAt() != null ? g.getUpdatedAt().toString() : "");
        return m;
    }

    private static BigDecimal parseAmt(Object raw) {
        if (raw == null || raw.toString().isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
