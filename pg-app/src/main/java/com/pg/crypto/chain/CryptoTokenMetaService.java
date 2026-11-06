package com.pg.crypto.chain;

import com.pg.entity.CryptoAssetNetwork;
import com.pg.repository.CryptoAssetNetworkRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class CryptoTokenMetaService {

    private static final Set<String> EVM = Set.of(
            "ERC20", "BEP20", "POLYGON", "ARBITRUM", "BASE", "OPTIMISM", "AVAXC", "OPBNB");
    private static final Set<String> TRON = Set.of("TRC20");

    private final CryptoAssetNetworkRepository assetNetworkRepository;

    public CryptoTokenMetaService(CryptoAssetNetworkRepository assetNetworkRepository) {
        this.assetNetworkRepository = assetNetworkRepository;
    }

    public Optional<CryptoTokenMeta> resolve(String assetCode, String networkCode) {
        String asset = nz(assetCode).toUpperCase(Locale.ROOT);
        String network = nz(networkCode).toUpperCase(Locale.ROOT);
        if (asset.isEmpty() || network.isEmpty()) {
            return Optional.empty();
        }
        return assetNetworkRepository.findByUseYn("Y").stream()
                .filter(p -> asset.equalsIgnoreCase(p.getAssetCode()) && network.equalsIgnoreCase(p.getNetworkCode()))
                .findFirst()
                .flatMap(this::toMeta);
    }

    private Optional<CryptoTokenMeta> toMeta(CryptoAssetNetwork p) {
        String network = nz(p.getNetworkCode()).toUpperCase(Locale.ROOT);
        String contract = nz(p.getContractHint());
        if (contract.isEmpty()) {
            return Optional.empty();
        }
        int decimals = p.getDecimals() != null && p.getDecimals() > 0 ? p.getDecimals() : 6;
        boolean evm = EVM.contains(network);
        boolean tron = TRON.contains(network);
        if (!evm && !tron) {
            return Optional.empty();
        }
        return Optional.of(new CryptoTokenMeta(
                nz(p.getAssetCode()).toUpperCase(Locale.ROOT),
                network,
                contract,
                decimals,
                evm,
                tron));
    }

    public static boolean isSupportedNetwork(String networkCode) {
        String n = nz(networkCode).toUpperCase(Locale.ROOT);
        return EVM.contains(n) || TRON.contains(n);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
