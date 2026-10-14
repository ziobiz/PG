package com.pg.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.service.UrlPayDisplayFxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 크립토 표시통화 → 청구(USD) → 자산(USDC/USDT/EURC) FX.
 * <ul>
 *   <li>표시→USD: {@link UrlPayDisplayFxService#convertAmount} (BOT)</li>
 *   <li>USDC/USDT: USD 1:1</li>
 *   <li>EURC: USD→EUR (ECB Frankfurter, 캐시)</li>
 * </ul>
 */
@Service
public class CryptoFxService {

    private static final Logger log = LoggerFactory.getLogger(CryptoFxService.class);

    /** 청구·수수료·한도 앵커 (기준통화). */
    public static final String QUOTE_CURRENCY = CryptoCheckoutService.QUOTE_CURRENCY;

    public static final Set<String> DISPLAY_CURRENCIES = Set.of(
            "USD", "JPY", "KRW", "THB", "SGD", "HKD", "CNY");

    public static final Set<String> ASSETS = Set.of("USDC", "USDT", "EURC");

    /** Frankfurter 실패 시 폴백: 1 USD ≈ 이만큼 EURC */
    private static final BigDecimal FALLBACK_EUR_PER_USD = new BigDecimal("0.920000");

    private final UrlPayDisplayFxService displayFxService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private volatile BigDecimal cachedEurPerUsd = FALLBACK_EUR_PER_USD;
    private volatile Instant eurCacheAt = Instant.EPOCH;

    public CryptoFxService(UrlPayDisplayFxService displayFxService, ObjectMapper objectMapper) {
        this.displayFxService = displayFxService;
        this.objectMapper = objectMapper;
    }

    public String normalizeDisplayCurrency(String raw) {
        String c = raw != null ? raw.trim().toUpperCase(Locale.ROOT) : "";
        if (c.isBlank() || !DISPLAY_CURRENCIES.contains(c)) {
            return QUOTE_CURRENCY;
        }
        return c;
    }

    public String normalizeAsset(String raw) {
        String a = raw != null ? raw.trim().toUpperCase(Locale.ROOT) : "";
        return ASSETS.contains(a) ? a : "USDC";
    }

    /**
     * 표시 금액 → 청구(USD).
     */
    public Optional<BigDecimal> toQuoteUsd(BigDecimal displayAmount, String displayCurrency) {
        if (displayAmount == null || displayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        String from = normalizeDisplayCurrency(displayCurrency);
        if (QUOTE_CURRENCY.equals(from)) {
            return Optional.of(scaleMoney(displayAmount, 2));
        }
        return displayFxService.convertAmount(displayAmount, from, QUOTE_CURRENCY)
                .map(v -> scaleMoney(v, 2));
    }

    /**
     * 청구(USD) → 자산 송금 금액.
     */
    public BigDecimal toAssetAmount(BigDecimal quoteUsd, String assetCode) {
        if (quoteUsd == null || quoteUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        String asset = normalizeAsset(assetCode);
        if ("USDC".equals(asset) || "USDT".equals(asset)) {
            return scaleMoney(quoteUsd, 6);
        }
        /* EURC: USD × (EUR per 1 USD) */
        BigDecimal eurPerUsd = resolveEurPerUsd();
        return scaleMoney(quoteUsd.multiply(eurPerUsd), 6);
    }

    public Map<String, Object> estimate(BigDecimal displayAmount, String displayCurrency, String assetCode) {
        Map<String, Object> m = new LinkedHashMap<>();
        String disp = normalizeDisplayCurrency(displayCurrency);
        String asset = normalizeAsset(assetCode);
        m.put("displayCurrency", disp);
        m.put("quoteCurrency", QUOTE_CURRENCY);
        m.put("assetCode", asset);
        if (displayAmount == null || displayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            m.put("displayAmount", null);
            m.put("quoteAmount", null);
            m.put("assetAmount", null);
            m.put("ok", false);
            return m;
        }
        m.put("displayAmount", scaleMoney(displayAmount, moneyScale(disp)));
        Optional<BigDecimal> quote = toQuoteUsd(displayAmount, disp);
        if (quote.isEmpty()) {
            m.put("ok", false);
            m.put("error", "FX_UNAVAILABLE");
            return m;
        }
        BigDecimal q = quote.get();
        BigDecimal assetAmt = toAssetAmount(q, asset);
        m.put("quoteAmount", q);
        m.put("assetAmount", assetAmt);
        m.put("eurPerUsd", resolveEurPerUsd());
        m.put("ok", true);
        return m;
    }

    private BigDecimal resolveEurPerUsd() {
        Instant now = Instant.now();
        if (cachedEurPerUsd != null && eurCacheAt != null
                && Duration.between(eurCacheAt, now).toMinutes() < 60) {
            return cachedEurPerUsd;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.frankfurter.app/latest?from=USD&to=EUR"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300 && res.body() != null) {
                JsonNode root = objectMapper.readTree(res.body());
                JsonNode eur = root.path("rates").path("EUR");
                if (eur.isNumber()) {
                    BigDecimal v = eur.decimalValue();
                    if (v.compareTo(BigDecimal.ZERO) > 0) {
                        cachedEurPerUsd = v;
                        eurCacheAt = now;
                        return cachedEurPerUsd;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("EURC FX fetch failed, using cache/fallback: {}", e.toString());
        }
        if (cachedEurPerUsd == null || cachedEurPerUsd.compareTo(BigDecimal.ZERO) <= 0) {
            cachedEurPerUsd = FALLBACK_EUR_PER_USD;
        }
        eurCacheAt = now;
        return cachedEurPerUsd;
    }

    private static int moneyScale(String ccy) {
        return ("JPY".equals(ccy) || "KRW".equals(ccy)) ? 0 : 2;
    }

    private static BigDecimal scaleMoney(BigDecimal v, int scale) {
        return v.setScale(scale, RoundingMode.HALF_UP);
    }
}
