package com.pg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import com.pg.util.FeeListRoundingPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * URL 결제 「표시통화 → 실결제 통화」 본사 설정 — <strong>모든 결제대행사(PG) 공통</strong>.
 * <ul>
 *   <li>전역 {@code enabled}, 갱신 주기, 견적 TTL, {@code botRateAsOf}({@code PREVIOUS_DAY_CLOSE}|{@code LATEST_BOT_PERIOD}), {@code marginByCurrency}</li>
 *   <li>PG별 {@code pgSettings}: {@code amountMode} STANDARD|DISPLAY|BLIND, 표시·실결제 통화({@code settlementCurrency}=THB·USD·JPY 등),
 *       {@code displayCurrencyMode}({@value #DISPLAY_CURRENCY_MODE_FIXED}|{@value #DISPLAY_CURRENCY_MODE_MULTI}),
 *       멀티 시 선택지는 {@code displayCurrencies} 배열 또는 전역 순서, FX 자동(BOT)·수동, 마진.
 *       수동(THB 정산) 시 표시통화별 환산은 {@code manualThbPerByDisplayCurrency} JSON 객체(예: 키 KRW·JPY)로 지정 가능</li>
 *   <li>레거시: {@code pgSettings} 없을 때 가맹점 바인딩 {@code DISPLAY_FX_THB} + 전역 마진, 실결제 THB</li>
 * </ul>
 * <p>{@link #MODE_DISPLAY_FX_THB} 는 레거시 코드명입니다. 의미는 「표시통화 DP → PG 실결제 통화(THB에 한정되지 않음)」입니다.
 * 청구: {@code 실결제금액 = 표시금액 × (실결제/1표시단위) × (1+마진)} (실결제 통화별 소수는 ChillPay DirectCredit·일반 URL 결제와 동일: JPY/KRW 정수, 그 외 소수 둘째 HALF_UP).
 */
@Service
public class UrlPayDisplayFxService {

    /**
     * 표시통화 DP 모드 코드(레거시 이름). 실결제 통화는 {@link #settlementCurrencyForPg} (THB·USD 등).
     */
    public static final String MODE_DISPLAY_FX_THB = "DISPLAY_FX_THB";
    public static final String AMOUNT_MODE_STANDARD = "STANDARD";
    public static final String AMOUNT_MODE_DISPLAY = "DISPLAY";
    /** DISPLAY와 동일 FX·실결제; 공개 결제창에서 표시 통화·예상 청구 행만 숨김. */
    public static final String AMOUNT_MODE_BLIND = "BLIND";

    /** DISPLAY·BLIND — 표시통화 FX 결제 경로(실결제 로직 동일). */
    public static boolean isDisplayFxStyleAmountMode(String amountMode) {
        if (amountMode == null) {
            return false;
        }
        String m = amountMode.trim().toUpperCase(Locale.ROOT);
        return AMOUNT_MODE_DISPLAY.equals(m) || AMOUNT_MODE_BLIND.equals(m);
    }

    /** prepare·견적·결제창에서 쓰는 DP 금액모드 코드인지. */
    public static boolean isDisplayFxPricingMode(String pricingMode) {
        return MODE_DISPLAY_FX_THB.equalsIgnoreCase(pricingMode != null ? pricingMode.trim() : "");
    }

    public boolean isAllowedDisplayCurrency(String currency) {
        String c = currency != null ? currency.trim().toUpperCase(Locale.ROOT) : "";
        return DISPLAY_CURRENCIES.contains(c);
    }

    public boolean isAllowedSettlementCurrency(String currency) {
        String c = currency != null ? currency.trim().toUpperCase(Locale.ROOT) : "";
        return SETTLEMENT_CURRENCIES.contains(c);
    }
    /** 결제 페이지에서 표시 통화를 고객이 고를 수 있음(본사 전역 또는 PG별 {@code displayCurrencies}). */
    public static final String DISPLAY_CURRENCY_MODE_MULTI = "MULTI";
    /** 결제 페이지에는 본사가 지정한 표시 통화만 노출(셀렉트 없음). */
    public static final String DISPLAY_CURRENCY_MODE_FIXED = "FIXED";
    private static final String CHECKOUT = "CHECKOUT_CURRENCY";

    private static final Set<String> DISPLAY_CURRENCIES =
            Set.of("JPY", "USD", "KRW", "THB", "SGD", "HKD", "CNY");
    private static final Set<String> SETTLEMENT_CURRENCIES =
            Set.of("THB", "USD", "JPY", "KRW", "SGD", "HKD", "CNY");

    /** 결제 페이지·관리 UI 표시 통화 셀렉트 순서. */
    private static final List<String> DISPLAY_CURRENCY_UI_ORDER =
            List.of("THB", "JPY", "USD", "KRW", "SGD", "HKD", "CNY");

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

    /** DISPLAY FX 견적·결제 폼에서 선택 가능한 표시 통화 코드(고정 순서). */
    public List<String> allowedDisplayCurrencies() {
        return DISPLAY_CURRENCY_UI_ORDER;
    }

    /**
     * PG별 {@code pgSettings.displayCurrencyMode}: {@value #DISPLAY_CURRENCY_MODE_MULTI} 가 아니면 고정.
     * 미설정·레거시 JSON은 {@value #DISPLAY_CURRENCY_MODE_FIXED}.
     */
    public String displayCurrencyModeForPg(String operationalPgCd) {
        JsonNode pgNode = pgSettingsNode(normalizePgCd(operationalPgCd));
        if (pgNode == null || pgNode.isMissingNode() || pgNode.size() == 0) {
            return DISPLAY_CURRENCY_MODE_FIXED;
        }
        String raw = pgNode.path("displayCurrencyMode").asText(DISPLAY_CURRENCY_MODE_FIXED).trim();
        if (DISPLAY_CURRENCY_MODE_MULTI.equalsIgnoreCase(raw)) {
            return DISPLAY_CURRENCY_MODE_MULTI;
        }
        return DISPLAY_CURRENCY_MODE_FIXED;
    }

    public boolean isDisplayCurrencyMultiForPg(String operationalPgCd) {
        return DISPLAY_CURRENCY_MODE_MULTI.equalsIgnoreCase(displayCurrencyModeForPg(operationalPgCd));
    }

    /**
     * 공개 결제 페이지 셀렉트용 표시 통화 목록.
     * {@value #DISPLAY_CURRENCY_MODE_FIXED} 이면 {@link #defaultDisplayCurrencyForPg} 한 개만.
     * {@value #DISPLAY_CURRENCY_MODE_MULTI} 이면 {@code displayCurrencies} 배열(비면 전역 순서) 또는 전역 순서.
     */
    public List<String> allowedDisplayCurrenciesForCheckout(String operationalPgCd) {
        if (isDisplayCurrencyMultiForPg(operationalPgCd)) {
            JsonNode pgNode = pgSettingsNode(normalizePgCd(operationalPgCd));
            if (pgNode != null && pgNode.has("displayCurrencies") && pgNode.get("displayCurrencies").isArray()) {
                List<String> out = new ArrayList<>();
                for (JsonNode n : pgNode.get("displayCurrencies")) {
                    String c = normalizeDisplayCurrency(n.asText(""));
                    if (c != null) {
                        out.add(c);
                    }
                }
                if (!out.isEmpty()) {
                    return out;
                }
            }
            return allowedDisplayCurrencies();
        }
        return List.of(defaultDisplayCurrencyForPg(operationalPgCd));
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
            if (!isDisplayFxStyleAmountMode(pgNode.path("amountMode").asText(""))) {
                return CHECKOUT;
            }
            String setStr = pgNode.path("settlementCurrency").asText("THB").trim().toUpperCase(Locale.ROOT);
            if (setStr.isEmpty()) {
                setStr = "THB";
            }
            if (!SETTLEMENT_CURRENCIES.contains(setStr)) {
                return CHECKOUT;
            }
            /*
             * DISPLAY·통화·FX 모드는 본사 설정대로 반영한다. BOT 미설정·수동 환산 미입력 등은 견적/결제 단계에서 막히며,
             * 여기서 CHECKOUT으로 떨어지면 결제 페이지가 총판 기준화폐(THB)로만 열려 표시통화(JPY)와 어긋난다.
             */
            return MODE_DISPLAY_FX_THB;
        }
        if (MODE_DISPLAY_FX_THB.equalsIgnoreCase(merchantBindingUrlPayMode != null ? merchantBindingUrlPayMode.trim() : "")) {
            return MODE_DISPLAY_FX_THB;
        }
        return CHECKOUT;
    }

    /**
     * 공개 결제창(pay.html)에서 BLIND 모드인지.
     * {@code true}이면 고객에게 실결제 통화로의 환산(청구예상) 노출을 줄이기 위한 플래그로 쓰이며,
     * <strong>결제 방식 멀티</strong>일 때는 프런트에서 표시 통화 행은 유지하고 청구예상 행만 숨깁니다.
     */
    public boolean isUrlPayFxUiBlind(String operationalPgCd) {
        JsonNode pgNode = pgSettingsNode(normalizePgCd(operationalPgCd));
        if (pgNode == null || pgNode.isMissingNode() || !pgNode.isObject() || pgNode.size() == 0) {
            return false;
        }
        return AMOUNT_MODE_BLIND.equalsIgnoreCase(pgNode.path("amountMode").asText(""));
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
        boolean manualFx = false;
        if (pgNode != null && !pgNode.isMissingNode() && pgNode.size() > 0) {
            String fm = pgNode.path("fxMode").asText("AUTO").trim().toUpperCase(Locale.ROOT);
            manualFx = "MANUAL".equals(fm);
        }
        /* AUTO(BOT): 본사 표시통화별 마진(7종)만. 행의 marginRate·marginByDisplayCurrency는 수동 FX일 때만 적용. */
        if (manualFx) {
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
            if (!isDisplayFxStyleAmountMode(pgNode.path("amountMode").asText(""))) {
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
            settlementPerUnit = resolveManualSettlementPerUnit(pgNode, settlement, cur);
            if (settlementPerUnit == null) {
                return Optional.empty();
            }
            period = "MANUAL";
            rateDesc = "MANUAL fixed " + settlement + " per 1 " + cur;
        } else {
            BotRateAsOfMode botMode = resolveBotRateMode();
            BotThailandExchangeRateService.BotDailyRates rates = null;
            if (!cur.equals(settlement)) {
                rates = botThailandExchangeRateService.fetchThbPerUnitRates(botMode).orElse(null);
            }
            Optional<BigDecimal> auto = computeAutoSettlementPerUnit(cur, settlement, rates);
            if (auto.isEmpty()) {
                return Optional.empty();
            }
            settlementPerUnit = auto.get();
            period = rates != null && rates.period() != null ? rates.period() : "";
            rateDesc = cur.equals(settlement)
                    ? ("1:1, " + settlement + " per 1 " + cur)
                    : ("BOT DAILY_AVG (bridge THB, " + botMode + "), " + settlement + " per 1 " + cur);
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
        BigDecimal raw = displayAmount.multiply(tpu).multiply(factor);
        BigDecimal amt = applyChargeAmountRound(tokenSettlement, raw);
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

    /**
     * 본사 URL결제설정 화면용 <strong>미리보기</strong>.
     * HQ JSON·견적 토큰을 바꾸지 않으며, BOT 환율만 조회해 통화별 실결제 예상액을 계산한다.
     *
     * @param settlementCurrency 실결제 통화
     * @param displayAmount      각 표시통화에 동일하게 적용할 표시 금액
     * @param botRateAsOfRaw     {@code PREVIOUS_DAY_CLOSE}|{@code LATEST_BOT_PERIOD} (비면 본사 저장값)
     * @param marginByCurrency   표시통화→마진율(소수, 예: 0.02 = 2%). 없으면 0
     */
    public MarginSimResult simulateMargins(String settlementCurrency, BigDecimal displayAmount,
                                           String botRateAsOfRaw,
                                           Map<String, BigDecimal> marginByCurrency) {
        if (displayAmount == null || displayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("DISPLAY_AMOUNT_REQUIRED");
        }
        String settle = settlementCurrency != null
                ? settlementCurrency.trim().toUpperCase(Locale.ROOT) : "";
        if (!SETTLEMENT_CURRENCIES.contains(settle)) {
            throw new IllegalArgumentException("SETTLEMENT_CURRENCY_INVALID");
        }
        BotRateAsOfMode botMode = parseBotRateMode(botRateAsOfRaw);
        boolean needBot = DISPLAY_CURRENCY_UI_ORDER.stream().anyMatch(c -> !c.equals(settle));
        BotThailandExchangeRateService.BotDailyRates rates = null;
        if (needBot) {
            rates = botThailandExchangeRateService.fetchThbPerUnitRates(botMode).orElse(null);
            if (rates == null) {
                throw new IllegalArgumentException("BOT_RATE_UNAVAILABLE");
            }
        }
        String period = rates != null && rates.period() != null ? rates.period() : "";
        List<MarginSimRow> rows = new ArrayList<>();
        for (String cur : DISPLAY_CURRENCY_UI_ORDER) {
            BigDecimal margin = BigDecimal.ZERO;
            if (marginByCurrency != null) {
                BigDecimal m = marginByCurrency.get(cur);
                if (m == null) {
                    m = marginByCurrency.get(cur.toLowerCase(Locale.ROOT));
                }
                if (m != null && m.compareTo(BigDecimal.ZERO) >= 0) {
                    margin = m;
                }
            }
            Optional<BigDecimal> tpuOpt = computeAutoSettlementPerUnit(cur, settle, rates);
            if (tpuOpt.isEmpty()) {
                rows.add(new MarginSimRow(cur, displayAmount, margin, null, null, settle, false,
                        "RATE_UNAVAILABLE"));
                continue;
            }
            BigDecimal tpu = tpuOpt.get();
            BigDecimal factor = BigDecimal.ONE.add(margin);
            BigDecimal raw = displayAmount.multiply(tpu).multiply(factor);
            BigDecimal amt = applyChargeAmountRound(settle, raw);
            rows.add(new MarginSimRow(cur, displayAmount, margin, tpu, amt, settle, true, null));
        }
        return new MarginSimResult(
                botMode.name(),
                period,
                settle,
                displayAmount,
                rows,
                "SIMULATION_ONLY");
    }

    public record MarginSimRow(
            String displayCurrency,
            BigDecimal displayAmount,
            BigDecimal marginRate,
            BigDecimal settlementPerUnit,
            BigDecimal settlementAmount,
            String settlementCurrency,
            boolean ok,
            String message
    ) {}

    public record MarginSimResult(
            String botRateAsOf,
            String botPeriod,
            String settlementCurrency,
            BigDecimal displayAmount,
            List<MarginSimRow> rows,
            String note
    ) {}

    /**
     * DP/BL 실결제(청구) 금액 소수 처리 — 본사 URL결제 {@code chargeAmountRoundByCurrency}.
     * {@code enabled=true}: 통화별 커스텀 소수·모드.
     * {@code enabled=false}/미설정: {@code chargeAmountRoundPolicyDefaults}(정책 기본값) 적용.
     * 정책 기본값도 없으면 팩토리 폴백(JPY·KRW 0 HALF_UP, 그 외 2 HALF_UP).
     */
    public BigDecimal applyChargeAmountRound(String settlementCurrency, BigDecimal rawAmount) {
        if (rawAmount == null) {
            return null;
        }
        ChargeRoundSpec spec = resolveChargeAmountRoundSpec(settlementCurrency);
        return rawAmount.setScale(spec.decimalPlaces(), spec.roundMode());
    }

    /**
     * 견적·결제창용 — 실효 정책(비활성이면 정책 기본값, enabled=false).
     */
    public Map<String, Object> chargeAmountRoundPolicyForCurrency(String settlementCurrency) {
        ChargeRoundSpec spec = resolveChargeAmountRoundSpec(settlementCurrency);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", spec.enabled());
        out.put("decimalPlaces", spec.decimalPlaces());
        out.put("roundMode", roundingModeLabel(spec.roundMode()));
        out.put("legacyDefault", !spec.enabled());
        out.put("fromPolicyDefault", !spec.enabled());
        return out;
    }

    /** UI·저장용 — 통화별 정책 기본값(비활성 시 적용). 미저장 시 팩토리 폴백. */
    public List<Map<String, Object>> chargeAmountRoundPolicyDefaultRowsForUi() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String cur : DISPLAY_CURRENCY_UI_ORDER) {
            ChargeRoundSpec d = resolvePolicyDefaultSpec(cur);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("currency", cur);
            row.put("decimalPlaces", d.decimalPlaces());
            row.put("roundMode", roundingModeLabel(d.roundMode()));
            rows.add(row);
        }
        return rows;
    }

    private record ChargeRoundSpec(boolean enabled, int decimalPlaces, RoundingMode roundMode) {}

    private ChargeRoundSpec resolveChargeAmountRoundSpec(String settlementCurrency) {
        String cur = settlementCurrency != null
                ? settlementCurrency.trim().toUpperCase(Locale.ROOT) : "";
        ChargeRoundSpec policyDefault = resolvePolicyDefaultSpec(cur);
        JsonNode node = readHqFxJson().path("chargeAmountRoundByCurrency").path(cur);
        if (node == null || !node.isObject() || node.isEmpty()) {
            return new ChargeRoundSpec(false, policyDefault.decimalPlaces(), policyDefault.roundMode());
        }
        boolean enabled = node.path("enabled").asBoolean(false);
        if (!enabled) {
            String enRaw = node.path("enabled").asText("").trim().toUpperCase(Locale.ROOT);
            enabled = "Y".equals(enRaw) || "TRUE".equals(enRaw) || "1".equals(enRaw);
        }
        if (!enabled) {
            return new ChargeRoundSpec(false, policyDefault.decimalPlaces(), policyDefault.roundMode());
        }
        int scale = Math.min(8, Math.max(0, node.path("decimalPlaces").asInt(policyDefault.decimalPlaces())));
        RoundingMode mode = FeeListRoundingPolicy.parseRoundMode(
                node.path("roundMode").asText(roundingModeLabel(policyDefault.roundMode())));
        return new ChargeRoundSpec(true, scale, mode);
    }

    /**
     * HQ {@code chargeAmountRoundPolicyDefaults}.{CUR} — 없으면 팩토리(JPY/KRW=0, else=2, HALF_UP).
     */
    private ChargeRoundSpec resolvePolicyDefaultSpec(String currencyUpper) {
        String cur = currencyUpper != null ? currencyUpper.trim().toUpperCase(Locale.ROOT) : "";
        int factoryScale = factoryChargeScale(cur);
        JsonNode node = readHqFxJson().path("chargeAmountRoundPolicyDefaults").path(cur);
        if (node == null || !node.isObject() || node.isEmpty()) {
            return new ChargeRoundSpec(false, factoryScale, RoundingMode.HALF_UP);
        }
        int scale = Math.min(8, Math.max(0, node.path("decimalPlaces").asInt(factoryScale)));
        RoundingMode mode = FeeListRoundingPolicy.parseRoundMode(node.path("roundMode").asText("HALF_UP"));
        return new ChargeRoundSpec(false, scale, mode);
    }

    private static String roundingModeLabel(RoundingMode m) {
        if (m == RoundingMode.HALF_UP) {
            return "HALF_UP";
        }
        if (m == RoundingMode.DOWN) {
            return "DOWN";
        }
        return "CEILING";
    }

    /** 코드 팩토리 폴백(본사 정책 기본값이 비어 있을 때만). */
    private static int factoryChargeScale(String currencyUpper) {
        if ("JPY".equals(currencyUpper) || "KRW".equals(currencyUpper)) {
            return 0;
        }
        return 2;
    }

    /** URL결제설정 UI용 — 통화별 커스텀(활성) 행. */
    public List<Map<String, Object>> chargeAmountRoundRowsForUi() {
        JsonNode root = readHqFxJson().path("chargeAmountRoundByCurrency");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String cur : DISPLAY_CURRENCY_UI_ORDER) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("currency", cur);
            ChargeRoundSpec policy = resolvePolicyDefaultSpec(cur);
            JsonNode n = root.path(cur);
            boolean enabled = false;
            int dp = policy.decimalPlaces();
            String rm = roundingModeLabel(policy.roundMode());
            if (n != null && n.isObject() && !n.isEmpty()) {
                enabled = n.path("enabled").asBoolean(false);
                if (!enabled) {
                    String enRaw = n.path("enabled").asText("").trim().toUpperCase(Locale.ROOT);
                    enabled = "Y".equals(enRaw) || "TRUE".equals(enRaw) || "1".equals(enRaw);
                }
                dp = Math.min(8, Math.max(0, n.path("decimalPlaces").asInt(policy.decimalPlaces())));
                rm = n.path("roundMode").asText(roundingModeLabel(policy.roundMode())).trim().toUpperCase(Locale.ROOT);
                if (!Set.of("CEILING", "HALF_UP", "DOWN").contains(rm)) {
                    rm = roundingModeLabel(policy.roundMode());
                }
            }
            row.put("enabled", enabled);
            row.put("decimalPlaces", dp);
            row.put("roundMode", rm);
            row.put("policyDecimalPlaces", policy.decimalPlaces());
            row.put("policyRoundMode", roundingModeLabel(policy.roundMode()));
            rows.add(row);
        }
        return rows;
    }

    private BotRateAsOfMode parseBotRateMode(String botRateAsOfRaw) {
        if (botRateAsOfRaw != null && !botRateAsOfRaw.isBlank()) {
            String raw = botRateAsOfRaw.trim().toUpperCase(Locale.ROOT);
            if ("LATEST_BOT_PERIOD".equals(raw) || "LATEST".equals(raw) || "TODAY".equals(raw)) {
                return BotRateAsOfMode.LATEST_BOT_PERIOD;
            }
            if ("PREVIOUS_DAY_CLOSE".equals(raw) || "PREVIOUS".equals(raw) || "YESTERDAY".equals(raw)) {
                return BotRateAsOfMode.PREVIOUS_DAY_CLOSE;
            }
        }
        return resolveBotRateMode();
    }

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

    private Optional<BigDecimal> computeAutoSettlementPerUnit(
            String display, String settlement, BotThailandExchangeRateService.BotDailyRates rates) {
        if (display.equals(settlement)) {
            return Optional.of(BigDecimal.ONE);
        }
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
        if ("SGD".equals(currency)) {
            return rates.thbPerSgd();
        }
        if ("HKD".equals(currency)) {
            return rates.thbPerHkd();
        }
        if ("CNY".equals(currency)) {
            return rates.thbPerCny();
        }
        return null;
    }

    /** 본사 JSON {@code botRateAsOf}; 미설정·알 수 없는 값은 전일 종가. */
    private BotRateAsOfMode resolveBotRateMode() {
        String raw = readHqFxJson().path("botRateAsOf").asText("PREVIOUS_DAY_CLOSE").trim().toUpperCase(Locale.ROOT);
        if ("LATEST_BOT_PERIOD".equals(raw) || "LATEST".equals(raw) || "TODAY".equals(raw)) {
            return BotRateAsOfMode.LATEST_BOT_PERIOD;
        }
        return BotRateAsOfMode.PREVIOUS_DAY_CLOSE;
    }

    /**
     * 수동 FX: {@code manualSettlementPerUnit} 우선, 실결제 THB일 때는
     * 표시통화별 {@code manualThbPerByDisplayCurrency}{@code .JPY}/{@code .KRW}/… 가 있으면 우선,
     * 없으면 레거시 {@code manualThbPerUnit}(THB/1표시단위, 단일 통화 안내용).
     */
    private static BigDecimal resolveManualSettlementPerUnit(JsonNode pgNode, String settlement, String displayCurrency) {
        if (pgNode == null || pgNode.isMissingNode()) {
            return null;
        }
        String disp = displayCurrency != null ? displayCurrency.trim().toUpperCase(Locale.ROOT) : "";
        if ("THB".equals(settlement) && disp.length() == 3) {
            JsonNode byDisp = pgNode.path("manualThbPerByDisplayCurrency");
            if (byDisp != null && byDisp.isObject() && byDisp.has(disp)) {
                BigDecimal v = parsePositiveDecimal(byDisp.get(disp).asText(""));
                if (v != null) {
                    return v;
                }
            }
        }
        BigDecimal ms = parsePositiveDecimal(pgNode.path("manualSettlementPerUnit").asText(""));
        if (ms != null) {
            return ms;
        }
        if ("THB".equals(settlement)) {
            return parsePositiveDecimal(pgNode.path("manualThbPerUnit").asText(""));
        }
        return null;
    }

    /**
     * 본사 {@code pgSettings} 행 조회. 키는 가맹 {@code tb_merchant_pg_binding.pg_cd} 와 동일한 문자열이 이상적이나,
     * 바인딩이 {@code CHILLPAY} 등 짧은 코드이고 본사 JSON 키만 {@code CHILLPAY DP JP THB} 인 경우 등 접두 매칭으로 DISPLAY·FX 설정을 찾는다.
     */
    private JsonNode pgSettingsNode(String pgCdUpper) {
        String pgN = normalizePgCd(pgCdUpper);
        if (pgN.isEmpty()) {
            return OM.missingNode();
        }
        JsonNode root = readHqFxJson().path("pgSettings");
        if (!root.isObject()) {
            return OM.missingNode();
        }
        for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            if (name == null) {
                continue;
            }
            String kNorm = normalizePgCd(name);
            if (pgN.equals(kNorm)) {
                return root.get(name);
            }
        }
        /* 가맹 pg_cd 가 본사 키의 접두(예: CHILLPAY → CHILLPAY DP JP THB) — 가장 긴 키 우선 */
        JsonNode bestChild = null;
        int bestChildLen = -1;
        for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            if (name == null) {
                continue;
            }
            String kNorm = normalizePgCd(name);
            if (kNorm.isEmpty() || kNorm.length() <= pgN.length()) {
                continue;
            }
            if (kNorm.startsWith(pgN) && isPgCdTokenBoundary(kNorm, pgN.length())) {
                if (kNorm.length() > bestChildLen) {
                    bestChildLen = kNorm.length();
                    bestChild = root.get(name);
                }
            }
        }
        if (bestChild != null && bestChild.isObject()) {
            return bestChild;
        }
        /* 본사 키가 짧고 바인딩만 김(예: CHILLPAY DP JP THB 바인딩 + pgSettings.CHILLPAY) — 가장 긴 접두 키 */
        JsonNode bestParent = null;
        int bestParentLen = -1;
        for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            if (name == null) {
                continue;
            }
            String kNorm = normalizePgCd(name);
            if (kNorm.isEmpty() || kNorm.length() >= pgN.length()) {
                continue;
            }
            if (pgN.startsWith(kNorm) && isPgCdTokenBoundary(pgN, kNorm.length())) {
                if (kNorm.length() > bestParentLen) {
                    bestParentLen = kNorm.length();
                    bestParent = root.get(name);
                }
            }
        }
        return bestParent != null && bestParent.isObject() ? bestParent : OM.missingNode();
    }

    /** {@code full} 의 {@code prefixLen} 위치가 PG 코드 토큰 경계(공백·구분자 또는 문자열 끝). */
    private static boolean isPgCdTokenBoundary(String full, int prefixLen) {
        if (full == null || prefixLen < 0 || prefixLen > full.length()) {
            return false;
        }
        if (prefixLen == full.length()) {
            return true;
        }
        char c = full.charAt(prefixLen);
        return c == ' ' || c == '_' || c == '-' || c == '.';
    }

    private static String normalizePgCd(String pgCd) {
        if (pgCd == null) {
            return "";
        }
        return pgCd.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
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
