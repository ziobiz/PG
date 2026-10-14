package com.pg.crypto;

import com.pg.entity.CryptoAsset;
import com.pg.entity.CryptoAssetNetwork;
import com.pg.entity.CryptoNetwork;
import com.pg.entity.CryptoWalletBrand;
import com.pg.repository.CryptoAssetNetworkRepository;
import com.pg.repository.CryptoAssetRepository;
import com.pg.repository.CryptoNetworkRepository;
import com.pg.repository.CryptoWalletBrandRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/** USDT/USDC/EURC · 네트워크 · 지갑 카탈로그 (파스텔 메타 포함). */
@Service
public class CryptoCatalogService {

    private final CryptoAssetRepository assetRepository;
    private final CryptoNetworkRepository networkRepository;
    private final CryptoAssetNetworkRepository assetNetworkRepository;
    private final CryptoWalletBrandRepository walletBrandRepository;

    public CryptoCatalogService(CryptoAssetRepository assetRepository,
                                CryptoNetworkRepository networkRepository,
                                CryptoAssetNetworkRepository assetNetworkRepository,
                                CryptoWalletBrandRepository walletBrandRepository) {
        this.assetRepository = assetRepository;
        this.networkRepository = networkRepository;
        this.assetNetworkRepository = assetNetworkRepository;
        this.walletBrandRepository = walletBrandRepository;
    }

    public List<Map<String, Object>> assets() {
        return assetRepository.findByUseYnOrderBySortOrdAsc("Y").stream()
                .map(this::assetMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> networks() {
        return networkRepository.findByUseYnOrderBySortOrdAsc("Y").stream()
                .map(this::networkMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> assetNetworks() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CryptoAssetNetwork an : assetNetworkRepository.findByUseYn("Y")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("assetCode", an.getAssetCode());
            m.put("networkCode", an.getNetworkCode());
            m.put("contractHint", an.getContractHint());
            m.put("decimals", an.getDecimals());
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> wallets(String networkCode) {
        String net = networkCode == null ? "" : networkCode.trim().toUpperCase(Locale.ROOT);
        List<Map<String, Object>> list = walletBrandRepository.findByUseYnOrderBySortOrdAsc("Y").stream()
                .filter(w -> net.isEmpty() || csvContains(w.getNetworksCsv(), net))
                .map(this::walletMap)
                .collect(Collectors.toList());
        /* 목록에 없는 지갑 — 안내용 OTHER */
        Map<String, Object> other = new LinkedHashMap<>();
        other.put("code", "OTHER");
        other.put("name", "Other");
        other.put("pastelBg", "#F1F5F9");
        other.put("pastelFg", "#334155");
        other.put("accent", "#64748B");
        other.put("iconKey", "other");
        other.put("networks", List.of());
        list.add(other);
        return list;
    }

    public Map<String, Object> publicCatalog(Collection<String> allowAssets, Collection<String> allowNetworks) {
        Set<String> assets = normalizeSet(allowAssets);
        Set<String> nets = normalizeSet(allowNetworks);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("assets", assets().stream()
                .filter(a -> assets.isEmpty() || assets.contains(String.valueOf(a.get("code"))))
                .collect(Collectors.toList()));
        out.put("networks", networks().stream()
                .filter(n -> nets.isEmpty() || nets.contains(String.valueOf(n.get("code"))))
                .collect(Collectors.toList()));
        out.put("pairs", assetNetworks().stream()
                .filter(p -> (assets.isEmpty() || assets.contains(String.valueOf(p.get("assetCode"))))
                        && (nets.isEmpty() || nets.contains(String.valueOf(p.get("networkCode")))))
                .collect(Collectors.toList()));
        out.put("wallets", wallets(null).stream()
                .filter(w -> {
                    if ("OTHER".equalsIgnoreCase(String.valueOf(w.get("code")))) {
                        return true;
                    }
                    if (nets.isEmpty()) {
                        return true;
                    }
                    Object csv = w.get("networks");
                    if (!(csv instanceof List<?> list)) {
                        return false;
                    }
                    for (Object o : list) {
                        if (nets.contains(String.valueOf(o))) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList()));
        return out;
    }

    public boolean isValidPair(String assetCode, String networkCode) {
        if (assetCode == null || networkCode == null) {
            return false;
        }
        String a = assetCode.trim().toUpperCase(Locale.ROOT);
        String n = networkCode.trim().toUpperCase(Locale.ROOT);
        return assetNetworkRepository.findByUseYn("Y").stream()
                .anyMatch(p -> a.equalsIgnoreCase(p.getAssetCode()) && n.equalsIgnoreCase(p.getNetworkCode()));
    }

    private Map<String, Object> assetMap(CryptoAsset a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", a.getCode());
        m.put("name", a.getDisplayName());
        m.put("symbol", a.getSymbol());
        m.put("pastelBg", a.getPastelBg());
        m.put("pastelFg", a.getPastelFg());
        m.put("iconKey", a.getIconKey());
        return m;
    }

    private Map<String, Object> networkMap(CryptoNetwork n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", n.getCode());
        m.put("name", n.getDisplayName());
        m.put("chainHint", n.getChainHint());
        m.put("confirmations", n.getConfirmations());
        m.put("pastelBg", n.getPastelBg());
        m.put("pastelFg", n.getPastelFg());
        m.put("iconKey", n.getCode() != null ? n.getCode().toLowerCase(Locale.ROOT) : "");
        return m;
    }

    private Map<String, Object> walletMap(CryptoWalletBrand w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", w.getCode());
        m.put("name", w.getDisplayName());
        m.put("pastelBg", w.getPastelBg());
        m.put("pastelFg", w.getPastelFg());
        m.put("accent", w.getAccent());
        m.put("iconKey", w.getIconKey());
        m.put("networks", Arrays.stream(w.getNetworksCsv().split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList()));
        return m;
    }

    private static boolean csvContains(String csv, String token) {
        if (csv == null || token == null) {
            return false;
        }
        for (String p : csv.split(",")) {
            if (token.equalsIgnoreCase(p.trim())) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> normalizeSet(Collection<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<String> s = new LinkedHashSet<>();
        for (String r : raw) {
            if (r != null && !r.isBlank()) {
                s.add(r.trim().toUpperCase(Locale.ROOT));
            }
        }
        return s;
    }
}
