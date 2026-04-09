package com.pg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * URL 결제 「표시통화 → 실결제 통화」 본사 설정.
 * <ul>
 *   <li>전역 {@code enabled}, 갱신 주기, 견적 TTL, {@code marginByCurrency} (표시통화 JPY·USD·KRW별 마진)</li>
 *   <li>PG별 {@code pgSettings}: DISPLAY 시 표시·실결제 통화(THB/USD/JPY/KRW), FX 자동(BOT)·수동, PG 단일 마진 또는 {@code marginByDisplayCurrency}</li>
 *   <li>레거시: {@code pgSettings} 없을 때 가맹점 바인딩 {@code DISPLAY_FX_THB} + 전역 마진, 실결제 THB</li>
 * </ul>
 * 청구: {@code 실결제금액 = 표시금액 × (실결제/1표시단위) × (1+마진)} (소수 자릿수는 실결제 통화 기준).
 */
@Service
public class UrlPayDisplayFxService {

    public static final String MODE_DISPLAY_FX_THB = "DISPLAY_FX_THB";
    private static final String CHECKOUT = "CHECKOUT_CURRENCY";

    private static final Set<String> DISPLAY_CURRENCIES = Set.of("JPY", "USD", "KRW");
    private static final Set<String> SETTLEMENT_CURRENCIES = Set.of("THB", "USD", "JPY", "KRW");

    private static final ObjectMapper OM = new ObjectMapper();

    private final HqApiConfigRepository hqApiConfigRepository;
    private final BotThailandExchangeRateService botThailandExchangeRateService;

    private final String configuredHmacSecret;

