package com.pg.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 챗봇 상품 등록 티어(건수)·본사 설정 월 이용료 매핑(통화별).
 */
public final class ChatbotProductPricingUtil {

    public static final List<Integer> ALLOWED_SLOTS = List.of(10, 20, 50, 80, 100, 150, 200);
    /**
     * 플랜의 「판매 활성(고객 노출)」 상한(건) 대비, 미판매 보관용으로 추가로 등록해 둘 수 있는 건수.
     * 예: 플랜 10 → 판매 활성 최대 10, 총 등록(보관) 최대 12.
     */
    public static final int CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS = 2;
    /** 청구 플랜에 사용하는 ISO 통화(표시·입력 순서) */
    public static final List<String> BILLING_CURRENCIES = List.of("JPY", "KRW", "USD", "CNY", "THB");
    /** {@link com.pg.service.HqChatbotAiSettingsService} 의 config_json 키 */
    public static final String CONFIG_KEY_SLOTS_PRICING = "chatbot_product_slots_pricing";

    private ChatbotProductPricingUtil() {
    }

    public static boolean isAllowedSlot(Integer slot) {
        return slot != null && ALLOWED_SLOTS.contains(slot);
    }

    public static boolean isSupportedBillingCurrency(String iso) {
        if (iso == null || iso.isBlank()) {
            return false;
        }
        String u = iso.trim().toUpperCase(Locale.ROOT);
        return BILLING_CURRENCIES.contains(u);
    }

    /**
     * 본사 {@code chatbot_product_slots_pricing} 에서 슬롯별 금액이 0보다 큰 첫 통화({@link #BILLING_CURRENCIES} 순).
     * 가맹 기준통화가 청구 통화 목록에 없을 때 플랜 표시용 폴백.
     */
    public static String firstSupportedCurrencyWithAnyNonZeroSlotFee(Map<String, Object> hqConfig) {
        Object raw = hqConfig != null ? hqConfig.get(CONFIG_KEY_SLOTS_PRICING) : null;
        Map<String, Map<String, BigDecimal>> bySlot = normalizeSlotsPricing(raw);
        for (String ccy : BILLING_CURRENCIES) {
            for (Integer s : ALLOWED_SLOTS) {
                Map<String, BigDecimal> row = bySlot.get(String.valueOf(s));
                if (row == null) {
                    continue;
                }
                BigDecimal f = row.get(ccy);
                if (f != null && f.signum() > 0) {
                    return ccy;
                }
            }
        }
        return null;
    }

    /** {@code tb_merchant_profile.base_currency} CSV 등에서 첫 번째 토큰만 사용 */
    public static String firstIsoCurrencyToken(String baseCurrencyCsv) {
        if (baseCurrencyCsv == null || baseCurrencyCsv.isBlank()) {
            return null;
        }
        String[] parts = baseCurrencyCsv.split(",\\s*");
        if (parts.length == 0) {
            return null;
        }
        String p = parts[0].trim().toUpperCase(Locale.ROOT);
        if (p.isEmpty()) {
            return null;
        }
        return p.length() >= 3 ? p.substring(0, 3) : p;
    }

    /**
     * HQ 설정 또는 요청 바디 정규화.
     * 각 슬롯은 {@link #BILLING_CURRENCIES} 키만 갖는 맵.
     * 레거시(슬롯→단일 숫자)는 금액을 <strong>KRW</strong>에만 두고 나머지 통화는 0.
     */
    public static Map<String, Map<String, BigDecimal>> normalizeSlotsPricing(Object raw) {
        Map<String, Map<String, BigDecimal>> out = new LinkedHashMap<>();
        for (Integer s : ALLOWED_SLOTS) {
            LinkedHashMap<String, BigDecimal> cur = new LinkedHashMap<>();
            for (String ccy : BILLING_CURRENCIES) {
                cur.put(ccy, BigDecimal.ZERO);
            }
            out.put(String.valueOf(s), cur);
        }
        if (!(raw instanceof Map<?, ?> rm)) {
            return out;
        }
        for (Integer s : ALLOWED_SLOTS) {
            String sk = String.valueOf(s);
            Object v = rm.get(sk);
            if (v == null) {
                v = rm.get(s);
            }
            Map<String, BigDecimal> tgt = out.get(sk);
            if (v instanceof Map<?, ?> vm) {
                for (String ccy : BILLING_CURRENCIES) {
                    BigDecimal amt = parseMoney(vm.get(ccy));
                    if (amt != null && amt.signum() >= 0) {
                        tgt.put(ccy, amt);
                    }
                }
            } else {
                BigDecimal leg = parseMoney(v);
                if (leg != null && leg.signum() > 0) {
                    tgt.put("KRW", leg);
                }
            }
        }
        return out;
    }

    /** config 맵 또는 null 에서 해당 슬롯·통화 월 이용료 */
    public static BigDecimal monthlyFeeForSlotAndCurrency(Map<String, Object> hqConfig, int slot, String billingCurrencyIso) {
        if (!ALLOWED_SLOTS.contains(slot)) {
            return BigDecimal.ZERO;
        }
        String ccyNorm = billingCurrencyIso != null ? billingCurrencyIso.trim().toUpperCase(Locale.ROOT) : "";
        if (!isSupportedBillingCurrency(ccyNorm)) {
            return BigDecimal.ZERO;
        }
        Object raw = hqConfig != null ? hqConfig.get(CONFIG_KEY_SLOTS_PRICING) : null;
        Map<String, Map<String, BigDecimal>> bySlot = normalizeSlotsPricing(raw);
        Map<String, BigDecimal> row = bySlot.get(String.valueOf(slot));
        if (row == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal f = row.get(ccyNorm);
        return f != null ? f : BigDecimal.ZERO;
    }

    /**
     * {@code hqConfig} 에 넣어 JSON 저장하기 좋은 {@code Map<String, Object>} (중첩 맵 값은 Number/BigDecimal).
     */
    public static Map<String, Object> normalizeSlotsPricingForJsonPersistence(Object raw) {
        Map<String, Map<String, BigDecimal>> norm = normalizeSlotsPricing(raw);
        Map<String, Object> top = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, BigDecimal>> e : norm.entrySet()) {
            Map<String, Object> inner = new LinkedHashMap<>();
            for (Map.Entry<String, BigDecimal> c : e.getValue().entrySet()) {
                inner.put(c.getKey(), c.getValue());
            }
            top.put(e.getKey(), inner);
        }
        return top;
    }

    private static BigDecimal parseMoney(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(s.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    public static String memoKeyForBillingMonth(java.time.YearMonth ym) {
        return "CHATBOT_BILL:" + ym.toString();
    }

    /** 미수금 reason_code 컬럼(40자) 이내 — 월 정기 청구(전월) */
    public static final String RECEIVABLE_REASON_CHATBOT_MONTHLY = "CHATBOT_MONTHLY_SERVICE";
    /** 플랜 상향 시 잔여일 기준 차액(미수금). 월말까지 과금 기간은 달력 기준으로 동일하게 유지 */
    public static final String RECEIVABLE_REASON_CHATBOT_UPGRADE = "CHATBOT_PLAN_UPGRADE";

    /** 업그레이드 미수금 메모(중복 방지). 예: CHATBOT_UPGRADE:2026-05-12:10→100 */
    public static String memoKeyForPlanUpgrade(java.time.LocalDate changedDate, int fromSlot, int toSlot) {
        return "CHATBOT_UPGRADE:" + changedDate + ":" + fromSlot + "→" + toSlot;
    }
}
