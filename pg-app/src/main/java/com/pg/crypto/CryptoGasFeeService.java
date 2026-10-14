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

    private final CryptoNetworkGasFeeRepository repository;

    public CryptoGasFeeService(CryptoNetworkGasFeeRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CryptoNetworkGasFee g : repository.findAllByOrderByAssetCodeAscNetworkCodeAsc()) {
            out.add(toMap(g));
        }
        return out;
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
        return Map.of("success", true, "saved", n, "list", listAll());
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
