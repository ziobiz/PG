package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 태국은행(BOT) 일평균 환율(DAILY_AVG_EXG_RATE) 조회.
 * 단위: 바트/1 외화 단위(보고서 표기와 동일).
 */
@Service
public class BotThailandExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(BotThailandExchangeRateService.class);
    private static final ObjectMapper OM = new ObjectMapper();
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String baseUrl;
    private final String dailyAvgPath;
    private final String apiKey;

    private final AtomicReference<CachedRates> cache = new AtomicReference<>();

    public BotThailandExchangeRateService(
            @Value("${pg.bot-thailand.base-url:https://iapi.bot.or.th}") String baseUrl,
            @Value("${pg.bot-thailand.daily-avg-path:/Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/}") String dailyAvgPath,
            @Value("${pg.bot-thailand.api-key:}") String apiKey) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "https://iapi.bot.or.th";
        this.dailyAvgPath = dailyAvgPath != null && !dailyAvgPath.isBlank() ? dailyAvgPath : "/Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /**
     * 최신 일자 기준 JPY·USD 에 대한 THB/1단위 환율.
     */
    public synchronized Optional<BotDailyRates> fetchLatestThbPerUnitRates() {
        long now = System.currentTimeMillis();
        CachedRates c = cache.get();
        if (c != null && now - c.fetchedAtMs < 60_000L) {
            return Optional.of(c.rates);
        }
        if (!isConfigured()) {
            log.warn("BOT Thailand API key empty (pg.bot-thailand.api-key / BOT_THAILAND_API_KEY)");
            return Optional.empty();
        }
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(7);
        String path = dailyAvgPath.startsWith("/") ? dailyAvgPath : "/" + dailyAvgPath;
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + path)
                .queryParam("start_period", start.format(DF))
                .queryParam("end_period", end.format(DF))
                .build(true)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);

        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = res.getBody();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = OM.readTree(body);
            JsonNode success = root.path("result").path("success");
            if (!"true".equalsIgnoreCase(success.asText(""))) {
                log.warn("BOT exchange API success!=true: {}", body.length() > 500 ? body.substring(0, 500) : body);
                return Optional.empty();
            }
            JsonNode detail = root.path("result").path("data").path("data_detail");
            if (!detail.isArray() || detail.isEmpty()) {
                return Optional.empty();
            }
            String latestPeriod = "";
            Map<String, BigDecimal> jpy = new HashMap<>();
            Map<String, BigDecimal> usd = new HashMap<>();
            Map<String, BigDecimal> krw = new HashMap<>();
            for (JsonNode row : detail) {
                String period = row.path("period").asText("").trim();
                if (period.isEmpty()) {
                    continue;
                }
                if (latestPeriod.isEmpty() || period.compareTo(latestPeriod) > 0) {
                    latestPeriod = period;
                }
            }
            if (latestPeriod.isEmpty()) {
                return Optional.empty();
            }
            for (JsonNode row : detail) {
                if (!latestPeriod.equals(row.path("period").asText("").trim())) {
                    continue;
                }
                String name = row.path("currency_name_eng").asText("").toUpperCase(Locale.ROOT);
                String mid = row.path("mid_rate").asText("").trim();
                if (mid.isEmpty()) {
                    continue;
                }
                BigDecimal rate;
                try {
                    rate = new BigDecimal(mid);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (name.contains("JPY") || name.contains("YEN")) {
                    jpy.put(name, rate);
                }
                if (name.contains("USD") || (name.contains("DOLLAR") && name.contains("US"))) {
                    usd.put(name, rate);
                }
                if (name.contains("KRW")
                        || (name.contains("WON") && (name.contains("KOREA") || name.contains("KOREAN")))) {
                    krw.put(name, rate);
                }
            }
            Optional<BigDecimal> jpyRate = jpy.values().stream().findFirst();
            Optional<BigDecimal> usdRate = usd.values().stream().findFirst();
            Optional<BigDecimal> krwRate = krw.values().stream().findFirst();
            if (jpyRate.isEmpty() && usdRate.isEmpty() && krwRate.isEmpty()) {
                log.warn("BOT daily avg: no JPY/USD/KRW row for period {}", latestPeriod);
                return Optional.empty();
            }
            BotDailyRates out = new BotDailyRates(latestPeriod,
                    jpyRate.orElse(null),
                    usdRate.orElse(null),
                    krwRate.orElse(null));
            cache.set(new CachedRates(out, now));
            return Optional.of(out);
        } catch (Exception e) {
            log.warn("BOT exchange fetch failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record BotDailyRates(String period, BigDecimal thbPerJpy, BigDecimal thbPerUsd, BigDecimal thbPerKrw) {}

    private static final class CachedRates {
        final BotDailyRates rates;
        final long fetchedAtMs;

        CachedRates(BotDailyRates rates, long fetchedAtMs) {
            this.rates = rates;
            this.fetchedAtMs = fetchedAtMs;
        }
    }
}
