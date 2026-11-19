package com.pg.crypto.chain;

import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * 온체인 입금 감시 자격증명 — 본사 DB(크립토설정) 우선, 없으면 환경변수·yml.
 */
@Service
public class CryptoChainCredentialsService {

    private final HqApiConfigRepository hqApiConfigRepository;

    @Value("${pg.crypto.chain.enabled:true}")
    private boolean ymlEnabled;

    @Value("${pg.crypto.chain.alchemy-api-key:}")
    private String ymlAlchemyApiKey;

    @Value("${pg.crypto.chain.trongrid-api-key:}")
    private String ymlTrongridApiKey;

    @Value("${pg.crypto.chain.trongrid-base-url:https://api.trongrid.io}")
    private String ymlTrongridBaseUrl;

    public CryptoChainCredentialsService(HqApiConfigRepository hqApiConfigRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    public boolean isWatchEnabled() {
        Optional<HqApiConfig> row = hqApiConfigRepository.findAll().stream().findFirst();
        if (row.isPresent()) {
            String yn = row.get().getCryptoChainWatchEnabledYn();
            if (yn != null && !yn.isBlank()) {
                return "Y".equalsIgnoreCase(yn.trim());
            }
        }
        return ymlEnabled;
    }

    public String alchemyApiKey() {
        Optional<HqApiConfig> row = hqApiConfigRepository.findAll().stream().findFirst();
        if (row.isPresent()) {
            String k = row.get().getCryptoAlchemyApiKey();
            if (k != null && !k.isBlank()) {
                return k.trim();
            }
        }
        return nz(ymlAlchemyApiKey);
    }

    public String trongridApiKey() {
        Optional<HqApiConfig> row = hqApiConfigRepository.findAll().stream().findFirst();
        if (row.isPresent()) {
            String k = row.get().getCryptoTrongridApiKey();
            if (k != null && !k.isBlank()) {
                return k.trim();
            }
        }
        return nz(ymlTrongridApiKey);
    }

    public String trongridBaseUrl() {
        return ymlTrongridBaseUrl != null && !ymlTrongridBaseUrl.isBlank()
                ? ymlTrongridBaseUrl.trim()
                : "https://api.trongrid.io";
    }

    public boolean hasAlchemyKey() {
        String k = alchemyApiKey();
        return k != null && !k.isBlank();
    }

    public boolean hasTrongridKey() {
        String k = trongridApiKey();
        return k != null && !k.isBlank();
    }

    /** HQ 저장값만 (env 폴백 제외) — 화면 표시용. */
    public String hqAlchemyApiKeyRaw() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(HqApiConfig::getCryptoAlchemyApiKey)
                .map(s -> s != null ? s.trim() : "")
                .orElse("");
    }

    public String hqTrongridApiKeyRaw() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(HqApiConfig::getCryptoTrongridApiKey)
                .map(s -> s != null ? s.trim() : "")
                .orElse("");
    }

    public String hqWatchEnabledYn() {
        Optional<HqApiConfig> row = hqApiConfigRepository.findAll().stream().findFirst();
        if (row.isPresent()) {
            String yn = row.get().getCryptoChainWatchEnabledYn();
            if (yn != null && !yn.isBlank()) {
                return "Y".equalsIgnoreCase(yn.trim()) ? "Y" : "N";
            }
        }
        return ymlEnabled ? "Y" : "N";
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    public static String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String k = key.trim();
        if (k.length() <= 6) {
            return "••••";
        }
        return "••••" + k.substring(k.length() - 4);
    }

    public static boolean looksLikeMaskedPlaceholder(String v) {
        if (v == null) {
            return false;
        }
        String s = v.trim();
        return s.startsWith("••••") || s.equalsIgnoreCase("(unchanged)")
                || s.toLowerCase(Locale.ROOT).contains("unchanged");
    }
}
