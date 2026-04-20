package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 태국은행(BOT) 일평균 환율(DAILY_AVG_EXG_RATE) 조회.
 * 단위: 바트/1 외화 단위. BOT 표의 엔·원 등은 100·1000단위 고시가 흔해 1단위로 환산한다.
 * 본 서비스는 {@link BotRateAsOfMode}별로 <strong>방콕 달력일당 HTTP 1회</strong>만 호출하고 결과를 캐시한다(결제 시도마다 BOT를 두르지 않음).
 * <p>레거시: {@code https://iapi.bot.or.th} + 헤더 {@code api-key}.<br>
 * API 포털(IBM) Stat-ExchangeRate v2: {@code https://gateway.api.bot.or.th/Stat-ExchangeRate/v2} + 경로 {@code /DAILY_AVG_EXG_RATE/}
 * + 헤더 {@code Authorization}(구독 Client ID). OpenAPI: {@code clientIdHeader} → name {@code Authorization}.</p>
 */
@Service
public class BotThailandExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(BotThailandExchangeRateService.class);
    private static final ObjectMapper OM = new ObjectMapper();
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestTemplate restTemplate;

    private final String baseUrl;
    private final String dailyAvgPath;
    private final String apiKey;
    private final String apiKeyHeaderName;

    private final HqApiConfigRepository hqApiConfigRepository;

    private final AtomicReference<CachedRates> cache = new AtomicReference<>();

    public BotThailandExchangeRateService(
            HqApiConfigRepository hqApiConfigRepository,
            @Autowired(required = false) RestTemplate restTemplate,
            @Value("${pg.bot-thailand.base-url:https://iapi.bot.or.th}") String baseUrl,
            @Value("${pg.bot-thailand.daily-avg-path:/Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/}") String dailyAvgPath,
            @Value("${pg.bot-thailand.api-key:}") String apiKey,
            @Value("${pg.bot-thailand.api-key-header:api-key}") String apiKeyHeaderName) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "https://iapi.bot.or.th";
        this.dailyAvgPath = dailyAvgPath != null && !dailyAvgPath.isBlank() ? dailyAvgPath : "/Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.apiKeyHeaderName = (apiKeyHeaderName != null && !apiKeyHeaderName.isBlank())
                ? apiKeyHeaderName.trim()
                : "api-key";
    }

    /** DB(tb_hq_api_config)에 값이 있으면 우선, 없으면 yml·환경변수. */
    private ResolvedBotHttp resolved() {
        String effBase = baseUrl;
        String effPath = dailyAvgPath;
        String effKey = apiKey;
        String effHdr = apiKeyHeaderName;
        Optional<HqApiConfig> row = hqApiConfigRepository.findAll().stream().findFirst();
        if (row.isPresent()) {
            HqApiConfig c = row.get();
            if (c.getBotThailandApiKey() != null && !c.getBotThailandApiKey().isBlank()) {
                effKey = c.getBotThailandApiKey().trim();
            }
            if (c.getBotThailandBaseUrl() != null && !c.getBotThailandBaseUrl().isBlank()) {
                effBase = c.getBotThailandBaseUrl().trim().replaceAll("/+$", "");
            }
            if (c.getBotThailandDailyAvgPath() != null && !c.getBotThailandDailyAvgPath().isBlank()) {
                effPath = c.getBotThailandDailyAvgPath().trim();
            }
            if (c.getBotThailandApiKeyHeader() != null && !c.getBotThailandApiKeyHeader().isBlank()) {
                effHdr = c.getBotThailandApiKeyHeader().trim();
            }
        }
        if (effPath == null || effPath.isBlank()) {
            effPath = "/Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/";
        }
        if (effHdr == null || effHdr.isBlank()) {
            effHdr = "api-key";
        }
        return new ResolvedBotHttp(effBase, effPath, effKey, effHdr);
    }

    private record ResolvedBotHttp(String baseUrl, String dailyAvgPath, String apiKey, String apiKeyHeaderName) {}

    public boolean isConfigured() {
        return !resolved().apiKey().isEmpty();
    }

    /**
     * 레거시·단위테스트 호환: {@link BotRateAsOfMode#LATEST_BOT_PERIOD} 와 동일.
     */
    public synchronized Optional<BotDailyRates> fetchLatestThbPerUnitRates() {
        return fetchThbPerUnitRates(BotRateAsOfMode.LATEST_BOT_PERIOD);
    }

    /**
     * 방콕 달력일·모드별 BOT 일평균(THB/1외화). 같은 날·같은 모드에서는 HTTP 1회만 수행한다.
     */
    public synchronized Optional<BotDailyRates> fetchThbPerUnitRates(BotRateAsOfMode mode) {
        BotRateAsOfMode m = mode != null ? mode : BotRateAsOfMode.PREVIOUS_DAY_CLOSE;
        ZoneId bkk = ZoneId.of("Asia/Bangkok");
        LocalDate bangkokToday = LocalDate.now(bkk);
        String dayKey = bangkokToday.format(DF);
        CachedRates hit = cache.get();
        if (hit != null && m == hit.mode && dayKey.equals(hit.bangkokDay)) {
            return Optional.of(hit.rates);
        }

        ResolvedBotHttp cfg = resolved();
        if (cfg.apiKey().isEmpty()) {
            log.warn("BOT Thailand API key empty (결제로직설정·tb_hq_api_config 또는 pg.bot-thailand.api-key / BOT_THAILAND_API_KEY)");
            return Optional.empty();
        }
        LocalDate end = bangkokToday;
        LocalDate start = bangkokToday.minusDays(30);
        String path = cfg.dailyAvgPath().startsWith("/") ? cfg.dailyAvgPath() : "/" + cfg.dailyAvgPath();
        String url = UriComponentsBuilder
                .fromUriString(cfg.baseUrl() + path)
                .queryParam("start_period", start.format(DF))
                .queryParam("end_period", end.format(DF))
                .build(true)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(cfg.apiKeyHeaderName(), cfg.apiKey());

        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = res.getBody();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = OM.readTree(body);
            JsonNode success = root.path("result").path("success");
            if (!success.isMissingNode() && !success.isNull() && !"true".equalsIgnoreCase(success.asText(""))) {
                log.warn("BOT exchange API success!=true: {}", success.asText(""));
            }
            List<JsonNode> rows = extractDailyDataDetailRows(root);
            if (rows.isEmpty()) {
                log.warn("BOT exchange: empty data_detail (url={}, header={})", url, cfg.apiKeyHeaderName());
                return Optional.empty();
            }
            Optional<String> targetPeriod = resolveTargetPeriod(rows, m, bangkokToday);
            if (targetPeriod.isEmpty()) {
                log.warn("BOT exchange: could not resolve period (mode={})", m);
                return Optional.empty();
            }
            String period = targetPeriod.get();
            Optional<BotDailyRates> built = buildDailyRatesForPeriod(rows, period);
            if (built.isEmpty()) {
                return Optional.empty();
            }
            BotDailyRates out = built.get();
            cache.set(new CachedRates(dayKey, m, out, System.currentTimeMillis()));
            return Optional.of(out);
        } catch (Exception e) {
            log.warn("BOT exchange fetch failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> resolveTargetPeriod(List<JsonNode> rows, BotRateAsOfMode mode, LocalDate bangkokToday) {
        TreeSet<LocalDate> dates = collectEligibleDates(rows, bangkokToday);
        if (dates.isEmpty()) {
            return Optional.empty();
        }
        if (mode == BotRateAsOfMode.LATEST_BOT_PERIOD) {
            return Optional.of(dates.last().format(DF));
        }
        LocalDate anchor = bangkokToday.minusDays(1);
        for (int walk = 0; walk < 45; walk++) {
            LocalDate floor = dates.floor(anchor);
            if (floor != null) {
                return Optional.of(floor.format(DF));
            }
            anchor = anchor.minusDays(1);
        }
        return Optional.of(dates.last().format(DF));
    }

    /** 오늘(방콕) 이하 고시일만 담고, 없으면 테스트·샘플용으로 미래 고시일까지 포함한다. */
    private static TreeSet<LocalDate> collectEligibleDates(List<JsonNode> rows, LocalDate bangkokToday) {
        TreeSet<LocalDate> dates = new TreeSet<>();
        for (JsonNode row : rows) {
            LocalDate d = parsePeriodLocalDate(row.path("period").asText(""));
            if (d != null && !d.isAfter(bangkokToday)) {
                dates.add(d);
            }
        }
        if (!dates.isEmpty()) {
            return dates;
        }
        for (JsonNode row : rows) {
            LocalDate d = parsePeriodLocalDate(row.path("period").asText(""));
            if (d != null) {
                dates.add(d);
            }
        }
        return dates;
    }

    private static LocalDate parsePeriodLocalDate(String period) {
        if (period == null || period.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(period.trim(), DF);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * BOT iAPI·게이트웨이 v2 등에서 통화 영문명 필드 키가 버전마다 달라 여러 키·스캔으로 읽는다.
     */
    private static String botRowCurrencyNameUpper(JsonNode row) {
        String[] keys = {
                "currency_name_eng",
                "currency_name_ENG",
                "Currency_Name_ENG",
                "curr_name_en",
                "descr_eng",
                "DESCRIPTION_ENG"
        };
        for (String k : keys) {
            JsonNode v = row.get(k);
            if (v != null && !v.isNull()) {
                String t = v.asText("").trim();
                if (!t.isBlank()) {
                    return t.toUpperCase(Locale.ROOT);
                }
            }
        }
        Iterator<String> it = row.fieldNames();
        while (it.hasNext()) {
            String fn = it.next();
            if (fn == null) {
                continue;
            }
            String fl = fn.toLowerCase(Locale.ROOT);
            if (fl.contains("currency")
                    && (fl.contains("name") || fl.contains("descr"))
                    && fl.contains("eng")) {
                JsonNode v = row.get(fn);
                if (v != null && !v.isNull()) {
                    String t = v.asText("").trim();
                    if (!t.isBlank()) {
                        return t.toUpperCase(Locale.ROOT);
                    }
                }
            }
        }
        return "";
    }

    /** {@code mid_rate} 외 숫자/별칭 필드(포털 v2·레거시 iAPI). */
    private static String botRowMidText(JsonNode row) {
        String[] keys = {"mid_rate", "MID_RATE", "Middle_Rate", "middle_rate", "avg_mid_rate", "rate"};
        for (String k : keys) {
            if (!row.has(k)) {
                continue;
            }
            JsonNode v = row.get(k);
            if (v == null || v.isNull()) {
                continue;
            }
            if (v.isNumber()) {
                return v.decimalValue().stripTrailingZeros().toPlainString();
            }
            String t = v.asText("").trim();
            if (!t.isBlank()) {
                return t;
            }
        }
        Iterator<String> it = row.fieldNames();
        while (it.hasNext()) {
            String fn = it.next();
            if (fn == null) {
                continue;
            }
            String fl = fn.toLowerCase(Locale.ROOT);
            if (fl.contains("mid") && fl.contains("rate")) {
                JsonNode v = row.get(fn);
                if (v != null && !v.isNull()) {
                    if (v.isNumber()) {
                        return v.decimalValue().stripTrailingZeros().toPlainString();
                    }
                    String t = v.asText("").trim();
                    if (!t.isBlank()) {
                        return t;
                    }
                }
            }
        }
        return "";
    }

    /**
     * 일평균 표의 대한민국 원 행(북한 원화·명시적 배제 문자열 제외).
     */
    private static boolean isKrwBotRow(String n) {
        if (n == null || n.isBlank()) {
            return false;
        }
        String u = n.toUpperCase(Locale.ROOT);
        if (u.contains("NORTH KOREA")
                || u.contains("NORTH KOREAN")
                || u.contains("D.P.R.K")
                || u.contains("DPRK")
                || u.contains("N.KOREA")
                || u.contains("N. KOREA")) {
            return false;
        }
        if (u.contains("KRW")) {
            return true;
        }
        if (u.contains("S.KOREA") || u.contains("S. KOREA") || u.contains("SOUTH KOREA")) {
            return true;
        }
        if (u.contains("REP.OF KOREA")
                || u.contains("REP. OF KOREA")
                || u.contains("REP OF KOREA")
                || u.contains("REPUBLIC OF KOREA")) {
            return true;
        }
        if (u.contains("KOREAN WON")) {
            return true;
        }
        return u.contains("WON") && (u.contains("KOREA") || u.contains("KOREAN"));
    }

    private static Optional<BotDailyRates> buildDailyRatesForPeriod(List<JsonNode> rows, String targetPeriod) {
        BigDecimal bestJpy = null;
        int bestJpyPri = -1;
        BigDecimal bestKrw = null;
        int bestKrwPri = -1;
        BigDecimal bestUsd = null;
        int bestUsdPri = -1;
        BigDecimal bestSgd = null;
        int bestSgdPri = -1;
        BigDecimal bestHkd = null;
        int bestHkdPri = -1;
        BigDecimal bestCny = null;
        int bestCnyPri = -1;
        for (JsonNode row : rows) {
            if (!targetPeriod.equals(row.path("period").asText("").trim())) {
                continue;
            }
            String name = botRowCurrencyNameUpper(row);
            String mid = botRowMidText(row);
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
                int pri = botJpyRowPriority(name);
                BigDecimal perOne = botThbPerOneJpy(name, rate);
                if (pri > bestJpyPri) {
                    bestJpyPri = pri;
                    bestJpy = perOne;
                }
            }
            if (isUsdRow(name)) {
                int pri = botUsdRowPriority(name);
                BigDecimal perOne = botThbPerOneMajor(name, rate, "USD");
                if (pri > bestUsdPri) {
                    bestUsdPri = pri;
                    bestUsd = perOne;
                }
            }
            if (isKrwBotRow(name)) {
                int pri = botKrwRowPriority(name);
                BigDecimal perOne = botThbPerOneKrw(name, rate);
                if (pri > bestKrwPri) {
                    bestKrwPri = pri;
                    bestKrw = perOne;
                }
            }
            if (isSgdRow(name)) {
                int pri = botMajorRowPriority(name, "SGD", "SINGAPORE");
                BigDecimal perOne = botThbPerOneMajor(name, rate, "SGD");
                if (pri > bestSgdPri) {
                    bestSgdPri = pri;
                    bestSgd = perOne;
                }
            }
            if (isHkdRow(name)) {
                int pri = botMajorRowPriority(name, "HKD", "HONG KONG");
                BigDecimal perOne = botThbPerOneMajor(name, rate, "HKD");
                if (pri > bestHkdPri) {
                    bestHkdPri = pri;
                    bestHkd = perOne;
                }
            }
            if (isCnyRow(name)) {
                int pri = botCnyRowPriority(name);
                BigDecimal perOne = botThbPerOneMajor(name, rate, "CNY");
                if (pri > bestCnyPri) {
                    bestCnyPri = pri;
                    bestCny = perOne;
                }
            }
        }
        if (bestJpy == null && bestUsd == null && bestKrw == null && bestSgd == null && bestHkd == null && bestCny == null) {
            log.warn("BOT daily avg: no supported currency row for period {}", targetPeriod);
            return Optional.empty();
        }
        return Optional.of(new BotDailyRates(targetPeriod, bestJpy, bestUsd, bestKrw, bestSgd, bestHkd, bestCny));
    }

    private static boolean isUsdRow(String n) {
        return n.contains("USD") || (n.contains("DOLLAR") && n.contains("US"));
    }

    private static boolean isSgdRow(String n) {
        return n.contains("SGD") || n.contains("SINGAPORE");
    }

    private static boolean isHkdRow(String n) {
        return n.contains("HKD") || n.contains("HONG KONG");
    }

    private static boolean isCnyRow(String n) {
        if (n.contains("JPY") || n.contains("YEN")) {
            return false;
        }
        return n.contains("CNY")
                || n.contains("CNH")
                || n.contains("RMB")
                || (n.contains("YUAN") && (n.contains("CHINA") || n.contains("RENMINBI")));
    }

    private static int botUsdRowPriority(String n) {
        if (n.contains("1") && n.contains("USD")) {
            return 5;
        }
        if (n.contains("USD100") || isQuotedPer100Major(n, "USD")) {
            return 4;
        }
        if (n.contains("USD")) {
            return 3;
        }
        if (n.contains("DOLLAR") && n.contains("US")) {
            return 2;
        }
        return 0;
    }

    private static int botMajorRowPriority(String n, String code3, String geoToken) {
        String c = code3.toUpperCase(Locale.ROOT);
        if (n.contains(c + "100") || isQuotedPer100Major(n, c)) {
            return 4;
        }
        if (n.contains(c)) {
            return 3;
        }
        if (geoToken != null && n.contains(geoToken)) {
            return 2;
        }
        return 0;
    }

    private static int botCnyRowPriority(String n) {
        if (n.contains("CNY100") || n.contains("CNH100") || isQuotedPer100Major(n, "CNY") || isQuotedPer100Major(n, "CNH")) {
            return 4;
        }
        if (n.contains("CNY") || n.contains("CNH")) {
            return 3;
        }
        if (n.contains("RMB") || (n.contains("YUAN") && n.contains("CHINA"))) {
            return 2;
        }
        return 0;
    }

    private static boolean isQuotedPer100Major(String n, String code3) {
        String c = code3.toUpperCase(Locale.ROOT);
        if (n.contains(c + "100")) {
            return true;
        }
        if (!n.contains("100") || n.contains("1000")) {
            return false;
        }
        return n.contains(c);
    }

    private static BigDecimal botThbPerOneMajor(String n, BigDecimal mid, String code3) {
        if ("CNY".equalsIgnoreCase(code3)) {
            if (isQuotedPer100Major(n, "CNY") || isQuotedPer100Major(n, "CNH")) {
                return mid.divide(new BigDecimal("100"), 12, RoundingMode.HALF_UP);
            }
            return mid;
        }
        if (isQuotedPer100Major(n, code3)) {
            return mid.divide(new BigDecimal("100"), 12, RoundingMode.HALF_UP);
        }
        return mid;
    }

    /** BOT 일평균: 통상 {@code JPY100} 행은 100엔당 바트 → 1엔당으로 나눈다. */
    private static boolean isBotJpyQuotedPer100(String currencyNameUpper) {
        if (currencyNameUpper.contains("JPY100")) {
            return true;
        }
        return currencyNameUpper.contains("100")
                && (currencyNameUpper.contains("JPY") || currencyNameUpper.contains("YEN"));
    }

    private static BigDecimal botThbPerOneJpy(String currencyNameUpper, BigDecimal midFromBot) {
        if (isBotJpyQuotedPer100(currencyNameUpper)) {
            return midFromBot.divide(new BigDecimal("100"), 12, RoundingMode.HALF_UP);
        }
        return midFromBot;
    }

    /** {@code JPY100} 등 명시 행을 우선(다중 행 시). */
    private static int botJpyRowPriority(String n) {
        if (n.contains("JPY100")) {
            return 3;
        }
        if (isBotJpyQuotedPer100(n)) {
            return 2;
        }
        if (n.contains("JPY") || n.contains("YEN")) {
            return 1;
        }
        return 0;
    }

    private static boolean isBotKrwQuotedPer1000(String n) {
        if (!isKrwBotRow(n) || !n.contains("1000")) {
            return false;
        }
        /* BOT: 「#REP. OF KOREA (1000)」 등 원화 단위가 명에만 있고 KRW/WON 문자가 없을 수 있음 */
        return n.contains("KRW")
                || n.contains("WON")
                || n.contains("KOREA")
                || n.contains("KOREAN");
    }

    private static boolean isBotKrwQuotedPer100(String n) {
        if (isBotKrwQuotedPer1000(n)) {
            return false;
        }
        if (!isKrwBotRow(n)) {
            return false;
        }
        return n.contains("KRW100") || (n.contains("100") && (n.contains("KRW") || n.contains("WON")));
    }

    private static BigDecimal botThbPerOneKrw(String currencyNameUpper, BigDecimal midFromBot) {
        if (isBotKrwQuotedPer1000(currencyNameUpper)) {
            return midFromBot.divide(new BigDecimal("1000"), 12, RoundingMode.HALF_UP);
        }
        if (isBotKrwQuotedPer100(currencyNameUpper)) {
            return midFromBot.divide(new BigDecimal("100"), 12, RoundingMode.HALF_UP);
        }
        return midFromBot;
    }

    private static int botKrwRowPriority(String n) {
        if (!isKrwBotRow(n)) {
            return 0;
        }
        if (n.contains("KRW100") || n.contains("1000")) {
            return 3;
        }
        if (isBotKrwQuotedPer100(n)) {
            return 2;
        }
        return 1;
    }

    /** iAPI·게이트웨이 v2 모두: {@code result.data.data_detail} 배열 또는 단일 객체. */
    private static List<JsonNode> extractDailyDataDetailRows(JsonNode root) {
        JsonNode detail = root.path("result").path("data").path("data_detail");
        List<JsonNode> out = new ArrayList<>();
        if (detail.isArray()) {
            detail.forEach(out::add);
            return out;
        }
        if (detail.isObject() && !detail.path("period").asText("").isBlank()) {
            out.add(detail);
            return out;
        }
        return out;
    }

    public record BotDailyRates(
            String period,
            BigDecimal thbPerJpy,
            BigDecimal thbPerUsd,
            BigDecimal thbPerKrw,
            BigDecimal thbPerSgd,
            BigDecimal thbPerHkd,
            BigDecimal thbPerCny) {}

    private static final class CachedRates {
        final String bangkokDay;
        final BotRateAsOfMode mode;
        final BotDailyRates rates;
        final long fetchedAtMs;

        CachedRates(String bangkokDay, BotRateAsOfMode mode, BotDailyRates rates, long fetchedAtMs) {
            this.bangkokDay = bangkokDay;
            this.mode = mode;
            this.rates = rates;
            this.fetchedAtMs = fetchedAtMs;
        }
    }
}
