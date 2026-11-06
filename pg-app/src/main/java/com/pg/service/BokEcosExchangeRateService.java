package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 한국은행 ECOS Open API — 통계표 {@code 731Y001}(주요국 통화의 대원화환율) 일별 매매기준율.
 * <p>단위는 원/외화(엔은 원/100엔). 결제 FX에는 1외화당 KRW 로 환산한다.
 * URL: {@code /api/StatisticSearch/{key}/json/kr/1/N/731Y001/D/{from}/{to}/{itemCode}}</p>
 */
@Service
public class BokEcosExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(BokEcosExchangeRateService.class);
    private static final ObjectMapper OM = new ObjectMapper();
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String STAT_CODE = "731Y001";

    /** ISO → (항목코드, 고시배수: 엔=100 등) */
    private static final Map<String, ItemSpec> ITEMS = Map.of(
            "USD", new ItemSpec("0000001", 1),
            "JPY", new ItemSpec("0000002", 100),
            "EUR", new ItemSpec("0000003", 1),
            "GBP", new ItemSpec("0000012", 1),
            "CAD", new ItemSpec("0000013", 1),
            "HKD", new ItemSpec("0000015", 1),
            "AUD", new ItemSpec("0000017", 1),
            "SGD", new ItemSpec("0000024", 1),
            "THB", new ItemSpec("0000028", 1),
            "CNY", new ItemSpec("0000053", 1)
    );

    private final RestTemplate restTemplate;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final String ymlApiKey;
    private final String ymlBaseUrl;

    private final AtomicReference<CachedBundle> cache = new AtomicReference<>();

    public BokEcosExchangeRateService(
            HqApiConfigRepository hqApiConfigRepository,
            @Autowired(required = false) RestTemplate restTemplate,
            @Value("${pg.bok-ecos.api-key:}") String apiKey,
            @Value("${pg.bok-ecos.base-url:https://ecos.bok.or.kr/api}") String baseUrl) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
        this.ymlApiKey = apiKey != null ? apiKey.trim() : "";
        this.ymlBaseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "https://ecos.bok.or.kr/api";
    }

    public boolean isConfigured() {
        return !resolveApiKey().isEmpty();
    }

    /**
     * 1 {@code fromCcy} 당 {@code toCcy} 금액.
     * ECOS는 대원(KRW) 고시이므로 KRW를 경유한다. 같은 통화면 1.
     */
    public Optional<BigDecimal> settlementPerUnit(String fromCcy, String toCcy) {
        String from = norm(fromCcy);
        String to = norm(toCcy);
        if (from.isEmpty() || to.isEmpty()) {
            return Optional.empty();
        }
        if (from.equals(to)) {
            return Optional.of(BigDecimal.ONE);
        }
        Optional<Map<String, BigDecimal>> ratesOpt = fetchKrwPerUnitRates();
        if (ratesOpt.isEmpty()) {
            return Optional.empty();
        }
        Map<String, BigDecimal> krwPer = ratesOpt.get();
        BigDecimal kFrom = "KRW".equals(from) ? BigDecimal.ONE : krwPer.get(from);
        BigDecimal kTo = "KRW".equals(to) ? BigDecimal.ONE : krwPer.get(to);
        if (kFrom == null || kTo == null
                || kFrom.compareTo(BigDecimal.ZERO) <= 0
                || kTo.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        /* 1 from = kFrom KRW, 1 to = kTo KRW → to per 1 from = kFrom/kTo */
        return Optional.of(kFrom.divide(kTo, 12, RoundingMode.HALF_UP));
    }

    public Optional<String> latestRatePeriod() {
        CachedBundle c = cache.get();
        if (c != null && c.seoulDay.equals(LocalDate.now(SEOUL).toString())) {
            return Optional.ofNullable(c.period);
        }
        return fetchKrwPerUnitRates().map(m -> {
            CachedBundle b = cache.get();
            return b != null ? b.period : null;
        });
    }

    private synchronized Optional<Map<String, BigDecimal>> fetchKrwPerUnitRates() {
        String day = LocalDate.now(SEOUL).toString();
        CachedBundle hit = cache.get();
        if (hit != null && day.equals(hit.seoulDay) && hit.rates != null && !hit.rates.isEmpty()) {
            return Optional.of(hit.rates);
        }
        String key = resolveApiKey();
        if (key.isEmpty()) {
            log.warn("ECOS API key missing (HQ bokEcosApiKey / BOK_ECOS_API_KEY)");
            return Optional.empty();
        }
        LocalDate end = LocalDate.now(SEOUL);
        LocalDate start = end.minusDays(21);
        Map<String, BigDecimal> out = new HashMap<>();
        String bestPeriod = null;
        for (Map.Entry<String, ItemSpec> e : ITEMS.entrySet()) {
            Optional<RateRow> row = fetchLatestItem(key, e.getValue().itemCode(), start, end);
            if (row.isEmpty()) {
                continue;
            }
            BigDecimal raw = row.get().value();
            if (raw == null || raw.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            int mult = e.getValue().quoteMultiplier();
            BigDecimal perOne = mult <= 1
                    ? raw
                    : raw.divide(BigDecimal.valueOf(mult), 12, RoundingMode.HALF_UP);
            out.put(e.getKey(), perOne);
            if (bestPeriod == null || row.get().time().compareTo(bestPeriod) > 0) {
                bestPeriod = row.get().time();
            }
        }
        if (out.isEmpty()) {
            log.warn("ECOS 731Y001: no currency rows for {}..{}", start, end);
            return Optional.empty();
        }
        cache.set(new CachedBundle(day, bestPeriod, Map.copyOf(out)));
        log.info("ECOS FX cached period={} currencies={}", bestPeriod, out.keySet());
        return Optional.of(out);
    }

    private Optional<RateRow> fetchLatestItem(String apiKey, String itemCode, LocalDate start, LocalDate end) {
        String url = resolveBaseUrl() + "/StatisticSearch/" + apiKey + "/json/kr/1/30/"
                + STAT_CODE + "/D/" + start.format(YMD) + "/" + end.format(YMD) + "/" + itemCode;
        try {
            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = OM.readTree(body);
            if (root.has("RESULT")) {
                String code = root.path("RESULT").path("CODE").asText("");
                if (!code.isBlank() && !"INFO-000".equalsIgnoreCase(code)) {
                    log.warn("ECOS item {} RESULT {} {}", itemCode, code,
                            root.path("RESULT").path("MESSAGE").asText(""));
                    return Optional.empty();
                }
            }
            JsonNode rows = root.path("StatisticSearch").path("row");
            if (!rows.isArray() || rows.isEmpty()) {
                return Optional.empty();
            }
            RateRow best = null;
            for (Iterator<JsonNode> it = rows.elements(); it.hasNext(); ) {
                JsonNode n = it.next();
                String time = n.path("TIME").asText("").trim();
                String raw = n.path("DATA_VALUE").asText("").trim().replace(",", "");
                if (time.isEmpty() || raw.isEmpty()) {
                    continue;
                }
                BigDecimal v;
                try {
                    v = new BigDecimal(raw);
                } catch (Exception ex) {
                    continue;
                }
                if (best == null || time.compareTo(best.time()) > 0) {
                    best = new RateRow(time, v);
                }
            }
            return Optional.ofNullable(best);
        } catch (Exception e) {
            log.warn("ECOS fetch item {} failed: {}", itemCode, e.getMessage());
            return Optional.empty();
        }
    }

    private String resolveApiKey() {
        Optional<HqApiConfig> row = hqApiConfigRepository.findAll().stream().findFirst();
        if (row.isPresent() && row.get().getBokEcosApiKey() != null && !row.get().getBokEcosApiKey().isBlank()) {
            return row.get().getBokEcosApiKey().trim();
        }
        return ymlApiKey;
    }

    private String resolveBaseUrl() {
        return ymlBaseUrl != null && !ymlBaseUrl.isBlank() ? ymlBaseUrl.replaceAll("/+$", "")
                : "https://ecos.bok.or.kr/api";
    }

    private static String norm(String c) {
        return c != null ? c.trim().toUpperCase(Locale.ROOT) : "";
    }

    private record ItemSpec(String itemCode, int quoteMultiplier) {}

    private record RateRow(String time, BigDecimal value) {}

    private record CachedBundle(String seoulDay, String period, Map<String, BigDecimal> rates) {}
}
