package com.pg.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * JPAY 최소 주문 금액(약 USD 5.00 상당) — prepare·sale 사전 검증.
 */
public final class JpayCheckoutMinAmountUtil {

    public static final String CODE_BELOW_MIN_AMOUNT = "BELOW_MIN_AMOUNT";
    public static final String MESSAGE_KEY = "ICOPAY_JPAY_MIN_AMOUNT";

    private static final BigDecimal MIN_USD = new BigDecimal("5.00");

    private JpayCheckoutMinAmountUtil() {
    }

    /**
     * @return empty if OK, else localized messages map key ENG + error detail in optional message
     */
    public static Optional<Map<String, Object>> validate(BigDecimal amount, String currencyRaw) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        String cur = currencyRaw != null ? currencyRaw.trim().toUpperCase(Locale.ROOT) : "USD";
        if (cur.isBlank()) {
            cur = "USD";
        }
        BigDecimal min = minForCurrency(cur);
        if (min == null) {
            return Optional.empty();
        }
        if (amount.compareTo(min) >= 0) {
            return Optional.empty();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", CODE_BELOW_MIN_AMOUNT);
        out.put("messageKey", MESSAGE_KEY);
        out.put("messages", i18nMessages(cur, min));
        out.put("message", i18nMessages(cur, min).get("ENG"));
        out.put("minAmount", min.stripTrailingZeros().toPlainString());
        out.put("currency", cur);
        return Optional.of(out);
    }

    static BigDecimal minForCurrency(String cur) {
        return switch (cur) {
            case "USD" -> MIN_USD;
            case "JPY" -> new BigDecimal("750");
            case "KRW" -> new BigDecimal("7000");
            case "EUR" -> new BigDecimal("5");
            case "GBP" -> new BigDecimal("4");
            case "THB" -> new BigDecimal("180");
            case "SGD" -> new BigDecimal("7");
            case "HKD" -> new BigDecimal("40");
            case "CNY" -> new BigDecimal("36");
            case "AUD" -> new BigDecimal("8");
            case "NZD" -> new BigDecimal("8");
            case "MYR" -> new BigDecimal("23");
            case "CHF" -> new BigDecimal("5");
            default -> null;
        };
    }

    private static Map<String, String> i18nMessages(String cur, BigDecimal min) {
        String minPlain = min.stripTrailingZeros().toPlainString();
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", "최소 주문 금액은 " + minPlain + " " + cur + " (약 USD 5.00) 입니다.");
        m.put("ENG", "Minimum order amount is " + minPlain + " " + cur + " (about USD 5.00).");
        m.put("JPN", "最小注文金額は " + minPlain + " " + cur + "（約 USD 5.00）です。");
        m.put("CHN", "最低订单金额为 " + minPlain + " " + cur + "（约 USD 5.00）。");
        m.put("THA", "ยอดสั่งซื้อขั้นต่ำคือ " + minPlain + " " + cur + " (ประมาณ USD 5.00)");
        return m;
    }
}
