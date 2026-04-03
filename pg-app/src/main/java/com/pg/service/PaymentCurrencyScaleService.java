package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * 본사 결제통화로직설정: URL 결제 폼 입력 금액을 PG API(ChillPay) 전송 금액으로 변환.
 */
@Service
public class PaymentCurrencyScaleService {

    private static final ObjectMapper OM = new ObjectMapper();

    public static final String MODE_SAME = "SAME";
    public static final String MODE_MULTIPLY_100 = "MULTIPLY_100";
    public static final String MODE_DIVIDE_100 = "DIVIDE_100";

    private final HqApiConfigRepository hqApiConfigRepository;

    public PaymentCurrencyScaleService(HqApiConfigRepository hqApiConfigRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    public String getRulesJson() {
        return hqApiConfigRepository.findById(1L)
                .map(HqApiConfig::getPayCurrencyScaleRulesJson)
                .orElse(null);
    }

    /**
     * 표시(입력) 금액을 PG 전송 금액으로 변환.
     */
    public BigDecimal toPgAmount(BigDecimal displayAmount, String pgCd, String currencyCodeOrAlpha) {
        if (displayAmount == null) {
            return null;
        }
        String mode = resolveMode(getRulesJson(), pgCd, currencyCodeOrAlpha);
        return applyMode(displayAmount, mode);
    }

    public String resolveModeForUi(String pgCd, String currencyCodeOrAlpha) {
        return resolveMode(getRulesJson(), pgCd, currencyCodeOrAlpha);
    }

    static String resolveMode(String rulesJson, String pgCd, String currencyCodeOrAlpha) {
        String pc = pgCd != null ? pgCd.trim().toUpperCase(Locale.ROOT) : "";
        String cu = normalizeCurrency(currencyCodeOrAlpha);
        if (pc.isEmpty() || cu.isEmpty()) {
            return MODE_SAME;
        }
        if (rulesJson == null || rulesJson.isBlank()) {
            return MODE_SAME;
        }
        try {
            JsonNode root = OM.readTree(rulesJson);
            JsonNode rules = root.path("rules");
            if (!rules.isArray()) {
                return MODE_SAME;
            }
            for (JsonNode r : rules) {
                if (r == null || !r.isObject()) {
                    continue;
                }
                String rPg = textUpper(r, "pgCd");
                String rCur = normalizeCurrency(textUpper(r, "currency"));
                if (pc.equals(rPg) && cu.equals(rCur)) {
                    return normalizeMode(textUpper(r, "mode"));
                }
            }
        } catch (Exception ignored) {
            return MODE_SAME;
        }
        return MODE_SAME;
    }

    static BigDecimal applyMode(BigDecimal displayAmount, String mode) {
        String m = normalizeMode(mode);
        if (MODE_MULTIPLY_100.equals(m)) {
            return displayAmount.multiply(new BigDecimal("100"));
        }
        if (MODE_DIVIDE_100.equals(m)) {
            return displayAmount.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
        }
        return displayAmount;
    }

    static String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return MODE_SAME;
        }
        String u = mode.trim().toUpperCase(Locale.ROOT);
        if (MODE_MULTIPLY_100.equals(u) || MODE_DIVIDE_100.equals(u) || MODE_SAME.equals(u)) {
            return u;
        }
        return MODE_SAME;
    }

    static String textUpper(JsonNode obj, String field) {
        if (obj == null || field == null) {
            return "";
        }
        JsonNode n = obj.get(field);
        if (n == null || !n.isTextual()) {
            return "";
        }
        return n.asText("").trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 숫자 ISO 통화코드(392 등) 또는 알파(JPY)를 비교용 알파로 정규화.
     */
    static String normalizeCurrency(String currencyCodeOrAlpha) {
        if (currencyCodeOrAlpha == null) {
            return "";
        }
        String s = currencyCodeOrAlpha.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            return "";
        }
        if (s.matches("\\d+")) {
            return switch (s) {
                case "392" -> "JPY";
                case "410" -> "KRW";
                case "840" -> "USD";
                case "978" -> "EUR";
                case "156" -> "CNY";
                case "344" -> "HKD";
                case "702" -> "SGD";
                case "764" -> "THB";
                case "458" -> "MYR";
                case "608" -> "PHP";
                case "360" -> "IDR";
                case "704" -> "VND";
                case "356" -> "INR";
                case "826" -> "GBP";
                case "036" -> "AUD";
                case "124" -> "CAD";
                case "554" -> "NZD";
                case "756" -> "CHF";
                case "901" -> "TWD";
                default -> s;
            };
        }
        return s;
    }
}