    public UrlPayDisplayFxService(
            HqApiConfigRepository hqApiConfigRepository,
            BotThailandExchangeRateService botThailandExchangeRateService,
            @Value("${pg.url-pay-fx.quote-hmac-secret:}") String configuredHmacSecret) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.botThailandExchangeRateService = botThailandExchangeRateService;
        this.configuredHmacSecret = configuredHmacSecret != null ? configuredHmacSecret.trim() : "";
    }

    public boolean isHqFeatureEnabled() {
        JsonNode cfg = readHqFxJson();
        return cfg.path("enabled").asBoolean(false);
    }

    public int refreshSeconds() {
        JsonNode cfg = readHqFxJson();
        int s = cfg.path("refreshSeconds").asInt(600);
        return Math.max(60, Math.min(3600, s));
    }

    public int quoteTtlSeconds() {
        JsonNode cfg = readHqFxJson();
        int s = cfg.path("quoteTtlSeconds").asInt(600);
        return Math.max(120, Math.min(3600, s));
    }

    /**
     * 운영 PG 기준 URL 결제 금액 모드. {@code pgSettings} 우선, 없으면 가맹점 바인딩 레거시.
     */
    public String resolveUrlPayPricingMode(String operationalPgCd, String merchantBindingUrlPayMode) {
        if (!isHqFeatureEnabled()) {
            return CHECKOUT;
        }
        String pgU = normalizePgCd(operationalPgCd);
        JsonNode pgNode = pgSettingsNode(pgU);
        if (pgNode != null && !pgNode.isMissingNode() && pgNode.size() > 0) {
            if (!"DISPLAY".equalsIgnoreCase(pgNode.path("amountMode").asText(""))) {
                return CHECKOUT;
            }
            String setStr = pgNode.path("settlementCurrency").asText("THB").trim().toUpperCase(Locale.ROOT);
            if (setStr.isEmpty()) {
                setStr = "THB";
            }
            if (!SETTLEMENT_CURRENCIES.contains(setStr)) {
                return CHECKOUT;
            }
            if (!isPgFxRunnable(pgU, pgNode)) {
                return CHECKOUT;
            }
            return MODE_DISPLAY_FX_THB;
        }
        if (MODE_DISPLAY_FX_THB.equalsIgnoreCase(merchantBindingUrlPayMode != null ? merchantBindingUrlPayMode.trim() : "")) {
            return MODE_DISPLAY_FX_THB;
        }
        return CHECKOUT;
    }

    /** PG별 설정된 실결제 통화(레거시·미설정 시 THB). */
    public String settlementCurrencyForPg(String operationalPgCd) {
        JsonNode pgNode = pgSettingsNode(normalizePgCd(operationalPgCd));
        if (pgNode == null || pgNode.isMissingNode() || pgNode.size() == 0) {
            return "THB";
        }
        return defaultSettlementForBuild(pgNode);
    }

    /** 결제 페이지 기본 표시 통화 (PG별 설정, 없으면 JPY). */
    public String defaultDisplayCurrencyForPg(String operationalPgCd) {
        JsonNode pgNode = pgSettingsNode(normalizePgCd(operationalPgCd));
        String c = pgNode.path("displayCurrency").asText("JPY").trim().toUpperCase(Locale.ROOT);
        return DISPLAY_CURRENCIES.contains(c) ? c : "JPY";
    }

    public BigDecimal marginFor(String displayCurrency) {
        return marginFor(null, displayCurrency);
    }

    public BigDecimal marginFor(String pgCd, String displayCurrency) {
        String cur = displayCurrency != null ? displayCurrency.trim().toUpperCase(Locale.ROOT) : "";
        JsonNode pgNode = pgSettingsNode(normalizePgCd(pgCd));
        if (pgNode != null && !pgNode.isMissingNode() && pgNode.has("marginByDisplayCurrency")) {
            JsonNode byDisp = pgNode.get("marginByDisplayCurrency");
            if (byDisp != null && byDisp.isObject() && cur.length() == 3 && byDisp.has(cur)) {
                try {
                    BigDecimal mr = new BigDecimal(byDisp.get(cur).asText());
                    if (mr.compareTo(BigDecimal.ZERO) >= 0) {
                        return mr;
                    }
                } catch (Exception ignored) {
                    /* fall through */
                }
            }
        }
        if (pgNode != null && pgNode.has("marginRate") && !pgNode.get("marginRate").isNull()) {
            try {
                BigDecimal mr = new BigDecimal(pgNode.get("marginRate").asText());
                if (mr.compareTo(BigDecimal.ZERO) >= 0) {
                    return mr;
                }
            } catch (Exception ignored) {
                /* fall through */
            }
        }
        JsonNode cfg = readHqFxJson();
        JsonNode m = cfg.path("marginByCurrency");
        if (cur.isEmpty() || !m.has(cur)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal v = new BigDecimal(m.get(cur).asText());
            return v.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : v;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public Optional<QuoteResult> buildQuote(String compId, String displayCurrency, String operationalPgCd) {
        if (!isHqFeatureEnabled()) {
            return Optional.empty();
        }
        String pgU = normalizePgCd(operationalPgCd);
        JsonNode pgNode = pgSettingsNode(pgU);
        if (pgNode != null && pgNode.size() > 0) {
            if (!"DISPLAY".equalsIgnoreCase(pgNode.path("amountMode").asText(""))) {
                return Optional.empty();
            }
        }
        String cur = normalizeDisplayCurrency(displayCurrency);
        if (cur == null) {
            return Optional.empty();
        }
        String settlement = defaultSettlementForBuild(pgNode);
        String fxMode = pgNode != null && pgNode.size() > 0
                ? pgNode.path("fxMode").asText("AUTO").trim().toUpperCase(Locale.ROOT)
                : "AUTO";
        BigDecimal settlementPerUnit;
        String period;
        String rateDesc;
        if ("MANUAL".equals(fxMode)) {
            settlementPerUnit = resolveManualSettlementPerUnit(pgNode, settlement);
            if (settlementPerUnit == null) {
                return Optional.empty();
            }
            period = "MANUAL";
            rateDesc = "MANUAL fixed " + settlement + " per 1 " + cur;
        } else {
            Optional<BigDecimal> auto = computeAutoSettlementPerUnit(cur, settlement);
            if (auto.isEmpty()) {
                return Optional.empty();
            }
            settlementPerUnit = auto.get();
            BotThailandExchangeRateService.BotDailyRates rates =
                    botThailandExchangeRateService.fetchLatestThbPerUnitRates().orElse(null);
            period = rates != null && rates.period() != null ? rates.period() : "";
            rateDesc = "BOT DAILY_AVG (bridge THB), " + settlement + " per 1 " + cur;
        }
        BigDecimal margin = marginFor(pgU.isEmpty() ? null : pgU, cur);
        long exp = System.currentTimeMillis() / 1000L + quoteTtlSeconds();

        ObjectNode payload = OM.createObjectNode();
        payload.put("v", 3);
        payload.put("compId", compId != null ? compId.trim() : "");
        payload.put("pg", pgU);
        payload.put("cur", cur);
        payload.put("setCur", settlement);
        payload.put("period", period);
        payload.put("tpu", settlementPerUnit.stripTrailingZeros().toPlainString());
        payload.put("m", margin.stripTrailingZeros().toPlainString());
        payload.put("exp", exp);
        payload.put("fx", fxMode);

        String json = payload.toString();
        byte[] mac = hmac(json);
        String token = base64Url(json) + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac);

        QuoteResult qr = new QuoteResult(cur, settlement, period, settlementPerUnit, margin, exp, token, rateDesc);
        return Optional.of(qr);
    }

    /**
     * 견적 토큰 검증 후 실결제 통화 금액.
     *
     * @param operationalPgCd 견적 발급 시점 PG(레거시 토큰은 pg 비어 있을 수 있음)
     */
    public FxComputedSettlement computeSettlementFromQuote(
            String compId, String displayCurrency, BigDecimal displayAmount, String token, String operationalPgCd) {
        if (token == null || token.isBlank() || displayAmount == null || displayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_FX_QUOTE");
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("INVALID_FX_QUOTE");
        }
        String json;
        byte[] macBytes;
        try {
            json = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            macBytes = Base64.getUrlDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("INVALID_FX_QUOTE");
        }
        byte[] expect = hmac(json);
        if (!constantTimeEqualsBytes(macBytes, expect)) {
            throw new IllegalArgumentException("INVALID_FX_QUOTE_SIG");
        }
        JsonNode p;
        try {
            p = OM.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("INVALID_FX_QUOTE");
        }
        long exp = p.path("exp").asLong(0);
        if (exp < System.currentTimeMillis() / 1000L) {
            throw new IllegalArgumentException("FX_QUOTE_EXPIRED");
        }
        String qComp = p.path("compId").asText("").trim();
        if (compId == null || !compId.trim().equals(qComp)) {
            throw new IllegalArgumentException("FX_QUOTE_COMP_MISMATCH");
        }
        String qCur = p.path("cur").asText("").trim().toUpperCase(Locale.ROOT);
        String cur = normalizeDisplayCurrency(displayCurrency);
        if (cur == null || !cur.equals(qCur)) {
            throw new IllegalArgumentException("FX_QUOTE_CUR_MISMATCH");
        }
        int ver = p.path("v").asInt(1);
        String qpgNorm = p.path("pg").asText("").trim().toUpperCase(Locale.ROOT);
        if (ver >= 2) {
            String op = normalizePgCd(operationalPgCd);
            if (qpgNorm.isEmpty() != op.isEmpty()) {
                throw new IllegalArgumentException("FX_QUOTE_PG_MISMATCH");
            }
            if (!qpgNorm.isEmpty() && !qpgNorm.equals(op)) {
                throw new IllegalArgumentException("FX_QUOTE_PG_MISMATCH");
            }
        }
        String tokenSettlement;
        if (ver >= 3) {
            tokenSettlement = p.path("setCur").asText("").trim().toUpperCase(Locale.ROOT);
            if (tokenSettlement.isEmpty() || !SETTLEMENT_CURRENCIES.contains(tokenSettlement)) {
                throw new IllegalArgumentException("INVALID_FX_QUOTE");
            }
            String expectedSet = settlementCurrencyForPg(operationalPgCd);
            if (!tokenSettlement.equals(expectedSet)) {
                throw new IllegalArgumentException("FX_QUOTE_SETTLEMENT_MISMATCH");
            }
        } else {
            tokenSettlement = "THB";
        }
        BigDecimal tpu = new BigDecimal(p.path("tpu").asText("0"));
        BigDecimal m = new BigDecimal(p.path("m").asText("0"));
        if (tpu.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_FX_QUOTE");
        }
        BigDecimal marginCheck = (ver >= 2 && !qpgNorm.isEmpty())
                ? marginFor(normalizePgCd(operationalPgCd), cur)
                : marginFor(cur);
        if (m.subtract(marginCheck).abs().compareTo(new BigDecimal("0.0000001")) > 0) {
            throw new IllegalArgumentException("FX_QUOTE_MARGIN_MISMATCH");
        }
        BigDecimal factor = BigDecimal.ONE.add(m);
        int scale = settlementScale(tokenSettlement);
        BigDecimal amt = displayAmount.multiply(tpu).multiply(factor).setScale(scale, RoundingMode.HALF_UP);
        return new FxComputedSettlement(amt, tokenSettlement);
    }

    /** @deprecated 호환용 — {@link #computeSettlementFromQuote} 사용 */
    @Deprecated
    public BigDecimal computeThbFromQuote(String compId, String displayCurrency, BigDecimal displayAmount, String token,
                                          String operationalPgCd) {
        return computeSettlementFromQuote(compId, displayCurrency, displayAmount, token, operationalPgCd).amount();
    }

    public record FxComputedSettlement(BigDecimal amount, String settlementCurrency) {}

    public record QuoteResult(
            String displayCurrency,
            String settlementCurrency,
            String botPeriod,
            BigDecimal settlementPerUnit,
            BigDecimal marginRate,
            long expEpochSec,
            String quoteToken,
            String rateDescription
    ) {}

    private String defaultSettlementForBuild(JsonNode pgNode) {
        if (pgNode == null || pgNode.isMissingNode() || pgNode.size() == 0) {
            return "THB";
        }
        String u = pgNode.path("settlementCurrency").asText("THB").trim().toUpperCase(Locale.ROOT);
        if (u.isEmpty()) {
            return "THB";
        }
        return SETTLEMENT_CURRENCIES.contains(u) ? u : "THB";
    }

    private Optional<BigDecimal> computeAutoSettlementPerUnit(String display, String settlement) {
        if (display.equals(settlement)) {
            return Optional.of(BigDecimal.ONE);
        }
        if (!botThailandExchangeRateService.isConfigured()) {
            return Optional.empty();
        }
        BotThailandExchangeRateService.BotDailyRates rates = botThailandExchangeRateService.fetchLatestThbPerUnitRates().orElse(null);
        if (rates == null) {
            return Optional.empty();
        }
        BigDecimal thbPerDisp = thbPerOneUnit(display, rates);
        BigDecimal thbPerSet = thbPerOneUnit(settlement, rates);
        if (thbPerDisp == null
                || thbPerSet == null
                || thbPerDisp.compareTo(BigDecimal.ZERO) <= 0
                || thbPerSet.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        return Optional.of(thbPerDisp.divide(thbPerSet, 12, RoundingMode.HALF_UP));
    }

    private static BigDecimal thbPerOneUnit(String currency, BotThailandExchangeRateService.BotDailyRates rates) {
        if ("THB".equals(currency)) {
            return BigDecimal.ONE;
        }
        if ("JPY".equals(currency)) {
            return rates.thbPerJpy();
        }
        if ("USD".equals(currency)) {
            return rates.thbPerUsd();
        }
        if ("KRW".equals(currency)) {
            return rates.thbPerKrw();
        }
        return null;
    }

    private static BigDecimal resolveManualSettlementPerUnit(JsonNode pgNode, String settlement) {
        BigDecimal ms = parsePositiveDecimal(pgNode.path("manualSettlementPerUnit").asText(""));
        if (ms != null) {
            return ms;
        }
        if ("THB".equals(settlement)) {
            return parsePositiveDecimal(pgNode.path("manualThbPerUnit").asText(""));
        }
        return null;
    }

    private boolean isPgFxRunnable(String pgU, JsonNode pgNode) {
        if (pgNode == null || pgNode.isMissingNode()) {
            return botThailandExchangeRateService.isConfigured();
        }
        String fx = pgNode.path("fxMode").asText("AUTO").trim().toUpperCase(Locale.ROOT);
        if ("MANUAL".equals(fx)) {
            String set = defaultSettlementForBuild(pgNode);
            return resolveManualSettlementPerUnit(pgNode, set) != null;
        }
        return botThailandExchangeRateService.isConfigured();
    }

    private JsonNode pgSettingsNode(String pgCdUpper) {
        if (pgCdUpper == null || pgCdUpper.isEmpty()) {
            return OM.missingNode();
        }
        JsonNode root = readHqFxJson().path("pgSettings");
        if (!root.isObject()) {
            return OM.missingNode();
        }
        if (root.has(pgCdUpper)) {
            return root.get(pgCdUpper);
        }
        for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            if (pgCdUpper.equalsIgnoreCase(name)) {
                return root.get(name);
            }
        }
        return OM.missingNode();
    }

    private static String normalizePgCd(String pgCd) {
        if (pgCd == null) {
            return "";
        }
        return pgCd.trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal parsePositiveDecimal(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(s.trim());
            return v.compareTo(BigDecimal.ZERO) > 0 ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode readHqFxJson() {
        Optional<HqApiConfig> opt = hqApiConfigRepository.findAll().stream().findFirst();
        String raw = opt.map(HqApiConfig::getUrlPayDisplayFxJson).orElse(null);
        if (raw == null || raw.isBlank()) {
            return OM.createObjectNode();
        }
        try {
            return OM.readTree(raw);
        } catch (Exception e) {
            return OM.createObjectNode();
        }
    }

    private static String normalizeDisplayCurrency(String c) {
        if (c == null) {
            return null;
        }
        String u = c.trim().toUpperCase(Locale.ROOT);
        return DISPLAY_CURRENCIES.contains(u) ? u : null;
    }

    private static int settlementScale(String settlementCurrency) {
        if ("JPY".equals(settlementCurrency) || "KRW".equals(settlementCurrency)) {
            return 0;
        }
        return 2;
    }

    private String resolveHmacSecret() {
        if (!configuredHmacSecret.isEmpty()) {
            return configuredHmacSecret;
        }
        return hqApiConfigRepository.findAll().stream()
                .findFirst()
                .map(HqApiConfig::getChillpayMd5Key)
                .filter(s -> s != null && !s.isBlank())
                .orElse("pg-url-pay-fx-fallback-secret");
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(resolveHmacSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC init failed", e);
        }
    }

    private static String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEqualsBytes(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int d = 0;
        for (int i = 0; i < a.length; i++) {
            d |= a[i] ^ b[i];
        }
        return d == 0;
    }
}
